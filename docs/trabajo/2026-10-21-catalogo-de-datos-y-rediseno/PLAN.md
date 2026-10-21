# Plan — Catálogo de datos de punta a punta (semillas, contrato, servicio, pantallas) y rediseño integral del backoffice

- Fecha: 2026-10-21 · Repos afectados: monorepo Pasanaku (`scripts/`, `servicios/auditoria`, `packages/simulado`, `packages/ui`, `apps/backoffice`, `postman/`) · Rama: `marcelo/feat/catalogo-de-datos-y-rediseno-2026-10-21`
- Predecesores: `2026-10-18-catalogo-de-metadata` (CU-130 ficha de gobierno), `2026-10-18-backoffice-como-atlas` (anatomía de Atlas), CU-131 (revisión de columna).
- Referencia: `PabloArauzCaballero/AtlasAdminPortal` (`src/features/data-catalog`, `src/features/systems/services.ts`, `internal-shell/nav-groups-*.ts`, `tailwind.config.ts`).
- Pedido literal (Marcelo, 2026-10-21): «revisar transversalmente los seeders de la catalogación de datos: 1) seeders de tablas, columnas, relaciones, esquemas; 2) endpoints de catalogación, payload mínimos, DTOs; 3) servicios y otros artefactos; 4) todo catalogado como en Atlas Admin Portal … que el backoffice tenga la mejor calidad posible» + «hay demasiadas opciones en el admin portal … rediseño integral que haga que la app se vea ultra HD así como los visuales de Admin Portal».
- Resultado observable: un operador con `TABLERO_SISTEMAS_VER` abre **Datos › Catálogo de datos** y ve (a) las cifras del inventario servidas por el backend, (b) la lista de tablas paginada **en el servidor** con búsqueda y filtros, (c) una pestaña **Esquemas** con cada esquema, su servicio dueño y sus cifras, y (d) la ficha `esquema.tabla` con columnas, **relaciones con cardinalidad, columna destino, restricción y acción al borrar**, gobierno y revisión; todo dentro de un backoffice con un sistema visual neutro, de alto contraste y jerarquía clara como Atlas, y un menú lateral corto (secciones con pestañas en lugar de decenas de enlaces).
- Kill-test: (1) `curl …/indicadores/catalogo-de-datos/tablas?limite=20` devuelve 20 filas y `meta.total = 342` sin columnas dentro; (2) la ficha `aportes.pago` muestra «N:1 · RESTRICT · fk_pago_…» en sus relaciones; (3) el menú lateral del backoffice con todos los permisos tiene ≤ 12 enlaces visibles sin desplegar nada.

## Alcance
- IN:
  - Generador del catálogo (`scripts/catalogo_de_datos.py`) — es la **semilla** del catálogo: esquemas como objetos, relaciones enriquecidas, resumen de sensibilidad por tabla, referencia de columna con esquema. Prueba unitaria del generador.
  - Contrato `auditoria.yaml`: `GET …/catalogo-de-datos/resumen`, `GET …/catalogo-de-datos/tablas` (paginado), `GET …/catalogo-de-datos/tablas/{esquema}/{tabla}`; DTOs mínimos; el GET completo existente queda **deprecado, no se borra** (96.4.1).
  - Servicio `auditoria`: índice en memoria tipado del catálogo (se lee una vez), consulta paginada con filtros y orden determinista, ficha por esquema+tabla, 404 de contrato; tests web y unitarios.
  - Backoffice: pantallas del catálogo contra los endpoints nuevos (paginación del servidor), ruta `esquema/tabla`, pestaña Esquemas, vista de relaciones enriquecida.
  - Rediseño integral del backoffice: capa de tema del backoffice (neutro como Atlas, un solo acento, tipografía y sombras), shell y menú lateral consolidado (≤ 12 entradas; subpantallas como pestañas de sección), revisión visual por capturas en 3 anchos × 2 temas.
  - Postman y ejemplos del simulado regenerados.
