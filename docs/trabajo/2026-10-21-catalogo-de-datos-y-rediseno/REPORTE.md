# Reporte — Catálogo de datos de punta a punta y rediseño integral del backoffice

> **AVANCE: 33 / 34 — 97,1 %.** Ningún test en rojo: e2e del backoffice 165/165 (antes 150 + 14 rojos heredados de `dev`). La microtarea restante (retirar el GET completo deprecado) se DESCARTÓ en esta entrega por la regla 98.1.3; ver «Pendiente». La integración con `auditoria` desplegado (base real + gateway) **no** se ejercitó: backend en `TESTED`, backoffice verificado contra un doble declarado.

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `marcelo/feat/catalogo-de-datos-y-rediseno-2026-10-21` (sobre `dev` 7752e1bf)
- Peldaño de evidencia alcanzado:
  - Generador del catálogo y DDL (semilla): `TESTED` (13 pruebas contra el modelo, el DDL, los CU y la matriz reales; idempotencia demostrada). El esquema completo se aplicó sobre una base PostgreSQL 16 vacía (exit 0, dos veces).
  - Servicio `auditoria`: `TESTED` (44 unitarias contra el recurso real + 74 web con MockMvc, matriz 401/403 automática; `check` y `testBarrido` en verde).
  - Backoffice y `@aportaya/ui`: `VERIFIED_FUNCTIONAL_ONLY` + prueba visual mirada (3 anchos × 2 temas) **contra un doble declarado** de `auditoria`.

