# MODULE KNOWLEDGE BASE

**Scope:** `src/main/java/com/example/springbatchinvestment`

## OVERVIEW
Core batch module: job orchestration, step wiring, readers/writers, repositories, and integration clients.

## STRUCTURE
```text
springbatchinvestment/
|- BachConfig.java                      # Main Job + Step orchestration
|- SpringBatchInvestmentApplication.java
|- client/                              # FSS + Gemini clients and DTO contracts
|- domain/                              # Business enums/models + JPA entities
|- reader/                              # External and DB item readers
|- writer/                              # Persistence + history/queue writers
|- repository/                          # JPA repository interfaces
|- tasklet/                             # One-off status update step
`- listener/                            # Job lifecycle logging hooks
```

## WHERE TO LOOK
| Task | Location | Why |
|------|----------|-----|
| Change job flow/order | `BachConfig.java` | Owns `JobBuilder` chain and step sequence |
| Change application lifecycle | `SpringBatchInvestmentApplication.java` | CLI bootstrap and `System.exit` behavior |
| Add new FSS API behavior | `client/FssClient.java` | Timeouts, retries, error mapping |
| Modify product persistence logic | `writer/FinancialProductItemWriter.java` | Save/update + stale marker orchestration |
| Modify history/event path | `writer/FinancialProductHistoryPgmqItemWriter.java` | History insert + event enqueue |

## CONVENTIONS
- Step names and job names are constants in `BachConfig`; keep names explicit and stable.
- Most orchestration code uses constructor injection (`@RequiredArgsConstructor`) and `this.` field style.
- Reader/writer chunk size currently fixed at `10`; if changed, keep step behavior consistent.
- Transaction manager in steps/tasklet is `ResourcelessTransactionManager` (intentional current behavior).

## HOTSPOTS
- `client/FssClient.java` (largest class): reactive HTTP setup + retry policy, impacts all ingest readers.
- `writer/FinancialProductItemWriter.java`: business update logic + embedding interactions.
- `reader/AbstractFinancialItemReader.java`: pagination/state traversal behavior for FSS pulls.

## ANTI-PATTERNS (MODULE)
- Do not rename `BachConfig` in isolated edits; this typo is established and referenced.
- Do not introduce web assumptions here; app runs as non-web batch process.
- Do not bypass shared client/repository abstractions by adding ad-hoc HTTP or SQL in steps.
