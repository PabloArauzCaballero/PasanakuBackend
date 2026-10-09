# Merge · nucleo-financiero (agente NUCLEO-FINANCIERO)

Fecha: 2026-10-09 · Worktree `PasanakuBackend-cierre` · base `origin/test` = 59370bbd.
Lado "ours" = lo que ya corre en test (Pablo); "theirs" = nuestros carriles. Regla de dinero aplicada: nada float, moneda al lado,
append-only, idempotencia con UNIQUE/lock, saldo desde el mayor.

## Resultado de la corrida (una sola, bajo el guardián, Docker propio y bajado al terminar)
Comando: `nf_full.sh :servicios:nucleo-financiero:spotlessApply :test :webTest :integrationTest :testBarrido --continue`
(Postgres desechable `aportaya-pg-nf` en 5435 con `sql/aplicar.sql` fusionado, para `generateJooq`). Conteos desde `build/test-results/*.xml`:

| Tarea | tests | fallos | errores | skipped |
|---|---|---|---|---|
| test | 54 | 0 | 0 | 0 |
| webTest | 32 | 0 | 0 | 0 |
| integrationTest | 340 | 0 | 0 | 0 |
| testBarrido | 2 | 0 | 0 | 0 |

`BUILD SUCCESSFUL in 2m 26s`. Antes de eso hubo dos corridas rojas, ambas arregladas (ver "Arreglos tras compilar/probar").
`python scripts/verificar_criterios.py --servicio nucleo-financiero` → "Sin divergencias entre la boveda y el codigo" (10 CU verificados).
`python scripts/verificar_pruebas_web.py` → "La capa web esta cubierta segun ADR-043". `herramientas/proveedor_simulado`: 27 pruebas python OK.

## Conflictos resueltos (10) y qué se conservó de cada lado

