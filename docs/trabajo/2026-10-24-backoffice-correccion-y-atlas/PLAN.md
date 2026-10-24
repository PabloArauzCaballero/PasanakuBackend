# Plan — Backoffice: corregir lo roto en TEST y llevarlo al estándar del portal interno de Atlas

- Fecha: 2026-10-24 · Repos afectados: `PasanakuBackend` (fuente de TEST) y `PasanakuFrontend` (espejo) · Predecesores: `2026-10-18-backoffice-como-atlas` (H5.S1–S14 y H7 en TODO), `2026-10-21-catalogo-de-datos-y-rediseno` (solo en el espejo), `2026-10-21-menu-plegable-y-guardia-sistemas`, `2026-10-21-pulido-cargas-y-barra-clara`
- Estado del plan: **Fase 2 escrita (2026-10-24)** con las respuestas de Pablo (ver «Respuestas de Pablo»). H0 en curso; H1 bloqueado por la red del entorno.
- Resultado observable: un operador entra a `backoffice.aportaya…` con su cuenta real, recorre **cada** pantalla y **cada** botón, y en todas ve el esqueleto al cargar, el vacío con orientación cuando no hay datos, el error con traza y reintentar cuando falla, y la acción persiste y se ve al recargar.
- Kill-test: abrir en TEST tres pantallas con datos y tres sin datos: si alguna muestra la tabla en blanco (ni esqueleto ni vacío) o un botón que no hace nada, no está hecho.

## Alcance
- IN: backoffice (`apps/backoffice`, `packages/ui`) y los contratos de backend que cada pantalla necesite para no ser fachada.
- OUT (hasta que se decida lo contrario): app móvil, sitio web, pantallas de Atlas sin equivalente en el pasanaku (préstamos, cartera, comercios, productos de crédito, laboratorio QA, stress).
- Ambigüedades registradas: ver «Preguntas abiertas» al final. **Ninguna se resolvió por conveniencia.**

## Hallazgos (fase 1) — hechos, con ruta

