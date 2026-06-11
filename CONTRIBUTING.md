# Contributing / Contribuindo

🇺🇸 English below ⬇ | 🇧🇷 Português abaixo ⬇

---

## English

Thank you for considering a contribution! This project validates a single
hypothesis: **every information system reduces to Entidade, Estrutura and
Troca**. Contributions should respect the prime directive:

> **Never add a fourth concept.** If a feature seems to need a new table,
> first try to express it as data over the three existing ones (see how
> reservations became a `status`, types became `TIPO` entities, and rules
> became `REGRA` entities). Projections (like `posicao_snapshot`) are fine —
> they are derived and rebuildable, not concepts.

### How to run

See [README.md](README.md) — Docker + JDK 25 + Node 22.

### How to contribute

1. Open an issue describing the domain you want to model or the gap you found.
2. Fork, branch, and send a PR. Keep code style consistent with what exists
   (Portuguese domain vocabulary, plain Spring Boot, no extra frameworks).
3. Every behavioral change needs a validation script or test — this repo's
   culture is adversarial: prove it, don't claim it (see [VALIDATION.md](VALIDATION.md)).

### Good first issues

- Recursive cost derivation from exchange history
- UI for `TIPO` and `REGRA` entities
- Scheduled snapshot consolidation job
- Multi-tenant exploration

### Developer Certificate of Origin

By contributing you certify the DCO (https://developercertificate.org/):
sign your commits with `git commit -s`.

---

## Português

Obrigado por considerar contribuir! Este projeto valida uma única hipótese:
**todo sistema de informação se reduz a Entidade, Estrutura e Troca**.
Contribuições devem respeitar a diretriz primária:

> **Nunca adicione um quarto conceito.** Se uma funcionalidade parece exigir
> uma tabela nova, primeiro tente expressá-la como dados sobre as três que
> existem (veja como reserva virou `status`, tipos viraram entidades `TIPO`
> e regras viraram entidades `REGRA`). Projeções (como `posicao_snapshot`)
> são bem-vindas — são derivadas e reconstruíveis, não conceitos.

### Como rodar

Veja o [README.pt-BR.md](README.pt-BR.md) — Docker + JDK 25 + Node 22.

### Como contribuir

1. Abra uma issue descrevendo o domínio que quer modelar ou a lacuna encontrada.
2. Fork, branch e PR. Mantenha o estilo do código existente (vocabulário de
   domínio em português, Spring Boot puro, sem frameworks extras).
3. Toda mudança de comportamento precisa de script de validação ou teste —
   a cultura deste repositório é adversarial: prove, não afirme
   (veja [VALIDACAO.md](VALIDACAO.md)).

### Boas primeiras issues

- Custo recursivo derivado do histórico de trocas
- UI para entidades `TIPO` e `REGRA`
- Job agendado de consolidação de snapshot
- Exploração de multi-tenant

### Certificado de Origem do Desenvolvedor

Ao contribuir você certifica o DCO (https://developercertificate.org/):
assine seus commits com `git commit -s`.
