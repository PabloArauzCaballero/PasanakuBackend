# Plan — Recarga de saldo simulada de punta a punta, con puertos y adaptadores

- Fecha: 2026-10-24 · Repos afectados: PasanakuBackend (`servicios/nucleo-financiero`, `apps/movil`, `packages/diseno_flutter`, `apps/backoffice`, `docker-compose.coolify.yml`, `seeders/`, `sql/`) · Predecesor: `docs/trabajo/2026-10-18-app-aprobacion-recarga-invitar/`
- Ejecuta: la sesión de Pasanaku, en una rama nueva desde `dev`, con PRs nuevos y **sin fusionar** (el merge es humano).
- Origen: cuatro agentes de planificación en paralelo (backend, pasarela simulada, app y backoffice, inventario). Este plan consolida los tres primeros; el inventario de pendientes viejos va en [PENDIENTES-HEREDADOS.md](./PENDIENTES-HEREDADOS.md).
- **Resultado observable:** en TEST, un participante pide recargar 250 BOB en la app y ve un QR del banco con ese monto exacto. QA marca el cobro como pagado desde el backoffice. Sin que nadie toque nada más, el sistema le pregunta al banco, ve el pago y acredita una sola vez. La app pasa a «Acreditado» con el saldo nuevo, y en el backoffice la orden aparece con su asiento balanceado.
- **Kill-test:** pagar el QR simulado desde el backoffice y que, 60 s después, la app siga en «esperando» o el saldo no haya cambiado; o que, pagado dos veces, el saldo suba dos veces.

## Decisiones del usuario (Justin, 2026-10-24)

| # | Decisión | Consecuencia en el plan |
|---|---|---|
| U1 | El QR lo genera **el banco, con el monto exacto**. | El puerto `ProveedorDeRecargas` emite el cobro y devuelve el QR del banco. El adaptador simulado imita al banco. |
| U2 | **Se le pregunta automáticamente a la API del banco** si llegó el pago. | Confirmación por **consulta programada** del sistema, no por webhook ni por un botón del titular. El webhook queda **fuera de alcance**. |
| U3 | Lo que no cuadra (monto distinto, etc.) **se declara como problema**. | Nunca se acredita en automático algo anómalo: se registra la discrepancia, la orden no se acredita y queda para revisión humana. |

Supuestos derivados de U1–U3 (aplicar así; si algo resulta distinto, registrarlo como ambigüedad):

- **Quién acredita:** solo el sistema, con su trabajo de consulta y contexto de sistema. Se elimina el requisito de «el que confirma es el titular». El `POST …/acreditacion` manual queda solo para tesorería, como forzar una consulta, y nunca acredita sin la confirmación del banco.
- **Pago con la orden ya vencida, o que excede `SALDO_MAXIMO`:** se trata como problema (U3). Se registra una discrepancia, sin acreditar, y se emite una alerta. Hoy el exceso de saldo deja la orden en `PENDIENTE` con el dinero cobrado: eso se corrige.
- **Pagar desde la app:** no. Se simula el pago solo desde el backoffice (QA).
- **Simulación en TEST:** sí, porque es lo que el usuario pidió para probar. Va con tres candados: el modo por configuración, el permiso `PASARELA_SIMULAR` y una guardia de arranque que impide que corra en producción o en staging.
- **Estructura hexagonal (pedido explícito):** los puertos de persistencia se aceptan. Se registra la enmienda a ADR-033 (H0).
- **Cuentas del asiento espejo (CU-24):** se reusan las que ya usa el circuito de custodia y billetera (modelo `ConsumoDeAportes.java:201-223`). Se declaran en el reporte para que contabilidad las valide.

## Alcance

- **IN:**
  - Dominio y aplicación de la recarga en estructura hexagonal.
  - Puerto del banco con QR de monto exacto, y adaptador del banco simulado.
  - Trabajo de consulta y acreditación, y expiración automática.
  - Asiento espejo CU-24 y moneda en los eventos.
  - `GET` de la orden.
  - Endpoint de QA para simular pagos.
  - En la app: QR, seguimiento y resultado. En el backoffice: bandeja, ficha con asiento y panel de simulación.
  - Configuración de TEST en Coolify.
  - E2E y prueba visual.
