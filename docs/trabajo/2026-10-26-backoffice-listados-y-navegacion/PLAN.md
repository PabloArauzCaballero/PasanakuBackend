# Plan — Backoffice: navegación que responde, menú recortado y listados con una sola disciplina

- Fecha: 2026-10-26 · Repos afectados: PasanakuFrontend (`apps/backoffice`, `packages/ui`) · Rama: `claude/jolly-mccarthy-uwzef6` · Predecesor: `2026-10-21-catalogo-de-datos-y-rediseno`
- Pedido literal (Pablo, 2026-10-26): «casi ningún botón está funcionando como debe o tiene demasiada latencia del menú lateral · recortemos muchas opciones que están de más · cada vista mejorada · tablas paginadas con select de N por página, ir a la página N, atrás y adelante · buscador multicampo y los N filtros necesarios · descargar CSV y JSON de lo filtrado · los botones de acción de la tabla: editar y eliminar si corresponde, con ícono + nombre · la máxima cantidad de íconos · mejorar el layout de todas las vistas · que funcione en los casos aceptado, límite y error · dividir en subagentes en paralelo · probar como navegador». Referencia: `coreyhaines31/marketingskills` (skills `site-architecture`, `copy-editing`, `copywriting`, `cro`).
- Resultado observable: un operador con todos los permisos entra al backoffice, cada entrada del menú lo lleva a su sección sin rebotes ni esperas, el menú y las pestañas tienen menos opciones, y **cada listado** muestra: buscador multicampo, selectores de filtro, exportar CSV/JSON de lo filtrado, tabla con acciones de fila (ícono + nombre) y paginación (filas por página, ir a la página N, Anterior/Siguiente), resolviendo cargando / vacío / error.
- Kill-test: (1) la sonda `e2e/apoyo/sonda-de-navegacion.mjs` con todos los permisos: «Sistemas» abre `/sistemas/...` (hoy rebota a `/operacion/estado`); (2) en cualquier listado, escribir un texto que no coincide muestra el vacío orientado y «Descargar CSV» baja un archivo con solo el encabezado; (3) un listado con 25 filas y 10 por página muestra «Página 1 de 3» e «Ir a 3» muestra 5 filas.

## Hallazgos del descubrimiento (hechos, con evidencia)
- F1 · `rutas/sistemas/guardia-rol-sistemas.ts:14` exige rol PLATAFORMA/SEGURIDAD y redirige a `/operacion` en silencio; `layout/menu-lateral.ts` solo filtra por permiso → el botón «Sistemas» «no funciona» (sonda: `menu:Sistemas → /operacion/estado`). Evidencia: `evidencia/sonda-antes.json`.
- F2 · `app.config.ts` usa `provideRouter(routes, withComponentInputBinding())` sin estrategia de precarga: cada clic del menú descarga el chunk de la sección recién en ese momento (latencia percibida en despliegue). En dev local la navegación mide 60–190 ms.
- F3 · `operacion/solicitudes-escaladas` pide `GET /api/v1/grupos/solicitudes-escaladas` → 404 en el simulado (contrato inexistente).
- F4 · Solo 3 de ~43 listados usan `ap-paginacion`; ninguno exporta; `nucleo/exportador.ts` (CU-58) pide la exportación al servidor y no se usa en ningún listado.
- F5 · `buildSrc/.../aportaya.openapi.gradle.kts:42` no compila con `openapi-generator 7.26.0` (bump de Dependabot `e10b3399`); los clientes se generaron localmente con 7.14.0 sin commitear el cambio. Fuera de alcance: se registra.
- Restricción del repo: `scripts/verificar_frontend.py` bloquea archivos de pantalla > 200 líneas, literales de diseño y red fuera de `dominio/`/`nucleo/`.

## Alcance
- IN: `apps/backoffice/src/**`, `packages/ui/src/{icono,boton,tabla-de-datos,acciones-de-fila,exportar-listado}`, pruebas unitarias y e2e del backoffice, sonda de navegación.
- OUT: backend y contratos (`servicios/`, OpenAPI), `apps/web`, `apps/movil`, `packages/diseno_flutter`, tokens compartidos, `buildSrc` (F5).
- Ambigüedades registradas (a confirmar con Pablo):
  - A1 «Recortar opciones de más»: supuesto = menos entradas en menú y pestañas sin borrar rutas (las URLs viejas siguen andando o redirigen). Criterio de `site-architecture`: ≤ 7 entradas de primer nivel, agrupar por tarea, sin duplicados entre secciones.
  - A2 «Todos los botones de acción de la tabla deben ser editar y eliminar si corresponde»: supuesto = acciones de fila estándar Ver / Editar / Eliminar con ícono + nombre, **solo donde el contrato tiene la operación**; nunca Eliminar sobre registros append-only (asientos, bitácoras, movimientos — regla 91.1.3) ni un botón sin endpoint detrás. Las bandejas de trabajo conservan su acción de dominio (p. ej. «Revisar») con ícono + nombre.
  - A3 «Descargar CSV y JSON de lo filtrado»: supuesto = exportación en el navegador de las filas ya cargadas y filtradas, con los valores tal como se muestran (lo enmascarado sigue enmascarado). `nucleo/exportador.ts` (CU-58, exportación con huella y tope del servidor) no se reemplaza. Confirmar si alguna sección exige exportar solo por CU-58.
  - A4 Regla 70 (1 subagente, 1 navegador): el pedido explícito de Pablo pide subagentes en paralelo; se corren 6, cada uno en su worktree, con su propio `ng serve` y un navegador por agente. Desvío declarado.

