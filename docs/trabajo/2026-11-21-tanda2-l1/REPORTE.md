# Reporte — Tanda 2, lote L1 (`aportes` y `nucleo-financiero`)

> **AVANCE: 8 / 18 — 44,4 %.** Trabajo en curso. Nada en rojo por código propio; dos fallas del CI de #418 son de módulos ajenos (ver Riesgos).

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 50cf3be9
- PR hacia `dev`: #430 mergeado. Abiertos (sin mergear): #418, #425, #436, #441, #445, #447, #449, #450, #451, #452, #454, #455, #456. Pila a mergear en orden: #425 → #447 → #449 → #454 → #455 → #456.
- Peldaño de evidencia por área:
  - `aportes` y `nucleo-financiero` (código): `TESTED`, con pruebas dirigidas en verde contra PostgreSQL 16 real local y controles negativos (mutantes) que fallan sin el cambio. No llega a `VERIFIED`: el camino HTTP de punta a punta con los servicios levantados no se ejercitó.
  - Integración con Testcontainers: las clases que levantan su propio contenedor (`AislamientoEsquemaTest`, `ArranqueTest`) quedan **NO COMPROBADAS: requieren Docker** (máquina de Pablo).

Cálculo del avance: microtareas del plan = 14 de H1 + 4 de H2 = 18. `HECHO` = L1.M00, F1.L1.M03, F1.L1.M04, F1.L1.M05, F1.L1.M06, F1.L1.M09, F1.L1.M12 y F1.L1.M13. Las que quedaron `A MEDIAS` no suman. F1.S4.M2 quedó `DESCARTADO` para L1 (no suma y se declara abajo).

## Completado

| ID | Qué se logró (observable) | Comando | Resultado |
|---|---|---|---|
| L1.M00 | Los logs de CU-10, CU-12 y CU-21 ya no escriben el importe. Un barrido por servicio lo impide en adelante. | `./gradlew :servicios:{aportes,nucleo-financiero}:testBarrido` | PASS. Con el código de `dev`, el barrido falla (control negativo). `evidencia/M00-logs-sin-importes.txt` · PR #418 |
| F1.L1.M03 | Un reverso necesita dos actos y dos tokens. `autorizadaPor` desaparece del contrato. La misma persona recibe 422 `AP-CU14-02`. La base lo exige con R-BIL-27. | `webTest ReversoCuatroOjosWebTest` (7/7) + `integrationTest` de 12 clases que reversan (76/76) + psql con `SET ROLE svc_nucleo_financiero` | PASS. El mutante sin la segunda firma hace fallar 2 pruebas. `evidencia/M03-*` · PR #425 |
| F1.L1.M06 | La firma del webhook cubre `X-Timestamp`. Un reenvío con otro timestamp da `FIRMA_INVALIDA` y no escribe nada. | `test VerificadorDeFirmaWebhookTest` (12/12) + `integrationTest CU100WebhookTest` (9/9) | PASS. El mutante que firma solo el cuerpo hace fallar 4 pruebas. `evidencia/M06-firma-timestamp.txt` · PR #430 |
| F1.L1.M05 | El secreto del webhook de `aportes` sale de `APORTES_WEBHOOK_SECRETO`, sin valor por omisión. Si falta o es corto, el servicio no arranca. No queda ningún literal en `src/main`. | `test SecretoWebhookDeEntornoTest` (2/2) + grep | PASS. `evidencia/M05-secreto-webhook.txt` · PR #450. **Antes de desplegar hay que cargar la variable en Coolify.** |
| F1.L1.M04 | `nucleo-financiero` arranca con perfil productivo sin `@MockitoBean`. Instruir un retiro devuelve `AP-CU11-15 PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO`. | `ArranqueProduccionTest` contra la Postgres local (2/2) + `ProveedorDeRetiroNoConfiguradoTest` (2/2) | PASS. Sin el adaptador: «No qualifying bean». `evidencia/M04-desembolso-fail-closed.txt` · PR #451 |
| F1.L1.M13 | El sello del cierre diario cubre cuenta, fecha, moneda, saldos y el sello anterior. `primeraRuptura` señala el primer día alterado. | `test SelloDelDiaTest` (6/6) + `integrationTest CU51* CU15*` | PASS. El mutante con el sello viejo hace fallar 6 pruebas. `evidencia/M13-sello-cierre.txt` · PR #436 |
| F1.L1.M09 | Solicitud de reembolso, aprobación y alta de proveedor son idempotentes. La misma clave devuelve el mismo id y deja una sola fila, en serie y en concurrencia. R-BIL-28 y R-BIL-31 están en la base. | `integrationTest CU19IdempotenciaTest` (6/6) + `CU99IdempotenciaTest` (4/4) | PASS. Los mutantes hacen fallar 5 pruebas. `evidencia/M09-*`, `evidencia/M09b-*` · PR #447 y #456 |
| F1.L1.M12 | `reembolso`, `disputa_pago`, `conciliacion`, `excepcion_conciliacion` y `extracto_bancario` llevan moneda (R-BIL-29). Un reembolso de un pago en USD queda en USD. La conciliación anota la moneda del pago. | `integrationTest CU19MonedaTest` (3/3) + `CU132ConciliacionTest` (3/3) + migración sobre una base existente | PASS. Los mutantes fallan. `evidencia/M12-*`, `evidencia/M12b-*` · PR #449 y #454 |

