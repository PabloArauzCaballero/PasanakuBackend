---
name: skills-router
description: Índice maestro del catálogo de skills de Pasanaku — mapea la situación concreta (arrancar una tarea, tocar un endpoint, mover dinero, cruzar un límite de servicio, diseñar una pantalla, escribir un test, desplegar, cerrar un carril) a la skill obligatoria, y fija el orden de precedencia cuando dos se pisan. Usar al empezar cualquier trabajo para saber qué cargar, cuando no sabés si existe una skill para lo que estás por hacer, al revisar si te salteaste un gate antes de cerrar, o al incorporar a alguien al repo.
---

# Router de skills — qué cargar y cuándo

Con casi 200 skills, el cuello de botella no es que falte conocimiento: es **encontrarlo**.
Esta skill es la tabla de ruteo. No enseña nada por sí misma; te manda a la que corresponde.

> [!important] Jerarquía de autoridad
> 1. El `CLAUDE.md` del proyecto en el que estás trabajando (hechos, comandos e invariantes reales).
> 2. Los gates de disciplina (§1): mandan sobre cualquier skill de conocimiento.
> 3. La skill específica del dominio o tecnología.
> 4. Este router.
>
> Si una skill contradice al `CLAUDE.md` del proyecto, **gana el `CLAUDE.md`**. Registrá la
> contradicción para corregir la skill después (`prompt-governance-versioning`).

## 1. Siempre — el ciclo de disciplina

Estas no son opcionales ni dependen del tipo de tarea. Son el esqueleto de todo trabajo.

| Momento | Skill | Qué te impide hacer |
|---|---|---|
| Al recibir la tarea | `outcome-first` | Empezar a leer código sin saber qué resultado observable se busca |
| Antes de investigar | `context-thrift` | Quemar contexto en dumps que no cambian ninguna decisión |
| Antes de escribir | `factual-discovery` | Asumir cómo funciona el sistema en vez de confirmarlo |
| Antes de crear algo nuevo | `anti-hallucination-guard` | Inventar una entidad, endpoint o API que ya existe o no existe |
| Antes del primer edit | `scope-discipline` | Refactorizar, renombrar o "aprovechar" fuera de lo pedido |
| Al escribir código | `native-code-patterns` | Que tu código se note distinto del vecino |
| Durante el trabajo | `progress-reporting` | Silencio largo en un trabajo de varias fases |
| Ante cualquier fallo | `root-cause-debugging` | Reintentar, silenciar o debilitar el test hasta que pase |
| Cuando te oís decir "debería" | `rationalization-guard` | Racionalizar un atajo |
| Antes de decir "listo" | `evidence-and-verification` | Afirmar sin salida literal pegada |
| Si tocaste UI | `visual-proof` | Cerrar sin mirar realmente las capturas |
| Al cerrar el turno | `finish-your-turn` | Devolver con trabajo en alcance a medias |

Si delegás en subagentes: `agent-orchestration` + `agent-resource-control`.

## 2. Por tipo de trabajo

### Backend — API
> [!important] Pasanaku es microservicios.
> Antes de "servicio o endpoint nuevo", pasá por `microservices-architecture` para saber **de
> quién es ese dato**. Si el cambio cruza un límite de servicio, la regla 98 y el bloque de
> microservicios de más abajo son obligatorios.

| Situación | Skills |
|---|---|
| Dónde vive esto · ¿hace falta un servicio nuevo? | `microservices-architecture` |
| Servicio o endpoint nuevo | `backend-development` → `nestjs-development` → `error-handling-contract` |
| Tipar DTO, respuesta, dominio | `typescript-standards` |
| Persistencia, repositorio, transacción | `mikroorm-patterns` · `concurrency-and-locking` |
| Query lenta, índice, DDL con tráfico | `postgresql-advanced` |
| Entidad con ciclo de vida (obligación, pago, entrega, expediente) | `state-machines-workflows` |
| Quién es el actor | `authn-identity` · Quién puede | `authz-access-control` |
| Datos con dueño organizacional | `multi-tenancy` |
| Sacar trabajo del request | `async-messaging-events` · `background-jobs-scheduling` |
| Cachear algo | `caching-strategy` |
| Documentar el contrato | `api-openapi-docs` |
| Instrumentar / investigar sin visibilidad | `backend-observability` · `distributed-tracing-correlation` |
| Llamar a otro servicio | `service-communication-patterns` · `resilience-patterns` |
| El cambio toca un importe | **`money-movement-safety`** (siempre) |

