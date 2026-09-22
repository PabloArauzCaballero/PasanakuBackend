# Carril PR2 — Núcleo financiero (Justin)

> **Avance calculado:** ver `repartos/2026-09-21/PromptNoche/Backend/Justin/Justin-Daily-Noche-2026-09-21.md`
> (HECHO/total). Este documento es la bitácora técnica del carril; el daily es el resumen de cierre.
>
> **Rama:** `justin/feature/carril-PR2-nucleo-financiero` desde `origin/dev`. PRs: #7, #8, #11 (mergeados),
> #21 (H2 final + H4.S2).

## H1 — Idempotencia del núcleo con el scope exacto de su índice

- **Problema:** `LibroDeBilletera.porClaveIdempotencia`, `OrdenRetiroRepositorio.porClaveIdempotencia`
  y `OrdenRecargaRepositorio.porClaveIdempotencia` buscaban solo por `clave_idempotencia`, sin las
  columnas restantes de sus índices únicos (`uq_tx_idem`, `uq_retiro_idem`, `uq_recarga_idem`).
- **Causa raíz:** el lookup no replicaba el scope real del índice (`COALESCE(iniciada_por,…),
  origen_tipo, clave` en transferencias; `cuenta_billetera_id, clave` en retiro/recarga).
- **Solución:** los tres métodos ahora reciben el scope completo y lo usan en el `WHERE`
  (`servicios/nucleo-financiero/src/main/java/.../LibroDeBilletera.java`,
  `.../OrdenRetiroRepositorio.java`, `.../OrdenRecargaRepositorio.java`). Bug adicional hallado en el
  camino: el replay de un retiro devolvía `entrada.costo()` (costo de la petición repetida) en vez del
  `orden.costo()` almacenado — corregido, con `Orden.costo` agregado al repositorio.
- **Test:** `CU10Test`, `CU11Test`, `CU12Test` (dos titulares/cuentas distintas con la misma clave →
  transacciones distintas; mismo titular → misma transacción/orden; replay del costo almacenado) +
  `CU12ConcurrenciaTest` (50 hilos, misma clave, misma cuenta).
- **Evidencia:** `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU10Test*' --tests
  '*CU11Test*' --tests '*CU12Test*'` → BUILD SUCCESSFUL (37 tests, 0 fallos);
  `--tests '*CU12ConcurrenciaTest*'` → BUILD SUCCESSFUL (1 transacción nueva, suma de saldos
  preservada). Gate completo del módulo (`spotlessApply :servicios:nucleo-financiero:webTest
  :servicios:nucleo-financiero:integrationTest spotlessCheck`) → BUILD SUCCESSFUL.
- **Riesgo residual:** ninguno conocido. `pg_advisory_xact_lock` (PR #8, endurecimiento de la
  concurrencia bajo reintentos simultáneos) ya está mergeado sobre esta base.

## H2 — Retiro exige evidencia MFA step-up real; sin bypass en producción

- **Problema:** `SegundoFactorLocal` (adaptador de desarrollo, siempre aprueba) era alcanzable en
  cualquier perfil, incluido `production`.
- **Causa raíz:** faltaba un adaptador real que validara la evidencia emitida por `identidad`, y el
  adaptador de desarrollo no estaba confinado por `@Profile`.
- **Solución:** `SegundoFactorLocal` confinado a `@Profile({"local","test"})`.
  `SegundoFactorStepUp` (único adaptador fuera de `local/test`) valida firma y vigencia del JWT vía el
  `JwtDecoder` común, valida a mano `iss`/`aud`/`sub`/`proposito=RETIRO`/`acr=mfa`, y consume el `jti`
  una sola vez contra `nucleo_financiero.evidencia_mfa_consumida` (`INSERT … ON CONFLICT DO NOTHING`;
  tabla generada por el pipeline real `scripts/modelo.py`/`.puml` → `generar_ddl.py`, no a mano).
  Doble de `identidad` en tres niveles (`EmisorDeEvidenciaDePrueba`: RSA de prueba + JWKS real servido
  por HTTP), bajo `src/test`. `ArranqueProduccionTest`: perfil `production` arranca con el adaptador
  real; `doble-local=true` forzado en `production` no tiene efecto.
