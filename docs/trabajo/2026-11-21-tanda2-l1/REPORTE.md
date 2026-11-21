# Reporte — Tanda 2, lote L1 (`aportes` y `nucleo-financiero`)

> **AVANCE: 11 / 22 — 50,0 %.** Trabajo en curso: M11, M01, M07, M08 y el listado de bloqueos (M10c) a medias, con el código en verde y esperando lugar para su PR (tope 3) o la app de L6; el evento `aportes.periodo_recaudado` COMPLETO (pedido de L2) quedó hecho en su rama, sin PR por el tope; GRACIA_VENCIDA espera diseño. Nada en rojo por código propio.

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 50cf3be9
- PR hacia `dev`: **todos mergeados**. Se mergearon #430, #454 (que trajo a #425, #447 y #449), #456 (que trajo a #455), #418, #461 y #458 (que consolidan al resto). Abierto: el PR de M10 parte 2 (permisos y ruta de levantamiento).
- Peldaño de evidencia por área:
  - `aportes` y `nucleo-financiero` (código): `TESTED`, con pruebas dirigidas en verde contra PostgreSQL 16 real local y controles negativos (mutantes) que fallan sin el cambio. No llega a `VERIFIED`: el camino HTTP de punta a punta con los servicios levantados no se ejercitó.
  - Integración con Testcontainers: las clases que levantan su propio contenedor (`AislamientoEsquemaTest`, `ArranqueTest`) quedan **NO COMPROBADAS: requieren Docker** (máquina de Pablo).

Cálculo del avance: microtareas del plan = 14 de H1 + 4 de H2 + 1 pedido de L2 (L1.P01) = 19. `HECHO` = L1.M00, F1.L1.M03, F1.L1.M04, F1.L1.M05, F1.L1.M06, F1.L1.M09, F1.L1.M10, F1.L1.M12, F1.L1.M13 y L1.P01. Las que quedaron `A MEDIAS` no suman. F1.S4.M2 quedó `DESCARTADO` para L1 (no suma y se declara abajo).

## Completado

