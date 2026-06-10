# Sistema de Troca — a hipótese

> Todo sistema de informação pode ser reduzido a três conceitos:
> **Entidade** (a coisa), **Estrutura** (uma coisa é formada por outras coisas)
> e **Troca** (uma coisa sai de alguém e vai para alguém).
> O resto — venda, imposto, alocação, estoque, saldo — é abstração sobre esses três.

## O modelo

| Tabela      | O que guarda                                                                 |
|-------------|------------------------------------------------------------------------------|
| `entidade`  | Qualquer coisa: pessoa, produto, carro, projeto, dinheiro. Tipo livre + atributos JSONB. |
| `estrutura` | Composição: pai é formado por filho, com quantidade e papel (carro → 4 rodas). |
| `troca`     | O fluxo: `de` → `para` : `objeto` (quantidade). Pernas da mesma transação compartilham um `grupoId`. |

### O insight central

- **Dinheiro é Entidade** — não existe caso especial. Uma venda são duas pernas
  do mesmo grupo: `Fábrica → Cliente : TV (1)` e `Cliente → Fábrica : BRL (2500)`.
- **Estoque e saldo não são cadastrados** — emergem das trocas:
  `posição = soma(recebido) − soma(entregue)`, por objeto.
- A história das trocas é imutável; entidades são desativadas, nunca apagadas.

## Stack

- Backend: Java 25 + Spring Boot 4 + PostgreSQL 17 (Docker) — porta 8080
- Frontend: React + Vite — porta 5173

## Como rodar

```powershell
# 1. Banco (uma vez; depois: docker start hipotese-postgres)
docker run -d --name hipotese-postgres -e POSTGRES_USER=hipotese `
  -e POSTGRES_PASSWORD=hipotese -e POSTGRES_DB=hipotese `
  -p 5432:5432 -v hipotese_pgdata:/var/lib/postgresql/data postgres:17

# 2. Backend (JAVA_HOME precisa apontar para o JDK 25)
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
cd backend
.\mvnw.cmd spring-boot:run

# 3. Frontend
cd frontend
npm run dev   # abre http://localhost:5173
```

## API

| Método | Rota                          | O quê                                       |
|--------|-------------------------------|---------------------------------------------|
| GET    | `/api/entidades?tipo=`        | Lista entidades ativas                      |
| POST   | `/api/entidades`              | Cria entidade `{nome, tipo, descricao, atributos}` |
| DELETE | `/api/entidades/{id}`         | Desativa (exclusão lógica)                  |
| GET    | `/api/entidades/{id}/posicao` | **Posição derivada das trocas**             |
| GET    | `/api/estruturas?paiId=`      | Do que o pai é formado                      |
| POST   | `/api/estruturas`             | `{paiId, filhoId, quantidade, papel}`       |
| GET    | `/api/trocas?entidadeId=`     | Histórico de trocas                         |
| POST   | `/api/trocas`                 | `{tipo, descricao, pernas:[{deId, paraId, objetoId, quantidade}]}` |
| POST   | `/api/producoes`              | `{produtoId, quantidade, produtorId, transformadorId, cascata}` — explode a Estrutura em Trocas (recursivo com `cascata`) |
| POST   | `/api/trocas/grupo/{id}/efetivar` | Reserva vira realidade: passa a contar no saldo |
| DELETE | `/api/trocas/grupo/{id}`      | Cancela reserva (só RESERVADA — efetivada é imutável) |

## Validação já executada

Caso da fábrica de TV, registrado via API:

1. Entidades: Fábrica TVCo, João, TV 50", Real BRL, Tela LED, Placa-mãe.
2. Estrutura: TV = Tela (1) + Placa-mãe (1).
3. Troca VENDA (grupo único, atômica): Fábrica → João: TV (1); João → Fábrica: BRL (2500).
4. Posição resultante — **sem nenhum cadastro de estoque**:
   - Fábrica: `+2500 BRL`, `−1 TV`
   - João: `+1 TV`, `−2500 BRL`

## Produção — validada

Produção não precisou de conceito novo: é uma Troca com uma entidade
transformadora (a Linha de Produção). O produtor entrega os componentes
(a Estrutura diz quanto), o transformador devolve o produto — mesmo grupo,
atômico, com validação de estoque derivado:

1. COMPRA: Fornecedor → Fábrica: 10 Telas + 10 Placas; Fábrica → Fornecedor: 3000 BRL.
2. Produzir 100 TVs foi **bloqueado**: "Estoque insuficiente de Tela LED 50:
   precisa de 100, disponível 10" — e o estoque consultado também é derivado.
3. PRODUCAO de 5 TVs: Fábrica → Linha: 5 Telas + 5 Placas; Linha → Fábrica: 5 TVs.
4. Posição final da Fábrica (tudo derivado): TV `4` (−1 vendida +5 produzidas),
   Tela `5`, Placa `5`, BRL `−500` (2500 da venda − 3000 da compra = dívida).

## BOM recursivo — validado

Placa-mãe ganhou estrutura própria (2 Chips + 4 Capacitores), dois níveis de
composição. Produzir 8 TVs com só 5 placas em estoque:

- Sem cascata: bloqueado ("precisa de 8, disponível 5 — use cascata").
- Com cascata: o sistema produziu sozinho o déficit (3 placas, consumindo
  6 chips + 12 capacitores) e depois as 8 TVs — dois grupos de troca,
  uma transação atômica, com proteção contra ciclo na Estrutura.

## Reserva — validada (sem quarto conceito)

A "troca futura" não precisou de tabela nova: é um `status` na própria Troca
(`RESERVADA` → `EFETIVADA`). A posição ganhou quatro números, todos derivados:

| Número       | O que é                                   |
|--------------|-------------------------------------------|
| saldo        | efetivadas que entraram − que saíram      |
| comprometido | reservadas que vão sair                   |
| aReceber     | reservadas que vão entrar                 |
| disponivel   | saldo − comprometido (produção valida por este) |

Maria reservou 3 TVs por 7500 BRL: a Fábrica ficou com TV `saldo 12,
comprometido 3, disponível 9` e BRL `a receber 7500` — sem mexer no saldo.
Ao efetivar: TV `9`, BRL `6000`. Cancelar grupo efetivado é bloqueado —
só reservas são canceláveis; o que aconteceu é história imutável.

## Próximos passos da hipótese

- Modelar o caso GestaoProjeto: projeto/tarefa como Estrutura, alocação e
  conclusão como Troca.
- Testar onde a teoria range: orçamento, permissões, precificação por
  estrutura (custo da TV = soma recursiva dos componentes).
