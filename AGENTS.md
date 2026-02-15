# PROJECT KNOWLEDGE BASE

**Generated:** 2026-02-15 16:02:58 KST
**Commit:** 0b0672e
**Branch:** main

## OVERVIEW
Spring Boot batch application that syncs FSS financial products into PostgreSQL and emits change events via PGMQ. Single executable process (non-web) orchestrates one multi-step `Job`.

## STRUCTURE
```text
spring-batch-investment/
|- src/main/java/com/example/springbatchinvestment/  # Batch orchestration and domain modules
|  |- client/      # External API clients + DTOs/errors
|  |- domain/      # Enums/models/entities
|  |- reader/      # Batch item readers
|  |- writer/      # Batch item writers
|  |- repository/  # JPA repositories
|  |- service/     # Embedding-related services
|  |- tasklet/     # Tasklet-based step(s)
|  `- listener/    # Job listener(s)
|- src/main/resources/application.yml
|- src/test/java/com/example/springbatchinvestment/client/
|- .github/workflows/ci.yml
|- build.gradle
`- docker-compose.yml
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| App entrypoint | `src/main/java/com/example/springbatchinvestment/SpringBatchInvestmentApplication.java` | Non-web process; exits after run |
| Job/step wiring | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | Defines `Job` + 5 `Step` beans |
| External FSS calls | `src/main/java/com/example/springbatchinvestment/client/FssClient.java` | WebClient, retry/backoff, error mapping |
| History + queue write | `src/main/java/com/example/springbatchinvestment/writer/FinancialProductHistoryPgmqItemWriter.java` | Timescale history insert + `pgmq.send` |
| Runtime profiles | `src/main/resources/application.yml` | `local`/`test`/`prod`, `job.name` selector |
| CI/release flow | `.github/workflows/ci.yml` | Release-branch gated build/push/deploy-tag update |

## CODE MAP
| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| `main(String[] args)` | method | `src/main/java/com/example/springbatchinvestment/SpringBatchInvestmentApplication.java` | n/a | Process bootstrap |
| `financialSyncJob()` | `Job` bean | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | n/a | Main batch pipeline |
| `financialSyncStep()` | `Step` bean | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | n/a | Company ingest |
| `financialProductStatusUpdateStep()` | `Step` bean | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | n/a | Status reset tasklet |
| `financialProductSavingsSyncStep()` | `Step` bean | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | n/a | Savings product ingest |
| `financialProductInstallmentSavingsSyncStep()` | `Step` bean | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | n/a | Installment ingest |
| `financialProductHistoryPgmqSyncStep()` | `Step` bean | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | n/a | RDB -> history + PGMQ sync |

## CONVENTIONS
- Formatting enforced via Spotless in `build.gradle` (`googleJavaFormat`, custom import order, trim/newline).
- Java toolchain pinned to 25 (`kotlin { jvmToolchain(25) }`), Gradle wrapper pinned to 9.1.0.
- Field access style consistently uses `this.` for member references.
- Batch app runs as CLI (`spring.main.web-application-type: none`) and selects job by `job.name`.
- CI only triggers on `releases-**` branches.
- Code change policy: every functional code change must include or update unit tests.
- Authoritative coding/testing rules: `docs/coding-rules.md`.

## LLM STARTUP RULES
- Any new LLM session must read this file (`AGENTS.md`) first.
- Then read `docs/coding-rules.md` before editing code.
- If rules conflict, `docs/coding-rules.md` is authoritative for coding/testing policy.
- Do not mark work complete unless related tests were executed and passed.

## TESTING GUIDELINE
- Repository tests: use PostgreSQL Testcontainers as default (`@DataJpaTest` + container).
- Embedded DB is only for DB-agnostic smoke tests; avoid it for PostgreSQL-specific behavior.
- Batch flow changes require step/job-level integration tests plus relevant unit tests.
- Any code change must run related tests locally and they must pass before completion.

## ANTI-PATTERNS (THIS PROJECT)
- No explicit `DO NOT`/`NEVER` policy markers found in repository text.
- Avoid assuming chunk/tasklet rollback semantics here: steps currently use `ResourcelessTransactionManager` in `BachConfig`.
- Avoid introducing `@EnableBatchProcessing` unless intentionally replacing Boot batch auto-configuration.

## UNIQUE STYLES
- Main class exits JVM using `System.exit(SpringApplication.exit(...))`.
- Batch config class name is `BachConfig` (typo retained in codebase); do not "fix" name casually without full refactor.
- FSS client uses explicit Reactor Netty timeouts and retry-on-unavailable policy.

## COMMANDS
```bash
./gradlew clean test
./gradlew build
./gradlew spotlessCheck
./gradlew spotlessApply
./gradlew bootRun --args='--job.name=FINANCIAL_COMPANY_SYNC_JOB --spring.profiles.active=local'
```

## NOTES
- Required env keys at runtime: `FSS_AUTH_KEY`, `GEMINI_AUTH_KEY` (and prod datasource/PGMQ secrets).
- Docker image expects `build/libs/spring-batch-investment.jar` (`Dockerfile`).
- Release pipeline also updates tag in external helm repo (`hjj1991/helm-charts`).