| ID | Qué se logró (observable) | Comando | Resultado |
|---|---|---|---|
| L1.M00 | Los logs de CU-10, CU-12 y CU-21 ya no escriben el importe. Un barrido por servicio lo impide en adelante. | `./gradlew :servicios:{aportes,nucleo-financiero}:testBarrido` | PASS. Con el código de `dev`, el barrido falla (control negativo). `evidencia/M00-logs-sin-importes.txt` · PR #418 |
| F1.L1.M03 | Un reverso necesita dos actos y dos tokens. `autorizadaPor` desaparece del contrato. La misma persona recibe 422 `AP-CU14-02`. La base lo exige con R-BIL-27. | `webTest ReversoCuatroOjosWebTest` (7/7) + `integrationTest` de 12 clases que reversan (76/76) + psql con `SET ROLE svc_nucleo_financiero` | PASS. El mutante sin la segunda firma hace fallar 2 pruebas. `evidencia/M03-*` · PR #425 |
| F1.L1.M06 | La firma del webhook cubre `X-Timestamp`. Un reenvío con otro timestamp da `FIRMA_INVALIDA` y no escribe nada. | `test VerificadorDeFirmaWebhookTest` (12/12) + `integrationTest CU100WebhookTest` (9/9) | PASS. El mutante que firma solo el cuerpo hace fallar 4 pruebas. `evidencia/M06-firma-timestamp.txt` · PR #430 |
| F1.L1.M05 | El secreto del webhook de `aportes` sale de `APORTES_WEBHOOK_SECRETO`, sin valor por omisión. Si falta o es corto, el servicio no arranca. No queda ningún literal en `src/main`. | `test SecretoWebhookDeEntornoTest` (2/2) + grep | PASS. `evidencia/M05-secreto-webhook.txt` · PR #458 (antes #450). **Antes de desplegar hay que cargar la variable en Coolify.** |
| F1.L1.M04 | `nucleo-financiero` arranca con perfil productivo sin `@MockitoBean`. Instruir un retiro devuelve `AP-CU11-15 PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO`. | `ArranqueProduccionTest` contra la Postgres local (2/2) + `ProveedorDeRetiroNoConfiguradoTest` (2/2) | PASS. Sin el adaptador: «No qualifying bean». `evidencia/M04-desembolso-fail-closed.txt` · PR #461 (antes #451) |
| F1.L1.M13 | El sello del cierre diario cubre cuenta, fecha, moneda, saldos y el sello anterior. `primeraRuptura` señala el primer día alterado. | `test SelloDelDiaTest` (6/6) + `integrationTest CU51* CU15*` | PASS. El mutante con el sello viejo hace fallar 6 pruebas. `evidencia/M13-sello-cierre.txt` · PR #461 (antes #436) |
| F1.L1.M09 | Solicitud de reembolso, aprobación y alta de proveedor son idempotentes. La misma clave devuelve el mismo id y deja una sola fila, en serie y en concurrencia. R-BIL-28 y R-BIL-31 están en la base. | `integrationTest CU19IdempotenciaTest` (6/6) + `CU99IdempotenciaTest` (4/4) | PASS. Los mutantes hacen fallar 5 pruebas. `evidencia/M09-*`, `evidencia/M09b-*` · PR #447 y #456 |
| F1.L1.M12 | `reembolso`, `disputa_pago`, `conciliacion`, `excepcion_conciliacion` y `extracto_bancario` llevan moneda (R-BIL-29). Un reembolso de un pago en USD queda en USD. La conciliación anota la moneda del pago. | `integrationTest CU19MonedaTest` (3/3) + `CU132ConciliacionTest` (3/3) + migración sobre una base existente | PASS. Los mutantes fallan. `evidencia/M12-*`, `evidencia/M12b-*` · PR #449 y #454 |
| L1.P01 | Pedido de L2: `nucleo-financiero` deja de consultar la oferta de `entregas`. Se borraron el puerto, el cliente HTTP, el pago a tercero, el pacto y sus pruebas, y `pagarAlTercero` responde 410 `AP-CU13-10` sin efecto. | `test` + `webTest` (`pagoATerceroRetirado`) + integración CU12*/CU13* (95/97; las 2 restantes requieren Docker) | PASS. `evidencia/L2PEDIDO-retiro-ofertas.txt` · PR #461 |
| F1.L1.M10 | El bloqueo por autoridad guarda quién lo registró y lo levanta otra persona (R-BIL-32); el saldo vuelve en la misma transacción. Registrar y levantar exigen permisos propios (`BLOQUEO_AUTORIDAD_REGISTRAR` y `BLOQUEO_AUTORIDAD_LEVANTAR`) y existe la ruta `POST /billetera/bloqueos/{id}/levantamiento`. | `BloqueoAutoridadWebTest` (7/7) + CU17 contra la base (16/16) + dos siembras idénticas | PASS. El mutante hace fallar 5 de 7. `evidencia/M10-*`, `evidencia/M10b-*` |
| L1.P02a | Pedido de L2 (F3.L2.M01): el pago que deja sin cuotas `APORTE_PERIODICO` por cobrar a un periodo emite UN `aportes.periodo_recaudado` (`periodoId`, `grupoId`, `motivo` COMPLETO, `ocurridoEn`, sin importes) por outbox, en la misma transacción. Un pago que no completa, un parcial o un reintento con la misma clave no emiten; dos últimos pagos simultáneos emiten exactamente uno (cerrojo transaccional por periodo). La carga cumple el pacto `aportes-entregas-periodo-recaudado.json`. | `integrationTest CU21PeriodoRecaudadoTest` (6/6) + regresión de aportes (`test` 59/59, `webTest` 83/83, `integrationTest` 164/167: las 3 restantes requieren Docker) + verificadores | PASS. El mutante sin cerrojo falla 3 de 3 corridas (`expected: 1 but was: 0`: el aviso se perdía). `evidencia/P02a-periodo-recaudado.txt` · rama `correcciones/tanda2-l1-periodo-recaudado`, sin PR (tope 3) |

