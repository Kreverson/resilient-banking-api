# resilient-banking-api

API de ledger bancário (depósito, saque e transferência) usada para comparar **padrões de resiliência sob sobrecarga**:
o que o sistema faz para continuar útil quando chega mais pedido do que ele dá conta.

Cada padrão vive numa branch própria. A `main` é a linha de base, **sem proteção nenhuma**. Todas as branches têm o
mesmo domínio, os mesmos custos, os mesmos logs e as mesmas métricas, então a única diferença entre duas execuções é o
padrão aplicado. Os testes de carga ficam no repositório irmão
[`resilient-banking-loadtest`](../resilient-banking-loadtest) (k6).

## Stack

- Java 21, Spring Boot 4, threads virtuais no Tomcat
- PostgreSQL 17 com Flyway
- Micrometer + Prometheus (scrape de 1s) + Grafana
- Docker Compose para subir tudo; k6 para a carga

Não há testes unitários: o objetivo é validar o comportamento dos padrões sob carga, não regras de negócio.

## Arquitetura

Clean Architecture enxuta, com três camadas:

```
domain/        modelo (Account, Transaction, LedgerEntry, Statement...), exceções e portas. Sem Spring, sem JPA.
application/   casos de uso (ExecuteTransaction, GetStatement, PrefetchReceipts), commands e results.
infra/         tudo que é tecnologia:
  web/           controllers, DTOs de entrada/saída, WebMapper, erros globais (Problem Details)
  persistence/   entidades JPA, repositórios, mappers entidade <-> domínio, lock pessimista
  execution/     a "porta" (AdmissionFilter) e a "cozinha" (WorkerPool) — onde os padrões são aplicados
  observability/ métricas, access log e trace de cada pedido
  simulation/    custo de processamento simulado
  config/        propriedades (bank.*) e montagem dos casos de uso
```

- Objeto de domínio não é entidade de banco: `Account` (domínio) ≠ `AccountEntity` (JPA), com mapper em `persistence/mapper`.
- Objeto de domínio não é DTO: `TransactionRequest` (entrada) vira `TransactionCommand` pelo `WebMapper`.
- Erros globais no `GlobalExceptionHandler`, todos no formato Problem Details (RFC 9457) com `code`, `timestamp` e
  `requestId`. Recusas feitas na porta usam o `ProblemResponseWriter`, que escreve o mesmo formato sem passar pelo
  Spring MVC e sem ler o corpo — recusar precisa ser barato.

## O cenário: do restaurante para o banco

A ideia vem de um vídeo sobre padrões de resiliência que usa uma API de restaurante. Aqui a mesma estrutura virou um banco:

| Vídeo (restaurante) | Esta API (banco) | Rota | Prioridade | Custo simulado |
|---|---|---|---|---|
| Pedido de comida ("o dinheiro") | Lançamento no ledger: depósito, saque, transferência | `POST /v1/transactions` | `CRITICAL` | antifraude 45 ms |
| Cardápio + sessão "para você" | Extrato + **insights** personalizados (parte opcional) | `GET /v1/accounts/{id}/statement` | `IMPORTANT` | 25 ms + 40 ms |
| Pré-carregamento em segundo plano | Pré-carregamento de comprovantes, ninguém olhando | `GET /v1/accounts/{id}/receipts` | `SHEDDABLE` | 50 ms |

- **20 workers** processando (`bank.workers`), mistura de tráfego 50/30/20, capacidade medida perto de **350 pedidos/s**.
- O cliente desiste depois de **3 s** (`bank.client-timeout`).
- O teste de pico: 30 s a 150/s, **30 s a 450/s** (o triplo, como um push de promoção para todo mundo), 60 s a 150/s.
- A prioridade vem do header `X-Request-Priority`; sem header, sai da rota (`PriorityClassifier`).
- Transações exigem `Idempotency-Key`: o mesmo pedido repetido não lança duas vezes.
- O banco tem 10.000 contas de clientes e uma conta interna de liquidação (seed no Flyway `V2`).

### Porta e cozinha

```
cliente ──► AdmissionFilter (porta) ──► fila ──► WorkerPool (20 workers) ──► controller ──► caso de uso ──► Postgres
              thread virtual do Tomcat            leitura do corpo, MVC, regra, persistência
```

A thread do Tomcat só recebe a conexão e coloca o pedido na fila. Todo o trabalho roda numa das 20 workers. Isso deixa
a fila visível e mensurável, e é nela que cada branch atua: recusando na porta, escolhendo a ordem ou descartando antes
de ocupar uma worker.

### Goodput

O que importa não é o **throughput** (tudo que o servidor responde), mas o **goodput**: resposta de sucesso que chega
**antes de o cliente desistir**. Uma API pode responder 350 pedidos/s com goodput zero — basta que todos sejam de
clientes que já foram embora.

## Branches

