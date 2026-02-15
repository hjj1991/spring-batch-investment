# PROJECT KNOWLEDGE BASE

**Generated:** 2026-02-15 16:02:58 KST
**Commit:** 0b0672e
**Branch:** main

## OVERVIEW
Spring Boot batch application that syncs FSS financial products into MySQL and Elasticsearch. Single executable process (non-web) orchestrates one multi-step `Job`.

## STRUCTURE
```text
spring-batch-investment/
|- src/main/java/com/example/springbatchinvestment/  # Batch orchestration and domain modules
|  |- client/      # External API clients + DTOs/errors
|  |- domain/      # Enums/models/entities/documents
|  |- reader/      # Batch item readers
|  |- writer/      # Batch item writers
|  |- processor/   # Batch processor(s)
|  |- repository/  # JPA + Elasticsearch repositories
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
| ES client config | `src/main/java/com/example/springbatchinvestment/ElasticsearchConfig.java` | RestClient + template wiring |
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
| `financialProductEsSyncStep()` | `Step` bean | `src/main/java/com/example/springbatchinvestment/BachConfig.java` | n/a | RDB -> ES sync |

## CONVENTIONS
- Formatting enforced via Spotless in `build.gradle` (`googleJavaFormat`, custom import order, trim/newline).
- Java/Kotlin toolchain pinned to 21 (`kotlin { jvmToolchain(21) }`), Gradle wrapper pinned to 8.5.
- Field access style consistently uses `this.` for member references.
- Batch app runs as CLI (`spring.main.web-application-type: none`) and selects job by `job.name`.
- CI only triggers on `releases-**` branches.
- Code change policy: every functional code change must include or update unit tests.

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
- Required env keys at runtime: `FSS_AUTH_KEY`, `GEMINI_AUTH_KEY` (and prod datasource/ES secrets).
- Docker image expects `build/libs/spring-batch-investment.jar` (`Dockerfile`).
- Release pipeline also updates tag in external helm repo (`hjj1991/helm-charts`).