## A medias

### F1.L1.M07 — BOLA en `aportes` (estado y reembolso en `dev` por #458; pasos 1+2 en la rama `correcciones/tanda2-l1-identidad-servicio`)
- **Qué anda:**
  - En `dev`: el estado de un participante ajeno y el reembolso de un pago ajeno devuelven 404.
  - En la rama (commit f0d32a7b):
    - `morosos` y `primera-cuota` exigen `SERVICIO_GRUPOS`.
    - `recaudo` exige `SERVICIO_ENTREGAS` o `SERVICIO_GARANTIA`, y transitoriamente `ENTREGA_EJECUTAR`.
    - `grupos`, `entregas` y `garantia` preguntan con el token de su cuenta de servicio y, sin él, fallan cerrados.
    - `garantia` tiene su rol `SERVICIO_GARANTIA` (semillas idempotentes), su configuración, sus variables en el compose y su fila de rotación.
  - Lo prueban `IdentidadDeServicioWebTest` (17/17), las pruebas de los adaptadores y un mutante sobre el inventario de rotación. `test`, `webTest` y `testBarrido` están en verde en los cuatro servicios (`evidencia/M07b-*`).
- **Qué no anda:** la rama no está en `dev`. Las pruebas de integración de `grupos`, `entregas` y `garantia` necesitan Docker y aquí quedan sin correr.
- **Qué falta exactamente:**
  1. Abrir el PR cuando haya lugar (tope 3).
  2. Cargar en Coolify `GARANTIA_SERVICIO_TELEFONO`, `GARANTIA_SERVICIO_CLAVE` y `GARANTIA_SERVICIO_TOTP_SEMILLA` (o el factor fijo en TEST). Lo hace el coordinador.
  3. Paso 3: retirar `ENTREGA_EJECUTAR` del recaudo, cuando los consumidores lleven credencial en TEST.
- **Dónde quedó:** rama pusheada. Compila y está en verde.

### F1.L1.M10c — `GET /billetera/bloqueos`, la bandeja de Cumplimiento (rama `correcciones/tanda2-l1-listado-bloqueos`, apilada sobre #476)
- **Qué anda:**
  - Contrato 1.4.0, aditivo, con paginación por cursor `(recibidoEn, id)` descendente, filtro por estado en lista blanca y respuesta mínima (sin número de oficio ni documento; el importe con la moneda de la cuenta).
  - `puedeLevantar` es falso para quien registró el bloqueo.
  - Lo prueban `BloqueoListadoWebTest` (14/14, matriz negativa) y `CU17ListadoTest` (4/4 contra PostgreSQL), con dos mutantes detectados. `CU17CuatroOjosTest` confirma contra la base que quien registra no levanta (`evidencia/BLOQUEOS-*`).
- **Qué no anda:** la rama no está en `dev` y la pantalla no existe (es de L4L6).
- **Qué falta exactamente:**
  1. Abrir el PR cuando entre #476 y haya lugar.
  2. Avisar a L4L6 para la pantalla.
- **Dónde quedó:** rama pusheada (commit 01074b9f). Compila y está en verde.

### F1.L1.M01 — Importe nunca negativo, parte API (PR #458 en `dev`; parte `nucleo-financiero` en la rama `correcciones/tanda2-l1-importe-nucleo`, sin PR)
- **Qué anda:**
  - En `aportes` (ya en `dev`): `Importe` en reembolso y disputa, que da 400 `AP-VAL-01` con el campo. El cobro dejó de recibir el importe por el cuerpo en la operación nueva de M11.
  - En la rama `correcciones/tanda2-l1-importe-nucleo` (commit ef39fd33): `nucleo-financiero.yaml` 1.3.0 usa `Importe` en los 13 campos de importe de las 11 entradas, y un negativo da 400 `AP-VAL-01` con el campo.
  - Lo prueban: `ImporteDeEntradaTest`, que recorre el contrato; las pruebas web de recarga, transferencia y retención negativas; `webTest` 91/91; `integrationTest` en verde salvo 25 pruebas que necesitan Docker. Evidencia en `evidencia/M01b-importe-en-nucleo-financiero.txt`.
