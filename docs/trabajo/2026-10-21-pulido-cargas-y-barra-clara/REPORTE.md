# Reporte — Terminar el pulido que dejó Gemini

> **AVANCE: 6 / 7 — 85,7 %.** Pulido corregido, verificado en pantalla y en verde (940/940). Commit y PRs abiertos; el merge lo hace Pablo (el permiso bloqueó el merge sin revisión).

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/pulido-cargas-barra-clara-20261021` (sobre `claude/menu-plegable-guardia-sistemas-20261021`)
- Peldaño de evidencia alcanzado: `VERIFIED` (visual + regresión 940/940); sin merge todavía.

## Qué había dejado Gemini (sin commit, sin plan, última edición 16:31)
- Tablas (`ap-tabla-de-datos`, tabla virtualizada) y `ap-estado-de-pantalla`: girador + esqueleto al cargar y aviso «Actualizando…» al revalidar.
- Barras de desplazamiento finas con colores del tema (`packages/ui/src/estilos.css`).
- Barra lateral clara en tema claro (`shell-financiero.{ts,css}`, colores de `menu-lateral.css`).
- Pendiente que dejó: 3 literales de diseño, `tabla-de-datos-virtualizada.ts` sobre 200 líneas, y documentación de contrato borrada.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Bordes `1px/2px/3px` → `--borde-fino/desfase/foco` (mismos valores, `tokens.json`) | `yarn lint` | ya no lista `shell-financiero.css` ni `menu-lateral.css` |
| H1.S1.M2 | Estilos de la tabla virtualizada a `tabla-de-datos-virtualizada.css` (196 líneas) | `npx ng test --include=src/app/nucleo/tabla/**` | 3 archivos, 17/17 PASS |
| H1.S1.M3 | Documentación del contrato de `EstadoColeccion` y del organismo restaurada | `git diff` | solo 1 línea nueva (estados de carga) |
| H1.S2.M1 | Carga con girador + esqueleto y barra lateral legible, claro/oscuro, escritorio/celular | Playwright | girador=1 esqueleto=1 en las 4 · `evidencia/carga-*.png`, `menu-celular-*.png` |
| H1.S2.M2 | Regresión sobre el contenido exacto del commit | `yarn typecheck`; `yarn test:front` | exit 0; 940/940 · `evidencia/H1.S2.M2-regresion.txt` |
| H1.S2.M4 | Las secciones plegadas no dejan hueco debajo | posiciones de `.entrada-fila` | paso 38 px (antes 48) · `evidencia/menu-espaciado-escritorio-claro.png` |

## A medias
### H1.S2.M3 — Commit y PRs
- Qué anda: commit en `claude/pulido-cargas-barra-clara-20261021`, PRs a dev y test abiertos.
- Qué no anda: no están mergeados (permiso de merge sin revisión denegado).
- Qué falta exactamente: que Pablo mergee los PRs (primero #181/#182, de los que depende este).
- Dónde quedó: rama empujada; compila y pasa 940/940.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| — | — | Ninguna |

## Evidencia
```text
$ yarn lint   (árbol compartido, tras H1.S1.M1 y M2)
  FALLA · sin literal de diseño ['apps/backoffice/src/app/nucleo/explicador-de-vista.component.ts:290']
  FALLA · ningún archivo de más de 200 líneas ['apps/backoffice/src/app/nucleo/explicador-de-vista.component.ts (314)']
$ yarn test:front   (árbol compartido con el trabajo de Gemini, antes de mis arreglos)
 Test Files  150 passed (150)
      Tests  940 passed (940)
```

## No cubierto
- Estado «Actualizando…» (revalidación con datos viejos) no se capturó en pantalla.
- Lector de pantalla sobre los nuevos `role=status`.

## Desvíos del plan
- Se agregó H1.S2.M4 (hueco bajo secciones plegadas), visto en las capturas.

## Riesgos residuales
- `explicador-de-vista.component.ts` sigue con 2 FALLAS de lint previas (fuera de alcance).

## Decisiones y ambigüedades
- La intención de Gemini se dedujo del diff. Confirmar con Pablo que quiere la barra lateral clara en tema claro (antes era verde oscuro en ambos temas).