| Archivo | Ours (test) | Theirs (nuestros carriles) | Resolución |
|---|---|---|---|
| `CU10RecargarSaldo` solicitar | `bloquearIdempotencia` + `porClaveIdempotencia(cuenta, clave)` | bloqueo de la cuenta + BOLA (solo el titular) + `exigirMismaSolicitud` | Todo: primero se bloquea la cuenta y se autoriza al titular, **después** se toma el lock de idempotencia (un tercero no ocupa el lock de otro), luego la búsqueda por (cuenta, clave). |
| `CU10RecargarSaldo` acreditar | B33: repetir una orden ACREDITADA devuelve la respuesta original | confirmación autentica del proveedor (referencia, importe, estado, liquidada_en) + titular | Se conserva la validación de la confirmación y el titular. **B33 se cumple en `RecargasConProveedor` (`yaAcreditada` y la carrera perdida)**: compara la confirmación con la referencia del proveedor que acreditó y devuelve `resultadoConfirmado` (mismo `transaccionId`, saldo del asiento original). El caso de uso en sí sigue rechazando un 2.º abono directo (`CU10Test.criterio2/concurrencia` de upstream esperan `ErrorDeNegocio`, ver "Decisiones"). `transaccionDe` queda disponible en el repositorio. |
| `OrdenRecargaRepositorio` | `bloquearIdempotencia`, `transaccionDe` | `enUtc`, `rechazar`, `saldoDeLaAcreditacion` | Todos los métodos de ambos lados. |
| `CU12TransferirSaldo` | `libro.bloquearIdempotencia/porClaveIdempotencia(Optional usuario…)`, `Datos.comoSistema` para bloquear/leer la cuenta del otro (B8) | `RegistroDeIdempotencia.turnarse`, BOLA del origen, `repetida()` (misma transferencia o 422) | Lock de idempotencia del libro (upstream), `comoSistema` para bloquear ambas cuentas y leer el destino (B8 sigue funcionando), BOLA sobre el origen y `repetida()`. Se quitó la dependencia de `RegistroDeIdempotencia` del constructor (queda el de 7 argumentos de upstream; el turno lo da el libro). |
| `LibroDeBilletera` | `bloquearIdempotencia` + `porClaveIdempotencia(Optional<UUID>…)` con `COALESCE`/centinela = `uq_tx_idem` | `porClaveIdempotencia(UUID…)` | Ambas firmas: la de upstream (superconjunto) y un atajo `UUID` que delega. `registrar` sigue escribiendo con `comoSistema`. |
| `CotizadorPorHttp` | `@Retry`/`@CircuitBreaker` en `costoDe`, la excepción de transporte se propaga, fallback = vacío; gratuita = cero | `cotizar` (desglose completo), `referenciaTipoDe(hecho)`, respuesta desconocida = vacío | `costoDe` conserva retry+CB y solo parsea `montoTotal` (lo que prueba `CotizadorResilienciaTest`); `cotizar` usa el mismo `pedir()` pero absorbe todo fallo a vacío (lo que prueban `CotizadorPorHttpTest` hechas a mano, sin proxy) y exige desglose completo. `referenciaTipo` por hecho (ORDEN_RECARGA / ORDEN_RETIRO / TRANSACCION_BILLETERA). |
| `OrdenRetiroRepositorio` | estado inicial por parámetro, `enProceso`, aprobación (EN_REVISION→AUTORIZADA/RECHAZADA), `pasarAEnProceso`, `referenciaProveedor` | `cotizacionId`, `bloquear`, `enviarAlProveedor`, `vincularTransaccion`, `Orden` ampliado | Superconjunto: `crear(…, estadoInicial, cotizacionId, ahora)`; un solo `aOrden(Record)` con todas las columnas (ver y enProceso); `Orden` con los campos de ambos. `enviarAlProveedor` ahora parte de AUTORIZADA/EN_PROCESO (no de PENDIENTE) y **no pisa** una `referencia_proveedor` ya devuelta por `instruirPago`. Las dos transiciones de aprobación pasaron a `AprobacionDeRetiroRepositorio` (el barrido de tamaño exige < 300 líneas). |
| `CU11RetirarSaldo` (8 marcas) | máquina de estados `EstadoDeRetiro` (AUTORIZADA/EN_REVISION), MFA con evidencia, métricas, `instruirPago`/`confirmarPago`/`aprobar`/`rechazarRevision` vía `ResolucionDeRetiro` | BOLA + idempotencia por billetera + `exigirMismaSolicitud`, cotización previa, despacho al proveedor firmado | Un solo flujo: cuenta bloqueada → BOLA → lock de idempotencia → replay (devuelve el costo ALMACENADO) → condiciones duras (con `evidenciaMfaProvista`, contador de rechazos) → límites → retención → orden en AUTORIZADA / EN_REVISION con `cotizacion_id` → evento. El cuerpo de `solicitar` pasó a `SolicitudDeRetiro` (tamaño). `confirmarPago` ahora también deja `orden_retiro.transaccion_id` (`vincularTransaccion`). `EntradaRetiro` tiene el constructor canónico de 9 campos y los atajos de 7 y 8 argumentos de ambos lados. |
| `BilleteraController` | `PreparacionDeRetiro` (costo vía `costoDe`, MFA con evidencia) | delega recargas/retiros/QR en `FondeoDeLaBilletera`/`QrDeLaBilletera` | Se queda la delegación; `FondeoDeLaBilletera.retirar` incorpora la evidencia MFA de upstream (`evidenciaMfa` con `factorMfa` deprecado y `evidenciaMfaProvista`), y el controlador recibe `CU11RetirarSaldo` para `aprobarRetiro` (H3.S2). `PreparacionDeRetiro` quedó sin uso y se eliminó. |
| `openapi/nucleo-financiero.yaml` | `evidenciaMfa` + `factorMfa` deprecado | `cotizacionId`, `EntradaCotizacionOperacion`, `SalidaCotizacionOperacion` | Se concatena todo (sin `factorMfa` duplicado). `additionalProperties: false` intacto y `fail-on-unknown-properties: true` sigue en `application.yml`. Se documentó `AP-CU11-13`. |
| `BaseDeBilletera` (test) | importa `ProveedorDeRetiroLocal` | importa `RegistroDeIdempotencia` | Ambos imports; `CU12` se arma con 7 argumentos. |

## Colisión de nombre `CU11ProveedorTest`
- `CU11ProveedorTest.java` (63 líneas, Pablo) queda **intacta** (rechazo firme del proveedor local → RECHAZADA y libera la retención).
- La nuestra (250 líneas, "Despacho al proveedor": liquida y pierde la respuesta, pierde antes de llegar, proveedor caído, rechazo, discrepancias,
  firma inválida, contradicción tras pagar, despacho ajeno, dos firmas, concurrencia) se reinstaló como `CU11ProveedorSimuladoTest.java`
  (clase renombrada; los `@DisplayName` no cambian, así que `verificar_criterios` los sigue alineando con CU-11). Único cambio: el estado inicial
  esperado en `otraPersonaNoDespacha` pasó de `PENDIENTE` a `AUTORIZADA` (decisión de estados, abajo).

## Decisiones donde chocaron de verdad (prima lo que corre en test)
1. **Estado inicial del retiro**: upstream = `AUTORIZADA` (o `EN_REVISION` con doble firma); nosotros nacíamos `PENDIENTE`. Gana upstream.
   `CU11InstruirRetiro.preparar` ahora despacha desde `AUTORIZADA`/`EN_PROCESO`; una orden `EN_REVISION` se rechaza con AP-CU11-08 («falta la segunda aprobación»).
