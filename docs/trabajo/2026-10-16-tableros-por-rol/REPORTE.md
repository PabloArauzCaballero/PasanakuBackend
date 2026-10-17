# Reporte — Tableros por rol, catálogos, pruebas con Faker y OpenTelemetry

> **AVANCE: 8 / 75 — 10,7 %.**

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `pablo/feat/tableros-por-rol-2026-10-16`
  (worktree `../Pasanaku-tableros`, desde `origin/dev` `d63e4fef`)
- Peldaño de evidencia alcanzado: H0 `DISCOVERED`; H1.S1 `TESTED` (integración contra Postgres real)

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H0.S1.M1 | Mapa operación → asiento del mayor | `grep -rln -iE "asiento|registrarAsiento|contabiliz" servicios/*/src/main/java` | PASS — `evidencia/H0-01-asientos.md` |
| H0.S1.M2–M6 | Patrón de lectura con `@Permiso`, tablas de billetera, A5 resuelta, guardias del backoffice y versiones verificadas | ver evidencia | PASS — `evidencia/H0-02-a-06.md` |
| H1.S1.M1 | Disparo diario `RecargosDeMoraDiarios` (`@Scheduled` + ShedLock, contexto de sistema) | `./gradlew :servicios:aportes:integrationTest --tests '*CU21*' --tests '*ArranqueTest'` | PASS — 11 suites de CU-21 con 0 fallas + `ArranqueTest` 2/2; `evidencia/H1-S1-recargos-diarios.txt` |
| H1.S1.M2 | Dos corridas el mismo día → un solo recargo | ídem (`dosCorridasElMismoDia`) | PASS |

## A medias

ninguna.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2 a H9 (67 microtareas) | TODO | Siguiente: H1.S2.M0a (`tipo` en el evento `aporte_cobrado`) |

Corrección: en la sesión se le dijo a Pablo que no había fondo de inversión. Existe en `dev` (servicio
`inversiones`) y se agregó como H4.S1.M8.

## Evidencia

- Decisiones D1 a D12, tomadas con Pablo una por una el 2026-10-16: registradas en el PLAN.
- Patrón de referencia leído en los clones locales de Atlas (`~/Documents/GitHub/atlas`):
  `AtlasDashboardsBackend/README.md`, `AtlasDashboardsBackend/src/modules/kpi-catalog/kpi-definition.types.ts`,
  `AtlasBackend/docs/endpoints/workflow-catalog.md`, `AtlasBackend/docs/data/entity-catalog.md` y
  `AtlasBackend/docs/observability/01-architecture-design.md`.
- Hallazgos en Pasanaku citados en el PLAN: `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/RecargoDeMora.java`,
  `servicios/aportes/README.md:50`, `sql/60_semillas/01-plan-de-cuentas.sql:103`,
  `sql/60_semillas/10-roles-y-permisos.sql`, `sql/10_tablas/02_grupos_turnos/turno.sql` y `gradle/libs.versions.toml:89-90`.

## No cubierto

- `RecargosDeMoraDiarios` se probó llamando a `correr()`, no esperando al reloj: el cron y el lock de ShedLock entre réplicas no se ejercitaron.
- Hallazgo H0-05 (fuera de alcance): `guardia-rol-sistemas.ts` admite los roles `PLATAFORMA` y `SEGURIDAD`, que no existen en el catálogo.

## Desvíos del plan

ninguno.

## Riesgos residuales

- **Hallazgo H0-01 (dinero, `DISCOVERED`, sin reproducir en runtime):** el evento `aportes.aporte_cobrado` no publica el `tipo` de la obligación, y `ConsumoDeAportes` asienta todo cobro hacia la billetera del grupo. Un `RECARGO_MORA`, `APORTE_FONDO_GARANTIA`, `REPOSICION_COBERTURA` o `COMISION_PLATAFORMA` cobrado por esa vía terminaría en el pozo del grupo. `RECARGO_MORA` se corrige en H1.S2. Los otros tres quedan fuera de alcance y necesitan una decisión aparte.

- El patrón de Atlas (un servicio de reporting que lee las bases hermanas) **no se puede copiar** porque
  contradice la regla 98.1.1. El PLAN lo adapta a endpoints por servicio dueño; ver A5.
- El recargo ya se genera todos los días, pero hasta cerrar H1.S2, al pagarlo **iría al pozo del grupo** (hallazgo H0-01). No hay que desplegar H1.S1 sin H1.S2.

## Decisiones y ambigüedades

Ver «Decisiones tomadas con Pablo» y «Ambigüedades registradas» (A1 a A5) en el PLAN.
