# Reporte — Tableros por rol, catálogos, pruebas con Faker y OpenTelemetry

> **AVANCE: 16 / 80 — 20,0 %.**

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
| H1.S2.M0a | `aportes.aporte_cobrado` publica `tipo` | `./gradlew :servicios:aportes:integrationTest --tests '*CU21*'` | PASS — `evidencia/H1-S2-M0a-tipo-en-evento.txt` |
| H1.S2.M0b | `nucleo-financiero` lee `tipo` opcional (compatibilidad con el productor viejo) | `ConsumidorDeAportesKafkaTest` + consumo de aportes | PASS — `evidencia/H1-S2-M0b-consumidor-tipo.txt` |
| H1.S2.M0c | Billetera de sistema `FONDO_GARANTIA` por moneda, sembrada | siembra doble en Postgres descartable (855 → 0 filas) | PASS — `evidencia/H1-S2-M0c-semilla-fondo.txt` |
| H1.S2.M0d | `RECARGO_MORA` en `TipoTransaccion` y `TipoMovimientoFondo`, con `CK_AMPLIADOS` | `generar_ddl.py` + reaplicar sobre base existente + gates del CI | PASS — `evidencia/H1-S2-M0d-esquema-recargo.txt` |
| H1.S2.M1 | El recargo cobrado va a la billetera del fondo; asiento DEBE `SALDO_USUARIOS` / HABER `RESERVA_RESPALDO`; el pozo no se mueve | `CUConsumoDeRecargoTest` + regresión de nucleo | PASS — 7/7; nucleo 84+482 en verde; `evidencia/H1-S2-M1-recargo-al-fondo.txt` |
| H1.S2.M1b | Sin billetera del fondo: AP-CU12-02 sin marcar (no cae en el pozo) | `sinBilleteraDelFondo` | PASS |
| H1.S2.M1c | `garantia` anota el ingreso en el fondo del grupo | `CUIngresoAlFondoTest` + regresión de garantia | PASS — 5/5; garantia 38+159 en verde; `evidencia/H1-S2-M1c-garantia-anota.txt` |
| H1.S2.M2 | Idempotencia de punta a punta (duplicado y republicación) | tests `duplicado`/`reintento` en nucleo y garantia | PASS |


## A medias

### H1.S2.M3 — Reversa del recargo
- Qué anda: CU-14 sobre una transacción `RECARGO_MORA` devuelve Bs 25 a la billetera del participante y los descuenta de la del fondo, con una transacción `REVERSO`; la original conserva sus 2 movimientos (`CUConsumoDeRecargoTest.reversaDelRecargo`, PASS).
- Qué no anda: (a) CU-14 no genera contra-asiento en el mayor, para ninguna transacción (preexistente: `CU14ReversarTransaccion` no llama a CU-24); (b) `garantia` no se entera de la reversa y su `saldo_disponible` queda +25.
- Qué falta exactamente: (b) es H1.S2.M4 (consumir `transaccion_reversada` en garantia). (a) necesita decidir si CU-14 asienta el espejo para todas las transacciones: cambia el comportamiento de todas las reversas y está fuera de este trabajo.
- Dónde quedó: rama `pablo/feat/tableros-por-rol-2026-10-16`; compila; los tests en verde.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M4 y H2 a H9 (63 microtareas) | TODO | Siguiente: H1.S2.M4 (reversa en garantia) y después H2 (semillas y Faker) |

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

- H1.S2.M0c: el primer diseño (una billetera del fondo por grupo) violó R-BIL-05 y rompió 7 tests existentes de apertura. Se revirtió (volvieron a 9/9 y 2/2) y se rediseñó como billetera de sistema por moneda, que es lo que el modelo define.
- H1.S2.M1b: sin billetera del fondo se rechaza sin marcar (AP-CU12-02), siguiendo el patrón de la billetera del grupo, en lugar de compensar.
- Pruebas de CU-21 renombradas con los prefijos que acepta `verificar_criterios.py`. El primer intento las dejó sin criterio en la bóveda (gate en rojo, corregido).

## Riesgos residuales

- **Hallazgo H1-02 (dinero):** antes de este trabajo no existía ningún camino de dinero hacia el fondo de garantía (sin billetera sembrada; `consumo-de-asientos` apagado por omisión). Se armó solo para `RECARGO_MORA`. `APORTE_FONDO_GARANTIA` y `REPOSICION_COBERTURA` siguen cayendo al pozo del grupo: decisión aparte.
- **Desplegar H1.S1 (el recargo diario) solo junto con H1.S2**, y con `RESERVA_RESPALDO` mapeada en el entorno. Sin mapeo, el pago del recargo se rechaza (AP-CU24-02) y queda reprocesándose. En TEST el mapeo es sintético (`9.1.02`).
- CU-14 no asienta el espejo en el mayor (ver A medias).
- **Hallazgo H1-03:** el índice `uq_cuenta_billetera_titular_moneda` que nombran `scripts/generar_errores.py` y los comentarios de `DestinoRepositorio` no existe en `sql/`. Existen `uq_cuenta_grupo_moneda` y el de usuario. Solo se anota.

- **Hallazgo H0-01 (dinero, `DISCOVERED`, sin reproducir en runtime):** el evento `aportes.aporte_cobrado` no publica el `tipo` de la obligación, y `ConsumoDeAportes` asienta todo cobro hacia la billetera del grupo. Un `RECARGO_MORA`, `APORTE_FONDO_GARANTIA`, `REPOSICION_COBERTURA` o `COMISION_PLATAFORMA` cobrado por esa vía terminaría en el pozo del grupo. `RECARGO_MORA` se corrige en H1.S2. Los otros tres quedan fuera de alcance y necesitan una decisión aparte.

- El patrón de Atlas (un servicio de reporting que lee las bases hermanas) **no se puede copiar** porque
  contradice la regla 98.1.1. El PLAN lo adapta a endpoints por servicio dueño; ver A5.
- El recargo ya se genera todos los días, pero hasta cerrar H1.S2, al pagarlo **iría al pozo del grupo** (hallazgo H0-01). No hay que desplegar H1.S1 sin H1.S2.

## Decisiones y ambigüedades

Ver «Decisiones tomadas con Pablo» y «Ambigüedades registradas» (A1 a A5) en el PLAN.