**Capacidades concretas:** notificaciones → `notifications-delivery` · archivos → `file-uploads-media` ·
buscador o listado → `search-and-filtering` · tiempo real → `realtime-websockets` ·
ubicación y cercanía → `maps-geolocation` · contenido de usuarios → `content-moderation-abuse` ·
cobro y QR → `payments-qr-integration` · desembolso → `disbursement-payouts`

### Datos y modelo
| Situación | Skills |
|---|---|
| Agregar tabla, columna, relación | `model-driven-schema` → `data-modeling-plantuml` → `database-design` |
| Catálogo o enum cerrado | `terminology-value-sets` (**antes** de crear un enum) |
| Seeds, catálogos, datos "reales" | `seed-data-catalogs` |
| Validar una carga | `data-quality-validation` |
| Acción sensible que debe dejar rastro | `audit-trail-history` |
| Backups, restore, simulacro | `backup-restore-dr` |
| Script o generador Python | `python-tooling-standards` |

### Frontend web (Angular)
| Situación | Skills |
|---|---|
| Componente, ruta, guard, interceptor | `angular-development` |
| Dónde vive un dato / traer datos | `angular-signals-state` · `frontend-data-access` |
| Cualquier código que toque el DOM | `angular-ssr-hydration` |
| Formulario | `angular-forms` + `frontend-forms-ux` |
| Crear un componente | `atomic-design-components` → `smart-dumb-components` → `component-architecture-solid` |
| Estilos | `css-architecture` · `frontend-design-system` |
| Tests de componente | `angular-testing` |
| Landing / marketing | `astro-development` · `seo-public-pages` |

### Diseño de interfaz
| Situación | Skills |
|---|---|
| Arrancar una pantalla | `frontend-ui-design` → `visual-hierarchy-composition` |
| Tokens, paleta, tipografía | `frontend-design-system` · `color-systems` · `typography-systems` |
| Pulido final / "se ve genérica" | `frontend-beautiful-ui` → `ui-quality-review` (gate) |
| ¿Se entiende? | `ux-clarity-usability` · `ux-writing-microcopy` |
| Estados de la vista | `frontend-ux-states` |
| Responsive | `frontend-responsive-layout` |
| Animación | `frontend-motion` |
| Accesibilidad | `frontend-accessibility` (diseño) + `accessibility-testing` (prueba) |
| Tablas, dashboards, iconos | `frontend-data-tables` · `dashboard-data-ui` · `iconography-imagery` |
| Navegación | `frontend-navigation-ia` |
| Performance / errores en cliente | `frontend-performance` · `frontend-error-monitoring` |
| Seguridad del cliente | `frontend-security` |
| Segundo idioma | `frontend-i18n-l10n` |

### Mobile (Flutter)
`mobile-ux-design` · `flutter-development` · `flutter-state-architecture` · `flutter-theming` ·
`flutter-testing` · `mobile-offline-sync` · `mobile-release-security`

### QA y testing
| Situación | Skills |
|---|---|
| Planificar qué se prueba | `qa-strategy` → `test-plan-authoring` → `qa-orchestration` |
| Diseñar los casos | `test-case-design-techniques` + `edge-case-data-catalog` |
| Datos de prueba | `synthetic-test-data-generation` · `test-data-management` |
| Test unitario | `unit-testing` |
| Endpoint | `api-testing` |
| Integridad de datos y contratos | `integrity-testing` |
| Flujo de usuario | `e2e-playwright` → falla → `e2e-failure-triage` |
| Visual | `visual-regression-testing` |
| Carga | `performance-load-testing` |
| Seguridad automatizada | `security-testing` |
| Mantener la suite | `regression-suite-management` |
| Explorar lo no cubierto | `exploratory-testing` |
| Aceptación del negocio | `uat-acceptance-signoff` |
| Reportar para poder cerrar | `qa-evidence-reporting` |