- **OUT:**
  - Webhook de pasarela (U2).
  - Adaptador del banco real: solo se deja el puerto listo.
  - Enlace `orden_recarga.pago_id` con la tabla `pago` de `aportes`, que es otro servicio (regla 98.1).
  - Notificación push al acreditar: la app consulta.
  - Quién absorbe el costo del proveedor en recargas gratuitas: se mantiene el tarifario actual.
- **Ambigüedades registradas:**
  - A1. La cadencia de consulta al banco: se toma 10 s en TEST. Confirmar con Justin.
  - A2. Las cuentas contables del espejo: confirmar con contabilidad.
  - A3. El estado en memoria del banco simulado: se pierde al reiniciar y exige una sola réplica en TEST. Queda declarado como riesgo.

## Hallazgos de partida (con ruta)

Rutas relativas a `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/`, salvo que se indique otra.

- **La recarga ya existe y tiene pruebas:**
  - `aplicacion/CU10RecargarSaldo.java`: `solicitar` en :77, `acreditar` en :169 y `expirarVencidas` en :262.
  - `aplicacion/RecargasConProveedor.java`: la consulta al proveedor está en :82-126.
  - Unas 10 clases de prueba `CU10*`.
- **No es hexagonal:** `CU10RecargarSaldo.java:6-9` importa `infraestructura`, y los estados son `String`.
- **Defecto: `acreditar` exige que quien confirma sea el titular** (`CU10RecargarSaldo.java:187-188`). En cambio, el endpoint exige `TESORERIA` (`web/BilleteraController.java:145`). Un actor de sistema no puede acreditar.
- **Faltan:**
  - El asiento espejo CU-24.
  - La moneda en los eventos (:130-139, :232-240).
  - El trabajo de expiración (nadie llama a `expirarVencidas`).
  - El `GET` de la orden.
  - El QR en `SalidaRecarga` (`openapi/nucleo-financiero.yaml:1240-1249`).
- **El puerto existe:** `dominio/puertos/ProveedorDeRecargas.java:8-14`. El adaptador `infraestructura/RecargasProveedorSimulado.java` llama al simulador Python `herramientas/proveedor_simulado/`.
- **En TEST hoy no se puede confirmar ninguna recarga:**
  - `docker-compose.coolify.yml:562` no define `PROVEEDOR_*`.
  - TEST corre con el perfil `local` (`despliegue/TEST.md:87-91`), así que un `@Profile("local")` solo **no** alcanza como candado.
- **App:**
  - `apps/movil/lib/pantallas/billetera/pantalla_recargar.dart:15-19` tiene un selector QR, tarjeta o transferencia, y contradice el flujo §4.2.
  - Sin QR, sin seguimiento y sin desenlaces.
  - `lib/dominio/cu10_recargar_saldo.dart` no cotiza, y el backend exige cotización (`CotizarOperacion.java:74,119-122`), así que responde `AP-CU10-06`.
  - No hay puertos de datos.
- **Backoffice:**
  - Solo muestra el saldo (`apps/backoffice/src/app/rutas/operacion/billetera/pantalla-de-billetera.ts`).
  - Tiene un patrón hexagonal para copiar: `rutas/sistemas/dominio/puertos.ts`, `fileReplacements` en `angular.json`, y las pruebas `produccion-honesta.spec.ts` y `sin-simulado-en-pantallas.spec.ts`.

## Interfaces compartidas (el contrato entre capas; se fija en H1 y no se cambia sin tocar este plan)

```java
// dominio/puertos/ProveedorDeRecargas.java — el «banco»
public interface ProveedorDeRecargas {
  CobroEmitido emitirCobro(SolicitudDeCobro s);        // idempotente por referencia; QR del banco con monto exacto
  ConfirmacionDeRecarga consultar(UUID referencia);    // PENDIENTE | PAGADO(monto, moneda, transaccionBanco, pagadoEn) | RECHAZADO(motivo)
  record SolicitudDeCobro(UUID referencia, Dinero monto, OffsetDateTime venceEn) {}
  record CobroEmitido(UUID referencia, String payloadQr, OffsetDateTime venceEn, boolean simulado) {}
}
```

