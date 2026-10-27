# Reporte — Backoffice: rediseño de la anatomía compartida y pasada pantalla por pantalla

> **AVANCE: 17 / 21 — 81,0 %.** La revisión visual independiente RECHAZÓ la primera entrega (58/100); se corrigieron sus hallazgos salvo dos (columna fija de acciones tapando columnas en Catálogo de datos y Usuarios internos). El despliegue a TEST depende de que se mergee el PR de PasanakuBackend a `dev` y luego a `test`.

- Fecha: 2026-10-27 · Plan: [PLAN.md](./PLAN.md) · Doble revisión: [evidencia/doble-revision.md](./evidencia/doble-revision.md)
- Ramas: PasanakuFrontend `claude/admiring-fermi-mdayfj` (`add44f26`, `40f66da1`) · PasanakuBackend `claude/admiring-fermi-mdayfj` (`071092e5`), PR [PabloArauzCaballero/PasanakuBackend#197](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/197) a `dev`.
- Peldaño de evidencia: **TESTED + e2e** en PasanakuFrontend (unitarias, a11y, e2e 166/166, build) contra el simulado OpenAPI. Visual: `ACEPTABLE CON RESERVAS` en general, `VERIFIED_FUNCTIONAL_ONLY` en Catálogo de datos y Usuarios internos. No llega a `REGRESSION_VERIFIED`: no hay backend real y la segunda pasada no se repitió sobre la re-captura final.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H0.M1–M2 | `main` vuelve a compilar: TypeScript `~6.0.3` (Dependabot #41 lo había subido a 7) y `sourcemap-codec ^1.6` para `compiler-cli` | `ng serve` / `yarn workspace @aportaya/backoffice build` | «bundle generation complete», build exit 0 |
| H1.M1–M10 | Anatomía compartida: un contenedor, ⓘ en vez de franja de notas, migas «Sección › Pantalla», H1 sin códigos internos, pestañas desplazables, filtros compactos, descargas discretas, tabla sin caption duplicado ni filas de 200 px, vacío liviano, barra lateral lisa | capturas `evidencia/despues-*` | revisadas |
| H2.M1–M2 | Pasada por las 44 rutas y corrección de lo encontrado (ver tabla de la primera pasada en `doble-revision.md`) | capturas | revisadas |
| H3.M1 | Gates en PasanakuFrontend | ver Evidencia | todo en verde, build sin avisos |
| H3.M2 | e2e del backoffice | `npx playwright test --retries=0` | **166 passed (5.1m)** |
| H3.M3 | Commit y push en PasanakuFrontend | `git push` | rama creada en origin |
| H4.M1 | Port a PasanakuBackend (el repo que construye TEST) | typecheck, lint, test, build allí | ver Evidencia |

## A medias

### H2.M3 — Segunda pasada adversarial
- Qué anda: el revisor independiente revisó las 44 rutas; se corrigieron 2 bloqueantes (uno era falso: no había desborde de página, igual se cambió el carril de acciones), 4 mayores y varios menores.
- Qué no anda: la columna fija de acciones tapa «Sensibilidad» (Catálogo de datos) y «Segundo factor/Estado» (Usuarios internos) a 1280. Un intento de acotar su ancho empeoró la superposición y se revirtió.
- Qué falta exactamente: decidir el patrón (menú «Más acciones» de fila, o acciones solo en la ficha) y repetir las dos pasadas sobre la re-captura.
- Dónde quedó: `packages/ui/src/acciones-de-fila/`, `tabla-de-datos/`; compila, tests en verde.

### H4.M2 — Llevarlo a `test`
- Qué anda: PR #197 a `dev` abierto con el CI corriendo.
- Qué no anda: el host todavía sirve la versión anterior (el bundle en vivo contiene «Notas de la vista»).
- Qué falta exactamente: CI verde → merge a `dev` → PR `dev` → `test` → merge; el temporizador del host reconstruye en ≤ 2 min.
- Dónde quedó: PasanakuBackend `claude/admiring-fermi-mdayfj`.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H4.M3 | TODO | Verificar el bundle en vivo tras el merge a `test` (`curl` + `grep -c "Notas de la vista"` = 0) |

## Evidencia

```text
PasanakuFrontend (código final)
backoffice typecheck exit=0 · ui typecheck exit=0
backoffice lint exit=0 All files pass linting. · ui lint exit=0 All files pass linting.
ui test:front Tests 196 passed (196) · ui test:a11y 9 passed (9) · tutoriales 9 passed (9)
backoffice test:front Tests 1221 passed (1221) · backoffice test:a11y Tests 92 passed (92)
backoffice build Initial total 451.42 kB (sin avisos)
e2e 166 passed (5.1m) --retries=0

PasanakuBackend (rama del PR #197)
backoffice/ui typecheck exit=0 · lint All files pass linting.
ui test:front 196 passed · backoffice test:front 1230 passed + entrada-estricta 10 passed (tras generar el simulado)
backoffice test:a11y 92 passed · build exit 0, aviso: bundle inicial 502.25 kB / 500 kB (en dev ya estaba en 500.59 kB)

Servidor de TEST antes del merge
curl https://backoffice.aportaya.161.97.85.216.sslip.io/admisiones → 200; main-B7OY5X42.js contiene «Notas de la vista»
```

Capturas: `evidencia/antes/`, `evidencia/despues-1280/`, `despues-oscuro/`, `despues-768/`, `despues-375/`.

## No cubierto
- Persistencia real: todo contra el simulado OpenAPI (el cambio es de presentación; no toca mutaciones ni contratos).
- La segunda pasada visual no se repitió sobre la re-captura final.
- e2e no se corrió en PasanakuBackend (sí en PasanakuFrontend, mismo código salvo los conflictos resueltos).
- Lector de pantalla real; solo Chromium.

## Desvíos del plan
- La franja «Notas de la vista» no se eliminó: pasó a un botón ⓘ (`ap-nota-de-la-vista`) para no perder texto que en algunas pantallas no repite la cabecera (A2).
- Se agregó H4 (desplegar a TEST) por pedido en la sesión: el host construye desde PasanakuBackend, no desde este repo.
- Las acciones de la cabecera de página se envuelven en el celular en vez de usar un carril desplazable.

## Riesgos residuales
- Columna fija de acciones con muchas acciones tapa columnas vecinas (ver A medias).
- Los dos repos tienen `apps/backoffice` divergidos (128 archivos distintos antes de este trabajo): cada cambio hay que portarlo.
- Presupuesto del bundle inicial de PasanakuBackend excedido (aviso, no error).
- El cliente generado versionado `clientes/angular/publicidad/model/entradaAnunciante.ts` quedó desactualizado respecto de su contrato; no se tocó.

## Decisiones y ambigüedades
- A1: «Rediseñemos» sin dirección visual. Se mantuvo la identidad AportaYa (tokens, verde de marca) y se corrigió jerarquía, densidad y consistencia. Confirmar con el dueño del producto.
- A2: notas de la vista en ⓘ. Confirmar con quien escribe los textos.
- A3: botones verdes rellenos («Volver a intentar», «Ver ejercicios anteriores») conviven con el primario negro: es la paleta del sistema de diseño; no se cambió el componente `ap-boton`. Decisión de diseño pendiente.
- El enlace a `coreyhaines31/marketingskills` son skills de marketing; se aplicaron solo a la microcopia (códigos internos fuera, plurales, textos de vacío).