2. **Costo en el replay del retiro**: upstream (`CU11Test.replayDevuelveCostoAlmacenado`) devuelve el costo almacenado ante un costo distinto con la misma clave;
   nuestro `exigirMismaSolicitud` lo rechazaba. Gana upstream: `exigirMismaSolicitud` ya no compara costo/neto (sigue comparando cuenta, destino, importe y cotización).
   Nuestra `CU11IdempotenciaTest.cambiarLaSolicitudSeRechaza` conserva sus rechazos de importe y destino y, para el costo, ahora afirma que devuelve el costo registrado
   (5.00) sin abrir otra orden. El costo real lo fija el servidor desde `tarifas` (cotización), nunca el cliente.
3. **Código AP-CU11-07**: upstream lo usa para `MFA_INVALIDO` (documentado en el contrato). Nuestra «la clave de idempotencia ya identifica otro retiro» usaba el mismo código;
   pasó a `AP-CU11-13`. 
4. **B33 a nivel de caso de uso**: `CU10Test.criterio2` y `concurrencia` (upstream) esperan `ErrorDeNegocio` al repetir `acreditar`; la rama B33 de upstream devolvía la original.
   Para que ambas convivan, el 2.º abono directo sigue siendo error y el reintento idempotente (misma referencia del proveedor) se resuelve en `RecargasConProveedor`
   (probado por `CU10IdempotenciaTest`/`CU10ServidorRealTest`, nuestras).
5. **BOLA en pruebas de upstream**: `CU10Test.mismaClaveDistintaCuenta` (de upstream) usaba contextos de usuarios ajenos a la cuenta; se pasó a `fixtura.titular(cuenta)`
   igual que ya hacíamos en el resto de `CU10Test`. Mismo patrón que `CU10Test` (sin cambiar aserciones).
6. **Web tests**: `BilleteraControllerWebTest` usa el `FondeoDeLaBilletera` real (`@Import`) con sus colaboradores doblados para que «acreditar» (TESORERIA) siga probando HTTP de punta a punta
   (ahora llama a `recargas.confirmar`, no a `cu10.acreditar`); `BilleteraAprobacionWebTest` y `QrControllerWebTest` recibieron los dobles que el controlador nuevo necesita.
   `ArranqueProduccionTest` y `CotizadorResilienciaTest` (upstream) suman `QR_INTERNO_CLAVE` (secreto obligatorio de los QR internos); no se tocó ninguna aserción.

## Arreglos tras compilar/probar (corrida 1 y 2)
- `OrdenRecargaRepositorio`: llave sobrante tras la fusión (spotless lo detectó).
- `BilleteraControllerWebTest`: `cu10.acreditar(any(), any())` ya no compila (firma de 3 argumentos) → ver punto 6.
- 6 `QrControllerWebTest`, 2 `ArranqueProduccionTest`, 2 `CotizadorResilienciaTest`, `CU10Test.mismaClaveDistintaCuenta`, `CU11Test.replayDevuelveCostoAlmacenado`,
  `BarridoTest.tamano-archivo` (`CU11RetirarSaldo` 363 y `OrdenRetiroRepositorio` 332 líneas → `SolicitudDeRetiro` + `AprobacionDeRetiroRepositorio`; hoy 256 / 299 líneas).

## Dos puertos de proveedor de retiros (deuda declarada, no resuelta)
Conviven `ProveedorDeRetiro` (upstream: `instruirPago` + `ReconciliacionDeRetiros`, doble `ProveedorDeRetiroLocal` solo en local/test; en producción no hay bean y el arranque falla cerrado,
como ya dejaba upstream) y `ProveedorDeRetiros` (nuestro: consulta-antes-de-reenviar, firma HMAC, discrepancias; adaptador `RetirosProveedorSimulado`, `deshabilitado` por omisión).
Los dos operan sobre la misma máquina de estados (`AUTORIZADA → EN_PROCESO → PAGADA/RECHAZADA`) y la misma `confirmarPago/rechazar`, pero no comparten el cliente. Unificarlos
(que la reconciliación use `RetirosConProveedor.resolver`) es trabajo de diseño, fuera de esta fusión.
Además `CotizadorPorHttp.cotizar` (cotización previa y `confirmar` del retiro/recarga) tiene timeout pero **no** retry/circuit breaker; solo `costoDe` los conserva. Fallar ahí rechaza la operación (fail-closed).

## No cubierto
- Sin prueba contra un proveedor real (solo el simulado y dobles); sin prueba de caída de `tarifas` a nivel HTTP de punta a punta.
- La nota `AP-CU11-13` solo está en el contrato; `docs/CasosDeUso/CU-11 Retirar saldo.md` es del agente de esquema/docs.
- El comentario nuevo del yaml se agregó después de la corrida verde (solo un comentario; `verificar_pruebas_web` se re-ejecutó y pasa).
