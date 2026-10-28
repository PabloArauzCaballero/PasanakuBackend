# Plan — Backoffice: rediseño de la anatomía compartida y pasada pantalla por pantalla

- Fecha: 2026-10-27 · Repos afectados: PasanakuFrontend (`apps/backoffice`, `packages/ui`, raíz por dependencias) · Predecesor: `2026-10-26-backoffice-listados-y-navegacion`
- Resultado observable: quien opera el backoffice abre cualquier pantalla del menú (44) y ve el título una sola vez, sin
  notas duplicadas ni espacio muerto, filtros y descarga en una barra compacta, tabla con filas de alto normal y sin
  columnas recortadas, pestañas que se desplazan, en claro y oscuro, a 1280 / 768 / 375.
- Kill-test: captura de `/operacion/desembolsos` a 1280: si el título «Desembolsos» aparece más de dos veces visibles
  (miga + H1), si la fila de la tabla mide más de ~64 px, o si hay > 64 px vacíos entre pestañas y cabecera, no está hecho.

## Pedido literal

Captura de `/operacion/desembolsos` (oscuro) + «corregí uno por uno absolutamente todo porque está pésimo… rediseñémoslo».
El enlace a `coreyhaines31/marketingskills` son skills de marketing ya instaladas en la sesión; aplican al texto
(microcopia), no a la maqueta.

## Hallazgos (fase 1)

Hechos (con ruta):
1. `main` del shell ya tiene relleno (`shell-financiero.css`) y **además** cada pantalla trae `.pantalla { padding: var(--s5) }`
   (≈ 40 componentes): doble relleno, anchos distintos entre pantallas (Desembolsos empieza en x=320, Servicios en x=296).
2. «Notas de la vista» (`ExplicadorDeVista` + `ap-nota-de-vista` en el shell) repite el mismo `proposito` que la cabecera
   muestra como descripción (p. ej. `bandeja-de-desembolsos.ts`): franja de 44 px + hueco, sin información nueva.
3. El título se ve **cuatro** veces en Desembolsos: título de la cabecera fija, pestaña, H1 y `caption` de la tabla.
4. El eyebrow muestra códigos internos («Operación · CU-28», «D-15», «CU-100»…), jerga del equipo, no de quien opera.
5. Pestañas de sección: a 1280 se cortan («Adm…», «Requerimientos de autor…») sin indicio de que hay más.
6. Barra de filtros: buscador en píldora y selectores rectangulares; ícono de ayuda suelto; contadores duplicados
   («1 desembolso en la página 1» + «1 fila»).
7. Tabla de Desembolsos: fila de ~200 px porque los identificadores (`.id { overflow-wrap: anywhere }`) se parten letra a
   letra en una columna apretada; las dos últimas columnas quedan fuera de la vista.