- **Qué no anda:** la rama no está en `dev`. Sin el cambio de la app, el job de Flutter de la CI queda en rojo, porque la clase Dart pasa de `Dinero` a `Importe`.
- **Qué falta exactamente:**
  1. Cambiar `Dinero(...)` por `Importe(..., moneda: ImporteMonedaEnum.BOB)` en `apps/movil/lib/dominio/cu10_recargar_saldo.dart:41`, `cu11_retirar_saldo.dart:40` y `cu12_transferir_saldo.dart:45`. Lo hace L4L6, o esta sesión si el coordinador lo autoriza (pregunta enviada).
  2. Abrir el PR.
  3. Después de #476, resolver el conflicto de la línea `version` del contrato.
- **Dónde quedó:** rama `correcciones/tanda2-l1-importe-nucleo`, pusheada. Compila y la regresión está en verde.

### F1.L1.M08 — Hechos de dinero inmutables (PR #458 (antes #455), apilado sobre #454)
- **Qué anda:**
  - `pago`, `reembolso` y sus dos historiales de estado son append-only: trigger R-AUD-01 y `REVOKE UPDATE, DELETE` a `svc_aportes`, generados desde `APPEND_ONLY` (`scripts/modelo.py`).
  - El estado cambia agregando una fila de historial, y desde cada estado se sale una sola vez (R-BIL-30).
  - `AppendOnlyTest` 9/9. `UPDATE` como `svc_aportes` da `permission denied` y, como superusuario, `R-AUD-01`. Dos reversos concurrentes dejan una sola fila.
  - Integración de `aportes`: 141/144, y las 3 en rojo son las de Docker. El mutante hace fallar 5 pruebas. El humo del esquema da 0 FALLA.
- **Qué no anda:** `webhook_pasarela` sigue cambiando su estado con `UPDATE`.
- **Qué falta exactamente:** particionar `webhook_pasarela` por fecha, como `bitacora_evento`. Su retención es ELIMINAR a los 24 meses, y una tabla append-only solo se purga con DETACH + DROP de la partición. Después, sellarla y llevar su estado a `webhook_transicion` o `historial_estado_webhook`.
- **Dónde quedó:** rama `correcciones/tanda2-l1-hechos-inmutables`. Compila y está en verde.

### F2.L1.M01 — Consumidor de respaldo y cobertura empresarial (PR #461 (antes #452))
- **Qué anda:** el listener escucha solo `entregas.desembolso_acreditado`, y una prueba fija esos tópicos. `CAJA_EMPRESA` salió de la configuración y del compose.
- **Qué no anda:** quedan los mapeos `RESERVA_RESPALDO`, `POZO_GRUPO` y `DESEMBOLSO`, que apuntan a las cuentas 9.x.
- **Qué falta exactamente:** las cuentas del fondo del grupo del modelo A (F3) para reemplazar esas cuentas. Después, F2.L1.M02. También que L2 retire el productor en `garantia`.
- **Dónde quedó:** rama `correcciones/tanda2-l1-retiro-consumidor-respaldo`. Compila y está en verde.

