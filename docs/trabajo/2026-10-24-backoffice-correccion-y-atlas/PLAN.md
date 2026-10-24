# Plan — Backoffice: corregir lo roto en TEST y llevarlo al estándar del portal interno de Atlas

- Fecha: 2026-10-24 · Repos afectados: `PasanakuBackend` (fuente de TEST) y `PasanakuFrontend` (espejo) · Predecesores: `2026-10-18-backoffice-como-atlas` (H5.S1–S14 y H7 en TODO), `2026-10-21-catalogo-de-datos-y-rediseno` (solo en el espejo), `2026-10-21-menu-plegable-y-guardia-sistemas`, `2026-10-21-pulido-cargas-y-barra-clara`
- Estado del plan: **Fase 0 y 1 (encuadre y descubrimiento)**. Las microtareas (fase 2) se escriben cuando Pablo responda las ambigüedades de abajo: varias cambian el alcance de forma material.
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

## Hitos previstos (esqueleto — se desglosan en microtareas al cerrar las preguntas)
- **H0 · Última versión absoluta.** Backend `claude/keen-cray-d0wpgf` sobre `test` (hecho). Decidir el destino del trabajo de Marcelo en el espejo (pregunta 1) y resincronizar el espejo desde Backend.
- **H1 · Acceso a la API real.** Red del entorno + credencial del operador de TEST + segundo factor (pregunta 2). Sin esto, todo lo demás queda `VERIFIED` solo contra dobles, que es exactamente lo que ya pasó cuatro veces.
- **H2 · Recorrido total de TEST, botón por botón.** Matriz pantalla × acción × resultado (UI → request → response → persistencia → recarga → UI), con captura y consola. Clasificar cada falla (`PRODUCT_BUG` front / back, `DATA`, `ENVIRONMENT`).
- **H3 · Estados obligatorios en las 52 pantallas.** Toda pantalla que trae datos pasa por `ap-estado-de-pantalla` con `[vacio]`; vacío que orienta; error con traza y reintentar; «Actualizando…» al revalidar. Prueba visual 3 anchos × 2 temas × 4 estados.
- **H4 · Corregir lo que H2 encontró**, en orden de impacto.
- **H5 · Estándar de datos y de decisiones (Atlas).** Retomar H5.S1–S14 del plan del 10-18 en el orden que fije la pregunta 4, cada uno con su backend real (nada de fachadas).
- **H6 · Regresión, capturas, PRs a `dev` y `test`, verificación en vivo tras el despliegue.**

## Preguntas abiertas (bloquean la fase 2)
Ver el mensaje de cierre de la sesión del 2026-10-24; se copian acá para que sobrevivan:
1. **Fuente de verdad y merge.** TEST sale de `PasanakuBackend/test`. El espejo `PasanakuFrontend` tiene 5 commits de Marcelo que Backend no tiene y chocan en diseño (pestañas de sección vs. acordeón). ¿Se portan a Backend resolviendo el choque (¿cuál diseño gana?) o se descarta ese trabajo y el espejo se resincroniza desde Backend como el 10-15?
2. **Acceso a TEST.** (a) Permitir `*.161.97.85.216.sslip.io` en la red del entorno; (b) credencial del operador de plataforma (por secreto del entorno, no por chat); (c) cómo obtener el código del segundo factor del operador en TEST.
3. **Qué está roto, con ejemplos.** Tres pantallas/botones concretos con lo que pasó, y si fue antes o después del 2026-10-23.
4. **«Estándar de centro de datos» y «de centro de decisiones».** ¿Son los grupos **Datos** y **Gobierno → Motor de decisiones** de Atlas, u otro documento? ¿Qué se prioriza de H5.S1–S14?
5. **Alcance Atlas.** ¿Solo lo que tiene equivalente en el pasanaku (los 14 bloques de H5) o también préstamos/cartera/comercios/QA lab?
6. **Entrega.** ¿Se mantiene la práctica de abrir el par de PRs (→ `dev` y → `test`) y el merge lo hace Pablo?

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Seguir verificando contra Prism | Repetir el ciclo «verde acá, roto en TEST» | H1 antes que nada; si TEST no se habilita, levantar el backend real acá sin Docker (PostgreSQL+Redis locales, Kafka desde Maven Central) y declararlo como doble parcial |
| Merge del espejo con 28 conflictos | Perder trabajo de alguno de los dos lados | Resolver conservando las dos partes; `-X ours/theirs` prohibido |
| 15 servicios y 4 CPU | Construcción local lenta (30–60 min) | Construir solo los que el backoffice necesita (identidad, auditoria, nucleo-financiero, cumplimiento, grupos, transparencia, tarifas, notificaciones, organizador, publicidad, erp) y el gateway |