- **Endpoints:**
  - `POST /billetera/recargas`: devuelve `SalidaRecarga`, que **suma** `qr {payloadQr, expiraEn, simulado}`, `estado` como enum y `cotizacionId`. Es un cambio aditivo.
  - `GET /billetera/recargas/{ordenId}`: devuelve `{estado, acreditara, saldoDespues?, motivo?, actualizadoEn}`. Solo para el titular; un tercero recibe **404**.
  - `GET /operacion/recargas?estado&cursor` y `GET /operacion/recargas/{id}`: orden, consultas al banco, discrepancias y asiento `lineas[{cuenta, debe, haber}]`. Requieren el permiso `RECARGA_VER`.
  - `POST /simulacion/banco/cobros/{ordenId}/pagos {escenario, montoInformado?}` con `Idempotency-Key`. Requiere el permiso `PASARELA_SIMULAR`. Sin el modo simulado responde **404**.
- **Eventos:** `nucleo_financiero.recarga_{solicitada,acreditada,rechazada,expirada}` con `{cuentaBilleteraId, monto, moneda}`, y `discrepancia_detectada`, que ya existe. Ninguno lleva PII.
- **Escenarios del banco simulado:**

  | Escenario | Nivel | Resultado esperado |
  |---|---|---|
  | `PAGADO` | correcto | `ACREDITADA`, una vez, con asiento espejo |
  | `PAGADO_DOS_VECES` | límite | El banco informa dos veces y hay dos consultas en paralelo: un solo movimiento y un solo asiento |
  | `PAGADO_AL_LIMITE` | límite | Pago 1 s antes de `expiraEn`: se acredita |
  | `SIN_RESPUESTA` | límite | El banco no responde: la orden sigue `PENDIENTE`, se reintenta y después se acredita una vez |
  | `MONTO_DISTINTO` | inválido | Discrepancia, sin acreditar (U3) |
  | `PAGADO_VENCIDO` | inválido | Discrepancia, sin acreditar (U3) |
  | `EXCEDE_SALDO_MAXIMO` | inválido | Discrepancia, sin acreditar; la orden no queda colgada (U3) |
  | `RECHAZADO` | inválido | `RECHAZADA` + `recarga_rechazada` |
  | `ERROR_BANCO` | inválido | 5xx del banco: no acredita y se reintenta con backoff |

## H0 — Decisión de arquitectura
**CA:** Dada la enmienda, cuando alguien lee ADR-033, entonces sabe que en la recarga los repositorios son puertos y por qué.
**DoD:** archivo en `docs/Arquitectura/`, más `python scripts/generar_boveda.py` sin errores, si aplica.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.M1 | Enmienda a ADR-033: puertos de persistencia para la recarga (pedido del usuario), con criterio de cuándo aplicarlos | La enmienda existe y la cita el plan | `ls "docs/Arquitectura/ADR-033 Puertos y adaptadores.md"` y el diff con la sección nueva | TODO |

## H1 — Dominio de la recarga (sin Spring ni jOOQ)
**CA:** Dada una orden y una respuesta del banco, cuando se evalúa, entonces el desenlace es determinista y no hace E/S.
**DoD:** `./gradlew :servicios:nucleo-financiero:test` en verde y `ArquitecturaTest` en verde.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `EstadoDeRecarga` con su tabla de transiciones | `EXPIRADA→ACREDITADA` se rechaza | `…:test --tests '*EstadoDeRecargaTest'` | TODO |
| H1.S1.M2 | `OrdenDeRecarga.abrir` y `exigirMismaSolicitud` (reusa `Dinero`) | Misma clave con otro monto da `AP-CU10-05` | `…:test --tests '*OrdenDeRecargaTest'` | TODO |
| H1.S1.M3 | `evaluar(ConfirmacionDeRecarga, ahora, saldoMaximo)` → `Desenlace` (ACREDITAR, YA_ACREDITADA, RECHAZAR, SIGUE_PENDIENTE, DISCREPANCIA(tipo)) | MONTO_DISTINTO, PAGADO_VENCIDO y EXCEDE_SALDO_MAXIMO dan DISCREPANCIA | `…:test --tests '*OrdenDeRecargaTest'` (incluye la prueba de propiedades) | TODO |
| H1.S1.M4 | `ConfirmacionDeRecarga` con `EstadoEnBanco` enum | Un estado desconocido no se puede construir | `…:test --tests '*ConfirmacionDeRecargaTest'` | TODO |
| H1.S1.M5 | `Pata` y `TransaccionDeBilletera` pasan al dominio | Patas descuadradas no se construyen | `…:test --tests '*CuadrarPartidas*'` | TODO |