- **Test:** `SegundoFactorStepUpTest` (evidencia ausente/firma ajena/vencida/otro `sub`/otro
  `proposito`/`jti` ya consumido/válida) + `ArranqueProduccionTest`.
- **Evidencia:** 12/12 tests verdes; gate completo del módulo → BUILD SUCCESSFUL.
- **Riesgo residual — declarado, no cerrado:** el consumo del `jti` corre en su propia transacción
  corta, no dentro de la misma transacción que `CU11.solicitar`, porque hoy `SegundoFactor.verificado`
  se invoca desde el controlador antes de que el caso de uso abra transacción. El `jti` nunca se
  reutiliza (no es un hueco de seguridad), pero si el retiro falla después por otra razón, la evidencia
  ya quedó consumida — peor UX, no inconsistencia de datos. Cerrarlo requiere cambiar la firma de
  `SegundoFactor` (cascada no abordada este turno). Integración real con el JWKS de `identidad` (en vez
  del doble) queda diferida a cuando el PR de Richard esté en `dev`.

## H3 — Doble aprobación de retiros (parcial: solo dominio)

- **Problema:** el `CHECK` de base de datos era la única barrera para la doble aprobación; la
  aplicación no tenía una máquina de estados explícita.
- **Causa raíz:** no existía un tipo `EstadoDeRetiro` que modelara las transiciones válidas
  (`PENDIENTE→{EN_REVISION,AUTORIZADA,RECHAZADA}`, `EN_REVISION→{AUTORIZADA,RECHAZADA}`,
  `AUTORIZADA→{EN_PROCESO,RECHAZADA}`, `EN_PROCESO→{PAGADA,RECHAZADA}`, `PAGADA→REVERSADA`).
- **Solución (parcial):** `EstadoDeRetiro` (dominio puro, sin Spring) con `puedePasarA` y la tabla
  completa de transiciones válidas e inválidas.
- **Test:** `EstadoDeRetiroTest` (7 casos, tabla completa de transiciones).
- **Evidencia:** `./gradlew :servicios:nucleo-financiero:test --tests '*EstadoDeRetiroTest*'` → BUILD
  SUCCESSFUL, 7/7.
- **Riesgo residual — A MEDIAS declarado:** `EstadoDeRetiro` todavía NO está cableado en
  `CU11RetirarSaldo.solicitar` (sigue creando toda orden como `PENDIENTE` fijo, no decide
  `EN_REVISION` vs `AUTORIZADA` según `aportaya.retiro.doble-aprobacion-desde`) ni en `confirmarPago`
  (sigue exigiendo `PENDIENTE` en vez de `EN_PROCESO`). El endpoint de aprobación
  (`POST /billetera/retiros/{ordenId}/aprobacion`), el permiso `RETIRO_APROBAR` → rol `TESORERIA`, las
  carreras entre aprobadores y el ADR-049 (que documenta la decisión, no la implementación completa)
  quedan pendientes de un turno siguiente.

## H4 — Proveedor de retiros: estados, resiliencia, propiedad y métricas (H4.S2 completo; H4.S1 y H4.S3 restante)

- **Problema:** no existía un puerto para el proveedor externo de retiros, ni reconciliación de
  órdenes que quedan `EN_PROCESO` tras un timeout, ni clientes HTTP resilientes, ni verificación de
  que el mecanismo de autorización realmente aplica ownership bajo el rol de producción.
- **Causa raíz / hallazgo crítico:** las pruebas de autorización negativa existentes usaban la
  conexión `BaseDeBilletera.dsl`, que corre con el rol DUEÑO de la base — ese rol **bypasea RLS
  incondicionalmente**, así que cualquier prueba contra él pasa sin verificar nada real. Reescribiendo
  `AutorizacionNegativaTest` con `SET ROLE svc_nucleo_financiero` + `set_config` real (patrón de
  `AislamientoEsquemaTest`), se descubrió que `orden_retiro` y `orden_recarga` **no estaban en la
  lista `visibles_por_titular` de `fn_seg_aplicar_rls`**: bajo el rol de producción real, ni siquiera
  el propio dueño podía crear o ver su propia orden de retiro/recarga. `CU-10` y `CU-11` — las dos
  operaciones centrales del servicio — estaban rotas para cualquier participante real, enmascarado
  enteramente por el arnés de pruebas anterior.
