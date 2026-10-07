# moto-service

Controle da(s) moto(s) do usuário: **abastecimentos e consumo (km/l)**, **km rodados**,
**trocas de óleo com a previsão da próxima** e **métricas mensais e anuais** de rodagem e gasto.

Resource server do ecossistema Workbox: não emite token nem tem login — valida o Bearer do
`workbox-api` por introspecção remota e só atende quem tem o **módulo `MOTO`** (403 sem ele; ADMIN
recebe todos os módulos). Cada usuário só enxerga as próprias motos: recurso de outro dono responde
**404**, nunca 403.

| | |
|---|---|
| Porta (host) | **7059** (container 8085) |
| Banco | PostgreSQL 18, schema **`moto`**, role `moto_service` (sem acesso a outros schemas) |
| Contrato | [`openapi/openapi.yaml`](openapi/openapi.yaml) — fonte da verdade da API |
| Swagger UI | `http://localhost:7059/swagger-ui/index.html` |

## Stack

Java 25, Spring Boot 3.5.16, Gradle 9.7.1 (`./gradlew`), Spring Data JPA + Liquibase, Hibernate
Envers (auditoria), Spring Security 6 (OAuth2 resource server, **opaque token**), springdoc,
JaCoCo, Lombok. Testes: JUnit 5, AssertJ, MockMvc, Testcontainers.

## Estrutura de pacotes (`br.com.moto`)

- `domain/` — **cálculo puro**, sem Spring nem banco: `ConsumptionCalculator`, `OdometerTimeline`,
  `OdometerRules`, `OilStatusCalculator` (+ records `FuelEntry`, `Segment`, `ConsumptionWindow`,
  `OdometerPoint`, `OilStatus`).
- `models/{entities,dto,enums}` · `repositories/` · `services/` · `controllers/` · `exceptions/`
  (+ `handler/RestExceptionHandler`, RFC 9457) · `config/` (segurança, introspector, `Clock`, OpenAPI,
  auditoria).

## API (`/api/v1`)

| Recurso | Rotas |
|---|---|
| Motos | `GET/POST /motorcycles` · `GET/PUT/DELETE /motorcycles/{id}` · `GET /motorcycles/{id}/history` |
| Abastecimentos | `GET/POST /motorcycles/{motoId}/refuelings` (paginado; `from`, `to`, `page`, `size ≤ 100`) · `PUT/DELETE …/{id}` |
| Trocas de óleo | `GET/POST /motorcycles/{motoId}/oil-changes` · `PUT/DELETE …/{id}` · `GET /motorcycles/{motoId}/oil-status` · `GET /oil-intervals` |
| Km avulso | `GET/POST /motorcycles/{motoId}/odometer-readings` · `DELETE …/{id}` |
| Métricas | `GET /motorcycles/{motoId}/stats?from&to` · `…/stats/monthly?year` · `…/stats/yearly` |

## Como o cálculo funciona

**Hodômetro.** Todo registro usa o hodômetro **total** (não o trip). A moto tem uma linha do tempo
única (abastecimentos + trocas de óleo + leituras avulsas) que deve ser **monotônica**: nenhum registro
pode ficar abaixo do hodômetro inicial, de um registro anterior ou acima de um posterior (registros na
mesma data não são comparados). Violação → **400** com o motivo. Km rodados num período = hodômetro ao
fim dele − hodômetro ao fim do período anterior (ou o inicial).

**Consumo (km/l).** Como o tanque nem sempre é completado, o litro abastecido só aproxima o que foi
gasto desde o abastecimento anterior:

- **Trecho** (abastecimento N-1 → N): `km / litros de N`. É **exato** só se N e N-1 completaram o tanque
  (`fullTank`); senão é **estimado**. Marcar `fullTank` é opcional e só melhora a precisão.
- **Janela** (mês, ano, qualquer período): `Σkm / Σlitros` dos trechos que **terminam** nela —
  ponderada, nunca média de médias. Menos de 3 trechos → `lowConfidence`.
- O primeiro abastecimento da moto não gera trecho (o nível do tanque antes dele é desconhecido), mas
  conta em litros e gasto.

**Próxima troca de óleo.** Vence o que ocorrer primeiro entre `hodômetro da troca + intervalKm` e
`data da troca + intervalMonths`. Status: `VENCIDA` (km ou dias restantes ≤ 0), `PERTO` (km restantes ≤
max(500, 10% do intervalo) **ou** dias ≤ max(30, 10% do intervalo)) e `OK`. O intervalo é o que o usuário
escolheu na troca; `GET /oil-intervals` só sugere o padrão por tipo (mineral 1.500 km/6 m, semissintético
4.000 km/6 m, sintético 6.000 km/12 m). "Hoje" usa o fuso `app.timezone` (padrão `America/Fortaleza`).

## Autenticação

`WorkboxTokenIntrospector` chama `POST /api/v1/auth/introspect` do `workbox-api` (HTTP Basic com o client
`moto-service`, cadastrado em `workbox.api_clients`) e transforma `roles` em authorities e `modules` em
`MODULE_<CODIGO>`. A trava é `MODULE_MOTO`.

## Rodando localmente

Pré-requisitos: Postgres do monorepo no ar (`docker compose up -d postgres` na raiz, porta 7050), role e
schema criados (`initdb/` na raiz; num banco existente: `initdb/04-create-moto-role.sql`) e o `workbox-api`
no ar para a introspecção.

```bash
./gradlew bootRun          # profile dev, porta 7059
./gradlew check            # testes (os ITs exigem Docker)
./gradlew generateOpenApiDocs
```

Variáveis (todas com default de desenvolvimento): `PORT`, `PROFILE_ACTIVE`, `DATABASE_URL`,
`POSTGRES_USER`/`POSTGRES_PASSWORD`, `SCHEMA`, `INTROSPECTION_URI`, `INTROSPECTION_CLIENT_ID`/
`INTROSPECTION_CLIENT_SECRET`, `CORS_ALLOWED_ORIGINS` (propriedade `cors.allowed-origins`), `APP_TIMEZONE`.
Em `prod` não há defaults de segredo.

## Contrato de API (OpenAPI)

`openapi/openapi.yaml` é gerado do código. Mudou rota, DTO, status ou auth → rode
`./gradlew generateOpenApiDocs` e commite junto; o CI (`contract-drift-check`) falha se divergir.

## Testes

- `domain/*Test`: regras de cálculo, sem Spring.
- `controllers/*Test`: `@WebMvcTest`, serviços via `@MockitoBean`, auth com `opaqueToken()`.
- `services/*IT`: serviço + Postgres real (Testcontainers, container singleton em `PostgresIT`),
  com dono único por teste. `ModuleAccessSecurityTest` cobre a trava do módulo.

## Convenção de commits

pt-BR, Conventional Commits (`tipo(escopo): descrição`), conforme o
[CLAUDE.md da raiz](../CLAUDE.md#convenção-de-mensagens-de-commit).

## CI/CD

`.gitlab-ci.yml`: `test` (docker:dind para os ITs) → `contract-drift-check` → `build`. Sem Sonar.