## H2 — Puertos de salida y adaptadores
**CA:** Dado `aplicacion..CU10*`, cuando compila, entonces no importa nada de `..infraestructura..`.
**DoD:** la regla ArchUnit nueva en verde y `…:integrationTest --tests '*CU10*'` en verde **sin editar las pruebas existentes**.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Puertos `OrdenesDeRecarga` (`transicionar` con UPDATE condicionado), `BilleterasOperables`, `LibroDeBilletera` y `EventosDeRecarga` | No dependen de jOOQ ni de Spring | `…:test --tests ArquitecturaTest` | TODO |
| H2.S1.M2 | Adaptadores jOOQ (envuelven los repositorios actuales); fallan si no hay transacción | Fuera de una transacción lanza un error explícito | `…:integrationTest --tests '*OrdenesDeRecarga*'` | TODO |
| H2.S1.M3 | `EventosDeRecargaOutboxAdaptador` con `moneda` | Todo evento de recarga trae `moneda` | `…:integrationTest --tests '*CU10*Eventos*'` | TODO |
| H2.S2.M1 | Extender `ProveedorDeRecargas` (`emitirCobro`, `consultar`) dejando `solicitar` como default | Los dobles existentes siguen compilando | `…:compileTestJava` | TODO |
| H2.S2.M2 | Adaptador `BancoSimuladoAdaptador` en `infraestructura/adaptadores/pagos/`: QR `SIMULADO\|APORTAYA\|<ref>\|<monto>\|<moneda>\|<vence>`, estado en memoria, escenarios programables; con `@ConditionalOnProperty(aportaya.pasarela.recargas.modo=simulada)` | `emitirCobro` es idempotente por referencia | `…:test --tests '*BancoSimuladoAdaptadorTest'` | TODO |
| H2.S2.M3 | `GuardiaDePasarelaSimulada`: aborta el arranque si `aportaya.entorno.productivo` es true o falta y el modo es `simulada` | En production o staging con modo simulado no arranca | `…:integrationTest --tests '*ArranqueProduccionTest'` | TODO |
| H2.S2.M4 | Prueba de contrato del puerto: el falso, el simulado y el adaptador Python cumplen lo mismo | Los tres pasan la misma suite | `…:integrationTest --tests '*ProveedorDeRecargas*ContratoTest'` | TODO |
| H2.S2.M5 | Regla ArchUnit `aplicacion..CU10*` ↛ `..infraestructura..` | Pasa | `…:test --tests ArquitecturaTest` | TODO |

## H3 — Organismos: solicitar, consultar al banco, acreditar, expirar
**CA:** Dada una orden pendiente pagada en el banco, cuando dos consultas corren en paralelo, entonces el saldo sube una sola vez y existe un asiento CU-24 con debe = haber.
**DoD:** `…:integrationTest --tests '*CU10*' --tests '*CU24*'` en verde, y la consulta de cuadre (saldo de la billetera = mayor) pegada en `evidencia/`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | `CU10RecargarSaldo` depende solo de puertos; `solicitar` llama a `emitirCobro` y guarda el QR | Las pruebas `CU10*` existentes pasan sin tocarlas | `…:integrationTest --tests '*CU10*'` | TODO |
| H3.S1.M2 | Acreditar con **contexto de sistema** (quitar el chequeo de titular) | Dado el actor sistema y el banco PAGADO, entonces queda `ACREDITADA` | `…:integrationTest --tests '*CU10AcreditacionSistemaTest'` | TODO |
| H3.S1.M3 | Asiento espejo CU-24 en la misma transacción | Recarga acreditada → asiento con debe = haber | `…:integrationTest --tests '*CU10AsientoEspejoTest'` | TODO |
| H3.S1.M4 | Desenlace DISCREPANCIA: registra `discrepancia_detectada`, no acredita y la orden no queda colgada | MONTO_DISTINTO, PAGADO_VENCIDO y EXCEDE_SALDO_MAXIMO no mueven el saldo | `…:integrationTest --tests '*CU10DiscrepanciasTest'` | TODO |
| H3.S2.M1 | Trabajo `ConsultaDeRecargasPendientes` (ShedLock + `FOR UPDATE SKIP LOCKED`, cadencia configurable, backoff ante `ERROR_BANCO`/`SIN_RESPUESTA`) | PAGADO_DOS_VECES con dos réplicas del trabajo da un solo movimiento | `…:integrationTest --tests '*CU10ConsultaConcurrenciaTest'` | TODO |
| H3.S2.M2 | Trabajo `ExpiracionDeRecargas` y evento `recarga_expirada` | Orden vencida sin pago → `EXPIRADA` + evento | `…:integrationTest --tests '*CU10ExpiracionTest'` | TODO |
| H3.S2.M3 | `POST …/acreditacion` pasa a ser «forzar consulta» de tesorería: nunca acredita sin PAGADO del banco | Tesorero que no es titular → 200 y el resultado de la consulta | `…:webTest --tests '*BilleteraFondeoWebTest'` | TODO |

