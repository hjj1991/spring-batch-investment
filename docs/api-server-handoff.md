# API Server Handoff (from Batch Project)

## 목적
API 서버 프로젝트가 배치 산출물(현재 상태 + 이력 + 이벤트)을 바로 소비할 수 있도록 핵심 계약을 정리한다.

## 배치가 생산하는 데이터
- 현재 상태
  - `financial_company`
  - `financial_company_area`
  - `financial_product`
  - `financial_product_option`
- 이력
  - `financial_product_history`
  - `financial_product_rate_history`
- 이벤트 큐
  - PGMQ queue: `product_change_events`

## 주요 키/조인 기준
- 회사 자연키: `financial_company.financial_company_code`
- 상품 자연키: `(financial_company_id, financial_product_code, financial_product_type)`
- 옵션 자연키: `(financial_product_id, interest_rate_type, reserve_type, deposit_period_months)`

## DB 외래키(FK) 정책
- 현재 상태(조회 원본) 테이블은 FK를 유지한다.
  - `financial_company_area.financial_company_id -> financial_company.financial_company_id`
  - `financial_product.financial_company_id -> financial_company.financial_company_id`
  - `financial_product_option.financial_product_id -> financial_product.financial_product_id`
- 이력(hypertable) 테이블(`financial_product_history`, `financial_product_rate_history`)은 대량 append/retention/compression 운영을 위해 FK를 두지 않는다.
- 운영 원칙: 무결성이 중요한 현재 상태 테이블은 DB FK로 보호하고, 이력 적재 경로는 쓰기/보관 효율을 우선한다.

## 상태/삭제 정책
- 배치는 매 실행 시작 시각 기준으로 `last_seen_at`을 갱신한다.
- 이번 실행에서 관측되지 않은 상품은 `status=DELETED`로 변경된다.
- 하드 삭제가 아니라 소프트 상태 전환이므로 API 서버는 `status` 필터를 반드시 적용해야 한다.

## 임베딩 정책
- 저장 컬럼: `financial_product.embedding_vector` (`vector(768)`)
- `api.gemini.embedding-enabled=false`일 때는 임베딩 API를 호출하지 않고 `NULL` 저장.
- API 서버는 `embedding_vector IS NULL` 케이스를 정상 케이스로 처리해야 한다.

## 큐 이벤트 계약 (PGMQ payload)
배치 writer(`FinancialProductHistoryPgmqItemWriter`)가 아래 JSON payload를 enqueue 한다.

```json
{
  "event_type": "NEW_PRODUCT | STATUS_CHANGED | CONTENT_CHANGED",
  "occurred_at": "2026-02-16T00:00:00Z",
  "financial_product_id": 123,
  "financial_company_id": 45,
  "financial_product_code": "PRDT-001",
  "status": "ACTIVE | DELETED"
}
```

### 이벤트 발행 조건(중요)
- 모든 배치 실행에서 모든 상품 이벤트를 발행하지 않는다.
- 직전 `financial_product_history` 스냅샷과 비교하여 아래 중 하나일 때만 발행한다.
  - `NEW_PRODUCT`: 이전 스냅샷이 없음
  - `STATUS_CHANGED`: `status` 값 변경
  - `CONTENT_CHANGED`: `product_content_hash` 또는 payload(JSON) 변경
- 비교 기준 payload는 옵션 목록을 정렬 후 직렬화한 값이다(결정적 순서).

### 이벤트 시각 의미
- `occurred_at`은 writer chunk 처리 시점(`OffsetDateTime.now()`)이다.
- 같은 chunk에서 처리된 여러 상품은 동일 `occurred_at` 값을 가질 수 있다.
- 따라서 API 서버는 `occurred_at` 단독으로 유니크 이벤트 키를 가정하면 안 된다.

## API 서버 권장 소비 전략
- Queue 소비: `product_change_events`를 poll/read 후 처리 성공 시 archive/delete
- 조회 기본 조건:
  - 상품 목록: `status='ACTIVE'`
  - 삭제 이력/변경 추적: history 테이블 조회
- 캐시 무효화/알림 트리거:
  - `event_type` 기반 분기 (`NEW_PRODUCT`, `STATUS_CHANGED`, `CONTENT_CHANGED`)

### Queue 소비 운영 계약(권장)
- 최소 1회(at-least-once) 전달을 전제로 멱등 소비를 구현한다.
- 처리 성공 후에만 archive/delete 한다.
- 처리 실패 시 archive/delete 하지 않고 재처리 가능하도록 둔다.
- 중복 수신 대비: 동일 상품/상태 조합 재수신 시에도 부작용 없이 처리한다.

### 큐 유실/지연 대비 재동기화
- 정기적으로 `financial_product_history`를 기준으로 보정 동기화를 수행한다.
- 권장 정렬/페이지 기준:
  - 기본 정렬: `observed_at DESC`
  - 보조 키: `financial_product_id`
- 큐 기반 실시간 반영 + history 기반 주기 보정의 하이브리드 방식을 권장한다.

## 상태/신선도 운영 규약(구현 기준)
- `last_seen_at`은 상품을 읽은 시각이 아니라 배치 Job 시작 시각(`runStartedAt`)으로 갱신된다.
- 상태 업데이트 스텝에서 `last_seen_at IS NULL OR last_seen_at < runStartedAt` 상품을 `DELETED`로 변경한다.
- 현재 구현은 `SAVINGS`, `INSTALLMENT_SAVINGS` 타입에 대해 위 규칙을 적용한다.
- 이후 실행에서 다시 관측되면 writer에서 `ACTIVE`로 재활성화된다.

## Null/스키마 해석 주의
- `embedding_vector`는 `NULL` 가능(`api.gemini.embedding-enabled=false` 또는 미생성 시).
- 옵션 payload의 `reserve_type`은 `NULL` 가능.
- API 서버는 위 `NULL`을 비정상 데이터로 간주하지 말고 정상 케이스로 처리해야 한다.

## 배치 실행 계약
- Job name: `FINANCIAL_COMPANY_SYNC_JOB`
- 로컬 프로파일: `spring.profiles.active=local`
- 배치는 API 서버 기능(좋아요/알림 발송)을 직접 수행하지 않으며, 데이터 산출만 담당한다.
