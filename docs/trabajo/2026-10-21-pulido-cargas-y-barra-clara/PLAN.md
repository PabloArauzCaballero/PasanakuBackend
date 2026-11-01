# Plan — Terminar el pulido que dejó Gemini: estados de carga, barras de desplazamiento y barra lateral clara

- Fecha: 2026-10-21 · Repos afectados: Pasanaku (apps/backoffice, packages/ui) · Predecesor: `2026-10-21-menu-plegable-y-guardia-sistemas`
- Resultado observable: en el backoffice, las tablas y pantallas que cargan muestran girador + esqueleto (no un texto suelto), las barras de desplazamiento son finas y del tema, y la barra lateral es clara en tema claro y oscura en oscuro, legible en ambos.
- Kill-test: `yarn lint` del backoffice con alguna FALLA nueva respecto de `676df14b`, o una captura donde el menú no se lee.

## Hallazgos (fase 1)
- Gemini editó sin commitear y sin plan (última edición 16:31): `packages/ui/src/{estilos.css, estado-de-pantalla/*, tabla-de-datos/*}`, `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.ts`, `layout/shell-financiero.{ts,css}`, colores de `layout/menu-lateral.css`.
- Árbol compartido: `yarn test:front` 150/150 archivos, 940/940 tests; `yarn typecheck` exit 0.
- `yarn lint`: FALLAS nuevas por Gemini: literales en `shell-financiero.css:11` (`1px`), `menu-lateral.css:73` (`3px`), `menu-lateral.css:110` (`2px`); `tabla-de-datos-virtualizada.ts` 224 líneas (HEAD 198, tope 200). Previa: `explicador-de-vista.component.ts`.
- Gemini borró de `tabla-de-datos.ts` la documentación del contrato provisorio (doble en tres niveles, regla 65) y de la adopción diferida de `ViewState`.

## Alcance
- IN: los archivos listados arriba.
- OUT: `explicador-de-vista.component.ts` (falla previa), cualquier otra pantalla.
- Ambigüedades: la intención de Gemini se infiere del diff (no dejó notas). Confirmar con Pablo que la barra lateral clara es lo que quiere.

## H1 — Pulido de Gemini terminado y publicado
**CA:** Dado el backoffice, cuando una tabla carga, entonces se ve girador y esqueleto; la barra lateral se lee en ambos temas; sin fallas nuevas de lint.
**DoD:** lint sin fallas nuevas · `test:front` verde · capturas claro/oscuro · commit + PR a dev y test.
**Estado:** TODO

### H1.S1 — Calidad estática
**CA:** Sin literales de diseño nuevos y sin archivos nuevos sobre 200 líneas.
**DoD:** `yarn lint` → solo las FALLAS previas de `explicador-de-vista`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Reemplazar `1px/2px/3px` por tokens (`--borde-fino`, `--borde-desfase`, `--borde-foco`) | lint no lista `shell-financiero.css` ni `menu-lateral.css` | `yarn lint` → ya no lista esos archivos | HECHO |
| H1.S1.M2 | `tabla-de-datos-virtualizada.ts` ≤ 200 líneas sin cambiar comportamiento (estilos a `.css`) | lint no lo lista; specs de la tabla verdes | 196 líneas; lint solo FALLAS previas; `ng test nucleo/tabla` 3 archivos 17/17 PASS | HECHO |
| H1.S1.M3 | Restaurar la documentación del contrato borrada en `tabla-de-datos.ts` | el bloque de doble en tres niveles y la adopción de `ViewState` vuelven a estar | `git diff` del comentario: solo 1 línea agregada (estados de carga) | HECHO |

### H1.S2 — Verificación y publicación
**CA:** Se ve bien y queda en git.
**DoD:** capturas + suite + PRs.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Capturas: carga de tabla y barra lateral en claro/oscuro, escritorio y celular | menú legible; girador + esqueleto visibles | `carga-{escritorio,celular}-{light,dark}.png`, `menu-celular-*.png`, `menu-espaciado-escritorio-claro.png`: girador=1 esqueleto=1 en los 4 | HECHO |
| H1.S2.M2 | Regresión `test:front` + typecheck | 0 rojos | typecheck exit 0; 940/940 (`evidencia/H1.S2.M2-regresion.txt`) | HECHO |
| H1.S2.M3 | Commit en rama propia + PR a dev y test | PRs abiertos y checks verdes | `gh pr checks` | TODO |

### Agregado durante la ejecución
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M4 | Una sección plegada no deja hueco debajo (margen del submenú oculto a 0) | Paso entre filas plegadas = paso normal | posiciones `.entrada-fila`: Cumplimiento 243 → Sistemas 281 (38 px, antes 48) | HECHO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Gemini retoma y edita a la vez | Cambios pisados | Verificar mtimes antes de cada edición; commit desde worktree |