## Qué estaba mal (hallazgos)
1. **El catálogo viajaba entero** (~2 MB) y la pantalla filtraba y paginaba en el navegador; el buscador global lo bajaba al abrir cualquier pantalla.
2. **Faltaban tablas reales en el catálogo**: las 49 de infraestructura (outbox, consumo, candado, saga) y `identidad.borrador_onboarding`.
3. **Relaciones incompletas** (sin columna destino, restricción, acción al borrar, cardinalidad ni obligatoriedad) y esquemas como simple lista de nombres.
4. **Notas perdidas o aplanadas**: viñetas de varias columnas no reconocidas; la narrativa de negocio perdía sus listas y se leía como un párrafo corrido.
5. **La ficha se abría solo por nombre de tabla**, ambiguo en cuanto hay nombres repetidos por esquema.
6. **Backoffice**: más de cuarenta enlaces en el menú lateral, lienzo y modo oscuro teñidos de verde, botón primario naranja, filtros desalineados, clave–valor partido palabra por palabra.
7. **`sql/aplicar.sql` incluía `15_infra/borradores.sql`, que no existía**: una base nueva no podía aplicar el esquema completo, aunque `identidad` usa esa tabla.
8. **14 e2e en rojo en `dev`**, de los cuales 2 eran defectos reales de la UI compartida: la pantalla de ingreso desbordaba 40 px a 360 px (`ap-campo`) y las pantallas de error anunciaban la alerta dos veces (`role="alert"` anidados).
9. `proxy.desarrollo.json` (el de `ng serve`) apuntaba al puerto 80 en vez de al simulado (4010).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1–M5 | Esquemas como objetos; relaciones enriquecidas leídas del DDL (las 686 coinciden con `sql/20_claves`); FK con esquema destino y `unica`; clasificación por tabla; +49 tablas de infraestructura; notas de varias columnas | `python3 -I scripts/test_catalogo_de_datos.py` | PASS · `evidencia/H1-semilla-catalogo.txt` |
| H2.S1.M1–M3 | Contrato `resumen` / `tablas` (paginado) / `tablas/{esquema}/{tabla}`; `ConsultarCatalogoDeDatos` con índice en memoria; 200/400/401/403/404 | `./gradlew :servicios:auditoria:test webTest` | PASS · `evidencia/H2-auditoria.txt` |
| H3.S1.M1–M5 | Backoffice contra el contrato nuevo (página del servidor, KPIs, ruta `esquema/tabla`, pestaña Esquemas, relaciones en palabras; buscador perezoso) | `ng test` + playwright | PASS · `evidencia/H3-backoffice-catalogo.txt` |
| H4.S1.M1–M4 | Rediseño con el lenguaje de Atlas por tokens (contraste AA medido), menú con una entrada por sección + pestañas de sección, shell pulido | suites + capturas | PASS · `evidencia/H4-rediseno.txt`, `capturas/{antes,despues}` |
| H5.S1.M1–M2 | Ejemplos del simulado y Postman; reporte | `generar_postman.py`; vitest simulado | PASS |
| H6.S1.M1 | Inventario y matriz de endpoints al día; el CI (paso 6j) ahora exige que la matriz esté regenerada | `inventario_endpoints.py --check` | PASS · `evidencia/H6-S1-inventario.txt` |
| H6.S1.M2 | `proxy.desarrollo.json` → simulado 4010 (`stack-local` sigue en el nginx :80 del compose) | `ng serve` + `curl /api/v1/...` | http=200 · `evidencia/H6-S1-proxy.txt` |
| H6.S2.M1 | `f2-posiciones-y-roles`: TEST_BUG (la entrada se llama «Inicio») | playwright | 10/10 · `evidencia/H6-S2-e2e-preexistentes.txt` |
| H6.S2.M2 | `f2-red-y-teclado`: PRODUCT_BUG en `ap-campo` (ancho intrínseco del input) | sonda + playwright | 7/7, `scrollWidth` 400 → 360 |
| H6.S2.M3 | `sistemas-sin-contrato`: PRODUCT_BUG de accesibilidad (alertas anidadas) | specs ui + playwright | 11/11 |
| H6.S3.M1 | `generar_ddl.py` escribe `15_infra/borradores.sql` desde el contrato `BorradorOnboarding` + el repositorio; `GRANT DELETE` a `svc_identidad` | `psql -f sql/aplicar.sql` en base vacía | exit 0 ×2; SQL del repositorio ejecutado como `svc_identidad` · `evidencia/H6-S3-borradores.txt` |
| H6.S3.M2 | `borrador_onboarding` catalogada (datos personales); `origenNegocio: ddl` en el contrato | `test_catalogo_de_datos.py` + `ConsultarCatalogoDeDatosTest` | PASS · `evidencia/H6-S3-S5-catalogo-y-auditoria.txt` |
| H6.S4.M1–M2 | La narrativa conserva párrafos y viñetas (41 campos) y el backoffice la pinta como `<p>`/`<ul>`/`<ol>` (`ClaveValor.texto`) | generador + specs + e2e | PASS · capturas `cu130-ficha-narrativa-*` |
| H6.S5.M1–M3 | **Pestaña «Operaciones»** (la de endpoints de Atlas): por tabla, los casos de uso que la enlazan y sus endpoints. 328 de 342 tablas de negocio nombradas por algún CU; 306 con endpoint | generador · auditoria 44/74 · specs 35 · e2e 5/5 | PASS · capturas `cu130-ficha-operaciones-*` |
| H6.S6.M2 | `auditoria` vuelve a pasar los gates de CI que H2 había roto (spotless, archivo ≥ 300 líneas, constante con nombre de umbral) | `check` + `testBarrido` | PASS |
| H6.S7.M1 | Regresión completa | ver «Evidencia» | PASS · `evidencia/H6-regresion.txt` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H6.S6.M1 · Quitar el GET completo deprecado | DESCARTADO en esta entrega (lo decidió Claude, regla 98.1.3) | El backoffice se despliega aparte de `auditoria` (`despliegue/coolify/backoffice.yml`): si se quita ahora y `auditoria` sale primero, el backoffice que está en producción se rompe. La condición de retiro quedó escrita en el contrato. Se quita cuando este backoffice esté en TEST y PROD, con la **fecha que confirmen Marcelo/Pablo**. El frontend ya no lo usa y en la app móvil no hay ningún consumidor (grep). |
| Verificación en TEST con `auditoria` desplegado (resumen, página, ficha con Operaciones, guardado de ficha y revisión con sesión real) | TODO | Desplegar la rama en TEST y entrar con un operador con `TABLERO_SISTEMAS_VER` (+ `CATALOGO_DATOS_EDITAR`). No se puede hacer desde este contenedor. |
| `:servicios:identidad:integrationTest` (`BorradorOnboardingRepositorioTest`) contra la tabla nueva | TODO | Necesita Docker (Testcontainers). Acá se ejecutó el SQL literal del repositorio contra la base aplicada. |