8. Vacío: caja punteada de 230 px de alto para decir «no hay nada».
9. Entorno: `main` no compila. Dependabot (#41) subió TypeScript a 7.0.2 y Angular 22.2 exige `>=6.0 <6.1`; además
   `@angular/compiler-cli` usa `encodeRangeMappings` de `@jridgewell/sourcemap-codec` ≥ 1.6 y el lock resolvía 1.5.5.

Desconocidos: comportamiento contra servicios reales (no hay backend desplegado; se trabaja contra el simulado Prism).
Hipótesis: ninguna se presenta como hecho.

## Alcance

- IN: dependencias que impiden compilar; shell (cabecera, lateral, pestañas, notas); `packages/ui` piezas de la anatomía
  de pantalla (cabecera de página, barra de filtros, campo de búsqueda, exportar, tabla, vacío, listado); el relleno de
  cada pantalla del backoffice; eyebrows con códigos internos; correcciones puntuales que aparezcan en la pasada.
- OUT: contratos de API, guardias/permisos, rutas, persistencia, lógica de listados (`crearListado`, cursor), la app
  móvil y `apps/web`. Ningún permiso ni ruta cambia.
- Ambigüedades registradas:
  - A1 «Rediseñemos» no fija dirección visual. Supuesto: se mantiene la identidad AportaYa (tokens, verde de marca) y se
    corrige jerarquía, densidad y consistencia; no se inventa otra marca. Confirmar con el dueño del producto.
  - A2 Quitar las «Notas de la vista» del shell: supuesto, la descripción de la cabecera ya dice lo mismo; donde el texto
    difiera, se conserva en la cabecera. Confirmar con quien escribió los textos.

## H0 — El backoffice compila otra vez
**CA:** Dado `main` actual, cuando se corre `ng build`/`ng serve`, entonces compila sin errores.
**DoD:** `yarn workspace @aportaya/backoffice build` exit 0 · `typecheck` exit 0.
**Estado:** ver microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.M1 | TypeScript `~6.0.3` en los 8 `package.json` + lock | `node_modules/typescript` = 6.0.x | `yarn why typescript` | HECHO |
| H0.M2 | `resolutions` para `@angular/compiler-cli/@jridgewell/sourcemap-codec` ^1.6 | `ng serve` arranca | log «bundle generation complete» | HECHO |

## H1 — Anatomía compartida (corrige las 44 pantallas a la vez)
**CA:** Dado cualquier pantalla del menú, cuando se abre, entonces título único, sin nota duplicada, sin doble relleno.
**DoD:** capturas 1280/768/375 claro + 1280 oscuro de las 44 rutas revisadas dos veces · `ng test` ui y backoffice en verde · lint · typecheck.
**Estado:** ver microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Un solo contenedor de contenido: `main` centra a un ancho máximo y las pantallas dejan de sumar relleno | x de inicio igual en Desembolsos y Servicios | captura + `grep -c "padding: var(--s5)"` en `.pantalla` = 0 | HECHO |
| H1.M2 | Shell sin «Notas de la vista» (la cabecera ya las muestra) | no existe el botón «Notas de la vista» | captura + spec del shell | HECHO |
| H1.M3 | Cabecera fija: una línea de migas con la pantalla actual al final; ayuda como botón discreto | el título no se repite en la cabecera fija | captura | HECHO |
| H1.M4 | Cabecera de página: H1 en color de texto, sin eyebrow con códigos internos | ningún «CU-», «D-15» visible | `grep` + captura | HECHO |
| H1.M5 | Pestañas: desplazamiento horizontal con la activa a la vista y degradé de borde | la última pestaña es alcanzable y se ve que hay más | captura + spec | HECHO |
| H1.M6 | Barra de filtros compacta: buscador con el mismo radio que los selectores, ayuda dentro del campo | radios iguales, sin hueco | captura | HECHO |
| H1.M7 | Barra del listado: resumen + descargas en una fila, sin contador duplicado | un solo contador visible | captura + specs de exportar | HECHO |
| H1.M8 | Tabla: `caption` visualmente oculto, identificadores en una línea, fila de alto normal | fila ≤ 64 px en Desembolsos | captura + medición | HECHO |
| H1.M9 | Vacío liviano | alto del vacío ≤ 180 px | captura | HECHO |
| H1.M10 | Barra lateral sobria (sin degradé verde), legible en claro y oscuro | contraste AA del menú | captura + axe | HECHO |

## H2 — Pasada pantalla por pantalla
**CA:** Dada cada una de las 44 rutas, cuando se revisa su captura, entonces no queda un defecto visual abierto sin registrar.
**DoD:** `evidencia/revision-por-pantalla.md` con una línea por pantalla (primera pasada) y la pasada adversarial (regla 35).
**Estado:** ver microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | Revisión de las 44 capturas después de H1, lista de defectos por pantalla | archivo con 44 líneas | `wc -l` | HECHO |
| H2.M2 | Corrección de los defectos encontrados (se agregan como microtareas H2.Mn al descubrirlos) | cada defecto con su estado | re-captura | HECHO |
| H2.M3 | Segunda pasada adversarial con nota por pantalla | ninguna RECHAZADA entregada | `evidencia/doble-revision.md` | HECHO |

## H3 — Regresión y entrega
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.M1 | `ng test` backoffice + ui, a11y, lint, typecheck, build | todo en verde | salidas en `evidencia/` | HECHO |
| H3.M2 | e2e del backoffice (`--retries=0`) | sin fallos nuevos | salida en `evidencia/` | HECHO |
| H3.M3 | Commit, push a `claude/admiring-fermi-mdayfj`, `REPORTE.md` | push OK | `git log` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Specs que afirman estructura vieja (nota de vista, eyebrow, contador de filas) | Rojo en la suite | Se actualizan solo donde el comportamiento cambió a propósito, declarado en el reporte; nunca se debilita una aserción de comportamiento |
| Sin backend real | No hay persistencia que demostrar | El cambio es de presentación; no toca mutaciones. Se declara en «No cubierto» |
| Maven Central limita (429) la generación de clientes | No se generan clientes con Gradle | Se generan con el CLI 7.26.0 y las mismas opciones; los clientes son artefactos ignorados por git |

## H4 — Que el servidor de TEST sirva esta versión (agregado 2026-10-27, pedido en la sesión)
**CA:** Dado `https://backoffice.aportaya.161.97.85.216.sslip.io`, cuando el autodespliegue toma `origin/test` de
PasanakuBackend (el repo que construye ese host), entonces el bundle servido ya no contiene «Notas de la vista».
**DoD:** `curl` del bundle en vivo + `grep`.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.M1 | Portar el cambio a PasanakuBackend en `claude/admiring-fermi-mdayfj` | rama empujada, typecheck y tests del backoffice allí en verde | salidas | HECHO |
| H4.M2 | PR a `dev` y de `dev` a `test` | PRs abiertos | URL | HECHO |
| H4.M3 | Verificar el bundle en vivo | sin «Notas de la vista» | `curl … \| grep -c` = 0 | HECHO |

## H5 — Nada en rojo por una lectura caída; botones con un solo color de acción (agregado 2026-10-27, pedido en la sesión)
**CA:** Dado el backoffice en TEST, cuando un servicio no responde, entonces la pantalla muestra una tarjeta neutra con
qué pasó y «Volver a intentar», sin rojo; y en toda pantalla hay un solo botón con color (el primario) y todo botón
lleva ícono.
**DoD:** typecheck, lint, `test:front`, `test:a11y`, build, e2e dirigido, capturas en `evidencia/h5/`, bundle en vivo.

Hallazgo: sondeo sin sesión de las 138 rutas GET que usa el backoffice contra el gateway de TEST. `/api/v1/erp/**`
responde 500 antes de autenticar (servicio ERP caído en el host); el resto responde 401 (enrutado). `extraccion/accesos`
responde 404 y no se llama (Accesos ya es «sin fuente»).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.M1 | `ap-estado-de-error` neutro: tarjeta de superficie, ícono ámbar, «Volver a intentar» con ícono | sin `--err` en el componente | captura `evidencia/h5/error-503-*` | HECHO |
| H5.M2 | 502/503/504 dicen que el servicio no está disponible y que se puede volver a probar | spec de `errores` | `test:front` | HECHO |
| H5.M3 | Secundario y fantasma neutros en el backoffice (solo `styles.css` del backoffice) | un solo botón con color por pantalla | captura `botones-neutros.png` | HECHO |
| H5.M4 | Ícono en los 13 botones que no lo tenían | `grep` de `<ap-boton` sin ícono = 0 | `grep` | HECHO |
| H5.M5 | Usuarios internos: Ver + «Más» (Roles, Suspender/Reactivar, Desbloquear, Editar) | fila de una línea; menú no recortado | captura `usuarios-internos-menu-mas*` + spec de mutaciones | HECHO |
| H5.M6 | Sistemas sin fuente sin «Actualizar» | spec compartida de Sistemas | `test:front` | HECHO |
| H5.M7 | Desplegar a TEST y verificar el bundle | bundle nuevo con los cambios | `curl` + `grep` | HECHO |
| H5.M8 | Catálogo de datos › Columnas: «Revisar» pasa a la celda del nombre y se quita la columna fija que tapaba «Sistema»/«Sensibilidad» | ninguna columna tapada a 1280/768/375, claro y oscuro | capturas `catalogo-columnas-*` + `vista-columnas.spec` + e2e `catalogo-gobierno` | HECHO |

## H6 — Segunda pasada visual independiente (agregado 2026-10-28, pedido en la sesión)
**CA:** Dado el backoffice en su versión final, cuando `visual-reviewer` lo revisa en 1280/375, claro/oscuro y estados de
error, entonces ninguna pantalla queda RECHAZADA por causa del frontend.
**DoD:** veredicto escrito del revisor por ronda (`evidencia/doble-revision.md`), gates en verde, e2e del backoffice.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.M1 | Ronda 1 del revisor sobre las 44 rutas × 3 variantes + especiales | veredicto escrito | informe | HECHO |
| H6.M2 | Corregir lo atribuible al frontend (tablas, ceros en rojo, títulos de error, jerga, plurales, enums, `color-scheme`, KPI, catálogo) | revisor: corregido | rondas 2–4 | HECHO |
| H6.M3 | Rondas 2–4 hasta que no quede RECHAZADA por el frontend | 0 rechazadas por el frontend | informe de cierre | HECHO |
| H6.M4 | e2e del backoffice | sin fallos nuevos | `playwright test --retries=0` | HECHO |
| H6.M5 | PR a `dev` y a `test`, y bundle en vivo | bundle nuevo con los cambios | `curl` + `grep` | TODO |
