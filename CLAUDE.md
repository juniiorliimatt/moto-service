# moto-service — instruções do serviço

> Complementa o [`CLAUDE.md` da raiz](../CLAUDE.md) (visão do monorepo, contrato, commits,
> infra) e as regras globais de `~/.claude/CLAUDE.md`. Aqui só o que é específico deste
> serviço. Detalhes de uso/endpoints/cálculo: [`README.md`](README.md). Desenho original:
> [`docs/superpowers/specs/2026-10-06-moto-service-design.md`](../docs/superpowers/specs/2026-10-06-moto-service-design.md).

## Papel e autoria
- Domínio de **moto pessoal**: abastecimentos, consumo (km/l), km rodados, troca de óleo e
  métricas mensais/anuais. *Resource server*: não emite token nem tem login — valida o Bearer
  do `workbox-api` por introspecção remota.
- Autoria: **padrão do monorepo é o desenvolvedor implementar e o Claude só aconselhar**, mas
  aqui há **permissão total** concedida pelo desenvolvedor em 2026-10-06 (Claude implementa
  tudo: backend, front e a migration do módulo no `workbox-api`). A exceção é **por serviço**,
  nunca herdada. Em dúvida se ainda vale (longo intervalo, mudança de tom), perguntar.

## Stack e execução
- Java 25 LTS, Spring Boot 3.5.16, Gradle 9.7.1 (`./gradlew`), Spring Data JPA + Liquibase,
  Hibernate Envers, Spring Security 6 (OAuth2 resource server, **opaque token**), springdoc,
  JaCoCo, Lombok. **Sem** Cache/Caffeine, Cucumber e Sonar (não há necessidade; não adicionar
  sem motivo).
- Porta **7059** (container 8085). Profiles: `dev` (default, Postgres `:7050`), `prod`, `test`
  (H2, usado só pelo `generateOpenApiDocs` e pelos `@WebMvcTest`). Role Postgres `moto_service`,
  schema **`moto`**.
- Comandos: `./gradlew bootRun`, `./gradlew check`, `./gradlew generateOpenApiDocs`.

## Estrutura (`br.com.moto`)
`config/` (`SecurityConfig`, `WorkboxTokenIntrospector`, `AuditorAwareImpl`, `ClockConfig`,
`MessageConfig`, `OpenApiConfig`, `audit/`) · `domain/` (**cálculo puro**) · `exceptions/`
(+ `handler/`) · `models/{dto,entities,enums}` · `repositories/` · `services/` · `controllers/`.
O contrato é o `openapi/openapi.yaml`.

## Regras de domínio e armadilhas
- **Escopo por dono**: toda entidade tem `owner_username` (de `Authentication.getName()`,
  **nunca do payload**) e toda query filtra por ele. Recurso de outro dono ou de **outra moto**
  (id da moto errado na rota) → **404**, nunca 403 (IDOR, OWASP API1). Todo recurso filho passa
  por `MotorcycleService.exigirDoDono` — mantenha ao criar endpoint novo.
- **Hodômetro é o total** (não o trip) e a linha do tempo da moto é **monotônica**:
  `OdometerService.validarNovo` (usado por abastecimento, troca e leitura avulsa; na edição
  passa `ignorarId`) aplica `OdometerRules`. Registros na **mesma data** não são comparados.
  Toda fonte nova de km precisa entrar em `OdometerService.carregarPontos`.
- **Consumo** (`ConsumptionCalculator`): trecho = `km / litros do abastecimento final`; `exact`
  só entre dois `fullTank`. A média de uma janela é **ponderada** (Σkm / Σlitros) — **nunca média
  de médias**. O trecho pertence à janela do abastecimento que o **termina**. O primeiro
  abastecimento não gera trecho. `lowConfidence` = menos de `MIN_SEGMENTS_CONFIDENT` (3) trechos.
- **Km do período** vem de `OdometerTimeline` (todas as fontes), **não** da soma dos trechos —
  uma leitura avulsa conta km sem alterar o consumo.
