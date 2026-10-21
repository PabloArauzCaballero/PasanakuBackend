# Reporte — Catálogo de datos de punta a punta y rediseño integral del backoffice

> **AVANCE: 19 / 19 — 100 %.** Ninguna microtarea en rojo. La integración con `auditoria` desplegado (base real + gateway) **no** se ejercitó: el backend llega a `TESTED` y el backoffice a verificado contra un doble declarado (ver «No cubierto»).

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `marcelo/feat/catalogo-de-datos-y-rediseno-2026-10-21` (5 commits sobre `dev` 7752e1bf)
- Peldaño de evidencia alcanzado:
  - Generador del catálogo (semilla): `TESTED` (10 pruebas contra el modelo y el DDL reales; idempotencia demostrada).
  - Servicio `auditoria`: `TESTED` (unitarias contra el recurso real + web con MockMvc, matriz 401/403 automática).
  - Backoffice: `VERIFIED_FUNCTIONAL_ONLY` + prueba visual mirada (3 anchos × 2 temas) **contra un doble declarado** de `auditoria`.

## Qué estaba mal (hallazgos)
1. **El catálogo viajaba entero**: `GET /indicadores/catalogo-de-datos` devolvía ~1,5 MB (ahora ~2 MB) y la pantalla filtraba y paginaba en el navegador. El buscador global lo descargaba al abrir **cualquier** pantalla.
2. **Faltaban 49 tablas reales** en el catálogo: el outbox, la marca de consumo, el candado de trabajos y el estado de saga que `generar_ddl.py` crea en cada esquema.
3. **Relaciones incompletas**: solo `columna → tabla`; sin columna destino, restricción, acción al borrar, cardinalidad ni obligatoriedad. Los esquemas eran una lista de nombres.
4. **Notas de columnas perdidas**: una viñeta que explicaba dos columnas («`a` y `b`: …») no se reconocía; algunas columnas tenían `COMMENT 'NULL'` en el DDL.
5. **La ficha se abría solo por nombre de tabla**, ambiguo en cuanto se catalogan tablas que se repiten por esquema.
6. **Backoffice**: menú lateral con más de cuarenta enlaces, lienzo y modo oscuro teñidos de verde, botón primario naranja, filtros desalineados, secciones clave–valor que partían el texto palabra por palabra, tablas con celdas recortadas.
7. `apps/backoffice/proxy.e2e.json` apuntaba al puerto 80 aunque los e2e levantan el simulado en 4010.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Esquemas como objetos (servicio, descripción, cifras) | `python3 -I scripts/test_catalogo_de_datos.py -v` | PASS · `evidencia/H1-semilla-catalogo.txt` |
| H1.S1.M2 | Relaciones con columna destino, restricción, acción al borrar, cardinalidad y obligatoriedad, leídas del DDL; las 686 coinciden con `sql/20_claves` | ídem (`test_relaciones_coinciden_con_el_ddl`) | PASS |
| H1.S1.M3 | Columna FK con esquema destino; columna `unica` | ídem | PASS |
| H1.S1.M4 | Clasificación por tabla (personales, financieros, secretos, derivados) | ídem | PASS |
| H1.S1.M5 | Generador idempotente; +49 tablas de infraestructura; notas de varias columnas (cobertura 529 → 566, piso subido a 566) | `--escribir` ×2 + `cmp`; `verificar_narrativas.py` | PASS |
| H2.S1.M1 | Contrato: `resumen`, `tablas` (paginado), `tablas/{esquema}/{tabla}`; GET completo deprecado sin cambiar forma | `./gradlew generarClientes` | PASS |
| H2.S1.M2 | `ConsultarCatalogoDeDatos`: índice en memoria, filtros, orden fijo, límite 1..100 | `:servicios:auditoria:test` | PASS 43/43 · `evidencia/H2-auditoria.txt` |
| H2.S1.M3 | Controlador: 200/400/401/403/404 | `:servicios:auditoria:webTest` | PASS 74/74 |
| H3.S1.M1–M5 | Backoffice contra el contrato nuevo: página del servidor, KPIs del resumen, ruta `esquema/tabla` (la vieja redirige a la búsqueda), pestaña Esquemas, relaciones en palabras; buscador global perezoso | `ng test` + a11y + playwright | PASS 294/294, 2/2, 7/7 · `evidencia/H3-backoffice-catalogo.txt` |
| H4.S1.M1 | Capturas «antes» | script de capturas | `evidencia/capturas/antes/` |
| H4.S1.M2 | Capa visual del backoffice (rampa neutra de Atlas, tinta como primario, oscuro neutro) por tokens, con contraste AA medido | `node --test packages/tokens/pruebas/roles-contraste.test.mjs` · `vitest` (tokens) | PASS 2/2 · 15/15 |
| H4.S1.M3 | Menú con una entrada por sección (10 enlaces con todos los permisos) y pestañas de sección | `menu-lateral.spec`, `pestanas-de-seccion.spec`, `shell.e2e` | PASS |
| H4.S1.M4 | Shell, filtros, clave–valor y tabla virtualizada pulidos; cajón por debajo de 64rem | suite completa | PASS 920/920, a11y 78/78, lint, tsc, `verificar_frontend.py` TODO OK · `evidencia/H4-rediseno.txt` |
| H5.S1.M1 | Ejemplos del simulado con la estructura real y Postman regenerado | `generar_postman.py`; `vitest` (simulado) | PASS 344/344 |
| H5.S1.M2 | Este reporte | — | — |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Verificación en TEST con `auditoria` desplegado (resumen, página, ficha y guardado de ficha/revisión con sesión real) | TODO | Desplegar la rama en TEST y entrar con un operador con `TABLERO_SISTEMAS_VER` (+ `CATALOGO_DATOS_EDITAR`). |
| Retirar el GET completo deprecado | TODO | Confirmar que ningún cliente lo usa (el backoffice ya no). |
| Pestaña «Operaciones» (endpoint ↔ tabla) de Atlas | TODO | Requiere inferir impactos de endpoints sobre tablas; no existe en el modelo. Decisión de alcance de Marcelo/Pablo. |
| `sql/aplicar.sql` incluye `15_infra/borradores.sql`, que **no existe en ninguna rama** (la tabla `identidad.borrador_onboarding` se usa en código sin DDL versionado) | BLOQUEADO (fuera de alcance) | Lo debe resolver quien implementó el borrador de onboarding (servicio `identidad`): versionar el DDL en el generador. Una base nueva falla al aplicar el esquema completo. |
| 14 e2e que ya fallaban en `dev` (f2-posiciones ×4, ingreso con teclado ×1, sistemas-sin-contrato ×9) | TODO (preexistente) | Ver `evidencia/H4-e2e-base.txt`: fallan igual en 7752e1bf sin estos cambios. |