## A medias

### F1.L1.M07 — BOLA en `aportes` (PR #441)
- **Qué anda:** el estado de un participante ajeno y el reembolso de un pago ajeno devuelven 404 sin cuerpo. Están probadas la matriz negativa web y `AutorizacionNegativaTest` 4/4 contra la base, con un doble declarado de `grupos`. El mutante hace fallar 2 pruebas.
- **Qué no anda:** `morosos`, `primera-cuota` y `recaudo` siguen autorizados solo por rol.
- **Qué falta exactamente:** que `grupos` (L3) consulte `morosos` y `primera-cuota` con identidad de servicio, y que `entregas` y `garantia` (L2) consulten `recaudo` también con identidad de servicio. Después, `aportes` exige `SERVICIO_*` o pertenencia. Hoy `grupos` las consulta con el token de un postulante que todavía no es miembro (`MapeoDePostulacion.java:45`) y en el sorteo.
- **Dónde quedó:** rama `correcciones/tanda2-l1-bola-aportes`. Compila y las suites de `aportes` están en verde.

### F1.L1.M01 — Importe nunca negativo, parte API (PR #445)
- **Qué anda:** en `aportes`, el esquema `Importe` en reembolso y disputa devuelve 400 `AP-VAL-01` con el campo. En el cobro y en todas las entradas de `nucleo-financiero`, un importe negativo da 400 `AP-VAL-02` en el borde. El mutante hace fallar 4 pruebas.
- **Qué no anda:** en `nucleo-financiero` y en el cobro, el 400 es genérico y no señala el campo.
- **Qué falta exactamente:** usar `Importe` en esas entradas. Eso cambia el tipo que construye la app en `apps/movil/lib/dominio/cu10_recargar_saldo.dart:41`, `cu11_retirar_saldo.dart:37`, `cu12_transferir_saldo.dart:42` y `pantallas/pasanaku/dominio/cu21_cobrar_aporte.dart:39` (L6). Además falta el tipo `Importe` en `plataforma/comun-dominio`.
- **Dónde quedó:** rama `correcciones/tanda2-l1-importe-contrato`. Compila y está en verde.

### F1.L1.M08 — Hechos de dinero inmutables (PR #455, apilado sobre #454)
- **Qué anda:**
  - `pago`, `reembolso` y sus dos historiales de estado son append-only: trigger R-AUD-01 y `REVOKE UPDATE, DELETE` a `svc_aportes`, generados desde `APPEND_ONLY` (`scripts/modelo.py`).
  - El estado cambia agregando una fila de historial, y desde cada estado se sale una sola vez (R-BIL-30).
  - `AppendOnlyTest` 9/9. `UPDATE` como `svc_aportes` da `permission denied` y, como superusuario, `R-AUD-01`. Dos reversos concurrentes dejan una sola fila.
  - Integración de `aportes`: 141/144, y las 3 en rojo son las de Docker. El mutante hace fallar 5 pruebas. El humo del esquema da 0 FALLA.
- **Qué no anda:** `webhook_pasarela` sigue cambiando su estado con `UPDATE`.
- **Qué falta exactamente:** particionar `webhook_pasarela` por fecha, como `bitacora_evento`. Su retención es ELIMINAR a los 24 meses, y una tabla append-only solo se purga con DETACH + DROP de la partición. Después, sellarla y llevar su estado a `webhook_transicion` o `historial_estado_webhook`.
- **Dónde quedó:** rama `correcciones/tanda2-l1-hechos-inmutables`. Compila y está en verde.

