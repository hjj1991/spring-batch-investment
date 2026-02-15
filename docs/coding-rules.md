# Coding Rules

## 1) General
- Functional changes must include or update tests.
- Follow Spotless formatting and existing import order.
- Prefer small, isolated changes; avoid mixing refactor with behavior changes.
- Keep batch code deterministic: same input should produce same output.
- Avoid magic numbers in business logic; extract meaningful named constants.
- Exception: protocol/spec-defined literals may remain inline only with clear context.

### 1.1 Constant Naming and Placement
- 클래스 내부에서만 쓰는 값은 해당 클래스에 `private static final`로 선언.
- 여러 클래스에서 공유되는 값은 도메인별 constants 클래스로 분리(예: batch step names, queue names).
- 이름은 의미 중심으로 작성(예: `DEFAULT_CHUNK_SIZE`, `PRODUCT_CHANGE_QUEUE_NAME`).
- 단위가 있는 값은 이름에 단위를 포함(예: `RETRY_DELAY_SECONDS`, `MAX_LOOKBACK_DAYS`).
- SQL/쿼리 문자열의 반복 리터럴도 상수로 추출한다.
- 테스트 코드도 동일 원칙 적용(의도 파악이 어려운 리터럴 금지).

## 2) Domain/Schema
- Treat FSS natural keys as first-class keys in schema and queries.
- Keep enum fields as `EnumType.STRING`.
- Use explicit null handling when mapping FSS fields.
- Do not add columns that are not written/read by current batch flow without a concrete use case.

## 3) Batch Flow
- Do not introduce temporary "all deleted" windows visible to API consumers.
- Use mark-and-sweep with `last_seen_at` style markers for stale cleanup.
- Emit queue events only on meaningful changes (new/status/content/rate changes).
- Skip unchanged history snapshots to avoid redundant writes.

## 4) Test Strategy

### 4.1 Unit tests (fast)
- 대상: pure mapping, hash generation, change-detection logic, utility methods.
- 도구: JUnit5 + Mockito.
- 외부 I/O(DB/HTTP) 없이 실행.

### 4.2 Repository tests (DB behavior)
- 기본 권장: **PostgreSQL Testcontainers** (`@DataJpaTest` + Testcontainers).
- 이유: 이 프로젝트는 PostgreSQL/Timescale/JSONB/실제 SQL 동작 차이가 중요하며 H2로는 오탐 가능.
- 포함해야 하는 검증:
  - 자연키/유니크 제약 위반 동작
  - nullable + unique 동작
  - 실제 쿼리(`@Query`) 필터 조건과 업데이트 결과

### 4.3 Embedded DB 사용 기준
- 허용: DB 특정 기능을 사용하지 않는 단순 CRUD smoke 테스트.
- 비권장: PostgreSQL 문법, 타입, 인덱스, JSONB, batch update semantics 검증.

### 4.4 Batch integration tests
- 대상: Step/Job wiring, reader-processor-writer 연결, stale 처리/이력 적재/queue enqueue 흐름.
- 권장: 테스트 프로필 + Testcontainers PostgreSQL 조합.

## 5) Test Naming and Scope
- 테스트명은 한국어/영어 혼용 가능하되, "조건-행위-결과" 구조 유지.
- 한 테스트는 한 규칙만 검증.
- flaky 원인이 되는 시간 의존 로직은 고정 시각 주입 또는 비교 허용 오차 명시.

## 6) PR/Review Checklist
- [ ] 기능 변경에 대응하는 테스트가 추가/수정되었는가
- [ ] 코드 작성/수정 시 연관 테스트를 실제로 실행했는가
- [ ] 실행한 연관 테스트가 모두 통과했는가
- [ ] Repository 쿼리는 PostgreSQL 기준 테스트로 검증되었는가
- [ ] 배치 실행 중 API 공백을 유발하는 상태 전환이 없는가
- [ ] 동일 데이터 이력이 중복 적재되지 않는가

## 7) Mandatory Test Execution Rule
- 코드를 작성/수정하면, 변경 범위와 직접 연관된 테스트를 반드시 실행한다.
- 연관 테스트가 하나라도 실패하면 작업 완료로 간주하지 않는다.
- 최소 실행 기준:
  - 단일 클래스/메서드 수정: 해당 단위 테스트
  - Repository 쿼리/매핑 수정: Repository 테스트(PostgreSQL Testcontainers)
  - Batch step/job 흐름 수정: step/job 통합 테스트 + 연관 단위 테스트