| Branch | Padrão | O que muda |
|---|---|---|
| `main` | Nenhum (linha de base) | Fila sem limite, FIFO. Sob sobrecarga a fila cresce sem parar e as workers passam a trabalhar para quem já desistiu (falha metaestável). |
| `pattern/01-deadline-discard` | Descarte por deadline | Antes de começar um pedido, a worker olha há quanto tempo ele chegou; passou de 3 s, descarta sem processar e devolve 503 `REQUEST_EXPIRED`. |
| `pattern/02-load-shedding` | Load shedding | Fila limitada a **100 lugares**. Quem chega com a fila cheia recebe na hora **503 + `Retry-After: 1`**, sem ler o corpo e sem ocupar worker. |
| `pattern/03-adaptive-lifo` | LIFO adaptativo + CoDel (Facebook) | Com mais de 20 na fila, atende o **mais novo primeiro**. CoDel: limite de espera de 100 ms, que cai para 5 ms quando a fila não esvazia. `LIFO_ENABLED` / `CODEL_ENABLED` permitem medir cada parte. |
| `pattern/04-priority-shedding` | Load shedding por prioridade (Google / Netflix) | A mesma fila com três limites: `SHEDDABLE` só entra abaixo de 20, `IMPORTANT` até 100, `CRITICAL` até 150. Sempre sobra lugar para a transação. |
| `pattern/05-graceful-degradation` | Degradação graciosa (brownout) | Um **dimmer** entre 0 e 1 decide a chance de o extrato sair com insights. A cada 100 ms: fila acima de 20 desce 0,1; abaixo de 5 sobe 0,01. O extrato sem insights sai com `degraded: true`. |
| `pattern/06-shuffle-sharding` | Bulkhead por cliente / shuffle sharding (AWS) | 8 cópias da API atrás de um gateway nginx. Parceiros BaaS mandam lotes de pagamento; um parceiro com bug manda um lote que derruba o nó. Três roteamentos: `random`, `cells` (4 grupos fixos de 2 nós) e `shuffle` (um par de nós sorteado entre 28). |
| `pattern/07-combined` | Prioridade + degradação | Junta a 04 e a 05: degrada o extrato primeiro e só recusa quando nem a degradação basta. |

Para ver exatamente o que cada padrão mudou: `git diff main pattern/<branch>`. Os commits são granulares de propósito.

## Como rodar

```bash
# 1. Subir a API de uma branch (o nome vira a tag da imagem e o label "branch" das métricas)
git checkout pattern/02-load-shedding
BRANCH=02-load-shedding docker compose up -d --build --force-recreate -V postgres api

# 2. No repositório de carga: aquece a JVM e roda o pico
cd ../resilient-banking-loadtest
./run.sh 02-load-shedding

# 3. Comparativo lado a lado (results/report.html e results/summary.json)
node tools/report.mjs
```

Recrie só `postgres` e `api` entre execuções: o Prometheus guarda as execuções anteriores e o Grafana mostra todas juntas.

Branch 06 (sem aquecimento, roteamento escolhido por `SHARDING`):

```bash
git checkout pattern/06-shuffle-sharding
SHARDING=shuffle docker compose up -d --build --force-recreate -V
cd ../resilient-banking-loadtest
./run.sh 06-shuffle shuffle-sharding
node tools/sharding-report.mjs results/06-random.csv.gz results/06-cells.csv.gz results/06-shuffle.csv.gz
```

Endereços: API `http://localhost:8080`, Prometheus `http://localhost:9090`, Grafana `http://localhost:3000`
(dashboard "Resiliência" como página inicial, sem login).

## Observabilidade (igual em todas as branches)

Métricas (Micrometer, todas com o label `branch`):

| Métrica | O que mede |
|---|---|
| `bank.requests.received` | pedidos que chegaram na porta, por prioridade |
| `bank.requests.finished` | pedidos terminados, por prioridade, status e `outcome` (`on_time` / `late`) |
| `bank.queue.size` | pedidos esperando na fila |
| `bank.queue.wait` | tempo de espera na fila (histograma) |
| `bank.service.time` | tempo de processamento numa worker (histograma) |
| `bank.workers.busy` / `bank.workers.capacity` | workers ocupadas e total |
| `bank.brownout.dimmer` | valor do dimmer (branches 05 e 07) |
| `bank.statement.degraded` | extratos que saíram sem insights |

Access log estruturado (ECS) com um registro por pedido: `requestId`, `priority`, `method`, `path`, `status`,
`outcome`, `queueMs`, `serviceMs`, `totalMs`. O `X-Request-Id` volta em toda resposta.

No k6, cada resposta é classificada como `goodput`, `timeouts`, `rejected` (503 da porta), `failed` ou `degraded`.

## Resultados

Pico de 150 → 450 → 150 pedidos/s, máquina de 12 CPUs, API limitada a 4 cores e 2 GB.