## H7 — Port a PasanakuBackend (de donde se despliega TEST)
TEST se despliega desde **PasanakuBackend** `test` (`despliegue/host/reconstruir-si-cambio.sh:34`); PasanakuFrontend es un espejo con una foto del 2026-10-19. El trabajo se portó sobre Backend `dev` (`14195f55`) archivo por archivo: 64 entraron tal cual (Backend no los había tocado), 14 chocaban y se resolvieron a mano. Evidencia: `evidencia/H7-port-backend.txt`.
- **Entró:** catálogo por partes (contrato, `auditoria`, pantalla), esquemas, relaciones del DDL, 50 tablas de infraestructura (incluido `borrador_onboarding`, cuyo «qué es» sale del encabezado de su DDL), narrativa con viñetas, arreglos de `ap-campo` y de la doble alerta, e2e corregidos, proxies al simulado, ejemplos y Postman. El generador conserva lo de Pablo (explicación de toda columna con `inyectar_metadata`, piso 3734).
- **No entró, a propósito (sin regresión sobre lo que Pablo integró el 2026-10-20 y ya está en TEST):** menú con pestañas de sección y capa visual propia (Pablo rediseñó menú y shell), pestaña «Operaciones» propia y el campo `operaciones` del contrato (queda la de Pablo, con impacto real en la base), `origenNegocio: ddl`, y el `borradores.sql` propio (queda el de Backend, ya aplicado en TEST).
- **Resultado contra la línea base de `dev`:** e2e 162 ✓ / 3 ✗ (base 146 / 18; los 3 ya fallaban y no se tocan: admisiones ×2, shell ×1), unitarias y a11y iguales o más, `auditoria` 44 + 74 + check + barrido verdes, esquema aplicado en base vacía y sobre el de `dev` sin errores.
- **Preexistente en `dev`, no tocado:** `verificar_frontend.py` falla en `explicador-de-vista.component.ts` (literal de diseño y 314 líneas).

## Evidencia
- `evidencia/H1-…`, `H2-…`, `H3-…`, `H4-rediseno.txt`, `H4-e2e-base.txt` (los 14 rojos de `dev`), `H6-S1-inventario.txt`, `H6-S1-proxy.txt`, `H6-S2-e2e-preexistentes.txt`, `H6-S3-borradores.txt`, `H6-S3-S5-catalogo-y-auditoria.txt`, `H6-regresion.txt`.
- `evidencia/capturas/antes/` y `despues/` (lista, ficha, relaciones, esquemas, narrativa, operaciones, ingreso a 360 px; 1440/768/375 y claro/oscuro, todas miradas).

```text
$ python3 -I scripts/test_catalogo_de_datos.py            Ran 13 tests — OK
$ psql -f sql/aplicar.sql   (base vacía, PostgreSQL 16)   exit=0 · 0 líneas con ERROR (y 2.ª corrida igual)
$ ./gradlew :servicios:auditoria:test :servicios:auditoria:webTest   test 44 · webTest 74 · 0 fallas
$ ./gradlew :servicios:auditoria:check … :servicios:auditoria:testBarrido   BUILD SUCCESSFUL
$ backoffice  test:front 923 passed · test:a11y 78 passed · typecheck exit 0 · lint «TODO OK»
$ packages/ui test:front 164 passed · test:a11y 9 passed
$ apps/web    test:front 74 passed  · test:a11y 8 passed
$ packages/simulado vitest 344 passed
$ playwright (backoffice, todo, 1 worker)   165 passed (4.8m)
```

## No cubierto
- **Integración real**: `auditoria` no se levantó con base + gateway; la web se probó con MockMvc y el backoffice contra un doble declarado (`servirCatalogo` en `e2e/apoyo/tableros-sinteticos.ts`) que lee el recurso real y filtra y pagina como `ConsultarCatalogoDeDatos`.
- `integrationTest` de `auditoria` e `identidad` (Testcontainers): no hay Docker en el contenedor.
- `proxy.stack-local.json` (nginx :80) no se ejercitó: requiere el stack Docker.
- La app móvil y `diseno_flutter` no se tocaron; la capa de tema aplica solo con `data-producto="backoffice"`. `apps/web` usa `ap-campo`: sus specs pasan, pero no se tomaron capturas de la web.
- Lectores de pantalla reales: solo axe automático + navegación por teclado de los e2e.

