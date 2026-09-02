# GreenShift API

**Energia Inteligente: Otimizando o Consumo através da Computação**

Middleware que intercepta cargas de trabalho corporativas **não críticas** (backups, ETLs,
retreino de modelos) e as desloca para as janelas em que a rede elétrica está mais limpa —
sem violar o prazo de negócio do cliente.

> O alvo não é o consumidor final: são os **datacenters**. A mesma carga, rodada 16 horas
> depois, pode emitir metade do carbono. O GreenShift descobre quando, faz o agendamento e
> audita o resultado para o relatório de ESG.
> 
- **Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL · Docker · Arquitetura Hexagonal

---

## Resultado real de uma execução

Backup de 2h submetido às **19h** (horário de ponta) com deadline de 20 horas:

| | |
|---|---|
| Intensidade se rodasse agora | **505,44** gCO₂/kWh |
| Janela escolhida pelo motor | dia seguinte, **12h** |
| Intensidade na janela | **243,81** gCO₂/kWh |
| **Redução** | **51,76 %** |
| **Carbono evitado** | **6.540 g** (6,54 kg de CO₂) |

A carga é a mesma. Só o *quando* mudou.

---

## Subindo o projeto

### Opção 1 — Docker (não precisa de Java nem Maven na máquina)

```bash
docker compose up --build
```

Sobe a API já conectada a um **PostgreSQL 16**. Disponível em `http://localhost:8080`.

### Opção 2 — Local, com o Maven Wrapper

```bash
./mvnw spring-boot:run          # Linux / macOS
.\mvnw.cmd spring-boot:run      # Windows
```

Usa **H2 em memória** — zero configuração, ideal para a demonstração.

### Testes

```bash
./mvnw test
```

13 testes: 7 no núcleo de domínio (Java puro, sem Spring) e 6 de integração da API.

---

## Roteiro de demonstração

**1. Ver a curva de carbono das próximas 24h** — os "vales verdes" que o motor persegue:

```bash
curl http://localhost:8080/api/v1/forecast
```

**2. Submeter uma carga de trabalho:**

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
        "comando": "pg_dump producao",
        "descricao": "backup diario",
        "duracaoMinutos": 120,
        "deadline": "2026-08-29T15:00:00Z",
        "consumoEstimadoKw": 12.5
      }'
```

A resposta já traz a janela escolhida e o carbono que deixará de ser emitido:

```json
{
  "id": "5af62db2-a71e-4fd9-91f8-bddda4730198",
  "status": "AGENDADO",
  "agendadoPara": "2026-08-29T12:00:00Z",
  "deslocamentoHoras": 16,
  "intensidadeJanelaGco2Kwh": 243.81,
  "intensidadeBaselineGco2Kwh": 505.44,
  "economiaEstimadaGramasCO2": 6540.75,
  "reducaoPercentual": 51.76
}
```

**3. Consultar o status:**

```bash
curl http://localhost:8080/api/v1/jobs/{id}
```

**4. Adiantar o relógio** (para não esperar a janela chegar durante a apresentação):

```bash
curl -X POST "http://localhost:8080/api/v1/metrics/executar?ate=2026-08-29T13:00:00Z"
```

**5. Abrir o painel de ESG:**

```bash
curl http://localhost:8080/api/v1/metrics/dashboard
```

```json
{
  "jobsRecebidos": 1,
  "jobsPorStatus": { "PENDENTE": 0, "AGENDADO": 0, "EXECUTADO": 1 },
  "carbonoEvitadoGramas": 6540.75,
  "carbonoEvitadoKg": 6.54,
  "equivalenteArvoresPlantadas": 0.31
}
```

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/jobs` | Recebe a carga e devolve a janela verde escolhida |
| `GET` | `/api/v1/jobs/{id}` | Status do job (`PENDENTE` · `AGENDADO` · `EXECUTADO`) |
| `GET` | `/api/v1/jobs` | Lista todos os jobs |
| `GET` | `/api/v1/forecast` | Curva de intensidade de carbono das próximas 24h |
| `GET` | `/api/v1/metrics/dashboard` | Painel corporativo de ESG |
| `POST` | `/api/v1/metrics/executar` | Antecipa o ciclo do executor (apoio à demo) |
| `GET` | `/actuator/health` | Health check |

