---
name: restriccion
description: "Agregar, cambiar o revisar una restricción de base de datos en docs/Restricciones.md (CHECK, UNIQUE, EXCLUDE, triggers, RLS, GRANT) y regenerar scripts/sql/restricciones.sql. Úsala cuando una regla deba ser imposible de violar —dinero, plazos legales, inmutabilidad, límites, segregación de funciones— o cuando alguien proponga validar algo 'solo en el backend'."
---

# Agregar una restricción

## Cuándo una regla va en la base de datos

Va en la base **siempre** que su violación implique dinero mal contabilizado,
incumplimiento normativo o pérdida de evidencia. Criterio práctico:

| Si la regla… | ¿Va en la base? |
| --- | :-: |
| Protege un saldo, un asiento o una conciliación | Sí, sin excepción |
| Impide doble cobro o doble acreditación | Sí (`UNIQUE` de idempotencia) |
| Guarda o limita un plazo legal | Sí |
| Impide editar algo que debe ser inmutable | Sí (`REVOKE` + trigger) |
| Es una preferencia de interfaz o un mensaje amable | No, solo aplicación |

La aplicación valida igual, pero **para dar buen mensaje**, no para garantizar.

## Numeración

`R-<FAMILIA>-<nn>` con familias fijas:

| Prefijo | Dominio |
| --- | --- |
| `R-AUD` | Auditoría, inmutabilidad y conservación |
| `R-BIL` | Billetera, saldo y custodia |
| `R-LIM` | Límites operativos |
| `R-TAR` | Tarifas, comisiones y facturación |
| `R-UIF` | Prevención de LGI/FT y reportes |
| `R-CON` | Consumidor financiero |
| `R-SEG` | Seguridad y datos personales |
| `R-LIC` | Licencia y gobierno |
| `R-GRP` | Circuito del pasanaku y gobernanza del grupo |
| `R-RIS` | Riesgo operativo y continuidad |
| `R-REP` | Transparencia verificable y reputación |
| `R-NOT` | Notificaciones y consentimiento de contacto |

Los códigos **no se reutilizan**. Si una restricción se retira, se marca como
derogada indicando desde cuándo y por qué.

## Nomenclatura de objetos

```
ck_<tabla>_<regla>     CHECK
uq_<tabla>_<columnas>  UNIQUE
ex_<tabla>_<regla>     EXCLUDE
ix_<tabla>_<columnas>  índice (parcial cuando aplica)
tg_<tabla>_<regla>     trigger
fn_<dominio>_<accion>  función
```

## Procedimiento

1. Elegir familia y número siguiente libre.
2. Agregar la fila a la tabla de la sección correspondiente en
   `docs/Restricciones.md`: **código | regla | qué la obliga | caso de uso donde se
   verifica**. La columna "obliga" no puede quedar vacía: si ninguna norma ni
   invariante la exige, probablemente no debería existir.
3. Escribir el DDL en el bloque ```sql de esa sección, comentado con el código:

```sql
-- R-BIL-16 · descripción corta de la regla
ALTER TABLE tabla ADD CONSTRAINT ck_tabla_regla CHECK (...);
```

4. Citar el código en el caso de uso correspondiente, en
   **Restricciones aplicables**.
5. Regenerar el SQL:

```bash
python3 scripts/extraer_sql.py     # → scripts/sql/restricciones.sql
```

## Cómo elegir el mecanismo

| Necesidad | Mecanismo |
| --- | --- |
| Valor dentro de un rango o enum cerrado | `CHECK` |
| Unicidad simple o compuesta | `UNIQUE` / índice único parcial |
| Vigencias que no se solapan | `EXCLUDE USING gist` + `btree_gist` |
| Invariante entre varias filas (partida doble, sumas) | `CONSTRAINT TRIGGER … DEFERRABLE INITIALLY DEFERRED` |
| Inmutabilidad | `REVOKE UPDATE, DELETE` **más** trigger que aborta |
| Un usuario solo ve lo suyo | Row Level Security con `current_setting('app.usuario_id')` |
| Regla que depende de catálogo con vigencia | Función `fn_*` invocada por la aplicación, con **denegar por omisión** |

## Reglas duras del catálogo

1. **Nunca poner una cifra regulatoria dentro de un `CHECK`.** Un umbral de
   USD 1.000 o un plazo de 5 días son filas de catálogo con vigencia, no
   constantes en el esquema: cuando cambian, no puede requerirse una migración.
   El `CHECK` valida *la forma* (que exista el plazo, que sea posterior al
   ingreso), no *el valor*.
2. **Denegar por omisión.** Si falta el límite, la licencia o la política, la
   función lanza excepción. Nunca "si no hay regla, permito".
3. **Diferir los invariantes multi-fila.** Un trigger inmediato sobre partida
   doble rompe inserciones legítimas: usar `DEFERRABLE INITIALLY DEFERRED`.
4. **El rol de aplicación no edita catálogos regulatorios.** Umbrales, límites,
   tarifarios, licencias y políticas se cargan por seeder versionado
   (`seeders/minimos/`, skill `semillas-catalogos`).
5. **La restricción se prueba por su rechazo.** Toda `R-XXX-nn` nueva necesita una
   prueba que provoque la violación **saltándose la capa de aplicación** y espere el
   error de la base (skill `pruebas-cu`). Una restricción que solo se probó por el
   camino feliz no está probada.
6. **El código de la restricción se cita donde vive.** En el caso de uso
   ("Restricciones aplicables"), en el comentario del DDL y en el mensaje de error
   traducido de la API. Así el `409` que ve el usuario se rastrea hasta la norma.

## Verificación

`docs/Restricciones.md` incluye consultas de control que **deben devolver cero
filas**: transacciones descuadradas, saldo cacheado que no coincide con el libro,
días con encaje incumplido, reportes vencidos, comisiones sin trazabilidad,
reclamos favorables sin reparación. Al agregar una restricción, evaluar si
corresponde agregar también su consulta de verificación.

## Ver también

Skills `boveda-modelo`, `caso-de-uso`, `norma-nueva`, `pruebas-cu`,
`contabilidad-partida-doble`, `seguridad-sesion-rls`, `semillas-catalogos`.
