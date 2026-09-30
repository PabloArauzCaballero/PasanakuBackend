# Cómo cambia el esquema hoy — y qué falta para que sea "migraciones"

> H4.S2.M3 del carril PR4-seguridad (Marcelo). Q-01/AMB-7, DECIDIDA 2026-09-21: este
> turno sigue con `sql/aplicar.sql` generado e idempotente, con cambios aditivos
> solamente. La adopción de Flyway/Liquibase queda como ADR posterior a la
> promoción — no se decide ni se implementa en este documento.

## 1. Lo que existe hoy

**No hay migraciones versionadas.** No existe una secuencia `V1__algo.sql`,
`V2__otra_cosa.sql` que el motor aplique en orden y registre en una tabla de
control. Lo que existe es:

1. **El modelo** (`docs/Modelos/*.puml`, diagramas de clase PlantUML) es la fuente de
   verdad del dominio.
2. **`scripts/modelo.py`** parsea esos `.puml` a una representación intermedia.
3. **`scripts/generar_ddl.py`** emite, a partir de esa representación, el árbol
   `sql/00_base/` … `sql/60_semillas/`: tablas (`CREATE TABLE IF NOT EXISTS`),
   claves foráneas, índices, reglas (`restricciones.sql`), grants y semillas.
   Cada archivo generado lleva la cabecera *"Generado por
   `scripts/generar_ddl.py` — no editar a mano"*.
4. **`sql/aplicar.sql`** es un guion de `psql` (usa `\ir`, no es un lote de
   sentencias JDBC) que aplica TODO el árbol en orden, de punta a punta, sobre una
   base vacía o sobre una que ya tiene el esquema.
5. El CI (`ci.yml`, job `base`) verifica que `generar_ddl.py` no deje diff contra lo
   versionado: **todo cambio de esquema pasa por el generador**, nunca por DDL a
   mano. `scripts/verificar_seguridad.py`/`verificar_boveda.py` no lo tocan, pero el
   job `base` sí lo aplica en limpio y con semillas dos veces (ver H4.S2.M4/H0.S4.M1
   del plan madre).

## 2. Cómo se EXPANDE el esquema hoy (agregar algo)

- Agregar una tabla, columna o índice: se edita el `.puml` correspondiente, se corre
  `python3 scripts/generar_ddl.py`, y el árbol `sql/` cambia. Como todas las
  sentencias son `CREATE TABLE IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS` /
  `CREATE INDEX IF NOT EXISTS`, **volver a aplicar `aplicar.sql` sobre una base que
  ya tiene el esquema anterior es seguro**: lo que ya existe no se toca, lo nuevo se
  crea.
- Esto es exactamente lo que este carril verificó con el webhook de `aportes`: la
  tabla `aportes.webhook_pasarela` y su índice `uq_webhook_idem` YA estaban en el
  árbol generado (de un `.puml` anterior) antes de que este carril escribiera una
  sola línea de Java — el esquema soportaba la feature antes que el código.

## 3. Cómo se CONTRAE el esquema hoy (quitar o cambiar algo) — el hueco real

**No hay una forma segura de quitar una columna, cambiar su tipo, o borrar una tabla
con datos.** `generar_ddl.py` puede dejar de EMITIR una columna que ya no está en el
`.puml`, pero:

- No emite el `DROP COLUMN` correspondiente (evitar pérdida de datos por omisión no
  declarada explícitamente sería peligroso de automatizar).
- Si alguien agregara un `DROP COLUMN`/`DROP TABLE`/`ALTER COLUMN ... TYPE` a mano en
  el árbol generado, el próximo `generar_ddl.py` lo pisaría sin aviso (el archivo se
  regenera entero) — o, peor, quedaría un diff que el CI marcaría como "el generador
  no reproduce lo versionado", sin decir por qué.
- **No existe un mecanismo de rollback**: no hay una migración `N → N-1` que
  deshaga un cambio aplicado. La única forma de "deshacer" hoy es restaurar un
  respaldo (PITR, AMB-8) o revertir el commit y volver a aplicar sobre una base
  vacía — inviable con datos de producción.

Este turno **no introduce ningún cambio de esquema que requiera contraer nada**: los
cambios de este carril (`aportes.webhook_pasarela` ya existía; ningún `ALTER
… DROP`) son 100% aditivos, cumpliendo la restricción de Q-01/AMB-7.

## 4. Verificado en este carril (H4.S2)

| Verificación | Comando | Estado |
|---|---|---|
| `empty → latest` (base vacía, aplicar desde cero) | `docker exec -i aportaya-postgres psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku -f - < sql/aplicar.sql` | **TODO en esta corrida** — `aportaya-postgres` está arriba (`despliegue/compose/base.yml`, healthy) pero el esquema no se aplicó todavía por presupuesto de tiempo (ver `evidencia/H1-entorno-docker.md`) |
| `latest → latest` con datos (sembrar, insertar filas, re-aplicar, comparar conteos) | conteos antes/después | **TODO** — depende del paso anterior |
| Guarda de semillas dev en producción | job `base` del CI, pasos 9–10 | **Referenciado, no reproducido localmente en esta corrida** — ver `ci.yml` |

## 5. `DECISION_REQUIRED`

Adoptar Flyway o Liquibase (migraciones versionadas con rollback) es una decisión de
arquitectura que este carril **no toma por su cuenta** (regla 00, Q-01/AMB-7 ya
decidida así para este turno). Argumentos a favor, para cuando se abra el ADR:
rollback real, historial auditable de cambios de esquema, y una forma de expresar
un `DROP COLUMN` con una ventana de "expand/contract" (agregar la columna nueva,
migrar datos, dejar de leer la vieja, recién ahí borrarla) que hoy no existe.
