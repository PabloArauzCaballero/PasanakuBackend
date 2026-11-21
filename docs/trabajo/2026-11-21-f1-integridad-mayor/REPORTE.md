# Reporte — F1.S1: el mayor y los importes, garantizados por la base

> **AVANCE: 13 / 15 — 86,7 %.** Nada en rojo. Dos microtareas quedan TODO en otros carriles (API 422 y clave en tablas sin columna); la integración Java contra la base no se corrió (sin Docker).

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `correcciones/f1-integridad-mayor-2026-11-21` (base `origin/dev` 850e0f51)
- PR: __PR__
- Peldaño de evidencia alcanzado:
  - Esquema (M1, M3): `TESTED` contra PostgreSQL 16 real con el rol de la aplicación (`SET ROLE svc_*`), en
    tres niveles, con regresión del esquema en verde. **No** `VERIFIED`: el camino de punta a punta por la API
    no se ejercitó, y las pruebas de integración Java (Testcontainers) no corren sin Docker.
  - M2 y M4a: `TESTED` (ya resueltos en `dev`, demostrado con guion y control negativo).
- Hallazgos de origen: L5 `TYA-DINERO-001` (S1, reproducido acá en `dev` y cerrado en la base), L1
  `ANF-DINERO-001` (S1, parte base), L1 `ANF-DINERO-004` (S2).

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H1.S1.M1 | Reproducción en `origin/dev` de los 5 casos de TYA-DINERO-001 más uno nuevo (`SET CONSTRAINTS ALL IMMEDIATE` + línea descuadrante) | `psql -f evidencia/repro_tya_dinero_001.sql` y `repro_set_constraints.sql` | Entran 5 asientos anómalos — `evidencia/M0a-*.txt` |
| H1.S1.M2 | Línea base de regresión del esquema en `dev` | `evidencia/regresion.sh reg_base` | humo `OK=165 FALLA=0`, verificaciones 0 filas — `evidencia/M0b-linea-base.txt` |
| H1.S2.M1 | La base rechaza una línea del mayor negativa, con dos lados o en cero (R-AUD-13, `ck_movimiento_contable_un_lado`) | `psql -f evidencia/repro_tya_dinero_001.sql` como `svc_nucleo_financiero` | PASS: `23514 ck_movimiento_contable_un_lado` — `evidencia/M1-mayor-despues.txt` |
| H1.S2.M2 | Una línea solo entra a un asiento en firme creado en la misma transacción (R-AUD-14); los savepoints siguen funcionando | ídem + `verificar_mayor_valido.sql` | PASS: línea y par tardíos → `R-AUD-14`; asiento, contra-asiento y savepoint aceptados |
| H1.S2.M3 | Asiento en firme sin líneas → rechazado; el cuadre se revalida desde cada línea | ídem + `repro_set_constraints.sql` | PASS: `R-AUD-05 ... 0 lineas` y `R-AUD-05 descuadrado (debe=1099.00, haber=100.00)` |
| H1.S2.M4 | `verificaciones.sql` encuentra los asientos anómalos que hayan entrado antes | `psql -f sql/50_verificacion/verificaciones.sql` | PASS: 5 filas sobre la base de `dev` con las anomalías; 0 sobre el esquema nuevo — `evidencia/M1d-verificaciones.txt` |
| H1.S3.M1 | `UPDATE` y `DELETE` dan `permission denied` en las 97 tablas append-only (47 + 50 particiones) con cada rol de aplicación que escribe en ellas | `psql -f evidencia/verificar_append_only.sql` | PASS: 1650 intentos, 0 permitidos; control negativo (un `GRANT UPDATE` a propósito) detectado — `evidencia/M2-*.txt` |
| H1.S3.M2 | Las 6 categorías de la regla 91.1.3 están selladas | mismo guion | PASS: 6/6 |
| H1.S4.M1 | Importes de aporte, pago, reembolso y disputa nunca negativos (9 columnas), también al re-aplicar sobre una base existente | `psql -f evidencia/verificar_importes.sql` como `svc_aportes` | Antes 18/29 (11 negativos aceptados); después 29/29 — `evidencia/M3-importes-*.txt` |
| H1.S5.M1 | Las 33 columnas `clave_idempotencia` tienen índice `UNIQUE` nombrado; la clave repetida se rechaza con `23505` por ese índice | `psql -f evidencia/verificar_idempotencia.sql` | PASS: 0 sin `UNIQUE`; 10/10 tablas ejercitadas rechazadas — `evidencia/M4a-idempotencia.txt` |
| H1.S6.M1 | Sin deriva modelo → `sql/` → base; regeneración idempotente | `python3 -I evidencia/verificar_deriva.py` + `generar_ddl.py` dos veces | PASS: 0 problemas en 7 tablas; control negativo con 3 defectos sembrados detectado; segunda corrida sin diff — `evidencia/M5a-deriva.txt` |
| H1.S6.M2 | Regresión del esquema completo | `evidencia/regresion.sh reg_final` | PASS: humo idéntico a `dev` (`OK=165 FALLA=0`), semillas dev cargan, verificaciones 0 filas — `evidencia/M5b-regresion.txt` |
| H1.S6.M3 | Las suites `test` de `nucleo-financiero` y `aportes` siguen en verde, con jOOQ regenerado desde la base nueva | `./gradlew :servicios:nucleo-financiero:test` y `:servicios:aportes:test` (`--max-workers=1`) | PASS: `BUILD SUCCESSFUL`, 94/94 y 53/53, 0 fallos, 0 saltadas — `evidencia/M5c-*.txt` |

