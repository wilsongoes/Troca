🇧🇷 [Versão em português](README.pt-BR.md)

# Sistema de Troca — *Everything is an Exchange*

> Every information system can be reduced to three concepts:
> **Entidade** (the thing), **Estrutura** (a thing is made of other things)
> and **Troca** (a thing flows from someone to someone).
> Everything else — sales, taxes, payroll, inventory, balances — is an
> abstraction over these three.

**Author of the hypothesis:** Wilson Goes — a model conceived over years and
validated in this proof of concept in June 2026. MIT licensed.

**Intellectual lineage (cited with pride):** REA — Resources, Events, Agents
(William McCarthy, 1982 / ISO 15944-4), double-entry bookkeeping (Luca
Pacioli, 1494), and event sourcing. What this synthesis adds: an
AI-operated abstraction layer, **rules as data** (meta-model + event engine),
and the practical validation of 13+ business domains on a single atomic
backend. Full results: [VALIDATION.md](VALIDATION.md).

## The model — three tables, nothing else

| Table       | What it holds |
|-------------|---------------|
| `entidade`  | Any *thing*: person, product, car, project, money, **time**. Free-form type + JSONB attributes. |
| `estrutura` | Composition: parent is made of child, with quantity and role (car → 4 wheels; wall → 8 work-hours + 1000 bricks). |
| `troca`     | The fundamental flow: `from` → `to` : `object` (quantity). Legs of the same transaction share a `grupoId` and commit atomically. |

### The core insights

- **Money is just an Entidade** — no special case. A sale is two legs of one
  group: `Factory → Customer : TV (1)` and `Customer → Factory : BRL (2500)`.
- **Time is just an Entidade** — a salary is hours exchanged for money; a
  deliverable is *produced* from hours via the BOM.
- **Inventory and balances are never stored** — they *emerge* from the
  exchange history: `position = Σ received − Σ given`, per object.
- **Effectuated history is immutable** — only reservations (future
  exchanges, a `status` on Troca) can be cancelled.
- **Rules live in data, not code** — a `TIPO` entity declares required
  attributes; a `REGRA` entity triggers automatic replenishment. New domains
  ship with **zero deploys**.

## Validation status — Phase 2: 5/5 passed

| Test | Result |
|------|--------|
| Position query @ 1M exchanges | P50 **35 ms** on a hot entity with 101k exchanges (snapshot projection, 7× speedup; identical to full aggregation across 1000 objects) |
| Concurrency: 50 parallel productions, stock for 25 | Naive code: 34 accepted, stock −9 (race demonstrated). With per-entity advisory lock: **exactly 25** |
| Projection is derivable | Snapshot + delta ≡ full aggregation; new exchanges picked up live without re-consolidation |
| Semantic integrity | `PACIENTE` type (data!) requires `cpf` — creation without it rejected (400). A full clinic domain ran with **no deploy** |
| Event engine | "TV sale + available < 5 → reserve 10 from supplier" stored as **data**; fired exactly once, no duplicates |

13+ domains validated end-to-end: sale, purchase, production (recursive BOM
with cascade), debt, reservation, payroll, project management, MRP purchase
planning, fiscal invoice with taxes, government→citizen cycle, clinic.

## Stack & how to run

Backend: Java 25 + Spring Boot 4 + PostgreSQL 17 (Docker). Frontend: React + Vite.

```powershell
# 1. Database (once; later: docker start hipotese-postgres)
docker run -d --name hipotese-postgres -e POSTGRES_USER=hipotese `
  -e POSTGRES_PASSWORD=hipotese -e POSTGRES_DB=hipotese `
  -p 5432:5432 -v hipotese_pgdata:/var/lib/postgresql/data postgres:17

# 2. Backend (requires JDK 25)
cd backend
.\mvnw.cmd spring-boot:run

# 3. Frontend
cd frontend
npm install && npm run dev   # http://localhost:5173
```

## API

| Method | Route | What |
|--------|-------|------|
| GET/POST | `/api/entidades` | Things. `DELETE` is logical (history is immutable) |
| GET | `/api/entidades/{id}/posicao` | **Derived position**: saldo, comprometido, aReceber, disponivel |
| POST | `/api/entidades/{id}/consolidar` | Consolidate position as a projection (snapshot + live delta) |
| GET/POST | `/api/estruturas` | Composition (BOM, project breakdown, anything) |
| GET/POST | `/api/trocas` | Exchanges; `reservada: true` creates a future commitment |
| POST | `/api/trocas/grupo/{id}/efetivar` | A reservation becomes reality |
| POST | `/api/producoes` | Explodes Estrutura into Trocas; `cascata` produces missing components recursively |
| GET | `/api/planejamentos` | MRP: structure × available stock → shopping list. Pure read |

## Glossary (the model keeps its original Portuguese vocabulary — like *kanban* kept its Japanese)

| Term | Meaning |
|------|---------|
| **Entidade** | Entity — any thing that exists |
| **Estrutura** | Structure — composition: a thing made of things |
| **Troca** | Exchange — the fundamental flow between entities |
| **Posição** | Position — emergent inventory/balance (saldo, comprometido, a receber, disponível) |
| **Efetivar** | To effectuate — a reservation becomes immutable history |
| **Reverberação** | Reverberation — events triggering events (the `REGRA` engine) |

## Roadmap / good first issues

- Recursive cost derivation (what did the wall *cost*? — derivable from the exchange history)
- UI for `REGRA` and `TIPO` entities (the meta-model deserves a face)
- Multi-tenant; permissions modeled... as Estrutura? (open question)
- NF-e (Brazilian fiscal invoice) connector as exchange groups
- Snapshot consolidation job (scheduled)

## Contributing & support

See [CONTRIBUTING.md](CONTRIBUTING.md). If this model is useful to you,
consider sponsoring via GitHub Sponsors — and if you build something on it,
**cite the source**: that is how an open model pays its author.

MIT © Wilson Goes