### F2.L1.M01 — Consumidor de respaldo y cobertura empresarial (PR #452)
- **Qué anda:** el listener escucha solo `entregas.desembolso_acreditado`, y una prueba fija esos tópicos. `CAJA_EMPRESA` salió de la configuración y del compose.
- **Qué no anda:** quedan los mapeos `RESERVA_RESPALDO`, `POZO_GRUPO` y `DESEMBOLSO`, que apuntan a las cuentas 9.x.
- **Qué falta exactamente:** las cuentas del fondo del grupo del modelo A (F3) para reemplazar esas cuentas. Después, F2.L1.M02. También que L2 retire el productor en `garantia`.
- **Dónde quedó:** rama `correcciones/tanda2-l1-retiro-consumidor-respaldo`. Compila y está en verde.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| F1.L1.M11 | TODO | Lo ejecuta esta sesión. Cambia el contrato que usa `apps/movil/.../cu21_cobrar_aporte.dart` (L6): hay que coordinar el cambio de la app en el mismo PR o en uno de L6. |
| F1.L1.M10 | TODO | Los permisos nuevos se siembran en `sql/60_semillas`/`seeders` (compartido). AMB-L1-3: confirmar con Cumplimiento. |
| F2.L1.M02 | TODO | Depende de F3: el árbol 9.x lo usan `POZO_GRUPO` y `DESEMBOLSO`, que asientan el desembolso vigente. Sacarlo antes rompe ese asiento. |
| F2.L1.M03 | TODO | Depende de F1.L1.M11 (el contrato del cobro deja de llevar `canal`) y de que la app (L6) no ofrezca los canales retirados. |
| F2.L1.M04 | TODO | Necesita el evento de apertura de período de `grupos` (L3) y su pacto, o un trabajo con ShedLock, y el ADR de inicio automático (F0·ADR-054). |
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
- `M08-hechos-inmutables.txt`: sellado, historial, concurrencia, humo del esquema y mutante.
- `M05-*`, `M04-*`, `F2M01-*`: secreto del webhook, desembolso fail-closed y consumidor de respaldo.

## No cubierto
- **Integración con Testcontainers.** Las pruebas Java corrieron contra PostgreSQL 16 local, con el gancho `APORTAYA_TEST_DB_URL`, el mismo de `BaseDeCU04` de identidad. Lo agregué a `BaseDeBilletera` (#425) y a `BaseDeAportes` (#430). Las clases que levantan su propio contenedor no corrieron.
- **RLS desde Java.** La conexión de prueba es como superusuario. Las invariantes nuevas se probaron con `psql` y `SET ROLE`.
- **Cobertura jacoco local.** Maven Central respondió 429 y no la pude correr. La mide el CI.
- **Camino HTTP de punta a punta** con los servicios levantados: no se ejercitó.

## Desvíos del plan
- F1.L1.M03 usa una tabla nueva, `solicitud_reverso`, y no el estado `SOLICITADO` de `reverso_transaccion`. El motivo: R-SEG-04 (sección compartida de `Restricciones.md`) exige `autorizada_por NOT NULL` en esa tabla. Precedente: `solicitud_cierre_billetera`.
- F1.L1.M01 no cambió el tipo `Dinero` de las entradas de `nucleo-financiero`, para no romper la compilación de la app (L6).
- F1.L1.M07 se acotó a estado y reembolso. El resto cruza servicios.
- #447 va apilado sobre #425 porque los dos agregan una regla R-BIL y suben la misma cifra.
- F1.L1.M08 usa el nombre `historial_estado_*`, el patrón de `historial_estado_entrega`, y no `*_transicion` como decía el anexo (regla 00.1.4). `webhook_pasarela` queda fuera: hay que particionarla primero.
- F1.L1.M08 actualizó a mano la cifra fija de `sql/50_verificacion/prueba_humo.sql`, de 47 a 51 tablas selladas. Es un archivo escrito a mano que el generador conserva, y el propio archivo pide subir esa cifra con `APPEND_ONLY`. Lo declaré en #455.
- F1.L1.M12 deja las columnas nullable (fase expandir, regla 97.3.5). El anexo pedía `NOT NULL`; contraer va después de rellenar las filas viejas.
- `CU19Test.concurrencia` se actualizó al requisito de M09: el reintento del mismo aprobador recibe la misma respuesta. Conserva la aserción de efecto único y el rechazo para otra persona.
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
- **Fallo cerrado de `grupos`.** Con #441, `grupos` recibe 404 cuando pide el estado de otra persona: la contraparte de una permuta o el titular en un traspaso. Cae a «no al día» (falla cerrado). Esas rutas están decididas para eliminarse (tanda 7).
- **Filas viejas.** Las filas de `saldo_diario_billetera` cerradas antes de #436 tienen el sello viejo. Si se verifica la cadena sobre ellas, aparece una ruptura en el primer día nuevo.

## Decisiones y ambigüedades
- **AMB-L1-3** (bloqueo judicial como acción humana, con dos personas): supuesto del anexo. Confirmar con Cumplimiento antes de F1.L1.M10.
- **Idempotencia de la aprobación por estado + actor:** no se persiste el valor del `Idempotency-Key` de la aprobación. La unicidad la da la única salida de `SOLICITADO` (R-BIL-30). Confirmar con Pablo si quiere la clave guardada.
- **Clave de alta reusada para otro código:** responde `AP-CU99-01`; no hay un código propio en el catálogo de CU-99. Confirmar si se agrega uno.
- **Permiso para solicitar un reverso:** se usa `REVERSO_AUTORIZAR` para los dos actos. No existe `REVERSO_SOLICITAR` en el catálogo, que es compartido. `CuatroOjos` garantiza que sean dos personas. Confirmar con Pablo si quiere un permiso separado.