## A medias

### H1.S4.M2 — Respuesta 422 de la API ante un reembolso negativo
- Qué anda: la base rechaza el reembolso negativo (`ck_reembolso_monto`, demostrado con `svc_aportes`).
- Qué no anda: la API no se probó; un monto negativo que pase el contrato llegaría a la base y el error
  `23514` no está mapeado a 422 (no se verificó qué devuelve hoy).
- Qué falta exactamente: el esquema `Importe` en el OpenAPI de `aportes`/`nucleo-financiero`, el tipo
  `Importe` en `comun-dominio` y la prueba web de `PagosController` con monto negativo → 400/422. Es
  F1.L1.M01 partes (1) y (2), del carril L1 (`servicios/aportes`, fuera del alcance de archivos de esta sesión).
- Dónde quedó: no se tocó código Java. La parte base está en esta rama.

### H1.S5.M2 — `clave_idempotencia` en las tablas de dinero que no la tienen, y captura como 200 con el mismo `id`
- Qué anda: toda tabla que ya tiene la columna tiene su `UNIQUE` y rechaza el duplicado (M4a).
- Qué no anda: `aportes.reembolso`, `garantia.movimiento_fondo`, `garantia.abono_recuperacion`,
  `nucleo_financiero.retencion_saldo`, `tarifas.devengo`/`devolucion_comision` y `entregas.deduccion_entrega`
  no tienen la columna; la violación de unicidad no se captura como respuesta idempotente en los casos de uso.
- Qué falta exactamente: agregar la columna `NOT NULL` con su `UNIQUE` nombrado exige que el caso de uso la
  reciba (hoy `PagosController` descarta el `Idempotency-Key`): cambio de contrato y de caso de uso por
  servicio. Está asignado en los anexos: F1.L1.M09 (reembolso, depende de F1.L1.M08), F1.L5.M07 y F1.L2.M11
  (garantía, depende de F2). Agregar solo la columna desde acá rompería los `INSERT` de jOOQ o quedaría
  anulable y sin efecto.
- Dónde quedó: sin cambios para esta parte.


## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Pruebas de integración Java contra la base (Testcontainers) de `nucleo-financiero` y `aportes` | TODO — `NO COMPROBADO — requiere Docker (máquina de Pablo)` | Correr en la máquina de Pablo, con Docker activo y en esta rama: `./gradlew :servicios:nucleo-financiero:integrationTest :servicios:aportes:integrationTest --max-workers=1`. Las más sensibles al cambio: `CU24Test`, `CU24RechazosTest`, `CU24MonedaTest`, `CU10RechazosTest`, `CU12RechazosTest`, `CU51Test`, `AppendOnlyTest`, `CU19Test`, `CU19RechazosTest`, `CU21Test` y, en `erp`, `CU100Test` (inserta en el mayor) |
| Prueba de integración Java que reproduzca TYA-DINERO-001 con el rol `svc_nucleo_financiero` (F1.L5.M06 DoD) | TODO | Se escribe en `servicios/nucleo-financiero/src/test` (carril L1/L5) sobre `BaseDeCU24.rechazaLaBase`; hoy cubierto con `psql` |