| Branch | Goodput | Goodput no pico (/s) | Recuperação após o pico | Transação | Extrato | Comprovantes | Timeouts | Recusados (503) | Degradados | p50 | p99 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `main` | 48,6% | 104 | 26 s | 48,6% | 48,7% | 48,4% | 13.874 | 0 | 0 | 68 ms | 2.851 ms |
| `01-deadline-discard` | 62,4% | 115 | 4 s | 62,1% | 63,3% | 61,7% | 10.125 | 24 | 0 | 68 ms | 2.912 ms |
| `02-load-shedding` | 87,5% | 330 | 0 s | 87,7% | 87,4% | 87,0% | 0 | 3.384 | 0 | 70 ms | 426 ms |
| `03-adaptive-lifo` (LIFO + CoDel) | 84,7% | 310 | 0 s | 85,1% | 84,4% | 84,1% | 0 | 4.127 | 0 | 68 ms | 188 ms |
| `04-priority-shedding` | 88,5% | 340 | 0 s | **100%** | 94,2% | 51,1% | 0 | 3.111 | 0 | 69 ms | 369 ms |
| `05-graceful-degradation` | **100%** | 434 | 0 s | 100% | 100% | 100% | 0 | 0 | 4.333 | 104 ms | 845 ms |
| `07-combined` | 96,4% | 413 | 0 s | 100% | 100% | 82,3% | 0 | 959 | 4.131 | 69 ms | 274 ms |

Goodput e percentuais por tipo são sobre os 120 s do teste (≈ 27.000 pedidos). "Goodput no pico" é a média por
segundo durante os 30 s de 450/s.

O que os números contam:

- **`main`**: a fila cresce, a espera passa de 3 s e a API passa a preparar respostas para quem já desistiu. Depois do
  pico ela ainda leva 26 s para voltar a atender a tempo — a falha metaestável: a causa sumiu, o estado ruim ficou.
- **01**: descartar quem já desistiu salva a recuperação (4 s), mas no pico continua quase sem goodput: a fila é FIFO,
  então o pedido passa no teste dos 3 s e o cliente desiste enquanto ele é processado.
- **02**: limitar a fila resolve na porta. 100 lugares com 350/s de vazão dão menos de 0,3 s de espera, então todo
  pedido na fila ainda tem cliente esperando. A API fica no máximo durante o pico e volta ao normal no segundo seguinte.
- **03**: o LIFO sozinho (`CODEL_ENABLED=false`, sem limite) ficou em 43,9% numa execução anterior: quem cai no fim da
  fila espera os 3 s inteiros para descobrir que não vai ser atendido. Com o CoDel o p99 cai para 188 ms, o menor de todos.
- **04**: a mesma vazão da 02, mas quem é recusado passa a ser quem ninguém está vendo: transação 100%, extrato 94%,
  comprovantes 51%.
- **05**: em vez de recusar, entrega menos. Mais da metade dos extratos sai sem insights e ninguém é recusado. O p99 sobe
  (veja a nota abaixo).
- **07**: degrada primeiro e recusa só o que sobra, quase todo em pré-carregamento. Melhor equilíbrio entre goodput e latência.

### Nota sobre a calibração da degradação

No vídeo, o cardápio sem a sessão personalizada leva a capacidade a cerca de 540/s, acima do pico. Aqui o tempo real de
serviço inclui uns 15 ms de banco além do custo simulado (transação ≈ 60 ms, comprovantes ≈ 54 ms). Com o dimmer em
zero a capacidade fica perto de 390/s, **abaixo** dos 450/s do pico. Por isso a 05 sozinha ainda acumula fila (p99 de
845 ms) e a 07, que recusa o excedente, fica com p99 de 274 ms. Para reproduzir o número do vídeo, seria preciso
aumentar a parte opcional do extrato ou reduzir o custo das outras rotas e rodar tudo de novo.

### Shuffle sharding (branch 06)

1.000 parceiros mandando 200 lotes/s para 8 nós. Aos 30 s, o parceiro `p0000` passa a mandar um lote cujo header
declara 500 milhões de registros: o parser pré-aloca e o nó cai. O gateway tenta o próximo nó (até 3 vezes), então o
retry espalha o estrago. Os números contam o que aconteceu com cada parceiro depois que o lote envenenado começou.

| Roteamento | Parceiros sem servidor | Sem impacto | Lotes OK / com falha (90 s) |
|---|---|---|---|
| `random` — qualquer parceiro em qualquer nó | **1.000** | 0 | 6.373 / 11.688 |
| `cells` — 4 grupos fixos de 2 nós | 244 | 756 | 15.076 / 2.986 |
| `shuffle` — par de nós sorteado entre 28 | **39** | 961 | 17.534 / 527 |

- **random**: o retry leva o lote envenenado para todos os nós, os 8 caem e ninguém mais é atendido.
- **cells**: o parceiro derruba a célula dele e leva junto o quarto de parceiros que estava nela.
- **shuffle**: só cai quem sorteou exatamente o mesmo par (≈ 1/28 dos parceiros). Quem dividia um nó só com ele
  continua atendido pelo outro.

O roteamento é gerado por `sharding/generate-nginx.sh` (`random.conf`, `cells.conf`, `shuffle.conf`). O gateway
repassa o `Host` original: sem isso o Tomcat recebe o nome do upstream (`shard_1_2`) e recusa o underscore com 400.