### Seguridad
| Situación | Skills |
|---|---|
| Gate de cualquier cambio sensible | `security-guardrails` |
| Antes de construir una feature | `threat-modeling` |
| Revisar un PR sensible | `secure-code-review` |
| Evaluación sobre sistemas propios | `pentest-methodology` → `pentest-recon-mapping` → `web-app-pentest` / `api-pentest` / `auth-session-pentest` → `pentest-reporting-remediation` |
| Plataforma GitHub | `github-security-features` |
| Servidor | `server-hardening` |

### Microservicios — **si el cambio cruza un límite de servicio, esto no es opcional**
| Situación | Skills |
|---|---|
| Dónde vive una funcionalidad · crear un servicio | `microservices-architecture` |
| Agregar una llamada entre servicios | `service-communication-patterns` |
| Cambiar un endpoint interno, un evento o un payload | `service-contracts-versioning` |
| Flujo que cambia estado en dos o más servicios | `saga-distributed-transactions` |
| Exponer algo hacia afuera · componer una pantalla | `api-gateway-bff` |
| Timeouts, reintentos, circuit breaker, degradación | `resilience-patterns` |
| Instrumentar o diagnosticar cruzando servicios | `distributed-tracing-correlation` |
| Replicar datos ajenos · panel o listado que cruza servicios | `eventual-consistency-read-models` |
| Qué tests escribir para todo esto | `microservices-testing` |
| Desplegar sin romper a los vecinos · migración con dos versiones vivas | `microservices-deployment` |
| Autenticación entre servicios · webhook entrante | `service-to-service-security` |
| Que los números cuadren entre servicios | `distributed-data-integrity` |

### Dominio dinero — **obligatorias, no opcionales**
Si el cambio calcula, promete, mueve, deduce o reporta un importe:

| Situación | Skills |
|---|---|
| **Cualquier código que toque un importe** | `money-movement-safety` (siempre) + regla 91 |
| Asientos, mayor, plan de cuentas | `accounting-double-entry` |
| Cobro, QR, enlace de pago, webhook de la pasarela | `payments-qr-integration` |
| Conciliar contra el banco o la pasarela | `payment-reconciliation` |
| Entrega, deducciones, desembolso | `disbursement-payouts` |
| Fondo de garantía, coberturas, avales | `guarantee-fund-workflows` |
| Mora, cobranza, sanciones | `collections-delinquency` |
| Disputas, descargos, apelaciones | `dispute-resolution` |
| Cierre diario, reportes y exportes financieros | `financial-close-reporting` |

### Dominio Pasanaku — grupo, identidad y confianza
| Situación | Skills |
|---|---|
| Grupo, cupo, turno, reglamento, acuerdo, sorteo | `rosca-group-lifecycle` |
| Calendario, periodos, vencimientos, recordatorios | `contribution-calendar` |
| Alta de usuarios, documento, niveles de verificación | `kyc-identity-verification` |
| Listas, alertas de cumplimiento, monitoreo | `aml-sanctions-screening` |
| Panel de transparencia, score, certificados | `transparency-reputation` |
| Búsqueda de grupos, postulación, emparejamiento | `group-discovery-matching` |
| Consentimiento antes de exponer a alguien | `consent-management` |
| Catálogos cerrados (monedas, bancos, motivos, estados) | `terminology-value-sets` |
| Mapear una exigencia normativa a un control | `regulatory-compliance-mapping` |
| Privacidad de datos personales y financieros | `data-privacy-financial` (gate, siempre) |

### Calidad de código
| Situación | Skills |
|---|---|
| Escribir o revisar código | `clean-code` · `solid-principles` |
| Diseñar clases o módulos | `solid-principles` · `code-efficiency` |
| Revisar un PR | `code-review-standard` |
| Configurar el gate | `code-quality-gates` · `static-analysis-linting` |
| Elegir qué refactorizar | `code-complexity-metrics` → `technical-debt-management` |
| Refactorizar | `refactoring-safely` |
| Limpiar | `dead-code-duplication` |
| Auditar un repo | `code-quality-audit` |