## Evidencia

Todo en [`evidencia/`](./evidencia/). Base: PostgreSQL 16 local (puerto 5433), esquema aplicado con
`sql/aplicar.sql`, semillas `60_semillas/sembrar.sql` y `61_dev/sembrar_dev.sql`. Datos sintéticos (UUID fijos
`00000000-…-a1` y similares); ningún dato de personas.

```text
$ psql -f evidencia/repro_tya_dinero_001.sql   # origin/dev
 00000000-0000-0000-0000-0000000000a1 | CONFIRMADO |      5 | 180.00 | 130.00
 00000000-0000-0000-0000-0000000000a2 | CONFIRMADO |      2 | -10.00 | -10.00
 00000000-0000-0000-0000-0000000000a3 | CONFIRMADO |      1 |  20.00 |  20.00
 00000000-0000-0000-0000-0000000000a4 | CONFIRMADO |      0 |      0 |      0

$ psql -f evidencia/repro_tya_dinero_001.sql   # esquema de la rama
ERROR:  R-AUD-14: el asiento 00000000-0000-0000-0000-0000000000a1 ya esta en firme; una linea nueva no entra (corrija con un contra-asiento)
ERROR:  R-AUD-14: el asiento 00000000-0000-0000-0000-0000000000a1 ya esta en firme; una linea nueva no entra (corrija con un contra-asiento)
ERROR:  new row for relation "movimiento_contable" violates check constraint "ck_movimiento_contable_un_lado"
ERROR:  new row for relation "movimiento_contable" violates check constraint "ck_movimiento_contable_un_lado"
ERROR:  R-AUD-05: asiento 00000000-0000-0000-0000-0000000000a4 en firme con 0 lineas; necesita al menos dos
 00000000-0000-0000-0000-0000000000a1 | CONFIRMADO |      2 | 100.00 | 100.00

$ psql -f evidencia/repro_set_constraints.sql   # esquema de la rama
ERROR:  R-AUD-05: asiento 00000000-0000-0000-0000-0000000000b1 descuadrado (debe=1099.00, haber=100.00)

$ psql -f evidencia/verificar_append_only.sql
 permission denied | DELETE |      825 |     97
 permission denied | UPDATE |      825 |     97
--- combinaciones PERMITIDAS (esperado: 0 filas)
(0 rows)

$ psql -f evidencia/verificar_importes.sql   # antes / después
   18 |   11
   29 |    0

$ psql -f evidencia/verificar_idempotencia.sql
 columnas_sin_unique = 0 ; 10 tablas → RECHAZADO 23505 <índice de la clave>

$ python3 -I evidencia/verificar_deriva.py
deriva: 0 problema(s)   (control negativo: 3 problema(s), exit=1)

$ evidencia/regresion.sh reg_final
humo: OK=165 FALLA=0 ERROR=1   (el ERROR es el rechazo esperado de R-BIL-01, igual que en dev)
verificaciones: lineas_no_vacias=0
```

```text
$ ./gradlew :servicios:nucleo-financiero:test --console=plain --max-workers=1 -Dorg.gradle.parallel=false
> Task :servicios:nucleo-financiero:compileJava        (generateJooq introspecta la base con el esquema nuevo)
> Task :servicios:nucleo-financiero:test
BUILD SUCCESSFUL in 2m 51s
nucleo-financiero: clases=19 tests=94 failures=0 errors=0 skipped=0

$ ./gradlew :servicios:aportes:test --console=plain --max-workers=1 -Dorg.gradle.parallel=false
BUILD SUCCESSFUL in 24s
aportes: clases=11 tests=53 failures=0 errors=0 skipped=0
```
Las dos primeras corridas de `nucleo-financiero` fallaron en `:buildSrc:compilePluginsBlocks` por `429 Too Many
Requests` de Maven Central (clasificación `EXTERNAL`); la tercera, con `--max-workers=1`, resolvió y pasó. El
corredor `test` excluye por diseño (`buildSrc/.../aportaya.base.gradle.kts`) `CU*Test`, `*RepositorioTest`,
`AppendOnlyTest`, `Libro*Test` y demás que necesitan Testcontainers: esas corren en `integrationTest`.