### 1. Dónde vive la versión desplegada y cuál es «la última versión absoluta»
| Hecho | Evidencia |
|---|---|
| TEST se reconstruye **desde `PasanakuBackend`, rama `test`**, con un temporizador en el host | `despliegue/host/reconstruir-si-cambio.sh:34` (`github.com/PabloArauzCaballero/PasanakuBackend`) |
| URLs de TEST: backoffice `https://backoffice.aportaya.161.97.85.216.sslip.io`, gateway `https://api.aportaya.161.97.85.216.sslip.io` | `despliegue/TEST.md:7-10` |
| En `PasanakuBackend`, `dev` == `test` (`040e0bd7` / `b4ba8263`, 2026-10-23, PRs #185/#186). **No hay PRs abiertos.** | `git fetch` + `list_pull_requests` |
| `PasanakuFrontend` es un **espejo** que quedó atrás: le faltan **90 commits** de `PasanakuBackend/dev` (incluidos los dos últimos del backoffice: `3d4344e3` guardia de Sistemas + acordeón, `2157ff50` esqueleto/girador al cargar) | `git log origin/dev..frontend/dev` y viceversa; último sync del espejo: «Sincronizar dev del espejo con PasanakuBackend test (#24)», 2026-10-15 |
| El espejo tiene **30 commits que no están en Backend**: 25 de dependabot/CI y **5 de Marcelo** (`c021bc97`, `47182ccb`, `05c0a2ee`, `7b07fcb7`, `21ef40f2`: catálogo por partes, 14 e2e cerrados, menú «una entrada por sección + pestañas de sección», borrador de onboarding). No están en Backend ni por patch-id | `git patch-id` sobre `frontend/dev..origin/dev` |
| Merge de prueba `frontend/dev → origin/dev` (sin commit, abortado): **28 archivos en conflicto**, 11 del backoffice/ui. El conflicto de fondo es de **diseño**: Marcelo hizo pestañas de sección; Backend hizo menú en acordeón (`layout/menu-lateral.ts`, `shell-financiero.*`, `tabla-de-datos-virtualizada.ts`, `catalogo/*`) | `evidencia/F0-merge-de-prueba-frontend-dev.log` |
| Único PR abierto en el espejo: #38 (dependabot gradle) | `list_pull_requests` |
| Ramas de trabajo de esta sesión: `claude/keen-cray-d0wpgf` en los dos repos. En Backend quedó **sobre `origin/dev` = `test`** (antes apuntaba a `main`, 2 días atrás); en el espejo ya estaba sobre `origin/dev` | `git checkout -B` |

### 2. Lo que está roto (lo que se pudo determinar sin entrar a TEST)
| Síntoma reportado | Qué encontré | Dónde |
|---|---|---|
| «Al cargar no muestra la animación de cargando» | El organismo `ap-estado-de-pantalla` **sí** pinta esqueleto (`loading`), «Actualizando…» (`reloading`), vacío y error — pero solo cuando la pantalla le pasa un `ResourceRef`. **29 de 52 pantallas** lo usan; **24 archivos de dominio** traen datos (`resource(`/`HttpClient.get`) sin pasar por él, y esas pantallas resuelven los estados a mano o no los resuelven | `packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts:14-20`; conteo en `evidencia/` (grep) |
| «Si está vacío debe mostrarlo como vacío» | Idem: depende de que la pantalla pase `[vacio]`. Hay pantallas con tabla sin `[vacio]` y la tabla queda en blanco | a confirmar pantalla por pantalla (fase 2) |
| «Todas las opciones no funcionan» | La causa de «toco una opción y no cambia nada» en **Sistemas** ya está diagnosticada y corregida en `test` el 2026-10-21 (`guardia-rol-sistemas.ts` solo dejaba pasar `rol ∈ {PLATAFORMA, SEGURIDAD}`; identidad emite `rol: OPERADOR` y los reales van en `permisos`). **Nunca se verificó en TEST con un usuario real**: «No cubierto» del reporte | `docs/trabajo/2026-10-21-menu-plegable-y-guardia-sistemas/REPORTE.md` §Causa raíz y §No cubierto |
| Verificación contra la API real | **Ningún trabajo del backoffice desde el 2026-10-17 se verificó contra el backend real**: e2e contra Prism (`packages/simulado`, puerto 4010) y sesión simulada interceptando `/sesion/refrescar` | `apps/backoffice/playwright.config.ts` (webServer: `simulado mock`), reportes del 10-17, 10-18, 10-21 |
| Desde este contenedor | El host de TEST está **denegado por la política de red del entorno** (`CONNECT … 403` a `*.161.97.85.216.sslip.io`). Docker **no está disponible** (no hay daemon), así que el compose de 15 servicios no se puede levantar acá. Sí hay: PostgreSQL 16 server, Redis, Java 21 + Gradle con Maven Central alcanzable, Node 22, Playwright/Chromium. **No hay Kafka** (descarga de apache.org denegada) ni MinIO | `curl`, `docker ps`, `ls /usr/lib/postgresql/16/bin` |

### 3. Qué tiene Atlas y qué ya se portó
| Hecho | Evidencia |
|---|---|
| Atlas: Next.js 15, **127 páginas** bajo `/internal`, 10 grupos de menú (Operaciones, Datos, Gobierno, Sistemas, QA y reportes, Proveedores externos, Administración, Soporte, Mi cuenta, Avisos de pago) | `src/shared/components/layout/internal-shell/nav-groups-*.ts` (clon de solo lectura en `/home/user/pabloarauzcaballero/atlasadminportal`) |
| Inventario completo ya hecho (1128 líneas) y mapa de roles (123 líneas) | `docs/trabajo/2026-10-18-backoffice-como-atlas/evidencia/atlas-inventario.md`, `atlas-roles-y-permisos.md` |
| Del plan del 10-18: H0–H4 y H6 **HECHO** (sistema visual, 20 componentes de `@aportaya/ui`, shell, catálogo de datos, 41 pantallas migradas a la cabecera nueva, usuarios/roles/permisos internos). **TODO**: H2.S1.M4 campana (hecha después como H5.S0), **H5.S1–S14** (bitácora, catálogos operativos, cola de revisión, gobierno de datos/PII, privacidad, procesos automáticos, expedientes, soporte, linaje, esquema/versiones, vistas del negocio, consentimientos, deriva de permisos, glosario/dominios/calidad) y **H7** (regresión, capturas 41×3×2, verificación en vivo) | `docs/trabajo/2026-10-18-backoffice-como-atlas/PLAN.md` (59 HECHO / 18 TODO) |
| «Estándar de centro de datos» y «estándar de centro de decisiones»: **no existe ningún documento con ese nombre** ni en Atlas ni en Pasanaku. Lo más cercano en Atlas: grupo **Datos** (catálogo, revisión, dominios y glosario, definiciones del motor, catálogos operativos, linaje, esquema, calidad de datos) y **Gobierno → Motor de decisiones** («Qué decide cada cosa», «Artefactos del motor», políticas de riesgo) | `grep -ri "centro de datos|decision center|…"` en los tres repos → 0 |

### 4. Cuentas para probar
| Hecho | Evidencia |
|---|---|
| TEST: el operador de plataforma y su credencial viven en el VPS (`/opt/aportaya/credenciales-admin-test.txt`), nunca en el repo. El operador **siempre** pasa por segundo factor; TEST tiene «el segundo factor de desarrollo encendido» | `despliegue/TEST.md:84-85`, `:146`, §«Lo que este entorno NO es» |
| Local: `USR000091` (`+59171000091`, `ADMIN_PLATAFORMA`) se siembra sin contraseña; `CLAVE_DEV=… scripts/clave_dev.py --huella …` le pone una y confía el dispositivo. Requiere la base en el contenedor `aportaya-postgres` (Docker) | `sql/61_dev/15-usuarios-dev.sql:25-40`, `scripts/clave_dev.py` |

## Desconocidos (no se pudieron verificar desde acá)
1. Qué commit está desplegado hoy en TEST (si el temporizador reconstruyó tras el 10-21/10-23). Se ve en `/opt/aportaya/logs/autodespliegue.log` del host.
2. Qué pantallas y botones concretos fallan en TEST y con qué error de consola/red (no hay evidencia en los reportes porque nadie entró con usuario real).
3. Si los 14 e2e que Marcelo cerró en el espejo siguen rojos en Backend (ahí nunca se aplicaron).

## Hipótesis (documentadas, no presentadas como hecho)
- H-a: parte de «todo está mal» es la guardia de Sistemas, ya corregida en `test` pero no vista en vivo.
- H-b: otra parte son pantallas que traen datos sin `ap-estado-de-pantalla` y quedan en blanco mientras cargan o cuando la API devuelve lista vacía.
- H-c: otra parte puede ser el backend (503 de olas que no arrancaron, ver `TEST.md:403-404`): un 503 se ve como «no funciona» aunque el frontend esté bien.

## Respuestas de Pablo (2026-10-24) que fijan el alcance
1. **Merge:** se porta el trabajo de Marcelo a `PasanakuBackend`; en el choque de diseño **gana el acordeón** (las pestañas de sección se descartan).
2. **Acceso a TEST:** operador de plataforma con segundo factor de desarrollo. Las credenciales **no se escriben** en el repo, en el plan ni en el reporte (regla 90.3); se leen de variables de entorno de la sesión. Queda pendiente de Pablo **habilitar el dominio de TEST en la red del entorno** (sigue en `HTTP 000`).
3. **Qué probar:** todo. Cada pantalla, cada botón, contra la API real.
4. y 5. **Atlas:** alcance máximo, lo que haga falta para que el backoffice funcione transversalmente.
6. **Entrega:** PRs en par (`→ dev` y `→ test`), merge de Pablo.

## Orden de ejecución
H0 (sin red) → H1 (necesita la red) → H2 (recorrido real) → H3 y H4 (corregir) → H5 (Atlas, bloque por bloque, cada uno con su backend) → H6 (entrega). Mientras la red de TEST esté cerrada, se avanza con H0, con la parte de H3 que es puramente de código, y con los **contratos de backend** de H5, que se verifican con tests reales contra PostgreSQL local (no Docker: `pg_ctl` de PostgreSQL 16 está en la máquina).

---

## H0 — Última versión absoluta: lo de Marcelo vive en Backend, con el acordeón
**CA:** Dado el `dev` de `PasanakuBackend`, cuando se compara con el `dev` del espejo, entonces todo cambio de Marcelo que no sea las pestañas de sección está en Backend (capa visual de Atlas por tokens, operaciones por caso de uso, DDL generado para `borradores`, comprobación de la matriz en CI), el menú sigue siendo el acordeón, y el espejo queda resincronizado desde Backend.
**DoD:** `yarn typecheck`, `yarn lint`, `test:front` de `tokens`, `ui` y `backoffice`, e2e del backoffice contra Prism, `test_catalogo_de_datos.py`, `./gradlew :servicios:auditoria:test webTest`, `psql -f sql/aplicar.sql` en base vacía ×2; capturas 3 anchos × 2 temas miradas (regla 35); PRs abiertos y mergeables.
**Estado:** EN CURSO

### H0.S1 — Capa visual del portal de Atlas por tokens (`data-producto="backoffice"`)
**CA:** Dado el backoffice en claro y en oscuro, cuando se abre cualquier pantalla, entonces el lienzo es neutro (sin tinte verde), las superficies blancas/grafito y hay un solo acento; la app móvil no cambia.
**DoD:** `yarn workspace @aportaya/tokens build` + `test:front` verdes; `tokens.css` con los tres bloques `:root[data-producto='backoffice']`; capturas miradas.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H0.S1.M1 | Portar `packages/tokens/tokens.json`: rampa `n50…n950`, `tinta`, temas `backofficeClaro` / `backofficeOscuro` (del espejo, `05c0a2ee`) | El JSON tiene los 4 temas y la capa solo redefine roles que ya existen en `claro` (verificado: 0 roles nuevos) | `yarn workspace @aportaya/tokens build` → exit 0 | TODO |
| H0.S1.M2 | Portar `scripts/a-css.mjs` (emite la capa) y `pruebas/roles-contraste.test.mjs` (mide la capa) | `grep -c "data-producto='backoffice'" tokens.css` = 3 | `yarn workspace @aportaya/tokens test:front` → verde | TODO |
| H0.S1.M3 | `index.html` e `index.desarrollo.html` con `data-producto="backoffice"` | Ambos `<html>` llevan el atributo | `grep -c data-producto apps/backoffice/src/index*.html` = 2 | TODO |
| H0.S1.M4 | Rampa de neutros en la bóveda `docs/Views/Sistema-Diseno/estilos.css` (la prueba `tokens-contra-boveda` compara tokens contra ese archivo) | La prueba de bóveda pasa | `yarn workspace @aportaya/tokens test:front` → verde | TODO |
| H0.S1.M5 | Prueba visual: ingreso, tablero, una lista (outbox) y una ficha (catálogo) en 390/820/1440 × claro/oscuro contra Prism; el acordeón se conserva | Ninguna pantalla con tinte verde en el lienzo; menú en acordeón; sin desborde | capturas en `evidencia/H0-S1/` + `doble-revision.md` | TODO |

### H0.S2 — Pestaña «Operaciones» del catálogo: las dos fuentes, no una
**CA:** Dada la ficha de una tabla, cuando se abre «Operaciones», entonces arriba sigue la tabla de Backend (endpoints con su operación directa sobre la tabla, enlace a `/sistemas/operaciones/:id`) y debajo la sección de Marcelo «Casos de uso que la nombran» (CU → endpoints, desde la bóveda y la matriz), y una tabla de infraestructura muestra el vacío que orienta.
**DoD:** generador + auditoria + simulado + backoffice + e2e verdes.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H0.S2.M1 | `scripts/catalogo_de_datos.py`: portar `operaciones_por_tabla` (CU `[[tabla]]` × `matriz.md`) y el campo `operaciones` por tabla; portar su test | 328 tablas de negocio con `operaciones`; infraestructura con `[]` | `python3 -I scripts/test_catalogo_de_datos.py` → OK | TODO |
| H0.S2.M2 | Regenerar `servicios/auditoria/.../catalogo-datos.json`, `docs/catalogo-de-datos.md`, `docs/catalogo-de-flujos/matriz.md`, `docs/auditoria-produccion/endpoints.md`, Postman | `git diff --exit-code` tras una segunda regeneración (idempotente) | `python3 scripts/catalogo_de_datos.py` ×2 | TODO |
| H0.S2.M3 | `auditoria.yaml`: `TablaDelCatalogo.operaciones[]` (aditivo) + clientes Angular/Dart regenerados + tests de auditoria | Contrato aditivo; tests verdes | `./gradlew :servicios:auditoria:test :servicios:auditoria:webTest` → BUILD SUCCESSFUL | TODO |
| H0.S2.M4 | Simulado: ejemplos con `operaciones` (vacío y con casos) | Prism sirve la ficha con el campo | `yarn workspace @aportaya/simulado test:front` → verde | TODO |
| H0.S2.M5 | `vista-operaciones.ts`: conservar la tabla de Backend y agregar la sección de casos de uso (textos de Marcelo); spec unitario de las dos partes | Spec: con CU muestra la lista; sin CU muestra el vacío que orienta | `npx ng test --include='src/app/rutas/tablero/catalogo/**'` → verde | TODO |
| H0.S2.M6 | e2e `catalogo-gobierno`: la prueba de Marcelo («Operaciones» lista CU-21 con `cobrarAporte`) adaptada a la vista combinada | PASS con captura | `yarn test:e2e catalogo-gobierno` → PASS | TODO |

### H0.S3 — `identidad.borrador_onboarding` generado, no escrito a mano (regla 97.1.3)
| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H0.S3.M1 | Portar `escribir_infra_borradores()` a `scripts/generar_ddl.py` de Backend (lee el schema `BorradorOnboarding` del contrato) y regenerar `sql/15_infra/borradores.sql` con sus `COMMENT` | El archivo generado reemplaza al manual y conserva columnas y CHECKs | `python3 scripts/generar_ddl.py` → exit 0; `git diff --stat sql/15_infra/borradores.sql` | TODO |
| H0.S3.M2 | Aplicar el esquema completo en PostgreSQL 16 local vacío, dos veces | exit 0 ×2, 0 líneas `ERROR` | `pg_ctl` + `psql -v ON_ERROR_STOP=1 -f sql/aplicar.sql` | TODO |
| H0.S3.M3 | `test_catalogo_de_datos.py` y `ConsultarCatalogoDeDatosTest` siguen verdes con la tabla catalogada | verde | ver H0.S2 | TODO |

### H0.S4 — Pequeños portes
| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H0.S4.M1 | `ci.yml`: comprobación de que `matriz_de_pruebas.py` no cambia `matriz.md` (6i de Marcelo) | El paso existe junto al de `test_catalogo` | `python3 scripts/matriz_de_pruebas.py && git diff --exit-code -- docs/catalogo-de-flujos/matriz.md` | TODO |
| H0.S4.M2 | Tabla virtualizada: `title` en celdas recortadas (de `7b07fcb7`); la tipografía de cabecera ya la fijó Backend | Celda con texto cortado tiene `title` | spec + `npx ng test --include='src/app/nucleo/tabla/**'` | TODO |

### H0.S5 — Descartes declarados (no se portan)
| Qué | Por qué | Quién |
|---|---|---|
| `layout/pestanas-de-seccion.{ts,spec.ts}`, cambios de `menu-lateral.*`, `shell-financiero.*` y `shell.e2e.ts` de Marcelo | Pablo: gana el acordeón de Backend (`3d4344e3`) | Pablo, 2026-10-24 |
| `verificar_narrativas.py` `PISO_COLUMNAS = 566` | Backend ya está en 3734 (trinquete: solo sube) | Claude |
| `docs/trabajo/2026-10-21-catalogo-de-datos-y-rediseno/*` de Marcelo | Backend ya tiene la versión portada con `evidencia/H7-port-backend.txt` | Claude |
| `e802caa4` (capa de bloqueo de la app móvil) | Fuera de alcance (móvil); Backend tiene su propio arreglo `9d2d1629`. **Anotado** para quien lleve la app | Claude |
| 25 commits de dependabot del espejo | Backend recibe los suyos por su propio dependabot | Claude |

### H0.S6 — Regresión de H0 y PRs
| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H0.S6.M1 | Regresión: typecheck, lint, `test:front` (tokens, ui, backoffice), e2e backoffice completo contra Prism | todo verde (línea base previa guardada en `evidencia/H0-linea-base.txt`) | salidas en `evidencia/H0-regresion.txt` | TODO |
| H0.S6.M2 | PRs `→ dev` y `→ test` en `PasanakuBackend`, mergeables | `mergeable=MERGEABLE`, checks verdes | `pull_request_read get` + check runs en `evidencia/` | TODO |
| H0.S6.M3 | Espejo `PasanakuFrontend`: rama `claude/keen-cray-d0wpgf` = merge de `Backend/test` sobre `Frontend/dev` conservando las dos partes (los 28 conflictos se resuelven a favor de lo ya portado); PR `→ dev` del espejo | `git log Backend/test..Frontend/rama` sin commits de código pendientes | PR mergeable | TODO |

---

## H1 — Acceso a la API real de TEST (lo que destraba todo lo demás)
**CA:** Dado este entorno, cuando se llama a `GET https://api.aportaya…/api/v1/sesiones` sin token, entonces responde `401` (no `000`), y con el operador y su segundo factor se abre sesión real.
**Estado:** BLOQUEADO — depende de Pablo (red del entorno).

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Pablo habilita `*.161.97.85.216.sslip.io` en la red del entorno (Edit → Network access → Allowed domains) | `curl` devuelve 401 | `curl -sS -o /dev/null -w '%{http_code}' https://api.aportaya…/api/v1/sesiones` → 401 | BLOQUEADO (Pablo) |
| H1.S1.M2 | Sesión real del operador: `POST /sesiones` → `AP-CU04-03` → segundo factor → cookie; con credenciales desde variables de entorno | 200 y `GET /sesion` devuelve permisos | script en `scripts`/scratch, salida enmascarada en `evidencia/H1-sesion.txt` | TODO |
| H1.S1.M3 | Backoffice local (`ng serve`) con `proxy.test.json` → gateway de TEST, y Playwright contra el **backoffice desplegado** (es la versión que Pablo ve) | Las dos vías cargan el tablero con datos reales | captura del tablero con la cabecera de red | TODO |
| H1.S1.M4 | Leer qué commit está desplegado (`/opt/aportaya/logs/autodespliegue.log` del host, o la versión servida) y anotarlo | Se sabe si `2157ff50`/`3d4344e3` están en TEST | `evidencia/H1-commit-desplegado.txt` | TODO |

## H2 — Recorrido total de TEST: cada pantalla, cada botón, con la API real
**CA:** Dadas las 60 rutas hoja del backoffice, cuando el operador las recorre, entonces cada una tiene una fila en la matriz con: carga (esqueleto visto), datos/vacío (cuál y si orienta), error (si aplica), y **cada acción** con `UI → request → response → persistencia → recarga → UI`, consola y red sin errores.
**DoD:** `evidencia/H2-recorrido/matriz.md` completa, una captura por pantalla y estado, trazas de Playwright de cada acción; cada fallo clasificado (`PRODUCT_BUG` front/back, `DATA`, `ENVIRONMENT`) con su reproducción.
**Estado:** TODO (depende de H1)

| ID | Subtarea | Rutas | Estado |
|---|---|---|---|
| H2.S0 | Script Playwright de recorrido: inicia sesión real, visita las 60 rutas, captura `loading` (bloqueando la respuesta hasta ver el esqueleto), `resolved`, consola y red; genera la matriz | — | TODO |
| H2.S1 | Ingreso y arranque: `/ingreso`, `/arranque`, restaurar sesión, salir | 2 | TODO |
| H2.S2 | Tablero: inicio, financiero, sistemas, comercial, catálogo de datos (lista, ficha ×5 pestañas, ficha de gobierno, revisar columna) | 8 | TODO |
| H2.S3 | Operación: bandejas, billetera, reclamos, solicitudes escaladas, retiros (+ aprobación), custodia/conciliación, cierres, reversos, desembolsos, estado | 11 | TODO |
| H2.S4 | Cumplimiento: verificaciones, alertas (+ ficha), casos (+ ficha), UIF reportes y requerimientos, continuidad, actas (+ ficha), eventos de riesgo, incidentes de seguridad, reclamos ficha, habilitación de organizador, expedientes, colas | 15 | TODO |
| H2.S5 | Admisiones: bandeja y ficha (CU-68) | 2 | TODO |
| H2.S6 | Sistemas: inicio, servicios, despliegues, base de datos, respaldos, proveedores, outbox, webhooks, accesos, incidentes, bitácora, catálogos operativos (+ detalle), operaciones (+ detalle), flujos, usuarios internos (+ detalle), roles, permisos | 19 | TODO |
| H2.S7 | Contabilidad: inicio, periodo, presupuesto, compras, cobros, activos, estados financieros | 7 | TODO |
| H2.S8 | Publicidad: anunciantes, campañas (+ aprobación), moderación, desempeño, liquidación | 6 | TODO |
| H2.S9 | Configuración: inicio, tarifario, mensajería, reportes · Ayuda: centro de tutoriales | 5 | TODO |
| H2.S10 | Transversales: buscador global (⌘K), campana de avisos, menú de usuario, acordeón (estado recordado), migas, modo oscuro, 390/820/1440 | — | TODO |

Cada subtarea se desglosa en una microtarea **por acción** (botón, formulario, filtro, paginación, exportación) al abrirla, con el ID de la fila de la matriz como DoD.

## H3 — Los cuatro estados en todas las pantallas (regla 95.2)
**CA:** Dada cualquier pantalla que depende de red, cuando la respuesta tarda, viene vacía o falla, entonces se ve el esqueleto, el vacío que orienta (por qué y qué hacer) o el error con traza y «Reintentar»; al revalidar se ve «Actualizando…».
**DoD:** por pantalla, spec de los 4 estados + captura de cada uno; `grep` de que toda pantalla con datos pasa por `ap-estado-de-pantalla` o declara por escrito por qué no.
**Estado:** TODO (la parte de código no depende de la red; la verificación final sí)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Inventario: para los 24 dominios que traen datos sin `ap-estado-de-pantalla`, qué pantalla los consume y cómo resuelve hoy cada estado | tabla en `evidencia/H3-inventario-estados.md` | revisión | TODO |
| H3.S1.M2 | Un helper único para listas por cursor (`bandeja-por-cursor`, `paginas.ts`, `listado.ts`) que exponga un `ResourceRef` y los estados | Las bandejas de operación y cumplimiento usan `ap-estado-de-pantalla` | specs + `test:front` | TODO |
| H3.S2.M1–Mn | Una microtarea por pantalla del inventario (se enumeran al cerrar M1), cada una: migrar + spec de 4 estados + captura | — | `npx ng test --include=<pantalla>` + captura | TODO |
| H3.S3.M1 | Textos de vacío revisados: todos dicen por qué y qué hacer (prohibido el vacío mudo) | `grep` de `mensajeVacio` sin `explicacion` → 0 | revisión + spec | TODO |
| H3.S3.M2 | Regla automática en `scripts/verificar_frontend.py`: pantalla con `resource(`/`HttpClient` y sin `ap-estado-de-pantalla` → falla el lint, salvo lista blanca justificada | el lint lo detecta en una pantalla de prueba | `yarn workspace @aportaya/backoffice lint` | TODO |

## H4 — Corregir lo que H2 encuentre
Se desglosa al cerrar H2. Cada fallo clasificado `PRODUCT_BUG` se convierte en microtarea con reproducción (test o e2e que falla antes y pasa después), en orden: lo que rompe un flujo de dinero o de cumplimiento primero, lo visual al final.

## H5 — El estándar de Atlas: datos, decisiones y operación, bloque por bloque y con backend real
**Principio:** ningún bloque se entrega como fachada. Si no hay contrato, primero el contrato (`.puml` → DDL → servicio → OpenAPI → cliente), después la pantalla. Lo que Atlas tiene y el pasanaku no necesita (préstamos, cartera, comercios, productos de crédito, laboratorio QA de dispositivos) **no se construye**: se declara acá.
**Estado:** TODO. Orden propuesto (de lo que ya tiene backend a lo que hay que modelar):

| Bloque Atlas | Backend hoy (paths OpenAPI encontrados) | Qué falta | Subtarea |
|---|---|---|---|
| **Datos · Registro del sistema (bitácora)** | `auditoria`: `/auditoria/bitacora`, `/{id}` | pantalla `sistemas/bitacora` con cursor, filtros (entidad, acción, actor, rango) y cajón de detalle con antes/después | H5.S1 |
| **Datos · Catálogos operativos** | `auditoria`: `/auditoria/catalogos`, `/{codigo}`, `/elementos`, `/elementos/{id}` | lista + ficha + conmutador activo/inactivo con bitácora (hoy la pantalla existe: verificar que escribe de verdad) | H5.S2 |
| **Datos · Revisión del catálogo (cola)** | CU-130/131 (`gobierno`, `revision`) | bandeja de pendientes con «Revisar» y contadores | H5.S3 |
| **Datos · Gobierno de datos y PII** | catálogo (clasificación PII), políticas de retención por tabla | dos vistas de solo lectura + exportación | H5.S4 |
| **Gobierno · Solicitudes de privacidad** | `auditoria`: `/auditoria/derechos` (CU-07) | bandeja + ficha + resolver con motivo | H5.S5 |
| **Datos · Linaje e impacto** | relaciones del catálogo (686 FKs) | grafo por tabla (hacia/desde) e impacto de una columna | H5.S9 |
| **Datos · Esquema: versiones e historial** | `esquema` (3 paths) + hash de `aplicar.sql` | versión desplegada vs. repo, cambios por commit | H5.S10 |
| **Datos · Dominios y glosario · Calidad de datos** | nada (0 paths) | modelo `glosario_termino`, `dominio_de_datos`, `regla_de_calidad`, `incidencia_de_calidad` → `auditoria`; pantallas lista/ficha | H5.S14 |
| **Decisiones · «Qué decide cada cosa» / Artefactos del motor / Políticas de riesgo** | `reglas` (2), `decision` (4) dispersos en identidad/cumplimiento; no hay un motor único | inventario de decisiones automáticas del sistema (KYC, límites, mora, sorteo, aprobación de retiro) con su regla, versión, umbral y dónde vive; pantalla de solo lectura primero; edición versionada después (skill `motor-de-reglas`) | H5.S15 |
| **Operación · Procesos automáticos** | `organizador`: `tareas` (3, CU-96) | lista + ficha + reintentar con idempotencia | H5.S6 |
| **Operación · Expedientes (archivos)** | `cumplimiento`/`identidad` KYC (CU-02/05) | expediente por persona + registro de lectura (R-SEG-02) | H5.S7 |
| **Soporte · Casos y base de conocimiento** | `ticket_soporte` (CU-52) + `ayuda` | bandeja + ficha + respuesta; la ayuda pasa a «Base de conocimiento» | H5.S8 |
| **QA y reportes · Vistas del negocio · Reportes** | `reportes` (9 paths, CU-58) + `transparencia` | consultas gobernadas con huella; reportes con ejecución y descarga reales | H5.S11 |
| **Gobierno · Consentimientos / Textos legales / Contenido de la app** | `identidad` `consentimiento` (0 paths expuestos) | contrato de versiones + aceptaciones; pantalla | H5.S12 |
| **Sistemas · Deriva de permisos** | backoffice local + `permisos` (1) | declarados vs. usados vs. asignados | H5.S13 |
| **Sistemas · Eventos de dominio · Jobs · Herramientas · Salud de red** | `eventos` (3), outbox, webhooks, incidentes (3) | completar lo que falte sobre lo que ya existe en `sistemas/` | H5.S16 |
| **Administración · Sesiones internas** | `sesiones` (3) | lista de sesiones activas + revocar | H5.S17 |
| **Proveedores externos** | `proveedores` (1) | catálogo y salud; auditorías y solicitudes si el backend las registra | H5.S18 |
| **Mi cuenta · Perfil · Seguridad de la sesión** | identidad | perfil del operador, cambio de contraseña, factores | H5.S19 |
| **Notificaciones · Campañas** | `notificaciones` `avisos` (3) + CU-80/83 | campañas con segmentación real o se declara OUT | H5.S20 |

Cada H5.Sn se abre con sus microtareas (modelo → DDL → servicio+tests → OpenAPI+clientes → simulado → pantalla+specs → e2e → captura) cuando le toque; no antes (regla 20.6.6: nada «de paso»).

## H6 — Entrega
| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H6.S1.M1 | Regresión completa (frontend + servicios tocados) | salidas en `evidencia/` | TODO |
| H6.S1.M2 | Capturas de todas las rutas × 3 anchos × 2 temas, doble revisión (regla 35) | `evidencia/doble-revision.md` | TODO |
| H6.S1.M3 | PRs en par, mergeables; tras el despliegue del temporizador, **recorrido en vivo en TEST** con el operador real | kill-test PASS en vivo | TODO |
| H6.S1.M4 | `REPORTE.md` con avance calculado | regla 40 | TODO |


## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Seguir verificando contra Prism | Repetir el ciclo «verde acá, roto en TEST» | H1 antes que nada; si TEST no se habilita, levantar el backend real acá sin Docker (PostgreSQL+Redis locales, Kafka desde Maven Central) y declararlo como doble parcial |
| Merge del espejo con 28 conflictos | Perder trabajo de alguno de los dos lados | Resolver conservando las dos partes; `-X ours/theirs` prohibido |
| 15 servicios y 4 CPU | Construcción local lenta (30–60 min) | Construir solo los que el backoffice necesita (identidad, auditoria, nucleo-financiero, cumplimiento, grupos, transparencia, tarifas, notificaciones, organizador, publicidad, erp) y el gateway |
