# DOMAIN MODULE GUIDE

**Scope:** `src/main/java/com/example/springbatchinvestment/domain`

## OVERVIEW
Business data model layer: enums/value models and JPA entity shape.

## STRUCTURE
```text
domain/
|- *.java                 # Enums and model objects
`- entity/                # JPA entities for RDB persistence
```

## WHERE TO LOOK
| Task | Location | Why |
|------|----------|-----|
| Product field/schema changes | `entity/FinancialProductEntity.java` | Core persisted aggregate used by writers/readers |
| Option model updates | `entity/FinancialProductOptionEntity.java` | Product option persistence rules |
| Status lifecycle changes | `ProductStatus.java` | ACTIVE/DELETED workflow semantics |
| Product type branching | `FinancialProductType.java` | Savings vs installment routing keys |

## CONVENTIONS
- Keep enum/value semantics in root `domain/` package for shared usage across modules.
- Preserve entity mutation helpers used by writers; avoid scattering update rules externally.

## HOTSPOTS
- `entity/FinancialProductEntity.java`: highest-impact schema/behavior class; many fields and update methods.

## ANTI-PATTERNS (DOMAIN)
- Do not place transport DTO concerns here; keep API payloads in `client/dto`.
- Do not duplicate status/type enums in other layers.
- Do not evolve entity fields without checking writer + repository query alignment.