Como sustituto parcial (no reemplaza correrlas), se reprodujeron en `psql` los dos escenarios de rechazo de las
pruebas de integración que más dependen del orden de los triggers diferidos, para confirmar que el primer error
al `COMMIT` sigue siendo el que esperan (`evidencia/M5c-mensajes-pruebas-java.txt`):

```text
$ psql -d reg_final -f evidencia/verificar_mensajes_pruebas_java.sql
ERROR:  R-AUD-12: el asiento 00000000-0000-0000-0000-0000000000e1 mezcla 2 monedas          <- CU24MonedaTest espera "R-AUD-12"
ERROR:  R-AUD-05: asiento 00000000-0000-0000-0000-0000000000e2 en firme con 1 lineas; necesita al menos dos   <- CU10/CU12/CU24RechazosTest esperan "R-AUD-05"
```
El texto del segundo cambió (antes decía «descuadrado»); las pruebas solo verifican que contenga `R-AUD-05`.

### Regla 91.6 (cambio que toca importes)
1. **Asientos generados:** este cambio no genera asientos; restringe los que se pueden escribir. Asiento y
   contra-asiento sintéticos aceptados: `c1` (debe 100.00 / haber 100.00, `CONFIRMADO`) y `c2` (debe 100.00 /
   haber 100.00, `REVERSADO` → `c1`) — `evidencia/M1-mayor-despues.txt`.
2. **Idempotencia:** el segundo `INSERT` con la misma clave se rechaza con `23505` en 10 tablas de dinero
   (M4a). El reintento del propio esquema: `aplicar.sql` re-aplicado sobre una base existente con datos dev
   termina `exit=0` (`evidencia/M3-importes-despues.txt`) y la regeneración no deja diff.
3. **Cuadre:** consulta nueva de `verificaciones.sql` (asientos en firme con < 2 líneas, descuadrados o con
   líneas inválidas): 0 filas sobre el esquema nuevo, 5 sobre `dev` con las anomalías.
4. **Reversa:** el contra-asiento `REVERSADO` de `c1` se acepta; corregir con una línea más se rechaza con R-AUD-14.
5. **Redondeo:** no aplica; no se calcula ni se divide ningún importe.

## No cubierto
- **Integración Java contra la base** (Testcontainers): `CU24*`, `CU10/12/14/51*`, `AppendOnlyTest`,
  `CU19*`, `CU21*` y demás que usan `BaseDePrueba`. Se revisó por lectura que los fixtures insertan cabecera y
  líneas en la misma transacción y que `Partida` ya exige lo mismo que R-AUD-13, pero **no se ejecutó**.
- **La API**: ningún endpoint se ejercitó; no se sabe qué código HTTP devuelve hoy una violación de estos
  `CHECK` (probablemente 500 si no está mapeada).
- `entregas.fondeo_entrega` y `erp.pago_a_proveedor`: el `UNIQUE` existe pero no hay filas con clave en las
  semillas, así que el duplicado no se ejercitó.
- Asientos en estado `BORRADOR`: R-AUD-14 no les impide recibir líneas en otra transacción (el CA habla de
  asientos confirmados). Como un `BORRADOR` no puede pasar a `CONFIRMADO` (append-only), esas líneas igual
  mueven `cuenta_contable.saldo` vía `tg_movimiento_contable_sincroniza_saldo`. Ver riesgos.
- Sin Docker no se corrió `./gradlew bd:humo` (usa el contenedor); se corrió el mismo `prueba_humo.sql` con
  `psql` sobre una base desechable.

## Desvíos del plan
- **Un PR para F1.S1.M1–M4 en vez de uno por microtarea**: esta sesión tiene una sola rama autorizada. Las
  microtareas comparten causa (la base no garantizaba el mayor) y van en commits separados, uno por microtarea.
- **M1**: además del alcance del anexo, se cerró una variante nueva no reportada por la auditoría
  (`SET CONSTRAINTS ALL IMMEDIATE` dentro de la misma transacción), reproducida en `dev`.
- **M1**: el anexo L5 proponía rechazar «líneas de asientos no creados en la misma transacción»; se
  implementó con el `xmin` del encabezado y `pg_xact_status`. La primera versión rechazaba líneas legítimas
  insertadas tras un `SAVEPOINT` (el xid de una subtransacción es mayor que el de la principal); se detectó con
  `verificar_mayor_valido.sql` y se corrigió antes del commit.