## H0 — Base compartida (la hace el agente principal antes de repartir)
**CA:** Dado cualquier listado del backoffice, cuando se arma con el kit, entonces tiene buscador multicampo, N selectores, exportar CSV/JSON de lo filtrado, acciones de fila con ícono + nombre y paginación completa.
**DoD:** `yarn workspace @aportaya/ui test:front` y `yarn workspace @aportaya/backoffice test:front` verdes en lo nuevo · `typecheck` y `lint` del backoffice y ui en 0.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.S1.M1 | ~45 íconos nuevos en `ap-icono` (editar, eliminar, descargar, csv, json, guardar, aprobar, rechazar, enviar…) | `nombre="editar"` pinta un trazo | spec `icono` | TODO |
| H0.S1.M2 | `ap-boton` acepta `icono` (aditivo) | con `icono` hay svg antes del texto; sin él, igual que antes | spec `boton` | TODO |
| H0.S1.M3 | `ap-acciones-de-fila`: Ver/Editar/Eliminar ícono + nombre, Eliminar con confirmación | emite `eliminar` solo tras confirmar | spec | TODO |
| H0.S1.M4 | `exportar-listado`: CSV (RFC 4180, BOM, `;`-seguro, anti fórmula) y JSON de filas filtradas | comillas/saltos/`=` escapados; vacío → solo encabezado | spec | TODO |
| H0.S1.M5 | `crearListado()` (señales): búsqueda multicampo sin acentos, N filtros, orden, página, filas por página, acotado | 25 filas · 10 por página → 3 páginas; búsqueda «jose» encuentra «José» | spec | TODO |
| H0.S1.M6 | `ap-listado`: compone barra de filtros + exportar + tabla + paginación + estados | cargando/vacío/error/lista | spec + a11y | TODO |

## H1 — Navegación y shell (franja A)
**CA:** Dado un operador con todos los permisos, cuando pulsa cada entrada del menú y cada pestaña, entonces llega a esa sección (sin rebote), en ≤ 300 ms tras la primera carga, y el menú tiene ≤ 7 entradas de primer nivel con ícono.
**DoD:** sonda de navegación sin rebotes + specs del shell + e2e `shell.e2e.ts` verdes · capturas 3 anchos × 2 temas.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | El menú oculta lo que la guardia de rol no deja abrir (F1) | sin rol de sistemas no aparece «Sistemas»; con él, abre `/sistemas/servicios` | spec menú + sonda | TODO |
| H1.S1.M2 | Precarga de rutas en reposo (F2) | los chunks de sección se piden antes del clic | spec/inspección de red | TODO |
| H1.S1.M3 | Recorte de opciones (A1) | ≤ 7 de primer nivel; pestañas sin duplicados | spec menú | TODO |
| H1.S1.M4 | Íconos en pestañas de sección y cabecera | cada pestaña con ícono | spec pestañas | TODO |

## H2…H6 — Listados por sección (franjas B–F)
Cada franja aplica H0 a **todos** los listados de su sección, con la misma DoD: typecheck + lint + specs de la sección + recorrido en navegador (aceptado, límite = vacío/1 fila/última página, error = 4xx/5xx simulado) + capturas.

| ID | Franja | Sección | Estado |
|---|---|---|---|
| H2 | B | `rutas/operacion` | TODO |
| H3 | C | `rutas/cumplimiento` | TODO |
| H4 | D | `rutas/sistemas` | TODO |
| H5 | E | `rutas/contabilidad`, `rutas/configuracion` | TODO |
| H6 | F | `rutas/publicidad`, `rutas/tablero`, `rutas/admisiones` | TODO |

El detalle por microtarea de cada franja lo escribe el subagente en `franjas/<letra>.md` de esta carpeta, con el mismo formato (CA binario + DoD con comando + estado).

## H7 — Integración y regresión (agente principal)
**CA:** Con las seis franjas integradas, todo compila, la suite del backoffice y de ui está verde y la sonda recorre todas las vistas sin errores nuevos de consola/red.
**DoD:** `yarn typecheck`, `yarn lint`, `test:front`, `test:a11y` (backoffice y ui) · sonda «después» · e2e del backoffice · revisión independiente (`frontend-reviewer`).
**Estado:** TODO

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| 6 `ng serve` en 4 CPU | builds lentos, timeouts espurios | un servidor por worktree en puertos 4301–4306; un navegador por agente; sin `waitForTimeout` |
| Conflictos al integrar | trabajo pisado | franjas disjuntas por directorio; `packages/ui` y `nucleo/listado` solo los toca H0 |
| Botones sin endpoint | botón muerto | cada acción nueva exige operación en `packages/simulado/generado/prism/todos.yaml` |