- OUT:
  - App móvil y `packages/diseno_flutter` (los tokens compartidos NO se cambian para la app: el tema del backoffice es una capa propia).
  - Cola de revisión global multi-familia de Atlas (endpoints, impactos, herramientas): Pasanaku no tiene esas familias.
  - Pestaña «Operaciones» (endpoint ↔ tabla) de Atlas: no hay inferencia de impactos en el modelo; se registra como pendiente.
  - Datos de gobierno inventados: **no se siembran fichas ni revisiones** (97.4.1). La ficha por omisión sigue siendo ACTIVA/DETECTADA.
- Ambigüedades registradas:
  1. «Seeders de tablas, columnas, relaciones, esquemas»: en este repo el catálogo **no se siembra en tablas SQL**: lo genera `catalogo_de_datos.py` desde el modelo PlantUML y lo sirve `auditoria`. Supuesto: «arreglar los seeders» = corregir y enriquecer ese generador (la semilla) y su JSON. Confirmar con Marcelo/Pablo.
  2. «Demasiadas opciones»: supuesto = el menú lateral muestra ~45 enlaces (secciones + subpantallas); se consolidan en secciones con pestañas como Atlas (`tabs` por ítem). Ninguna pantalla desaparece. Confirmar con Marcelo.
  3. «Ultra HD como Admin Portal»: supuesto = adoptar el lenguaje visual de Atlas (lienzo `#f5f5f7`, superficies blancas, líneas `#e4e4e7`, tinta `#1d1d1f`, un acento profundo y poco saturado, sombras suaves, tipografía Inter) **en el backoffice**, manteniendo el verde de la marca solo como acento. Confirmar con Marcelo.

## H1 — La semilla del catálogo describe esquemas, tablas, columnas y relaciones completas
**CA:** Dado el modelo PlantUML, cuando se corre el generador, entonces el JSON trae cada esquema con servicio, descripción y cifras; cada relación con columna destino, nombre de restricción, acción al borrar, cardinalidad y obligatoriedad; y la segunda corrida no cambia nada.
**DoD:** `python3 -I scripts/catalogo_de_datos.py --escribir` dos veces → `git diff --stat` igual · `python3 -I scripts/test_catalogo_de_datos.py` verde.
**Estado:** HECHO — `evidencia/H1-semilla-catalogo.txt`

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Esquemas como objetos `{nombre, servicio, descripcion, tablas, columnas, appendOnly}` | `esquemas[0]` es objeto con los 6 campos | test `test_esquemas_son_objetos` | HECHO |
| H1.S1.M2 | Relaciones: `columnaDestino`, `restriccion` (`fk_…` igual que el DDL), `alBorrar` (RESTRICT/SET NULL igual que el DDL), `cardinalidad` (N:1 / 1:1 por UNIQUE de una sola columna), `obligatoria` | las 686 FK del catálogo coinciden 1:1 con `sql/20_claves` (nombre + acción) | test `test_relaciones_coinciden_con_el_ddl` | HECHO |
| H1.S1.M3 | Columnas: `referencia` con esquema (`esquemaReferencia`), `unica` | columna FK trae esquema destino | test `test_columna_fk_trae_esquema` | HECHO |
| H1.S1.M4 | Resumen de sensibilidad por tabla (`clasificacion`: personales, financieros, secretos) | `pago.clasificacion.financieros > 0` | test `test_clasificacion_por_tabla` | HECHO |
| H1.S1.M5 | Idempotencia del generador | 2.ª corrida sin diff | `--escribir` ×2 + `git diff --quiet` | HECHO |