- **Solución:**
  - `ProveedorDeRetiro` (puerto) + `ProveedorDeRetiroLocal` (doble en tres niveles: acepta / rechaza /
    demora — el monto `111.11` marca el caso de timeout — más `resolverComoSiElProveedorHubieraContestado`
    como puerta de prueba), bajo `local/test`.
  - `ReconciliacionDeRetirosTest` / `ReconciliacionDeRetiros` (`@Scheduled` cada 5 min por defecto),
    resuelve órdenes `EN_PROCESO` preguntando al proveedor y aplicando `confirmarPago`/`rechazar` vía
    `CU11`, cada orden en su propia transacción (nuevo `CU11.ordenesEnProceso`, `@Transactional` de
    solo lectura, como punto de entrada para un job sin transacción ambiente).
  - `resilience4j` (`@Retry` + `@CircuitBreaker`) real en `CotizadorPorHttp`, con fallback y config en
    `application.yml`. Requirió agregar `spring-boot-starter-aop` (verificado: sin el starter, un 503
    se propagaba sin reintentar — las anotaciones no hacían nada).
  - **Fix del hallazgo crítico:** `orden_retiro` y `orden_recarga` agregadas a la lista
    `visibles_por_titular` (`docs/Restricciones.md`, regenerado a `sql/40_reglas/restricciones.sql`).
    Con el cambio: el dueño ve/crea su orden, el atacante no.
  - Métricas de negocio (Micrometer) en `CU11`: `withdrawal_requested_total`,
    `withdrawal_approved_total`, `withdrawal_failed_total`.
  - Runbooks: `docs/operacion/provider-timeout.md`, `docs/operacion/withdrawal-reconciliation.md`.
- **Test:** `AutorizacionNegativaTest` (IDOR, reescrito con rol real), `ReconciliacionDeRetirosTest`,
  `ClientesResilientesTest` (equivalente para `CotizadorPorHttp`).
- **Evidencia:** gate completo del módulo — 248 tests, 2 rojos **intencionales** en
  `ArranqueProduccionTest` (Q-04: `nucleo-financiero` con perfil `production` y sin adaptador real de
  proveedor no arranca — es el comportamiento esperado, no una regresión), resto verde.
- **Riesgo residual — declarado, no cerrado:** otras 7 tablas de `nucleo_financiero` tienen el MISMO
  patrón deny-by-default sin revisar caso por caso: `bloqueo_saldo`, `certificado_saldo`,
  `consumo_limite`, `estado_cuenta_billetera`, `evaluacion_antifraude`, `movimiento_billetera`,
  `retencion_saldo`, `saldo_diario_billetera`, `solicitud_cierre_billetera`. Algunas pueden ser
  intencionales (p. ej. `evaluacion_antifraude`, que un titular no debería poder leer directamente) —
  necesitan revisión individual, no se tocaron este turno. H4.S1 (baseline formal del módulo) y
  H4.S3.M2-M4 (regresión final documentada como tal, aunque el gate ya corrió completo) quedan como
  cierre administrativo pendiente.

## Pendiente de este carril (no cubierto este turno)

H2.S2.M5 (contrato `evidenciaMfa`/`MFA_INVALIDO` en el catálogo, regenerar clientes), H2.S3.M2 (E2E
sobre compose), H2.S4.M2 (integración real con el JWKS de `identidad` cuando su PR esté en `dev`),
H3.S1.M2/M3 (cablear `EstadoDeRetiro` en `CU11`), H3.S2-S4 completo (endpoint de aprobación, permiso
`RETIRO_APROBAR`, carreras entre aprobadores, ADR-049 como implementación y no solo como decisión).
