# FASE 1 — causa raiz del 503 de TEST (reproducida, con log)

Entorno: Docker local desechable, PostgreSQL 16 + PgBouncer (modo transaccion, scram) + Redis + MinIO + Kafka,
imagenes `aportaya/<servicio>:test` construidas con `despliegue/Dockerfile` desde este arbol, compose
`docker-compose.coolify.yml` SIN editar (solo memoria/puertos por `despliegue/ensayos/arranque/servicios.override.yml`).
Secretos sinteticos generados al vuelo fuera del repo. Perfil Spring `local` (el de TEST hoy).
Comando: `bash despliegue/ensayos/arranque_completo.sh` con `D3_MODO=individual D3_SOLO="identidad nucleo-financiero gateway"`.

## Veredicto

Se reprodujeron DOS causas independientes. Cada una, sola, deja el backend de TEST sin servir (503/504 en el borde):

| # | Causa | Escenario | Efecto en TEST | Evidencia |
|---|---|---|---|---|
| 1 | **El trabajo de esquema falla sobre una base que ya tiene datos de la version anterior** (`59370bbd`) | C | `esquema` sale con codigo 3; los 15 servicios dependen de `service_completed_successfully` => **no arranca ninguno**, el gateway queda sin backend | `arranque-f1-C-actualizacion.txt` |
| 2 | **`QR_INTERNO_CLAVE` sin cargar en Coolify**: `nucleo-financiero` muere al construir el bean `qrInterno` y reinicia en bucle | B | `nucleo-financiero` en `restarting`; las olas del compose (3a) no completan => las olas siguientes (publicidad, tarifas, transparencia) nunca arrancan; `/api/v1/billetera|contabilidad|custodia` sin backend | `arranque-f1-B-sin-claves.txt`, `log-f1-B-sin-claves-nucleo-financiero.txt` |
| 3 | `INVITACIONES_CLAVE` sin cargar | B | identidad ARRANCA (UP): el valor por omision es vacio. Falla en RUNTIME: `SecretoDeInvitacion.firmar` lanza `IllegalStateException` (clave < 32) => 500 al emitir/validar/consumir invitaciones. No tumba el servicio | `identidad/.../SecretoDeInvitacion.java:25` |

Con base limpia y las dos claves cargadas (escenario A): `identidad` y `nucleo-financiero` quedan `healthy`,
readiness `{"status":"UP"}`, 240 y 264 MiB bajo `mem_limit 320m` / `-Xmx144m`. El gateway, con la red
correcta, queda `healthy` (escenario B, 182 MiB). Es decir: el compose desplegado ES correcto cuando
las variables estan y la base es nueva; lo que lo rompe es el contenido de la base de TEST y los secretos.

## Causa raiz 1 (la mas grave): `aplicar.sql` no es expandible

```
esquema-1  | psql:/sql/10_tablas/02_grupos_turnos/invitacion.sql:34: ERROR:  column "consumidor_id" of relation "grupos.invitacion" does not exist
esquema-1 exited with code 3
```

- `sql/10_tablas/02_grupos_turnos/invitacion.sql` (generado) hace `CREATE TABLE IF NOT EXISTS grupos.invitacion (... consumidor_id, clave_consumo, solicitud_ingreso_id, huella_canje ...)`
  y despues `COMMENT ON COLUMN grupos.invitacion.consumidor_id ...` (linea 34).
- En TEST la tabla YA existe (version `59370bbd`, sin esas 4 columnas): el `IF NOT EXISTS` no la toca, las columnas
  nuevas no se agregan, el `COMMENT ON COLUMN` falla y, como `aplicar.sql` corre en una sola transaccion con
  `ON_ERROR_STOP`, **todo el esquema se revierte** y el trabajo termina en rojo.
- La base de la prueba tenia 402 tablas / 11 usuarios / 25 movimientos de billetera de `59370bbd`; el actual crea 436 tablas.
- Es exactamente el fallo que `despliegue/TEST.md` ya describe («el esquema solo se crea»): el parche de
  `sql/15_infra/nulabilidad.sql` solo afloja NOT NULL, no agrega columnas.

