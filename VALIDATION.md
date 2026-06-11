🇧🇷 [Versão em português](VALIDACAO.md)

# Validation — Phase 2 results (all executed)

Phase 1 (see [README](README.md)) proved **expressiveness**: 13+ domains on
3 tables. Phase 2 attacked the three objections that decide whether this
becomes a product: performance, concurrency, and semantic integrity.

## Test 1 — Benchmark (1,000,038 exchanges; hot entity holds 101,688)

| Scenario | P50 | P95 | Verdict |
|----------|-----|-----|---------|
| Cold entity (~2k exchanges) | 32 ms | 36 ms | PASS |
| Hot entity, full aggregation | 263 ms | 348 ms | fail |
| Hot + covering index | 256 ms | 361 ms | fail (the cost is aggregating 101k rows, not finding them) |
| **Hot + snapshot (Test 2)** | **35 ms** | **58 ms** | **PASS (7×)** |

## Test 3 — Concurrency (50 simultaneous productions, stock for 25)

- Without protection: **34 accepted, stock −9** — a real race, demonstrated.
- With `pg_advisory_xact_lock` per producer: **exactly 25 accepted,
  stock 0, 25 products**. Validation and consumption serialized per entity.

## Test 2 — Snapshot (a projection, not a fourth concept)

- Consolidating the hot entity (1000 objects): 2.3 s, once.
- Derivation proof: projection + delta **identical** to full aggregation
  across all 1000 objects.
- Live delta: an exchange of +777 made after consolidation appeared in the
  position without re-consolidating; mutating reservations
  (efetivar/cancelar) invalidates the projection automatically.

## Test 4 — Meta-model (rules live in data)

- `PACIENTE` is an Entidade of type `TIPO` with
  `atributos.obrigatorios = [cpf, convenio]`.
- Creating a PACIENTE without CPF: **rejected (400)**. With it: accepted.
- An entire clinic domain (patient, appointment, payment) ran with
  **zero deploys**.

## Test 5 — Event engine (the reverberation)

- A rule stored as an Entidade of type `REGRA`: "a TV sale leaving
  availability < 5 → reserve a purchase of 10 from the supplier".
- A sale that does not cross the threshold: nothing happens. One that does
  (4 < 5): **a 10-TV purchase order is born on its own**, as a Reservation.
  The next sale does not duplicate the pending order. Effectuating it:
  availability 13.

## Phase 2 conclusion

All three objections fell: performance is solvable with projections (as in
banking ledgers), concurrency with per-entity locks, and semantic integrity
with the meta-model — rules as data, no deploys. The model still fits in
**3 tables**.