### F1.L1.M11 — El cobro no toma monto, canal ni referencia del cliente (rama `correcciones/tanda2-l1-cobro-contrato`)
- Qué anda: fase de expansión del contrato (regla 98.1.3), `aportes.yaml` 1.1.0. La operación nueva `pagarCuotaConSaldo` (`POST /aportes/obligaciones/{id}/pago-con-saldo`) no admite campos: con cualquiera responde 400 `AP-VAL-02`. El servidor cobra el pendiente leído en la misma transacción, por `BILLETERA_MOVIL` y con la referencia que él mismo fija. La operación vieja `cobrarAporte` responde 422 `AP-CU21-10` ante un canal que no sea el saldo, un recargo, un proveedor o `esManual`, y 422 `AP-CU21-02` si el monto difiere del pendiente; el reintento con la misma clave devuelve el pago original. Lo prueban `PagoConSaldoWebTest` (21/21), `CU21PagoConSaldoTest` (6/6, contra PostgreSQL) y tres mutantes que hacen fallar las pruebas. Regresión de `aportes` en verde salvo 3 pruebas que necesitan Docker. Evidencia en `evidencia/M11-cobro-sin-confianza-en-el-cuerpo.txt`.
- Qué no anda: el CA completo, "un cuerpo con `monto` recibe 400", vale solo en la operación nueva. `cobrarAporte` todavía acepta el campo `monto` (aunque lo compara con el pendiente) hasta que se contraiga el contrato. La app sigue usando `cobrarAporte` y deja elegir el canal: con canal QR recibe ahora 422 `AP-CU21-10`.
- Qué falta exactamente: (1) L6 pasa la app a `DefaultApi.pagarCuotaConSaldo(obligacionId, idempotencyKey)` sin cuerpo y quita el selector de canal, la referencia y el monto (`cu21_cobrar_aporte.dart` y `pantalla_aportar.dart`). (2) Se borra `cobrarAporte` del contrato (fase de contracción). (3) Se ve compilar la app en el job "Frontend — Flutter" de la CI: en este contenedor no hay Flutter.
- Dónde quedó: rama `correcciones/tanda2-l1-cobro-contrato`, apilada sobre #476. Compila y la regresión está en verde.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| F2.L1.M02 | TODO | Depende de F3: el árbol 9.x lo usan `POZO_GRUPO` y `DESEMBOLSO`, que asientan el desembolso vigente. Sacarlo antes rompe ese asiento. |
| F2.L1.M03 | TODO | Depende de F1.L1.M11 (el contrato del cobro deja de llevar `canal`) y de que la app (L6) no ofrezca los canales retirados. |
| F2.L1.M04 | TODO | Necesita el evento de apertura de período de `grupos` (L3) y su pacto, o un trabajo con ShedLock, y el ADR de inicio automático (F0·ADR-054). |
| L1.P02b | TODO | `aportes.periodo_recaudado` motivo GRACIA_VENCIDA: falta el diseño de la marca de "ya emitido" por periodo (el trabajo que vence la gracia corre a diario y no puede repetir el aviso) y decidir quién lo dispara (el trabajo de recargos o uno propio). Lo puede tomar L1 cuando se libere lugar; no bloquea el fondeo por COMPLETO. |
| F1.S4.M2 | DESCARTADO para L1 | Vive en `grupos` (F1.L3.M03, lote L3): el `LIMIT 1` está en `ConsultasRepositorio.usuarioDelAlias`. La parte de `nucleo-financiero` (rechazar un destino ambiguo) depende del contrato nuevo que defina L3. |

## Evidencia
Índice de `evidencia/`, con comandos y salidas literales:
- `M00-logs-sin-importes.txt`: barrido y control negativo.
- `M03-pruebas.txt`, `M03-r-bil-27.sql`, `M03-r-bil-27-psql.txt`: reverso. Pruebas web e integración, más las invariantes con el rol de la app.
- `M06-firma-timestamp.txt`: firma del webhook, vector `openssl` y mutante.
- `M13-sello-cierre.txt`: sello encadenado y mutante.
- `M07-bola-aportes.txt`: matriz negativa y mutante.
- `M01-importe-api.txt`: importes negativos y mutante.
- `M09-reembolso-idempotente.txt`: idempotencia en serie y concurrente, más la migración sobre una base existente.
- `M09b-idempotencia-aprobacion-proveedor.txt`: aprobación y alta de proveedor, con mutantes.
- `M12-moneda-reembolso.txt` y `M12b-moneda-conciliacion.txt`: moneda al lado del importe, migración y mutantes.
- `L2PEDIDO-retiro-ofertas.txt`: retiro del consumidor de ofertas y del pacto con `entregas`.
- `M10b-permisos-y-levantamiento.txt`: semillas idempotentes, ruta y matriz negativa.
- `M11-cobro-sin-confianza-en-el-cuerpo.txt`: contrato 1.1.0, pruebas web y de integración, mutantes, verificadores e impacto en la app.
- `M10-bloqueo-autoridad.txt`: cuatro ojos, liberación de la retención y mutante.
- `M08-hechos-inmutables.txt`: sellado, historial, concurrencia, humo del esquema y mutante.
- `P02a-periodo-recaudado.txt`: evento `periodo_recaudado` COMPLETO, pacto con `entregas`, concurrencia y mutante sin cerrojo.
- `M05-*`, `M04-*`, `F2M01-*`: secreto del webhook, desembolso fail-closed y consumidor de respaldo.