Lo que NO es el origen: roles (`svc_*` quedan 15 de 15 con login tras el job con `ROLES_CLAVE`), PgBouncer, Kafka, MinIO.

### Correccion aplicada (despliegue/aplicar-esquema.sh) y su medicion

El generador (`scripts/generar_ddl.py`) no es de este carril; la red esta en el trabajo de esquema, que si lo es:
`despliegue/aplicar-esquema.sh` ahora **expande antes de aplicar** (aplica `aplicar.sql` en una base de
referencia descartable y agrega a las tablas reales las columnas que les faltan y los CHECK en linea cuya
lista cambio). Aditivo, derivado, sin DDL escrito a mano. Medido sobre la base de `59370bbd` con datos:

| Corrida | Resultado | Archivo |
| --- | --- | --- |
| C (sin expansion) | `exit 3`, `invitacion.sql:34 column "consumidor_id" does not exist` | `arranque-f1-C-actualizacion.txt` |
| C2 (solo columnas) | `exit 3`, ahora en la semilla: `22-cuentas-de-sistema.sql:42 violates check constraint "ck_cuenta_billetera_tipo"` (CHECK en linea viejo, falta `CUSTODIA_INVERSION`) | `arranque-f1-C2-...txt` |
| C3/C4 (columnas + CHECK) | `exit 0`, 436 tablas, **0 tablas pierden filas**; 2.a corrida repetia 5 CHECK por diferir el texto normalizado | `arranque-f1-C3-...txt`, `C4` |
| C5 (comparacion normalizada) | `exit 0`; **2.a corrida: 0 columnas y 0 restricciones**; identidad `healthy` | `arranque-f1-C5-...txt` |

Deriva residual contra una base nueva (`esquema-deriva-*.txt`): solo 5 CHECK con el mismo contenido y distinto
texto de `pg_get_constraintdef` (cast `::text` explicito), mas la funcion huerfana vieja
`aportes.fn_org_validar_contrato_grupo()` (el trigger ya apunta a `grupos.fn_org_validar_contrato_grupo`;
la vieja queda sin uso, no se borra: no se hace nada destructivo).

### Lo que sigue siendo del generador

`scripts/generar_ddl.py` deberia emitir el `ADD COLUMN IF NOT EXISTS` desde el modelo (regla 97.1/97.3) y no
depender de esta red; hasta entonces la expansion del trabajo de esquema cubre columnas y CHECK en linea.
**Siguen sin converger solos** (leidos, no medidos): tipos de columna distintos, `NOT NULL` que se aprieta,
`UNIQUE` nuevos con filas repetidas (fallarian con error claro de datos) y semillas con `ON CONFLICT DO NOTHING`
cuyo valor cambio (p. ej. `13-politicas-de-token.sql`: `INVITACION_GRUPO` pasa a admitir `ENLACE`, pero
la fila vieja con la misma clave no se actualiza => en TEST una invitacion por enlace puede ser rechazada por
politica; verificar `canales_permitidos` en la base real).

### Quien lo arregla (original)

La correccion correcta esta en el generador de DDL (`scripts/generar_ddl.py`, regla 97.1: nunca DDL a mano), que
no es de este carril: por cada columna del modelo debe emitir, tras el `CREATE TABLE IF NOT EXISTS`,
`ALTER TABLE ... ADD COLUMN IF NOT EXISTS <col> <tipo>` (expandir; las columnas nuevas NOT NULL necesitan DEFAULT
o ser opcionales para poder agregarse sobre filas existentes). Ver FASE2.md para el alcance medido (que columnas
faltan y cuales no se pueden expandir).

## No cubierto en esta fase
- Perfiles `staging`/`production`; el servicio `inversiones` y el resto de servicios (FASE2.md).
- El humo contra el gateway (en esta fase el gateway se probo solo, sin los 15).
