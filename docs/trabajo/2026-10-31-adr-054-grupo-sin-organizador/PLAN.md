# Plan — ADR-054: grupos sin organizador humano (confianza y emparejamiento)

- Fecha: 2026-10-31 · Repos afectados: Pasanaku (solo `docs/` y `planes/`) · Rama:
  `claude/adr-054-grupos-sin-organizador-20261031` desde `origin/test` 336e1e82f · Predecesor:
  `docs/trabajo/2026-10-31-simplificar-grupos-devolucion-pagos/` (Pablo aprobó D1…D10: «Sí a todo, D1 a D10,
  arrancá con el ADR»)
- Resultado observable: quien abra `docs/Arquitectura/_Arquitectura.md` encuentra el ADR-054 indexado; quien
  lo abra encuentra las diez decisiones con antes/después, qué parte de ADR-051 D-12 reemplaza, qué
  invariantes conserva y qué falta en el código, carril por carril. `verificar_boveda.py` termina con RC 0.
- Kill-test: `grep -c "ADR-054" docs/Arquitectura/_Arquitectura.md` → 0, o `python3 scripts/verificar_boveda.py`
  con RC ≠ 0.

## Alcance
- IN: `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md` (nuevo), una fila en
  `docs/Arquitectura/_Arquitectura.md`, cierre del trabajo predecesor (H3 → HECHO con las respuestas),
  este directorio. **Agregado durante la ejecución** (regla 20 §6.6, microtarea M6): las cifras de
  bóveda que `verificar_boveda.py` exige al día. En la rama vieja fueron `planes/00`, `05` y `06` (50 → 51
  ADR, 173 → 174 restricciones); al rehacer el carril desde `origin/test`, que ya traía 175 restricciones y
  51 ADR, quedó solo `planes/00` (51 → 52 ADR).
- OUT: código, DDL, `.puml`, seeds, CU, `Restricciones.md`, ADR-051 y ADR-052 (no se editan: el ADR-054
  declara qué supersede; ADR-051 queda `aceptada` porque D-02/03/04/15/16 siguen vigentes). La
  implementación va en carriles posteriores, listados en el ADR §«Lo que falta en el código».
- Ambigüedades registradas:
  1. **D9 aprobada como «`tipo` nuevo», pero el modelo ya tiene el equivalente**: `grupo.es_autogestionado`,
     `grupo.tipo_conformacion` (`MANUAL_POR_INVITACION` / `EMPAREJAMIENTO_AUTOMATICO` / `MIXTA`) y
     `grupo.visibilidad` (`sql/10_tablas/02_grupos_turnos/grupo.sql:20-24,38,40`), y el alta ya escribe
     `ES_AUTOGESTIONADO = organizador.isEmpty()` (`CreacionRepositorio.java:103-104`). Crear `tipo` sería un
     catálogo paralelo (regla 97.4.6, regla 00 §1.1). **Supuesto tomado**: D9 se cumple con las columnas
     existentes y sin cambio de esquema. Se le informa a Pablo en el cierre; si quiere igual la columna
     nueva, es un cambio del ADR.
  2. **R-ORG-06** («las acciones sensibles exigen confirmación humana», CU-95): el ADR decide que el ciclo
     central del grupo autogestionado (admitir por enlace, iniciar, sortear, cobrar, entregar, cancelar y
     devolver) **no es una regla de automatización** de CU-95 sino el flujo de los CU centrales, que ya
     ejecuta el sistema. Pendiente de ratificación de Cumplimiento; así se declara en el ADR.
  3. **Titularidad por consulta al proveedor** (D8): depende de que el proveedor de QR devuelva el titular
     del pagador. El ADR lo adopta condicionado; no promete nada que el proveedor no dé.