- **Próxima troca** (`OilStatusCalculator`, puro): `min(km, tempo)`; limiar "perto" =
  `max(500 km, 10%)` ou `max(30 d, 10%)`. O intervalo é o gravado na troca; `OilType` só carrega
  os padrões sugeridos. "Hoje" vem de `Clock` com o fuso `app.timezone` (padrão
  `America/Fortaleza` — o container é UTC); serviços recebem `hoje` por parâmetro, nunca chamam
  `LocalDate.now()`.
- **Paginação**: só abastecimentos são paginados (`size ≤ 100`, `spring.data.web.pageable.*`,
  `serialization-mode=via_dto`). Motos, trocas de óleo e leituras ficam sem paginação **de
  propósito** (poucas por usuário). **Não** use `@EnableSpringDataWebSupport`: desliga a
  auto-configuração do Boot e, com ela, o limite de página.
- **Auditoria**: `@CreatedBy/...` + Envers (`@Audited`, tabelas `*_aud`) em toda entidade.
  Entidade nova ganha tabela `_aud` no changeset. A FK `motorcycle` dos filhos é
  `NOT_AUDITED`. Só `Motorcycle` expõe `/history`.
- **Autenticação**: `WorkboxTokenIntrospector` chama `POST /api/v1/auth/introspect` (Basic com
  `INTROSPECTION_CLIENT_ID/SECRET`, que precisam bater com uma linha ativa em
  `workbox.api_clients`). Nunca decodificar JWT localmente nem conhecer `JWT_SECRET`.
- **Acesso por módulo**: `SecurityConfig` exige `MODULE_MOTO` em tudo que não é health/Swagger;
  autenticado sem o módulo = **403** (coberto por `ModuleAccessSecurityTest`).
- **Liquibase**: `includeAll` em `db/changelog/v0.0.1/create`; arquivo novo
  `yymmdd_nnnn_<acao>_<alvo>.sql`. **Nunca editar changeset já aplicado.** Backward-compatible
  (expand → migrate → contract); proibido `SELECT *`. FK dos filhos com `ON DELETE CASCADE`.
- Coluna do ano da moto é `model_year` (`year` é palavra reservada no H2 do profile `test`).
- `springdoc.writer-with-order-by-keys=true` — não remover (evita diff falso no CI).

## Convenção Java deste repo
- **`final` obrigatório** em todo parâmetro e variável local (`src/main` e `src/test`), exceto
  reatribuição real. Lombok permitido. Javadoc e nomes de métodos de negócio em **português**.

## Testes (test-first)
- `domain/*Test`: JUnit 5 + AssertJ, sem Spring — a regra de cálculo nasce aqui.
- `controllers/*Test`: `@WebMvcTest` (serviços via `@MockitoBean`), auth simulada com
  `opaqueToken()` — **não** `jwt()`; corpo de erro conferido (`errors[].field`, `detail`).
- `services/*IT` estendem `PostgresIT` (Testcontainers, **exige Docker**): container
  **singleton** iniciado em bloco estático — com `@Container` por classe o Spring reaproveitava o
  contexto em cache apontando para um container já parado. Cada teste usa um **dono único**
  (`"dono-" + UUID`), pois o container é compartilhado.
- Query param obrigatório ausente ou com tipo errado responde **400**, nunca 500 — mantenha ao
  criar handlers.

## Contrato (OpenAPI)
`openapi/openapi.yaml` é a fonte da verdade e o front consome só dele. Mudou rota/DTO/status/auth
→ regenerar (`./gradlew generateOpenApiDocs`) e commitar na mesma mudança (CI
`contract-drift-check`); ajustar `workbox-app` na mesma tarefa quando o contrato observável
mudar. O proxy do front roteia `motorcycles|oil-intervals` pra cá (`vite.config.ts` e
`nginx.conf.template`) — rota nova com prefixo diferente exige ajustar os dois.

## Commits
pt-BR, Conventional Commits, conforme o [CLAUDE.md da raiz](../CLAUDE.md#convenção-de-mensagens-de-commit).
Trabalhar em `develop`; push só com confirmação.
