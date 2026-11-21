# Reporte — Tanda 2, lote L1 (`aportes` y `nucleo-financiero`)

> **AVANCE: 4 / 18 — 22,2 %.** Trabajo en curso. Nada en rojo por código propio; dos fallas del CI de #418 son de módulos ajenos (ver Riesgos).

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 50cf3be9
- PR abiertos hacia `dev` (sin mergear): #418, #425, #430, #436, #441, #445, #447
- Peldaño de evidencia por área:
  - `aportes` y `nucleo-financiero` (código): `TESTED`, con pruebas dirigidas en verde contra PostgreSQL 16 real local y controles negativos (mutantes) que fallan sin el cambio. No llega a `VERIFIED`: el camino HTTP de punta a punta con los servicios levantados no se ejercitó.
  - Integración con Testcontainers: las clases que levantan su propio contenedor (`AislamientoEsquemaTest`, `ArranqueTest`) quedan **NO COMPROBADAS: requieren Docker** (máquina de Pablo).

Cálculo del avance: microtareas del plan = 14 de H1 + 4 de H2 = 18. `HECHO` = L1.M00, F1.L1.M03, F1.L1.M06 y F1.L1.M13. Las que quedaron `A MEDIAS` no suman. F1.S4.M2 quedó `DESCARTADO` para L1 (no suma y se declara abajo).

## Completado

| ID | Qué se logró (observable) | Comando | Resultado |
|---|---|---|---|
| L1.M00 | Los logs de CU-10, CU-12 y CU-21 ya no escriben el importe. Un barrido por servicio lo impide en adelante. | `./gradlew :servicios:{aportes,nucleo-financiero}:testBarrido` | PASS. Con el código de `dev`, el barrido falla (control negativo). `evidencia/M00-logs-sin-importes.txt` · PR #418 |
| F1.L1.M03 | Un reverso necesita dos actos y dos tokens. `autorizadaPor` desaparece del contrato. La misma persona recibe 422 `AP-CU14-02`. La base lo exige con R-BIL-27. | `webTest ReversoCuatroOjosWebTest` (7/7) + `integrationTest` de 12 clases que reversan (76/76) + psql con `SET ROLE svc_nucleo_financiero` | PASS. El mutante sin la segunda firma hace fallar 2 pruebas. `evidencia/M03-*` · PR #425 |
| F1.L1.M06 | La firma del webhook cubre `X-Timestamp`. Un reenvío con otro timestamp da `FIRMA_INVALIDA` y no escribe nada. | `test VerificadorDeFirmaWebhookTest` (12/12) + `integrationTest CU100WebhookTest` (9/9) | PASS. El mutante que firma solo el cuerpo hace fallar 4 pruebas. `evidencia/M06-firma-timestamp.txt` · PR #430 |
| F1.L1.M13 | El sello del cierre diario cubre cuenta, fecha, moneda, saldos y el sello anterior. `primeraRuptura` señala el primer día alterado. | `test SelloDelDiaTest` (6/6) + `integrationTest CU51* CU15*` | PASS. El mutante con el sello viejo hace fallar 6 pruebas. `evidencia/M13-sello-cierre.txt` · PR #436 |

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

### F1.L1.M09 — Idempotencia del reembolso (PR #447, apilado sobre #425)
- **Qué anda:** la solicitud de reembolso con la misma clave, en serie o con 8 envíos concurrentes, devuelve un solo id y deja una sola fila. R-BIL-28 queda en la base. La migración sobre una base existente se probó.
- **Qué no anda:** la aprobación y el alta de proveedor todavía no usan la clave. Un reintento de la aprobación recibe 422, aunque no ejecuta dos veces.
- **Qué falta exactamente:** agregar la clave a la transición de aprobación y a `proveedor_pago`, y después contraer: `NOT NULL` y `UNIQUE` total cuando no queden filas sin clave.
- **Dónde quedó:** rama `correcciones/tanda2-l1-reembolso-idempotente`. Compila y está en verde.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| F1.L1.M11 | TODO | Lo ejecuta esta sesión. Cambia el contrato que usa `apps/movil/.../cu21_cobrar_aporte.dart` (L6): hay que coordinar el cambio de la app en el mismo PR o en uno de L6. |
| F1.L1.M05 | TODO | Lo ejecuta esta sesión. La variable va en `docker-compose.coolify.yml`, que es compartido: se hace la parte del servicio y se describe el cambio exacto del compose. |
| F1.L1.M04 | TODO | Depende de F0·PUERTOS (puerto de desembolso). Se puede hacer contra un adaptador fail-closed (regla 65). |
| F1.L1.M10 | TODO | Los permisos nuevos se siembran en `sql/60_semillas`/`seeders` (compartido). AMB-L1-3: confirmar con Cumplimiento. |
| F1.L1.M12 | TODO | Lo ejecuta esta sesión (cambio de esquema, por expansión). |
| F1.L1.M08 | TODO | Es el cambio de esquema más grande (tablas de transición). Lo ejecuta esta sesión. |
| F2.L1.M01–M04 | TODO | Después de F1. M01 está coordinado con el retiro del productor en `garantia` (L2). |
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
- **Permiso para solicitar un reverso:** se usa `REVERSO_AUTORIZAR` para los dos actos. No existe `REVERSO_SOLICITAR` en el catálogo, que es compartido. `CuatroOjos` garantiza que sean dos personas. Confirmar con Pablo si quiere un permiso separado.