## H4 — Adaptadores REST y contrato
**CA:** Dado el titular, cuando consulta su orden, ve el estado y el QR; un tercero recibe 404. Dado QA con `PASARELA_SIMULAR` en modo simulado, cuando dispara un escenario, entonces el banco simulado cambia.
**DoD:** `…:generarServidorOpenApi`, `…:webTest` y `AutorizacionNegativaTest` en verde, y `yarn postman` sin deriva.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | YAML: `SalidaRecarga.qr`, `estado` enum, `cotizacionId`, y la operación `consultarRecarga` | El servidor se genera; los clientes viejos siguen funcionando | `./gradlew :servicios:nucleo-financiero:generarServidorOpenApi compileJava` | TODO |
| H4.S1.M2 | Controlador `GET /billetera/recargas/{id}` | Un tercero recibe 404 | `…:integrationTest --tests AutorizacionNegativaTest` | TODO |
| H4.S2.M1 | `GET /operacion/recargas` (cursor) y `/{id}` con consultas, discrepancias y asiento | Paginado determinista; sin `RECARGA_VER` → 403 | `…:webTest --tests '*OperacionRecargasWebTest'` | TODO |
| H4.S3.M1 | `POST /simulacion/banco/cobros/{ordenId}/pagos` con `Idempotency-Key`; 404 sin el modo simulado | Matriz negativa: sin sesión 401, sin permiso 403, en production 404 | `…:webTest --tests '*SimulacionBancoWebTest'` | TODO |
| H4.S3.M2 | Permisos `RECARGA_VER` y `PASARELA_SIMULAR` en el catálogo; `PASARELA_SIMULAR` solo en la semilla de desarrollo y TEST | `TodoEndpointDecideSuAcceso` en verde | `./gradlew :servicios:nucleo-financiero:webTest` | TODO |
| H4.S3.M3 | Los nueve escenarios de punta a punta con Testcontainers | Se cumple lo que dice la tabla de escenarios | `…:integrationTest --tests '*CU10SimulacionEscenariosTest'` | TODO |
| H4.S4.M1 | Colección Postman y recorrido de recarga simulada | newman sale con 0 fallos | `yarn postman` + `yarn newman run postman/recorrido.postman_collection.json -e postman/entornos/ensayo-local.postman_environment.json` | TODO |

