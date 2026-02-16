# Local Batch Bootstrap

## 1) Docker 시작
```bash
docker compose up -d --build
```

기본 접속 정보(로컬):
- host: `localhost`
- port: `5432`
- db: `api_server`
- user/password: `postgres` / `postgres`

## 2) 최초 1회 초기화 주의
- `docker/postgres/init/01-extensions.sql`이 컨테이너 최초 초기화 시 자동 실행된다.
- 자동 생성 확장: `timescaledb`, `pgmq`, `pg_trgm`, `vector`
- 이미 생성된 볼륨(`postgresdata`)을 재사용 중이면 init SQL이 다시 실행되지 않는다.

확장 관련 에러가 났다면 아래처럼 볼륨까지 재생성한다.

```bash
docker compose down -v
docker compose up -d --build
```

## 3) 스키마 적용
확장이 준비된 환경이면 아래를 그대로 실행한다.

```bash
psql "postgresql://postgres:postgres@localhost:5432/api_server" -f docs/postgresql-timescale-schema.sql
```

## 4) 배치 실행
```bash
./gradlew bootRun --args='--job.name=FINANCIAL_COMPANY_SYNC_JOB --spring.profiles.active=local'
```

필수 환경 변수:
- `FSS_AUTH_KEY`
- `GEMINI_AUTH_KEY` (단, `api.gemini.embedding-enabled=false`면 임베딩 호출은 생략)

## 5) 확장 미사용 임시 실행(권장 X)
- 확장 없는 순수 PostgreSQL에서 로컬 확인만 급히 해야 할 때는:
  - Timescale/PGMQ 관련 DDL을 제외하고 핵심 테이블만 먼저 생성
  - 단, 이 경우 `FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP`는 동작하지 않거나 실패할 수 있다.

운영/개발 표준은 확장 포함 DB에서 전체 SQL을 적용하는 방식이다.
