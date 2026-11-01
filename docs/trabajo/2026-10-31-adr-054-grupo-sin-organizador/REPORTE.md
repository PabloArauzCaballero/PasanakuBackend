# Reporte — ADR-054: grupos sin organizador humano

> **AVANCE: 6 / 6 — 100 %.**

- Fecha: 2026-10-31 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/adr-054-grupos-sin-organizador-20261031`
  desde `origin/test` 336e1e82f (solo `docs/` y `planes/`) · ADR: `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md`
- Peldaño de evidencia alcanzado: `TESTED` para el documento (los dos verificadores de la bóveda en verde
  sobre la rama). No hay código: nada del ADR está implementado.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | ADR-054 escrito: 6 secciones, D1…D10 con «cómo queda», antes/después, invariantes, «Lo que falta en el código» (11 piezas), ratificación pendiente | `grep -c` de secciones · `grep -cE "^\| D[0-9]+ \|"` | PASS: 6 · 10 — `evidencia/dod.txt` |
| H1.S1.M2 | Fila 054 en `_Arquitectura.md` | `grep -c "ADR-054 Grupos sin organizador humano" docs/Arquitectura/_Arquitectura.md` | PASS: 1 |
| H1.S1.M3 | `verificar_boveda.py` en verde (indexado, secciones, estado, número único, cifras) | `python3 scripts/verificar_boveda.py; echo RC=$?` | PASS: `RC=0`, `TODO OK` (en la rama vieja la primera corrida dio `RC=1` por cifras, ver M6; sobre `origin/test`, `52 ADR`) |
| H1.S1.M4 | `verificar_criterios.py` en verde sobre la rama nueva | `python3 scripts/verificar_criterios.py; echo RC=$?` | PASS: `RC=0`, «Sin divergencias entre la boveda y el codigo» (en la rama vieja daba RC=1 por CU-60, resuelto en `test` por #369/#370) |
| H1.S1.M5 | Trabajo predecesor cerrado 9/9 con las respuestas de Pablo | `python3 .claude/hooks/plan_status.py --path docs/trabajo/2026-10-31-simplificar-grupos-devolucion-pagos/PLAN.md` | PASS: `9/9 (100.0%)` |
| H1.S1.M6 | Cifras de los planes actualizadas (en `origin/test`: 52 ADR; 175 restricciones ya estaban) | `git diff --stat -- planes` | PASS: 1 archivo, 1 línea |

## A medias
Ninguna.

## Pendiente
Ninguna en este carril. La implementación del ADR son los once carriles de su tabla «Lo que falta en el
código», en el orden de D10; cada uno abre su propio `PLAN.md`.

## Evidencia
- `evidencia/dod.txt` — salida literal de los DoD y de los verificadores en la rama vieja (con el nombre
  `ADR-053`, tal como se corrieron) y, al final, la corrida completa sobre la rama nueva con `ADR-054`.

## No cubierto
- Ninguna lectura de código se ejecutó: las rutas citadas en el ADR son lecturas del árbol
  (`DISCOVERED`); las clases del carril de inicio al 75 % se leyeron por nombre en `origin/test` 5c06f9e95.
- No se consultó a Cumplimiento ni a Legal: los puntos de §Ratificación pendiente del ADR quedan abiertos.
- No se avisó a las otras sesiones de Pasanaku: el ADR no toca archivos que ellas editen.

## Desvíos del plan
- **Renumerado 053 → 054.** El ADR se escribió como 053 sobre la rama `pablo/feat/backoffice-como-atlas…`,
  que estaba 674 commits detrás de `test`. `origin/test` ya tenía «ADR-053 Rediseño bancario de la app» y
  51 ADR, así que al rehacer el carril desde `origin/test` el número pasó a **054**: archivo, título,
  fila del índice, carpeta del carril y todas las citas. El contenido del ADR no cambió. Las corridas
  viejas de `evidencia/dod.txt` conservan el nombre 053 porque son salidas literales.
- **Carril rehecho desde `origin/test`** (rama `claude/adr-054-grupos-sin-organizador-20261031`) en vez de
  commitear sobre la rama vieja: era la única forma de que `verificar_criterios.py` diera verde sin tocar
  CU-60, porque la corrección ya estaba mergeada en `test` (#369/#370).
- **D9 se cumple sin columna nueva.** Pablo aprobó «`tipo` nuevo»; al verificar el modelo aparecieron
  `es_autogestionado`, `tipo_conformacion` y `visibilidad` con `CHECK` y código que ya las escribe. Crear
  `tipo` sería un catálogo paralelo (regla 97.4.6). El ADR lo decide así y lo explica en §Motivo; se le
  informa a Pablo para que lo confirme o lo revierta.
- **M6 agregada en ejecución**: `verificar_boveda.py` exige que los planes digan la cantidad vigente de
  ADR y restricciones. El 50 → 51 es de este trabajo; el 173 → 174 venía de R-GRP-20 (2026-10-30) y
  bloqueaba el mismo gate, así que se corrigió en el mismo acto y se declara como deuda ajena saldada.
- **DoD de M1 corregido** después de correrlo: `^| D[0-9]` contaba 13 porque tres filas de
  §Ratificación empiezan con «D8», «D7», «D5». El patrón pasó a exigir `| Dn |` exacto; el documento no
  se cambió para pasar. La salida original (13) está en `evidencia/dod.txt`.

## Riesgos residuales
- El VPS de TEST seguía sin responder al momento del PR (`curl` a `api.test.aportaya.bo/actuator/health` →
  HTTP 000). El cambio es solo `docs/` y `planes/`, no despliega nada; el deploy pendiente de `test` es de
  los PR anteriores.
- Hasta que los carriles 2-4 del ADR se implementen, el código sigue rechazando `AP-CU20-02` a todo grupo
  sin organizador: el ADR y el comportamiento real divergen. Es esperado y está declarado en el ADR
  («Lo que falta en el código»).
- El ADR deja `ADR-051` en `aceptada` y reemplaza solo su D-12. Quien lea ADR-051 sin el índice puede
  creer que D-12 sigue en «No»; la fila del índice y el párrafo de apertura del ADR-054 lo aclaran, pero
  ADR-051 no fue editado (fuera de alcance; se puede agregar una nota cruzada en un carril posterior).
- La aprobación manual del KYC de cada participante sigue siendo la mayor fricción del alta y no la toca
  ningún ADR: es de Cumplimiento.

## Decisiones y ambigüedades
- Las tres del `PLAN.md` (D9 sin columna, lectura de R-ORG-06, D8 condicionado al proveedor), con el
  supuesto tomado y a quién confirmárselo: Pablo, Cumplimiento, Cumplimiento + proveedor.
- Pablo pidió cerrar todo («cerralo absolutamente todos», 2026-10-31): se commiteó en la rama nueva y se
  abrieron PR a `dev` y a `test` (números en la sección Entrega, agregada al cerrar).