- **M3**: los `CHECK` se registran también en `CK_AMPLIADOS` (`scripts/modelo.py`): sin eso, `CREATE TABLE IF
  NOT EXISTS` no los lleva a una base ya aplicada como TEST. El comentario que emite el generador dice «CHECK
  ampliado» aunque estos son nuevos; se dejó así para no tocar el generador.
- **M3**: no se agregó `moneda` a `reembolso` ni a `disputa_pago` (lo pide el plan maestro: «con su moneda al
  lado»): es F1.L1.M12 del carril L1 y exige cambiar el caso de uso que inserta.

## Riesgos residuales y deuda
- **Datos previos en TEST**: si TEST ya tiene una línea negativa o de dos lados, o un importe negativo en las 9
  columnas, `aplicar.sql` fallará al re-aplicar en el `ADD CONSTRAINT` correspondiente (los `CHECK` se validan
  sobre las filas existentes). Antes de desplegar: correr `sql/50_verificacion/verificaciones.sql` y estas
  consultas sobre TEST. Las filas del mayor no se pueden borrar (append-only): la salida sería un contra-asiento
  y, para la fila inválida en sí, una decisión de Pablo (ver ambigüedades).
- **`BORRADOR`**: ver «No cubierto». Ningún caso de uso de `src/main` crea asientos `BORRADOR` (solo
  `CierreDiarioRepositorio` los consulta y los fixtures de prueba los crean).
- R-AUD-14 depende de `pg_current_xact_id()` y `pg_xact_status()` (PostgreSQL ≥ 13). El proyecto exige 15+.
- `aportes.movimiento_bancario.monto` sigue sin `CHECK` (no hay columna de sentido).
- Importes de billetera (`orden_recarga.monto_acreditado`, `costo_proveedor`, `orden_retiro.monto_neto`,
  `costo_retiro`, `reverso_transaccion.monto_reversado`, `bloqueo_saldo.monto_bloqueado`) y de garantía siguen
  sin `CHECK >= 0`: fuera del alcance de F1.S1.M3 (garantía es F1.L2.M11).

## Decisiones y ambigüedades
- `DECISION_REQUIRED` — Qué hacer si TEST ya tiene filas que violan los `CHECK` nuevos del mayor
  (`movimiento_contable` es append-only: no se pueden corregir ni borrar). Opciones técnicas: reconstruir TEST
  (P-14: no hay dinero real) o crear la restricción `NOT VALID`. A confirmar con Pablo antes del despliegue.
- Ambigüedad — `aportes.movimiento_bancario.monto`: ¿un débito del extracto viene con signo? Supuesto: sin
  `CHECK`. Confirmar con quien diseñe la conciliación (carril L1).
- Ambigüedad — F1.L5.M07 propone `UNIQUE (origen_tipo, origen_id)` en `asiento_contable`, pero el flujo de
  corrección (`AsientoRepositorio.originalSinReversar`) admite varios asientos `CONFIRMADO` del mismo origen
  (original y reemplazo). No se agregó; se anota para el carril L5.
- Decisión técnica — `orden_cobro.monto_exacto` y los importes de `obligacion_aporte` con `>= 0` (no `> 0`):
  la regla 91.2.2 exige «nunca negativo»; si una orden de cobro de 0 debe prohibirse es una decisión del flujo
  de cobro (F3), no se tomó acá. `reembolso.monto > 0` sigue al anexo F1.L1.M01.
- Pruebas con el rol de la aplicación: `SET ROLE svc_<servicio>` desde el superusuario local. Los privilegios
  efectivos son los de ese rol, que es con el que conecta cada servicio (`application.yml`).

## Fuera de alcance encontrado (anotado, no arreglado)
- `sql/README.md` dice «Disparadores 18 + 19 de sellado append-only»: hoy son 97 tablas selladas (47 + 50
  particiones). Documento desactualizado.
- Las tablas append-only que piden otros anexos (`deduccion_entrega`, `fondeo_entrega`, `historial_estado_grupo`,
  hechos de `aportes`, bitácoras de identidad/cumplimiento) siguen fuera de la lista: F4.L2.M09, L3, F1.L1.M08,
  F1.L4.M24.

## Procesos al cerrar
PostgreSQL local apagado con `pg_ctl stop` (ver la salida al final de la sesión). Gradle: el daemon queda vivo (es el comportamiento normal de `./gradlew`); se detiene con `./gradlew --stop`.