## H2 — Contrato y servicio: resumen, lista paginada y ficha por esquema+tabla
**CA:** Dado un operador con `TABLERO_SISTEMAS_VER`, cuando pide `GET …/catalogo-de-datos/tablas?pagina=2&limite=20&esquema=aportes`, entonces recibe 20 filas o menos, sin columnas, con `meta {pagina, limite, total, totalPaginas}` y orden esquema→tabla; sin permiso 403; tabla inexistente 404 `AP-CAT-01`.
**DoD:** `./gradlew :servicios:auditoria:test :servicios:auditoria:webTest` verde (salida pegada) · `generarClientes` verde.
**Estado:** HECHO — `evidencia/H2-auditoria.txt` (test 43/43, webTest 74/74)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Contrato OpenAPI: 3 operaciones + DTOs mínimos (`ResumenDelCatalogo`, `EsquemaDelCatalogo`, `PaginaDeTablasDelCatalogo`, `FilaDelCatalogo`, `RelacionDelCatalogo` ampliada); GET completo `deprecated: true` | el YAML valida y genera | `generarClientes` | HECHO |
| H2.S1.M2 | `ConsultarCatalogoDeDatos`: índice tipado en memoria, filtros (q normalizado sin acentos, esquema, modulo, estado, revision, sensibilidad), orden determinista, límite 1..100 | test unitario de filtros, bordes de página y límite | `:servicios:auditoria:test` | HECHO |
| H2.S1.M3 | Controlador: 3 endpoints con permiso y gobierno superpuesto; 404 de contrato | 200/400/401/403/404 | `:servicios:auditoria:webTest` (CatalogoDeDatosWebTest) | HECHO |

## H3 — Pantallas del catálogo contra el contrato nuevo
**CA:** Dado el backoffice, cuando el operador filtra o pagina, entonces se pide la página al servidor (no se descarga el catálogo entero) y la URL conserva el estado; la ficha se abre por `esquema/tabla`; la pestaña Esquemas lista los 17 esquemas con su servicio y cifras; las relaciones muestran cardinalidad, columna destino, restricción y acción al borrar en palabras.
**DoD:** `yarn workspace @aportaya/backoffice test:front` verde · `typecheck` · `lint` · capturas 3 anchos × 2 temas miradas.
**Estado:** HECHO (funcional) — `evidencia/H3-backoffice-catalogo.txt`: 294/294 unitarias, a11y 2/2, e2e 7/7 contra el doble declarado; la apariencia se cierra en H4.
- Desvío registrado: el buscador global ya no baja el catálogo entero al abrir el backoffice; pide los nombres de tabla (de a 100) la primera vez que alguien busca.
- Desvío registrado: `proxy.e2e.json` apuntaba al puerto 80; los e2e levantan el simulado en 4010.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Acceso a datos: `httpResource` por resumen, página y ficha | spec de dominio | `test:front` | HECHO |
| H3.S1.M2 | Lista con paginación del servidor + KPIs del resumen | spec de pantalla | `test:front` | HECHO |
| H3.S1.M3 | Ruta `catalogo-de-datos/:esquema/:tabla` (la vieja `:tabla` redirige) | spec de rutas | `test:front` | HECHO |
| H3.S1.M4 | Pestaña Esquemas | spec | `test:front` | HECHO |
| H3.S1.M5 | Relaciones enriquecidas | spec de `vista-relaciones` | `test:front` | HECHO |

## H4 — Rediseño integral del backoffice (visual de Atlas) y menú consolidado
**CA:** Dado cualquier operador, cuando abre el backoffice, entonces ve un lienzo neutro, superficies blancas con líneas finas, un solo acento, tipografía con jerarquía clara, y un menú lateral con ≤ 12 entradas cuyas subpantallas aparecen como pestañas de sección; en tema claro y oscuro, en 375/768/1440 px, sin desbordes.
**DoD:** capturas antes/después en `evidencia/capturas/` miradas · `test:front` y `test:a11y` verdes · `verificar_frontend.py` sin literales.
**Estado:** HECHO — `evidencia/H4-rediseno.txt` (920/920, a11y 78/78, contraste AA de la capa, e2e 150 verdes y los 14 rojos preexistentes en dev: `H4-e2e-base.txt`), capturas en `evidencia/capturas/{antes,despues}`.
- Desvíos: el cajón lateral pasa a usarse por debajo de 64rem (antes 40rem), como el `lg` de Atlas; la paleta neutra se registró en la bóveda (`docs/Views/Sistema-Diseno/estilos.css`) porque `tokens-contra-boveda.spec.ts` exige que toda primitiva salga de ahí.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Capturas del estado actual (antes) | 3 anchos × 2 temas | archivos en `evidencia/capturas/antes-*` | HECHO |
| H4.S1.M2 | Capa de tema del backoffice (roles CSS) con el lenguaje de Atlas en claro y oscuro | contraste AA de texto sobre superficies | `test:a11y` + capturas | HECHO |
| H4.S1.M3 | Menú lateral: secciones sin subpantallas desplegadas; pestañas de sección en la cabecera | ≤ 12 enlaces en el menú | `menu.spec.ts` + `menu-lateral.spec.ts` | HECHO |
| H4.S1.M4 | Pulido del shell (barra superior, ancho de contenido, tarjetas, tablas) | capturas sin desbordes a 375 px | capturas después | HECHO |