Erros seguem **RFC 7807 (Problem Details)**: `400` para payload inválido,
`422` quando nenhuma janela cabe no deadline, `404` para job inexistente.

---

## Como o motor decide

1. Busca a previsão de intensidade de carbono (gCO₂/kWh) das próximas 24 horas.
2. Calcula quantos slots horários a carga precisa: `ceil(duração / 60)`.
3. Desliza essa janela sobre a previsão, **descartando toda posição que termine depois do
   deadline** — o prazo de negócio é uma restrição rígida, nunca uma sugestão.
4. Escolhe a janela de menor intensidade média.
5. Congela as duas intensidades (a da janela e a de "rodar agora") no job. É isso que torna
   a economia **auditável depois**, e não um número recalculado com dados que já mudaram.

```
gCO₂ evitados = (baseline − janela) [gCO₂/kWh] × potência [kW] × duração [h]
```

Se nenhuma janela couber no prazo, o job é **recusado com 422** em vez de agendado fora do
deadline. Prazo de negócio vence meta ambiental.

---

## Arquitetura

O núcleo de sustentabilidade é **Java puro**: não importa Spring, não importa JPA, não conhece
HTTP. Ele conversa com o mundo apenas através de interfaces (*portas*) que os *adapters*
implementam.

```
br.com.rytechh.greenshift
├── core                          ← ZERO dependência de framework
│   ├── domain                    WorkloadJob, CarbonForecast, GreenMetrics, JobStatus
│   └── usecase                   OtimizacaoEnergiaUseCase  ← o motor
├── application
│   └── ports
│       ├── in                    AgendarJobPort, ConsultarJobPort, ExecutarJobsPort
│       └── out                   JobRepositoryPort, CarbonForecastPort, GreenMetricsRepositoryPort
├── adapters
│   ├── in
│   │   ├── rest                  Controllers, DTOs, tratamento de erros
│   │   └── scheduler             Dispara o executor a cada 60s
│   └── out
│       ├── persistence           JPA / PostgreSQL
│       └── integration           Fonte de dados de carbono
└── config
    └── BeanConfig.java           ← único ponto onde o Spring encontra o core
```

**O que isso compra na prática:**

- Os 7 testes do motor rodam em **0,046 s** sem subir contexto Spring nem banco. Os 6 de
  integração levam ~10 s. A regra que importa é a barata de testar.
- Trocar a fonte de carbono (simulada → Electricity Maps) é **uma linha** em `BeanConfig`.
  Trocar PostgreSQL por outro banco não toca o motor.

---

## Fonte de dados de carbono

`SimuladoCarbonForecastAdapter` gera uma curva de 24h com o formato da matriz elétrica
brasileira: vale solar ao meio-dia, vale eólico de madrugada e pico no horário de ponta
(18h–21h).

A simulação é uma decisão consciente de MVP — elimina dependência de chave de API de
terceiros durante a avaliação. Plugar a fonte real é criar uma classe que implemente
`CarbonForecastPort` e trocar o bean:

```java
@Bean
public CarbonForecastPort carbonForecastPort() {
    return new SimuladoCarbonForecastAdapter();   // ← troque por ElectricityMapsAdapter
}
```

Nenhuma outra linha do sistema muda. É exatamente para isso que a porta existe.

---

## Modelo de dados

| Entidade | Papel |
|---|---|
| `WorkloadJob` | Fila de processamento: comando, duração, deadline, status, janela agendada e as intensidades que sustentam o cálculo |
| `CarbonForecast` | Ponto da curva de carbono (timestamp, gCO₂/kWh, região) |
| `GreenMetrics` | Auditoria ESG: job executado, quando, quanto carbono foi evitado |

---

## Limitações conhecidas (escopo de MVP)

Explicitadas por honestidade técnica — cada uma tem o caminho de evolução mapeado:

- **A previsão é simulada**, não vem de uma API real. A porta para trocar já existe.
- **O job não é executado de fato** — o sistema marca `EXECUTADO` e audita a economia.
  Disparar o comando exigiria um agente com credenciais na infraestrutura do cliente.
- **Janela mínima de 1 hora**: a granularidade acompanha a das APIs públicas de carbono.
- **Sem autenticação** — um middleware corporativo real exigiria mTLS ou OAuth2.
- **Região fixa em `BR`** no agendamento; a porta já recebe `regionCode` por parâmetro.
