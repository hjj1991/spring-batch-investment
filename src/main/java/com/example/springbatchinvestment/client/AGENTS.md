# CLIENT MODULE GUIDE

**Scope:** `src/main/java/com/example/springbatchinvestment/client`

## OVERVIEW
External integration boundary for FSS and Gemini APIs with DTOs and custom client-level errors.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| FSS request behavior | `FssClient.java` | URI assembly, timeout rules, retry/backoff, error translation |
| Embedding API behavior | `GeminiClient.java` | API key usage, response extraction, retry handling |
| FSS response contracts | `dto/FssResponse.java`, `dto/*` | DTO layer consumed by readers/writers |
| Client exceptions | `error/FssClientError.java`, `error/FssUnavailableError.java` | Distinguish client/server/unavailable flows |

## CONVENTIONS
- Keep API contract mapping in `dto/`; avoid leaking transport details into domain classes.
- Maintain explicit timeout + retry behavior in clients; these are resilience-critical.
- Preserve existing error taxonomy (`FssClientError` vs `FssUnavailableError`) for caller branching.

## UNIQUE STYLES
- `FssClient` configures custom Jackson naming/timezone and Reactor Netty timeouts directly.
- `FssClient` logs request lifecycle (`SNT`/`RCV`) around retry attempts.

## ANTI-PATTERNS (CLIENT)
- Do not swallow remote exceptions; map or rethrow with existing error classes.
- Do not duplicate DTOs in other packages; keep remote payload models centralized in `client/dto`.
- Do not add blocking calls outside current client boundary without explicit design review.