### Proceso, git y entrega
| Situación | Skills |
|---|---|
| Definir la unidad de trabajo | `lane-authoring` → `requirements-and-acceptance` → `vertical-slicing` |
| Reportar un bug | `bug-reporting-standard` |
| Ramas y commits entre repos | `git-workflow-multirepo` |
| PR | `github-pull-requests` · `github-multirepo-coordination` |
| Issues y tablero | `github-issues-projects` |
| Proteger ramas | `github-branch-protection-rulesets` |
| CI | `ci-cd-pipeline` (diseño) → `github-actions-ci` (implementación) |
| `gh` desde terminal | `github-cli-automation` |
| Repo nuevo | `github-repo-standards` |
| Versionar y publicar | `github-releases-versioning` · `release-and-rollback` |
| Dependencias | `dependency-management` |
| Documentar una decisión | `technical-docs-and-adr` |
| Se cayó producción | `incident-response-postmortem` |

### Despliegue y entorno
| Situación | Skills |
|---|---|
| Desplegar en Coolify | `coolify-deployment` → `deployment-verification-smoke` |
| Operar / diagnosticar | `coolify-operations` |
| Base y backups | `coolify-databases-backups` |
| Imagen de producción | `dockerfile-production` |
| Stack local | `docker-local-stack` |
| Config y secretos | `environment-secrets-config` |
| Máquina de desarrollo | `windows-dev-environment` |

### Trabajar sobre este repo de prompts
`skill-authoring` (escribir una skill) · `prompt-engineering` (redactar el cuerpo) ·
`prompt-evals` (probar que el cambio no rompió nada) · `prompt-governance-versioning` (publicarlo) ·
`subagent-design` · `hooks-and-guardrails` · `claude-md-authoring`

## 3. Precedencia cuando dos se pisan

| Choque | Manda |
|---|---|
| Gate de disciplina vs skill de conocimiento | El **gate** |
| Conveniencia de implementación vs regla 91 (dinero) | **La regla 91** |
| Skill de capa vs regla 98 cuando el cambio cruza servicios | **La regla 98** |
| `money-movement-safety` vs cualquier skill de feature | **`money-movement-safety`** |
| `data-privacy-financial` vs cualquier skill de feature | **`data-privacy-financial`** |
| `security-guardrails` vs conveniencia de implementación | **`security-guardrails`** |
| Skill general vs skill específica de la tecnología | La **específica** (`backend-development` < `nestjs-development`) |
| Diseño vs prueba del mismo tema | Ambas, en orden: diseño primero (`frontend-accessibility` → `accessibility-testing`) |
| Cualquier skill vs `CLAUDE.md` del proyecto | El **`CLAUDE.md`** |

## 4. Los siete gates que bloquean un cierre

Ninguno se puede declarar cumplido sin salida literal pegada:

1. `evidence-and-verification` — se ejercitó el artefacto real.
2. `security-guardrails` — si el cambio toca autorización, datos o entrada de usuario.
3. `data-privacy-financial` — si toca datos de personas o importes atribuibles.
4. **`money-movement-safety`** — si toca un importe: las cinco evidencias de la regla 91.6.
5. **`microservices-testing`** — si cruza servicios: duplicado, dependido caído y contrato
   (regla 98.8).
6. `visual-proof` — si toca UI.
7. `qa-evidence-reporting` — el reporte tiene comandos, salida y "No cubierto".

## Checklist

- [ ] Cargué las skills de disciplina del ciclo (§1), no solo la técnica del tema.
- [ ] Busqué acá antes de improvisar: si existe skill para lo que voy a hacer, la uso.
- [ ] Si el cambio toca datos de personas, cargué `data-privacy-financial` aunque nadie lo pidiera.
- [ ] Si el cambio toca un importe, cargué `money-movement-safety` y leí la regla 91.
- [ ] Si el cambio cruza un límite de servicio, leí la regla 98 y cargué lo que corresponde de §2.
- [ ] Si dos skills se contradicen, apliqué la precedencia de §3 y registré el choque.
- [ ] Antes de cerrar, pasé por los gates de §4 que aplican.
- [ ] Si una skill me faltó, la anoté para crearla con `skill-authoring`.
