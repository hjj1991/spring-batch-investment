# Batch Data Architecture (PostgreSQL + TimescaleDB + PGMQ)

## 목적
- 이 문서는 **배치 적재 로직 전용** 아키텍처를 정의한다.
- API 서버 기능(좋아요/구독/알림 발송)은 별도 프로젝트로 분리되어 있으며, 본 문서는 그 기능을 위한 **데이터 산출물**만 다룬다.

## 범위
- 포함: FSS 수집, 금융기관/금융상품 적재, 임베딩(Gemini), 변경 감지, 이력 적재.
- 제외: 사용자 인증/권한, API 응답 스펙, 알림 전송 채널(푸시/메일/SMS) 구현.

## 현재 배치 흐름 (코드 기준)
- Job: `FINANCIAL_COMPANY_SYNC_JOB` (`src/main/java/com/example/springbatchinvestment/BachConfig.java`)
- Step 순서:
  1) 금융기관 적재
  2) 예금 상품 적재
  3) 적금 상품 적재
  4) 이번 실행에서 관측되지 않은 상품만 `DELETED` 처리
  5) RDB -> History 적재 + PGMQ 이벤트 enqueue
- 임베딩: `FinancialProductItemWriter`에서 Gemini 임베딩 생성 후 `embeddingVector` 업데이트.

## 목표 아키텍처 (ES 제거, PostgreSQL 단일화)
- 저장소 단일화: MySQL/ES 분리 대신 PostgreSQL 하나로 통합.
- FSS 응답 원형을 잃지 않도록 핵심 테이블에 `source_payload(JSONB)` 저장.
- 임베딩 저장: `embedding_vector`(TEXT)에 임베딩 배열 직렬화 저장.
- 검색: PostgreSQL FTS(`tsvector` + GIN) + `pg_trgm` 유사도 검색.
- 문서형 데이터: `JSONB`로 이벤트/원본 payload 저장.
- 시계열/이력: `TimescaleDB` hypertable로 금리/상태 변경 이력 저장.
- 메시지 큐: `pgmq` 큐를 사용해 API/알림 서버가 비동기 소비.
- 배치 산출물:
  - 현재 상태(Current Snapshot)
  - 변경 이력(History)
  - 알림 후보 이벤트(Queue Message)

## 적재 모델
- `financial_company`: 금융기관 마스터(현재 상태)
- `financial_company_area`: 금융기관 지역/존재정보(`CompanyArea`) 마스터
- `financial_product`: 금융상품 마스터(현재 상태 + embedding, `last_seen_at` 포함)
- `financial_product_option`: 옵션 현재 상태(옵션명/기간/금리 정규화)
- `financial_product_history` (hypertable): 상품 주요 필드 변경 스냅샷
- `financial_product_rate_history` (hypertable): 금리 변화 이력
- `pgmq` queue `product_change_events`: 신규상품/금리상승/금리하락/상태변경 이벤트 저장

## 배치 적재 단계 (제안)
1. 회사/상품 원천 데이터 수집(FSS)
2. 회사 upsert
3. 상품 upsert
   - 콘텐츠 해시 변경 시에만 Gemini 임베딩 재생성
   - upsert 시 `last_seen_at`을 현재 배치 실행 시각으로 갱신
4. 옵션 upsert
   - `deposit_period_months`를 숫자형으로 저장
   - `interest_rate_type_name`, `reserve_type_name` 보존
5. stale 정리
   - `last_seen_at`이 현재 실행 시작시각보다 이전인 상품만 `DELETED`
6. 변경 감지
   - 신규 상품
   - 상태 변경
   - 옵션 금리 변경(기준금리/최고금리)
7. 이력/이벤트 적재
   - history hypertable append
   - pgmq send

## 변경 감지 규칙 (배치 관점)
- 상품 신규: `(financial_company_code, financial_product_code)` 미존재 -> `NEW_PRODUCT`
- 금리 상승: 동일 상품/옵션의 `max_rate` 또는 `base_rate` 증가 -> `RATE_UP`
- 금리 하락: 동일 상품/옵션의 `max_rate` 또는 `base_rate` 감소 -> `RATE_DOWN`
- 상태 변경: `ACTIVE <-> DELETED` 전환 -> `STATUS_CHANGED`

## 인덱스/성능 가이드
- 검색:
  - `financial_product_name`, `company_name`에는 `pg_trgm` GIN 인덱스
  - 상품 설명 결합 텍스트에는 `to_tsvector(...)` GIN 인덱스
- JSONB:
  - `financial_product_history.payload` / 큐 payload는 JSONB 경로 조회 패턴 기준으로 GIN 인덱스 설계
- 배치 키:
  - 회사 유니크: `financial_company_code`
  - 상품 유니크: `(financial_company_id, financial_product_code, financial_product_type)`
  - 옵션 유니크: `(financial_product_id, interest_rate_type, reserve_type, deposit_period_months)`
- 이력 테이블:
  - 시간축 + 상품키 복합 인덱스
  - Timescale compression + retention 정책 적용

## 운영 원칙
- 배치는 데이터 정합성과 재현성 우선(동일 입력 -> 동일 결과).
- 이벤트는 `pgmq`에 넣고, API/알림 서버가 `read` 후 처리 완료 시 `archive` 한다.
- API 서버 요구사항 확장 시에도 배치의 책임은 "정확한 적재 + 변경 이벤트 생산"으로 제한.

## 기능 매핑
| 기능 | 전문 도구 | Postgres 단일화 대안 |
|------|-----------|-----------------------|
| 벡터 검색 | Pinecone, Weaviate | 임베딩 벡터 문자열 저장(향후 pgvector 전환 가능) |
| 문서 데이터 | MongoDB | `JSONB` |
| 메시지 큐 | Kafka, RabbitMQ | `pgmq` |
| 검색 엔진 | Elasticsearch | FTS(`to_tsvector`) + `pg_trgm` |
| 시계열 | InfluxDB | `TimescaleDB` |