## H5 — Artefactos y cierre
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Ejemplos del simulado + Postman regenerados | colección con las 3 operaciones nuevas | `python3 scripts/generar_postman.py` | HECHO |
| H5.S1.M2 | REPORTE.md | 3 secciones + avance primera línea | archivo | HECHO |

## H6 — Cierre de pendientes (pedido: «implementalo todo lo que quede pendiente»)
**CA:** Dado el REPORTE con pendientes, cuando se cierra H6, entonces cada pendiente queda resuelto con evidencia o, si no se puede ejercitar en este contenedor, declarado con su causa; ningún e2e del backoffice en rojo.
**DoD:** salida literal de cada DoD en `evidencia/H6-*.txt` · suite del backoffice + e2e completos en verde · REPORTE actualizado.
**Estado:** EN CURSO

Hallazgos de descubrimiento (con fuente):
- `proxy.stack-local.json` → `127.0.0.1:80` es correcto: es el `nginx` del compose (`despliegue/compose/base.yml:263` `"80:80"`). `proxy.desarrollo.json` es el de `ng serve` por defecto y su trabajo de origen lo declara «(Prism)» (`docs/trabajo/2026-10-02-escaner-identidad-5-caras/PLAN.md` H12.S3.M3), pero apunta a 80 → defecto.
- `sql/aplicar.sql` incluye `15_infra/borradores.sql`, que `generar_ddl.py:1176` referencia pero nadie escribe. La tabla `identidad.borrador_onboarding` se usa en `BorradorOnboardingRepositorio.java` (SELECT/INSERT … ON CONFLICT (correo)/DELETE). Fuente de tipos: el contrato `identidad.yaml` `BorradorOnboarding` (longitudes y enums) + el repositorio (columnas, `ON CONFLICT (correo)`, `actualizado_en`/`expira_en` timestamptz). No se inventa nada fuera de esas dos fuentes.
- Pestaña «Operaciones»: no hay relación endpoint↔tabla en el modelo, pero sí dos fuentes reales encadenables: endpoint → CU (`scripts/matriz_de_pruebas.py`, que lee `Traza.marcarCasoDeUso` y el contrato) y CU → tablas (los `[[tabla]]` que cada `docs/CasosDeUso/CU-*.md` enlaza). Se muestra como «casos de uso que nombran esta tabla y sus endpoints», no como «endpoints que escriben esta tabla».
- La matriz `docs/catalogo-de-flujos/matriz.md` y `docs/auditoria-produccion/endpoints.md` no listan las 3 operaciones nuevas de `auditoria` → verificar si el CI las vigila.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Inventario y matriz de endpoints al día con las 3 operaciones nuevas | `--check` sale 0 | `python3 scripts/inventario_endpoints.py --check` (la matriz no tiene `--check`: se regenera) | HECHO — `evidencia/H6-S1-inventario.txt` |
| H6.S1.M2 | `proxy.desarrollo.json` apunta al simulado (4010); `stack-local` queda en 80 | `ng serve` + `curl /api/v1/...` responde el simulado | curl contra el dev server | HECHO — `evidencia/H6-S1-proxy.txt` (http=200) |
| H6.S2.M1 | e2e `f2-posiciones-y-roles` (×4) en verde, causa clasificada | 4/4 PASS | `playwright test f2-posiciones-y-roles` | HECHO — TEST_BUG, 10/10 · `evidencia/H6-S2-e2e-preexistentes.txt` |
| H6.S2.M2 | e2e `f2-red-y-teclado` «ingreso solo con teclado» en verde, causa clasificada | PASS | `playwright test f2-red-y-teclado` | HECHO — PRODUCT_BUG (`ap-campo` desbordaba a 360 px), 7/7 |
| H6.S2.M3 | e2e `sistemas-sin-contrato` (×9) en verde, causa clasificada | 9/9 PASS | `playwright test sistemas-sin-contrato` | HECHO — PRODUCT_BUG (alertas anidadas), 11/11 |
| H6.S3.M1 | `generar_ddl.py` escribe `15_infra/borradores.sql` (tabla + GRANT DELETE) desde contrato+repositorio | base nueva aplica `aplicar.sql` completo | `psql -f sql/aplicar.sql` sobre base vacía, exit 0 | HECHO — exit 0 dos veces + SQL del repositorio como `svc_identidad` · `evidencia/H6-S3-borradores.txt` |
| H6.S3.M2 | El catálogo de datos incluye `identidad.borrador_onboarding` con su clasificación (personales) | aparece en el JSON con `tipo` INFRAESTRUCTURA | `test_catalogo_de_datos.py` | HECHO — + `origenNegocio: ddl` en el contrato · `evidencia/H6-S3-S5-catalogo-y-auditoria.txt` |
| H6.S4.M1 | La narrativa de negocio conserva sus viñetas (párrafos y listas) en el JSON | una ficha con viñetas llega como líneas separadas | `test_catalogo_de_datos.py` | HECHO — 41 campos; separadores `---` fuera |
| H6.S4.M2 | El backoffice pinta la narrativa con párrafos y listas | spec del componente | `test:front` | HECHO — `ClaveValor.texto` en `ap-seccion-clave-valor`; capturas `cu130-ficha-narrativa-*` miradas |
| H6.S5.M1 | El generador agrega `operaciones` por tabla (CU + endpoints) con su fuente | tabla de ejemplo con sus CU | `test_catalogo_de_datos.py` | HECHO — 328/342 tablas nombradas por algún CU, 306 con endpoint |
| H6.S5.M2 | El contrato y `auditoria` exponen `operaciones` en la ficha | webTest de la ficha | `:servicios:auditoria:test webTest` | HECHO — 44/44 · 74/74 |
| H6.S5.M3 | Pestaña «Operaciones» en la ficha del backoffice | spec + e2e del catálogo | `test:front` + playwright catálogo | HECHO — 35/35 specs del catálogo, e2e 5/5; capturas `cu130-ficha-operaciones-*` miradas (la 1.ª versión en tabla cortaba la columna Servicio: se rehízo en tarjetas por CU) |
| H6.S6.M1 | Retiro del GET completo deprecado (contrato, controlador, clientes, Postman, simulado) si no queda consumidor | `grep` sin consumidores; build y tests verdes | `generarClientes` + tests auditoria + `test:front` | DESCARTADO en esta entrega (decisión: Claude, regla 98.1.3) — el backoffice se despliega aparte (`despliegue/coolify/backoffice.yml`); quitarlo ahora rompe el backoffice que está en producción si `auditoria` sale primero. Se declaró en el contrato la condición de retiro; la fecha la confirman Marcelo/Pablo |
| H6.S6.M2 | `auditoria` pasa los gates de CI que H2 rompía (spotless, `testBarrido`: archivo ≥300 líneas y constante con nombre de umbral) | `check` + `testBarrido` en verde | `./gradlew :servicios:auditoria:check … testBarrido` | HECHO — `evidencia/H6-S3-S5-catalogo-y-auditoria.txt` |
| H6.S7.M1 | Regresión completa + REPORTE actualizado | suites en verde | salidas en `evidencia/H6-regresion.txt` | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Gradle descarga dependencias con 429 desde Maven Central en este contenedor | No se pueden correr tests Java | Reintentos acotados; si persiste, BLOCKED con evidencia y verificación contra el doble (regla 65) |
| Sin backend real ni base en el contenedor | La integración real no se ejercita | Tests web con MockMvc + e2e contra doble declarado; verificar en TEST |
| Rediseño grande toca muchas pantallas | Regresiones visuales | Cambio por tokens/roles (no por componente) + capturas + suite completa |
