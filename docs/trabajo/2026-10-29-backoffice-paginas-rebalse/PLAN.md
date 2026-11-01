# Plan — Backoffice: rebalse de texto y defectos chicos de maquetación en las páginas

- Fecha: 2026-10-29 · Repos afectados: PasanakuBackend (`apps/backoffice`) · Predecesor: ninguno
- Resultado observable: un operador abre cualquier página del menú del backoffice a 1440, 1280, 1024 y 390 px, en
  tema claro y oscuro, y ningún texto se sale de su caja ni queda cortado sin «…» y sin el texto completo a mano;
  la página nunca desplaza en horizontal.
- Kill-test: el barrido automático (`scrollWidth > clientWidth` en elementos que no son contenedores de
  desplazamiento, y `documentElement.scrollWidth > innerWidth`) encuentra un rebalse en una página del menú.

## Alcance
- IN: `apps/backoffice/src/app/rutas/**`, `nucleo/tabla/**`, `nucleo/filtros/**`.
- OUT: `src/app/layout/**` y `nucleo/menu.ts` (otro agente), `packages/ui/**` (componentes compartidos: se anotan,
  no se tocan), rediseños grandes, datos del mock.
- Ambigüedades registradas: los datos del barrido son SINTÉTICOS (Prism + textos alargados a propósito); no hay
  backend real. Supuesto: el rebalse depende del largo del texto, no de su origen.

## H1 — Páginas del backoffice sin rebalse
**CA:** Dado el barrido en 4 anchos × 2 temas, cuando se recorren todas las páginas del menú, entonces no queda
rebalse de texto ni desplazamiento horizontal de página atribuible a `rutas/`.
**DoD:** barrido re-ejecutado sin hallazgos en las páginas corregidas · capturas antes/después miradas ·
`yarn lint` · `yarn typecheck` · `yarn test:front` en verde.
**Estado:** A MEDIAS (correcciones verificadas en navegador; la revisión independiente y los defectos de `packages/ui` quedan fuera)

### H1.S1 — Barrido y correcciones
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Barrido «antes» de todas las páginas del menú | Hay informe por página/ancho/tema | Barrido Playwright temporal → `informe.json` | HECHO |
| H1.S1.M2 | Fechas cortas de tabla en una línea (admisiones, postulaciones, grupos, custodia, cierres, UIF) | «15 ene 2026» no se parte | Captura 1024/1440 después | HECHO |
| H1.S1.M3 | Selector de estado de postulaciones no desborda a 390 | `scrollWidth - innerWidth = 0` a 390 | Barrido 390 | HECHO |
| H1.S1.M4 | Rutas largas de Operaciones se parten en la tarjeta móvil; flujo recortado con texto completo | Sin rebalse a 390 | Barrido 390 | HECHO |
| H1.S1.M5 | Rutas recortadas con «…» en Flujos muestran el texto completo (title) | Sin elipsis sin title | Barrido 1280/1024 | HECHO |
| H1.S1.M7 | Espaciado del tablero de sistemas y columnas Ruta/Errores de «Endpoints de hoy»; códigos CU enteros en Flujos; title en identificadores acortados | Sin «rut/a», secciones separadas | Captura 1024 después | HECHO |
| H1.S1.M6 | Gates | lint, typecheck, test:front verdes | `yarn lint && yarn typecheck && yarn test:front` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Causa en `packages/ui` (fuera de alcance) | No se puede corregir desde `rutas/` | Corrección local en la ruta o anotarlo en el reporte |