## Desvíos del plan
- Paginación por número de página y no por cursor (convención del servicio; el catálogo es un recurso estático del despliegue).
- El buscador global pide los nombres de tabla (de a 100) recién la primera vez que alguien busca.
- La rampa neutra se registró en la bóveda (`docs/Views/Sistema-Diseno/estilos.css`), porque `tokens-contra-boveda.spec.ts` exige que toda primitiva salga de ahí.
- El cajón lateral se usa por debajo de 64rem (antes 40rem).
- Se quitó el filtro «Módulo» de la lista: repetía al de esquema 1:1. Sigue en la API.
- H6: «Operaciones» se dibujó primero como tabla; la captura mostró la columna Servicio cortada y la ruta apretada en móvil. Se rehízo como tarjetas por caso de uso.
- H6: el generador lee endpoint → CU desde `docs/catalogo-de-flujos/matriz.md` (generado) y no importa `matriz_de_pruebas.py`, porque el paso 6i del CI corre con `python3 -I` sin PyYAML. Para que no vuelva a quedar desactualizada, el paso 6j verifica que la matriz esté regenerada.
- H6: se corrigieron defectos propios de H2 que el barrido detectaba y no se habían corrido (`testBarrido`, spotless).
- Aserciones e2e ajustadas a textos cambiados a propósito, sin debilitarlas: «Por revisar», «Sí · n columnas», el localizador de cabecera de `shell.e2e` y «Inicio» en `f2-posiciones-y-roles`.

## Riesgos residuales
- **`identidad.borrador_onboarding` guarda datos personales en claro** (nombres, número de documento) durante 7 días, mientras que `documento_identidad` guarda el número cifrado. El DDL reproduce lo que ya hace el código; no se cambió el comportamiento. Lo debe revisar el dueño de `identidad`, contra la regla 90.2.
- `monto_mensual_estimado` del borrador es texto (el contrato no fija formato) y la app móvil lo arma con `double.tryParse` (`borrador_alta.dart:111`): un importe en `double` va contra la regla 91.1.1. Está fuera de alcance y queda anotado.
- «Operaciones» dice qué casos de uso *nombran* la tabla, no qué endpoint la escribe. El CU de cada endpoint sale de la heurística de `matriz_de_pruebas.py` (el primer «CU-n» de la descripción del contrato, o si no la traza). Por eso `obtenerResumenDelCatalogo` figura como CU-130.
- `inventario_endpoints.py` muestra `SIN_ANOTACION` cuando hay un `@PermisoAlternativo` entre `@Permiso` y el método (la regex exige que estén pegados). Es una heurística preexistente y no se cambió.
- Las capturas de algunos e2e se escriben en `evidencia/` de la raíz (de otro trabajo). Se restauraron con `git checkout` y no se commitean.

## Decisiones y ambigüedades
1. **«Seeders de tablas, columnas, relaciones, esquemas»**: el catálogo no se siembra en tablas SQL. Lo generan `scripts/catalogo_de_datos.py` y `generar_ddl.py` desde el modelo, el DDL y la bóveda, y se trataron como la semilla. **Confirmar con Marcelo/Pablo.**
2. **No se sembraron fichas de gobierno ni revisiones**: serían datos inventados (regla 97.4.1).
3. **«Ultra HD como Admin Portal»**: lenguaje visual de Atlas solo en el backoffice, con el verde de la marca como acento. **Confirmar con Marcelo.**
4. **«Demasiadas opciones»**: una entrada por sección y pestañas de sección; no se eliminó ninguna pantalla. **Confirmar con Marcelo.**
5. **Columnas de `borrador_onboarding`**: longitudes y enums tomados del contrato. Donde el contrato no fija tope (`montoMensualEstimado`) se usó `TEXT` en lugar de inventar uno. **Confirmar con el dueño de `identidad`.**
6. **Retiro del GET completo**: no se quitó (ver Pendiente). **Fecha de retiro: confirmar con Marcelo/Pablo.**