## No cubierto
- **Integración con Testcontainers.** Las pruebas Java corrieron contra PostgreSQL 16 local, con el gancho `APORTAYA_TEST_DB_URL`, el mismo de `BaseDeCU04` de identidad. Lo agregué a `BaseDeBilletera` (#425) y a `BaseDeAportes` (#430). Las clases que levantan su propio contenedor no corrieron.
- **RLS desde Java.** La conexión de prueba es como superusuario. Las invariantes nuevas se probaron con `psql` y `SET ROLE`.
- **Cobertura jacoco local.** Maven Central respondió 429 y no la pude correr. La mide el CI.
- **Camino HTTP de punta a punta** con los servicios levantados: no se ejercitó.
- **`periodo_recaudado` contra el consumidor real.** La prueba de productor compara con el pacto propuesto por L2 (#490); no corrió el consumidor de `entregas` ni Kafka. Tampoco se probó el pago que entra por webhook o conciliación: el aviso cuelga de `CU21CobrarAporte.acreditar`, que es el camino común, pero el webhook no se ejercitó de punta a punta.
- **Cuotas `ANULADO`.** El conteo las excluye; un periodo con todas sus cuotas anuladas no emite porque ningún pago lo completa. No hay prueba de ese borde.

## Desvíos del plan
- F1.L1.M03 usa una tabla nueva, `solicitud_reverso`, y no el estado `SOLICITADO` de `reverso_transaccion`. El motivo: R-SEG-04 (sección compartida de `Restricciones.md`) exige `autorizada_por NOT NULL` en esa tabla. Precedente: `solicitud_cierre_billetera`.
- F1.L1.M01 no cambió el tipo `Dinero` de las entradas de `nucleo-financiero`, para no romper la compilación de la app (L6).
- F1.L1.M07 se acotó a estado y reembolso. El resto cruza servicios.
- #447 va apilado sobre #425 porque los dos agregan una regla R-BIL y suben la misma cifra.
- F1.L1.M08 usa el nombre `historial_estado_*`, el patrón de `historial_estado_entrega`, y no `*_transicion` como decía el anexo (regla 00.1.4). `webhook_pasarela` queda fuera: hay que particionarla primero.
- F1.L1.M08 actualizó a mano la cifra fija de `sql/50_verificacion/prueba_humo.sql`, de 47 a 51 tablas selladas. Es un archivo escrito a mano que el generador conserva, y el propio archivo pide subir esa cifra con `APPEND_ONLY`. Lo declaré en #455.
- F1.L1.M12 deja las columnas nullable (fase expandir, regla 97.3.5). El anexo pedía `NOT NULL`; contraer va después de rellenar las filas viejas.
- `CU19Test.concurrencia` se actualizó al requisito de M09: el reintento del mismo aprobador recibe la misma respuesta. Conserva la aserción de efecto único y el rechazo para otra persona.
- L1.P01 borró pruebas que ejercitaban la compra del mercado, una función eliminada por el negocio (D-L2-1). La justificación escrita está en #461 (regla 80.5.5).
- Archivos compartidos tocados, todos mecánicos o derivados, cada uno declarado en su PR:
  - cifras de tablas, restricciones y entidades en `README.md`, `planes/*` y `docs/Seguridad.md`;
  - una línea en `COLUMNAS_AGREGADAS` de `scripts/generar_ddl.py`;
  - catálogo de datos y notas de `docs/Modelos` regenerados;
  - ejemplos de `packages/simulado` para el contrato del reverso.

## Riesgos residuales
- **CI de #418, `Los cinco corredores`.**
  - Primera corrida: `plataforma:gateway` `ArranqueRateLimitGatewayTest` es intermitente, porque depende del tiempo de reposición del balde. Al relanzar el job pasó.
  - Re-ejecución: `servicios:entregas` cobertura de ramas 0,70 < 0,71.
  - Ninguno de los dos módulos lo toca el PR. Los dejo registrados para los dueños de `plataforma` y de L2. No bajé umbrales ni subí timeouts.
- **CI de #436, #441 y #445.**
  - `Los cinco corredores` falla por la misma cobertura de ramas de `servicios:entregas` (0,70 < 0,71). Ninguno de esos PR toca `entregas` (L2).
  - #445 tuvo además un rojo EXTERNAL en `Dependencias, secretos e imagen`: el repositorio de plugins de Gradle no resolvió `kotlin-stdlib:2.4.0`.
  - No bajé umbrales. Para el dueño de L2.
- **`CU40Test` de `nucleo-financiero`.** En local falla con `fn_lim_evaluar` también sin mis cambios (verificado con `git stash`): es previo.
- **`webhook_pasarela` sigue mutable.** Particionarla choca con la unicidad `uq_webhook_idem`, porque en una tabla particionada el `UNIQUE` debe incluir la clave de partición. La deduplicación tendría que vivir en una tabla aparte. Es una decisión de diseño a registrar antes de F1.L1.M08 parte 2.
- **Fallo cerrado de `grupos`.** Con #441, `grupos` recibe 404 cuando pide el estado de otra persona: la contraparte de una permuta o el titular en un traspaso. Cae a «no al día» (falla cerrado). Esas rutas están decididas para eliminarse (tanda 7).
- **Filas viejas.** Las filas de `saldo_diario_billetera` cerradas antes de #436 tienen el sello viejo. Si se verifica la cadena sobre ellas, aparece una ruptura en el primer día nuevo.

## Decisiones y ambigüedades
- **Cuándo un periodo está "recaudado" (COMPLETO):** cuando no le queda ninguna obligación `APORTE_PERIODICO` en un estado distinto de `PAGADO` o `ANULADO`. No cuenta recargos ni otros tipos, y una cuota cubierta por el fondo de garantía (no `PAGADO`) hoy no completa el periodo: esa rama es GRACIA_VENCIDA. Confirmar con L2 en #490 y con Pablo.
- **El aviso se emite desde `acreditar`, no solo desde el webhook firmado:** el pacto dice "confirma el último aporte por webhook firmado", pero el pago con saldo (BILLETERA_MOVIL) también deja la cuota `PAGADO` y entra por el mismo `acreditar`. Se emite en cualquier camino que salde la última cuota. Comentado en #490.
- **AMB-L1-3** (bloqueo judicial como acción humana, con dos personas): supuesto del anexo. Confirmar con Cumplimiento antes de F1.L1.M10.
- **Idempotencia de la aprobación por estado + actor:** no se persiste el valor del `Idempotency-Key` de la aprobación. La unicidad la da la única salida de `SOLICITADO` (R-BIL-30). Confirmar con Pablo si quiere la clave guardada.
- **Clave de alta reusada para otro código:** responde `AP-CU99-01`; no hay un código propio en el catálogo de CU-99. Confirmar si se agrega uno.
- **Permiso para solicitar un reverso:** se usa `REVERSO_AUTORIZAR` para los dos actos. No existe `REVERSO_SOLICITAR` en el catálogo, que es compartido. `CuatroOjos` garantiza que sean dos personas. Confirmar con Pablo si quiere un permiso separado.