## H5 — App móvil (Flutter), hexagonal del lado cliente
**CA:** Dado un monto válido, cuando confirmo, veo el QR del banco con el monto, el número de orden y la cuenta regresiva. Cuando el banco lo informa pagado, la app pasa a «Acreditado» y **vuelve a leer** el saldo; no lo suma en memoria.
**DoD:** `yarn workspace @aportaya/movil test`, `dart analyze --fatal-infos` y los goldens revisados.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Regenerar el cliente Dart con el contrato H4.S1.M1 | `SalidaRecarga.qr` existe | `dart analyze --fatal-infos` → No issues | TODO |
| H5.S1.M2 | Puerto `Recargas` (cotizar, solicitar, consultar) + adaptador HTTP + adaptador en memoria programable | Se intercambian por provider | `flutter test test/unidad/recargas_en_memoria_test.dart test/contrato/recargas_http_test.dart` | TODO |
| H5.S1.M3 | Máquina pura `cu10_estado_de_recarga.dart` (sealed: Editando, Cotizando, Cotizada, Creando, EsperandoPago, Acreditada, Rechazada, Vencida, Fallo) | Se prueban todas las transiciones | `flutter test test/unidad/cu10_estado_de_recarga_test.dart` | TODO |
| H5.S1.M4 | Seguimiento: consulta con espera creciente (2, 5 y 10 s), se corta al llegar a un estado final, al vencer o al salir, y se reanuda al volver | Sin consultas después del dispose | Mismo test con `fakeAsync` | TODO |
| H5.S1.M5 | Cotizar antes de solicitar, con la misma clave de idempotencia | Se envía `cotizacionId`; desaparece `AP-CU10-06` | `flutter test test/dominio/cu10_recargar_saldo_test.dart` | TODO |
| H5.S2.M1 | `pantalla_recargar.dart`: quitar el selector de medio y sumar `DesgloseDeCobro` | Solo QR (flujo §4.2) | `flutter test test/billetera/pantalla_recargar_test.dart` | TODO |
| H5.S2.M2 | Molécula `CuentaRegresiva` en `packages/diseno_flutter` (anuncia el tiempo a lectores de pantalla, respeta movimiento reducido) | Test del paquete en verde | `flutter test` en `packages/diseno_flutter` | TODO |
| H5.S2.M3 | `pantalla_qr_de_recarga.dart` con los 4 estados y la ruta `/billetera/recargas/:ordenId` | Cada estado es visible | Widget test con el adaptador en memoria | TODO |
| H5.S2.M4 | Resultado: acreditado (snackbar + `invalidate` de saldo y movimientos), rechazado (motivo llano + salida «elevar nivel» si `AP-CU10-01`), vencido («Generar otro QR») | Acreditado relee el saldo | Widget test que verifica la relectura | TODO |
| H5.S3.M1 | Goldens 360/412/600 × claro/oscuro × estados, generados en Linux y **revisados a ojo** | Revisión registrada en `evidencia/` | `yarn workspace @aportaya/movil test:goldens` | TODO |

## H6 — Backoffice (Angular), hexagonal del lado cliente
**CA:** Un operador con `RECARGA_VER` ve la bandeja y la ficha con el asiento balanceado. En TEST, QA con `PASARELA_SIMULAR` dispara los escenarios. En `production`, el panel de simulación no existe en el bundle.
**DoD:** `yarn workspace @aportaya/backoffice typecheck lint test`, `test:a11y` y la verificación con grep sobre `dist/`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Regenerar el cliente Angular con H4.S2/H4.S3 | Typecheck en verde | `yarn workspace @aportaya/backoffice typecheck` | TODO |
| H6.S1.M2 | Puertos `PUERTO_RECARGAS` y `PUERTO_SIMULADOR_BANCO` (`InjectionToken`) + adaptadores HTTP, «no disponible» y en memoria | Se intercambian | Vitest `cu10-recargas.spec.ts` | TODO |
| H6.S2.M1 | Bandeja `operacion/recargas` con 4 estados y paginación por cursor | Spec del componente en verde | Vitest | TODO |
| H6.S2.M2 | Ficha `operacion/recargas/:ordenId`: consultas al banco, discrepancias y asiento con la marca «balanceado» | Avisa si debe ≠ haber | Vitest | TODO |
| H6.S2.M3 | Rutas con `canMatch` por permiso | Sin permiso, redirige al tablero | `operacion.routes.spec.ts` | TODO |
| H6.S3.M1 | Proveedor seguro + `fileReplacements` solo en `development`, `demo` y `test` | `production` no incluye el simulador | Spec que lee `angular.json` + `produccion-honesta.spec.ts` | TODO |
| H6.S3.M2 | Panel «Simular pago»: escenarios con confirmación e `Idempotency-Key`; al terminar, relee la orden | PAGADO_DOS_VECES muestra «1 sola acreditación» | Vitest con el adaptador en memoria | TODO |
| H6.S3.M3 | El bundle de `production` no contiene «simular» | grep vacío | `yarn workspace @aportaya/backoffice build` + `grep -ri simular dist/ \| wc -l` → 0 | TODO |

