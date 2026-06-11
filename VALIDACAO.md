# Plano de validação — Fase 2: da expressividade à robustez

A Fase 1 (ver README) provou a **expressividade**: 12 domínios em 3 tabelas.
A Fase 2 ataca as três objeções que decidem se isso vira produto:
performance, concorrência e integridade semântica.

## Contexto estratégico

- **Prior art reconhecido**: REA (McCarthy, 1982 / ISO 15944-4), partida
  dobrada (Pacioli, 1494), event sourcing. O modelo não é patenteável
  (Lei 9.279/96 art. 10 exclui métodos comerciais e modelos abstratos).
- **Caminho do crédito**: publicação com data (anterioridade defensiva) +
  open source com marca + execução. O diferencial desta síntese é a camada
  de abstração via IA — o que faltou ao REA por 40 anos.
- **Tese de performance**: bancos operam ledgers imutáveis com saldo
  derivado em escala de bilhões de transações. É engenharia, não refutação.

## Teste 1 — Benchmark de posição (o medo da performance)

1. Gerar massa: 1.000 entidades, 1.000.000 de trocas (script de seed).
2. Medir `posicaoDaEntidade` em entidades frias e quentes (P50/P95).
3. Critério: < 100 ms por consulta de posição com 1M de trocas.
4. Se reprovar: índice composto `(de_id, status)` / `(para_id, status)`,
   depois repetir.

## Teste 2 — Snapshot de posição (a resposta dos bancos)

1. Tabela `posicao_snapshot` (entidade, objeto, saldo, ate_quando) —
   uma PROJEÇÃO, não um quarto conceito: derivada e reconstruível.
2. Posição = snapshot + delta das trocas após o corte.
3. Job de consolidação periódica.
4. Critério: posição < 10 ms com 10M de trocas; e `drop` do snapshot +
   reconstrução total bate com o valor incremental (prova de derivação).

## Teste 3 — Concorrência (a corrida do estoque)

1. Disparar 50 produções simultâneas consumindo o mesmo componente com
   estoque para apenas 25.
2. Hoje deve falhar (validação e gravação sem lock = estoque negativo).
3. Corrigir: lock por entidade (advisory lock) ou isolamento SERIALIZABLE
   com retry.
4. Critério: exatamente 25 produções aceitas, 25 rejeitadas, sob carga.

## Teste 4 — Integridade semântica via meta-modelo (a aposta da IA)

1. O Tipo vira Entidade ("PESSOA" é uma entidade tipo TIPO) cujos
   `atributos` carregam um JSON Schema dos atributos exigidos.
2. Validação na criação: PESSOA sem CPF é rejeitada — regra que vive em
   DADOS, não em código. O meta-modelo testa se a teoria se descreve
   a si mesma (a abstração máxima).
3. Critério: criar um domínio novo inteiro (ex: clínica - paciente,
   consulta, convênio) só cadastrando Tipos e regras, zero deploy.

## Teste 5 — Motor de eventos (a reverberação no rio)

1. Listener sobre Troca efetivada: regras `quando X então Y` cadastradas
   como dados (ex: venda efetivada e disponível < mínimo → gerar Reserva
   de compra com fornecedor).
2. Critério: cadeia venda → planejamento → pedido de compra disparando
   sem código específico do domínio.

## Ordem recomendada

1 → 3 → 2 → 4 → 5. Performance primeiro (é a objeção declarada), corrida
de concorrência em seguida (é o risco real de produção), snapshot depois
(resolve os dois), e os testes de abstração por último, já sobre uma base
sólida.

---

# RESULTADOS — todos executados

## Teste 1 — Benchmark (1.000.038 trocas, entidade quente com 101.688)

| Cenário                          | P50    | P95    | Veredito |
|----------------------------------|--------|--------|----------|
| Entidade fria (~2k trocas)       | 32 ms  | 36 ms  | PASSA    |
| Entidade quente, agregação total | 263 ms | 348 ms | reprova  |
| Quente + índice covering         | 256 ms | 361 ms | reprova (custo é agregar 101k linhas, não achá-las) |
| **Quente + snapshot (Teste 2)**  | **35 ms** | **58 ms** | **PASSA (7×)** |

## Teste 3 — Concorrência (50 produções simultâneas, estoque para 25)

- Sem proteção: **34 aceitas, estoque −9** — corrida real, demonstrada.
- Com `pg_advisory_xact_lock` por produtor: **exatamente 25 aceitas,
  estoque 0, 25 produtos**. Validação e consumo serializados por entidade.

## Teste 2 — Snapshot (projeção, não quarto conceito)

- Consolidação da quente (1000 objetos): 2,3 s, uma vez.
- Prova de derivação: projeção + delta **idêntica** à agregação completa
  nos 1000 objetos.
- Delta vivo: troca de +777 após consolidar apareceu na posição sem
  reconsolidar; mutação de reservas (efetivar/cancelar) invalida a projeção.

## Teste 4 — Meta-modelo (a regra vive em dados)

- `PACIENTE` é uma Entidade tipo `TIPO` com `atributos.obrigatorios=[cpf, convenio]`.
- Criar PACIENTE sem CPF: **rejeitado (400)**. Com CPF: aceito.
- Domínio clínica inteiro (paciente, consulta, pagamento) rodou **sem deploy**.

## Teste 5 — Motor de eventos (a reverberação)

- Regra cadastrada como Entidade tipo `REGRA`: "VENDA de TV com disponível
  < 5 → reservar compra de 10 com o fornecedor".
- Venda que não cruza o mínimo: nada acontece. Venda que cruza (4 < 5):
  **pedido de 10 TVs nasce sozinho como Reserva**. Venda seguinte não
  duplica o pedido pendente. Efetivar o pedido: disponível 13.

## Conclusão da Fase 2

As três objeções caíram: performance é resolvível com projeção (como nos
bancos), concorrência com lock por entidade, e integridade semântica com o
meta-modelo — regras como dados, sem deploy. O modelo segue em 3 tabelas.