## Evidencia
- `evidencia/H1-semilla-catalogo.txt`, `H2-auditoria.txt`, `H3-backoffice-catalogo.txt`, `H4-rediseno.txt`, `H4-e2e-base.txt`.
- `evidencia/capturas/antes/` y `evidencia/capturas/despues/` (lista, ficha, relaciones, esquemas, retiros, tablero; 1440/768/375; claro y oscuro). Las `cu130-*` y `h9-*` son de los e2e del catálogo contra el doble.

```text
$ python3 -I scripts/test_catalogo_de_datos.py
Ran 10 tests in 0.148s — OK
$ ./gradlew :servicios:auditoria:test :servicios:auditoria:webTest
BUILD SUCCESSFUL — test: 43 tests, 0 fallas · webTest: 74 tests, 0 fallas
$ ng test (backoffice)        Test Files 148 passed · Tests 920 passed
$ ng test (a11y)              Test Files 42 passed · Tests 78 passed
$ playwright (todo)           150 passed · 14 failed (los mismos 14 fallan en dev 7752e1bf)
$ node --test packages/tokens/pruebas/roles-contraste.test.mjs   pass 2 · fail 0
```

## No cubierto
- **Integración real**: `auditoria` no se levantó con base + gateway; la web se probó con MockMvc y el backoffice contra un doble declarado (`servirCatalogo` en `e2e/apoyo/tableros-sinteticos.ts`) que filtra y pagina como `ConsultarCatalogoDeDatos`.
- `:servicios:auditoria:integrationTest` (Testcontainers) no se corrió: el contenedor no tiene Docker. jOOQ se generó contra un PostgreSQL 16 local con el esquema aplicado (sin `borradores.sql`).
- La app móvil y `diseno_flutter` no se tocaron; la capa de tema solo aplica con `data-producto="backoffice"`. No se corrieron goldens de Flutter (los tokens base no cambiaron).
- Lectores de pantalla reales: solo axe automático + navegación de los e2e.

## Desvíos del plan
- Paginación por número de página y no por cursor (convención del servicio): el catálogo es un recurso estático del despliegue; se documentó en el contrato.
- El buscador global pide los nombres de tabla (de a 100) la primera vez que alguien busca, en vez de descargar el catálogo al abrir el backoffice.
- La rampa neutra se registró en la bóveda (`docs/Views/Sistema-Diseno/estilos.css`) porque `tokens-contra-boveda.spec.ts` exige que toda primitiva salga de ahí.
- El cajón lateral se usa por debajo de 64rem (antes 40rem): en tablet la barra fija apretaba la cabecera.
- Se quitó el filtro «Módulo» de la lista (repetía al de esquema 1:1); sigue en la API.
- Se corrigió `proxy.e2e.json` (puerto 4010).
- Dos aserciones e2e se ajustaron a textos cambiados a propósito («Por revisar» en lugar de «Hacia el retiro»; clasificación «Sí · n columnas»), y un localizador de `shell.e2e` se acotó a la cabecera porque el grupo «Plataforma» del menú y el rol del usuario tienen el mismo nombre.

## Riesgos residuales
- La narrativa de negocio larga se muestra como un solo párrafo (el generador colapsa los renglones de las fichas); las viñetas internas se leen como texto corrido.
- `proxy.desarrollo.json` y `proxy.stack-local.json` siguen apuntando al puerto 80; no se tocaron porque no se pudo confirmar qué escucha ahí en cada máquina.
- Las capturas de los e2e se escriben por defecto en la carpeta de evidencia de otro trabajo (`2026-10-08-endurecimiento-integral`); acá se redirigieron con `CAPTURAS_DIR`.

## Decisiones y ambigüedades
1. **«Seeders de tablas, columnas, relaciones, esquemas»**: el catálogo no se siembra en tablas SQL; lo genera `scripts/catalogo_de_datos.py` desde el modelo y el DDL. Se trató ese generador como la semilla. **Confirmar con Marcelo/Pablo.**
2. **No se sembraron fichas de gobierno ni revisiones**: serían datos inventados (regla 97.4.1). Toda tabla arranca ACTIVA/DETECTADA hasta que una persona la revise.
3. **«Ultra HD como Admin Portal»**: se adoptó el lenguaje visual de Atlas (neutros, tinta, un acento) solo en el backoffice, manteniendo el verde de la marca como acento y en la barra lateral. **Confirmar con Marcelo.**
4. **«Demasiadas opciones»**: el menú pasó a una entrada por sección y las pantallas de cada sección viven en pestañas; ninguna pantalla se eliminó. **Confirmar con Marcelo.**