## H7 — TEST en Coolify y prueba de punta a punta
**CA:** En TEST, el kill-test **no** se cumple: la recarga simulada se acredita una sola vez y la app lo muestra.
**DoD:** `yarn humo` en verde, el E2E del backoffice y la app en verde, y capturas revisadas en `evidencia/`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S1.M1 | `docker-compose.coolify.yml` y `despliegue/TEST.md`: `PASARELA_RECARGAS_MODO=simulada` y `ENTORNO_PRODUCTIVO=false` para TEST, una réplica de `nucleo-financiero`; production y staging sin modo simulado | Diff revisado; sin secretos en git | `git diff docker-compose.coolify.yml` + `yarn verificar:compose` (o el verificador equivalente del repo) | TODO |
| H7.S2.M1 | E2E de aceptación del backoffice contra el doble con estado (5446): los nueve escenarios | Pasan | `playwright test -c e2e/aceptacion/playwright.aceptacion.config.ts recarga` | TODO |
| H7.S2.M2 | Visual del backoffice 390/834/1440 × claro/oscuro, revisado | Revisión registrada | `yarn workspace @aportaya/backoffice test:e2e visual-recargas` | TODO |
| H7.S2.M3 | Recorrido de la app contra el stack local: solicitar → QA paga → acreditado | Saldo correcto | `yarn workspace @aportaya/movil test:e2e` + `yarn humo:recorrido` | TODO |
| H7.S2.M4 | Recorrido en TEST después del deploy de `test` (requiere red hacia `*.161.97.85.216.sslip.io`; si no hay, BLOQUEADO con contrato simulado según la regla 65 y lo que falta) | Saldo correcto en TEST | Capturas + salida en `evidencia/h7-test.md` | TODO |

## H8 — Regresión, gates y reporte
**CA:** Nada de lo anterior rompe otra cosa, y el reporte lo dice con evidencia.
**DoD:** `./gradlew verificar` + `yarn verificar` (o el gate global del repo) en verde, `REPORTE.md` con AVANCE en la primera línea, y gates `money-movement-safety`, `security-guardrails` y `data-privacy-financial` documentados (reglas 90.6 y 91.6).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H8.M1 | Regresión de backend completa | Verde | `./gradlew verificar` | TODO |
| H8.M2 | Evidencia de dinero (91.6): asientos pegados, idempotencia ejecutada, cuadre billetera = mayor, reversa CU-14 de una recarga, redondeo declarado | Los cinco puntos están en `REPORTE.md` | Salidas en `evidencia/` | TODO |
| H8.M3 | `grep` de logs: ni QR, ni referencias del banco, ni PII | Sin hallazgos | `grep -rE "payloadQr\|SIMULADO\\|" <logs>` → vacío | TODO |
| H8.M4 | `REPORTE.md` con Completado, A medias y Pendiente | `report_gate.py` no protesta | `python .claude/hooks/report_gate.py --self-test` + `python .claude/hooks/plan_status.py` | TODO |

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El simulador llega a producción | Se acredita dinero falso | Tres candados: el modo, el permiso y la guardia de arranque (asume productivo si falta la propiedad), más `ArranqueProduccionTest` y el grep sobre el bundle del backoffice |
| El estado en memoria del banco simulado se pierde al reiniciar | Las órdenes pagadas antes del reinicio no se acreditan | Una réplica en TEST, riesgo declarado; QA vuelve a pagar |
| Carrera entre la consulta programada y la de tesorería | Doble acreditación | UPDATE condicionado al estado + `uq_recarga_referencia` + prueba de concurrencia |
| Se pierde el contexto de RLS si un adaptador usa `DSLContext` fuera de `conContexto` | Se filtran datos | Los adaptadores fallan sin transacción, más una prueba que lea `current_setting('app.usuario_id')` |
| Refactorizar una zona con ~10 clases de prueba | Regresión silenciosa | Paso a paso, con `CU10*` en verde **sin editar** las pruebas |
| La sesión no llega a TEST (red) | No se puede ver en TEST | H7.S2.M4 se cierra contra el doble según la regla 65; Justin habilita la red |