## H1 — ADR-054 escrito, indexado y verificado
**CA:** Dado un lector sin acceso al chat, cuando abre el ADR-054, entonces entiende qué cambia respecto de
ADR-051 D-12, por qué, qué no cambia y qué falta hacer, sin leer esta sesión.
**DoD:** los comandos de la tabla, salida en `evidencia/dod.txt`.
**Estado:** HECHO

### H1.S1 — Escribir y verificar
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | ADR-054 con frontmatter válido y las 6 secciones obligatorias, las diez decisiones D1…D10 en tabla, antes/después y «Lo que falta en el código» | El archivo existe y tiene las 6 secciones | `grep -c "^## Contexto\|^## Decisión\|^## Motivo\|^## Alternativas descartadas\|^## Consecuencias\|^## Cómo se verifica" "docs/Arquitectura/ADR-054 Grupos sin organizador humano.md"` → 6 · `grep -cE "^\| D[0-9]+ \|" …` → 10 (corregido: el patrón original `^| D[0-9]` dio 13 porque también contaba las filas «D8 titularidad…», «D7 texto…», «D5 admisión…» de §Ratificación; ver REPORTE §Desvíos) | HECHO |
| H1.S1.M2 | Fila del ADR-054 en el índice | La fila existe | `grep -c "ADR-054 Grupos sin organizador humano" docs/Arquitectura/_Arquitectura.md` → 1 | HECHO |
| H1.S1.M3 | Verificador de la bóveda en verde | RC 0 | `python3 scripts/verificar_boveda.py; echo RC=$?` → `RC=0` | HECHO |
| H1.S1.M4 | Verificador de criterios en verde (no cambia: no se tocan CU) | RC 0 | `python3 scripts/verificar_criterios.py; echo RC=$?` → `RC=0` | HECHO — en la rama vieja (`pablo/feat/backoffice-como-atlas…`, 674 commits detrás de `test`) daba RC=1 por «CU-60: R-GRP-20 citado sin prueba de rechazo» y «AP-CU60-11 no declarado en openapi/grupos.yaml»; los PR #369/#370 ya lo resolvieron en `test`. Al rehacer el carril desde `origin/test` 336e1e82f, RC=0 sin tocar CU-60 ni `grupos.yaml` |
| H1.S1.M5 | Cierre del predecesor: H3.S1.M1 → HECHO con las diez respuestas y avance 9/9 | El plan y el reporte previos reflejan la decisión | `python3 .claude/hooks/plan_status.py --path docs/trabajo/2026-10-31-simplificar-grupos-devolucion-pagos/PLAN.md` → `9/9` | HECHO |
| H1.S1.M6 | Cifras de bóveda al día en los planes (agregada en ejecución: `verificar_boveda.py` falló por «50 ADR, son 51» y «173 restricciones, son 174»; sobre `origin/test` solo faltaba 51 → 52 ADR) | Las cifras coinciden con las que imprime el verificador | `git diff --stat -- planes` → 1 archivo, 1 línea · `verificar_boveda.py` → `TODO OK` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Dos verdades sobre D-12 (ADR-051 dice «no», ADR-054 dice «sí para autogestionados») | Confusión en carriles futuros | El ADR-054 nombra la cláusula exacta que reemplaza y ADR-051 se cita como vigente en lo demás |
| `verificar_boveda.py` exige el nombre exacto del archivo en el índice | RC ≠ 0 | La fila del índice usa `[[ADR-054 Grupos sin organizador humano\|054]]` como las anteriores |
| El número 053 ya estaba tomado en `origin/test` («ADR-053 Rediseño bancario de la app», que la rama vieja no tenía) | Dos ADR con el mismo número; `verificar_boveda.py` lo rechaza | El ADR se renumeró a **054** antes de commitear; la carpeta del carril y todas las citas se renombraron (ver REPORTE §Desvíos) |
| El carril «arenas e inicio 75» (`pasanaku-c1`) toca `CU69IniciarGrupo`, que el ADR cita | Desfase | El ADR cita nombres de clase de `origin/test` 5c06f9e95, no líneas |
