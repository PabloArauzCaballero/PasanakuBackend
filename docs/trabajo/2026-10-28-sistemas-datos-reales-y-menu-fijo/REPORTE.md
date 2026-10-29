# Reporte — Sistemas con datos reales y menú lateral fijo

> **AVANCE: 6 / 6 — 100 %.** Frontend `TESTED` + prueba visual contra un doble declarado. **Falta verificarlo en TEST con una sesión real** (desplegar el backoffice e ingresar con un `ADMIN_PLATAFORMA`). 7 de las 9 pestañas de Sistemas siguen sin backend: ver Pendiente.

- Fecha: 2026-10-28 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/sistemas-datos-reales-y-menu-fijo-20261028` sobre `origin/test` (`ec4e9ad0`)
- Peldaño alcanzado: `TESTED` (tests dirigidos por HTTP + regresión 1345/1345 + build) y prueba visual en 3 viewports y 2 temas **contra un doble** del backend. No es `VERIFIED`: no hubo sesión real contra TEST.

## Causa raíz de «todo sale vacío»
Las 9 pantallas de Sistemas leían un puerto que en producción resuelve `dominio/adaptador-fuente-no-disponible.ts`, que **siempre rechaza** («fuente no disponible»). Ninguna llamaba al backend. Para dos de ellas el backend ya existía en `auditoria` y nadie lo había conectado:
- `GET /indicadores/operacion/servicios` (`consultarSaludDeServicios`): readiness en vivo de los 14 servicios.
- `GET /indicadores/catalogo-de-datos/resumen` (`obtenerResumenDelCatalogo`): los esquemas de la base con su dueño y sus cifras.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Servicios y SLO pinta la salud real que contesta auditoria (salud, latencia, nivel, réplicas, hora de consulta) | `npx ng test --include='src/app/rutas/sistemas/servicios/*.spec.ts'` | PASS 6/6 · `evidencia/H1.S1.M1-servicios.txt` |
| H1.S1.M2 | Base de datos pinta cada esquema real con servicio dueño, conexión ahora, tablas, columnas, relaciones y datos personales; cada esquema enlaza al Catálogo de datos | `npx ng test --include=…/base-datos/*.spec.ts …` | PASS · `evidencia/H1.S1.M2-base-datos.txt` |
| H1.S1.M3 | La metadata real se encuentra: «Endpoints y flujos» (Operaciones del sistema = ficha de cada endpoint; Mapa de flujos) en Sistemas; Catálogo de datos visible en el menú y enlazado desde Base de datos | `npx ng test --include='src/app/nucleo/*.spec.ts' …` | PASS · `evidencia/H2.S1.M1-menu.txt` |
| H2.S1.M1 | Menú fijo: ningún grupo se pliega, cada sección lista todas sus pantallas con ícono (con subgrupos), la barra se desplaza, sin entrada «Inicio» (el logo AportaYa lleva al inicio) | `npx ng test --include='src/app/layout/*.spec.ts'` | PASS · `evidencia/H2.S1.M1-menu.txt` |
| H2.S1.M2 | Regresión completa del backoffice y build de producción | `npx ng test --include='src/**/*.spec.ts'` · `npx ng build` | PASS 1345/1345, 221 archivos · build OK · `evidencia/H2.S1.M2-regresion.txt`, `H2.S1.M2-build.txt` |
| H2.S1.M3 | Prueba visual mirada: escritorio claro y oscuro, tablet 820, celular 390 (pantalla y cajón), menú desplazado hasta abajo; consola con 0 errores y 0 avisos; sin desborde horizontal en el celular | Playwright contra `ng serve` + doble | PASS · `evidencia/*.png` |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Verificación en TEST con sesión real | TODO | Desplegar este backoffice en TEST e ingresar con un usuario `ADMIN_PLATAFORMA`. Hoy está demostrado que los dos endpoints existen en TEST y exigen sesión (401 sin token, `evidencia/H1-contrato-en-TEST.txt`) y que `ADMIN_PLATAFORMA` tiene `TABLERO_SISTEMAS_VER` (`sql/60_semillas/10-roles-y-permisos.sql:158`). |
| Despliegues, Respaldos, Proveedores, Outbox, Webhooks, Revisión de accesos, Incidentes | TODO (fuera de alcance) | No existe contrato en ningún servicio (`entregables/brecha-observabilidad.md`). Cada una necesita un endpoint nuevo; siguen mostrando «fuente no disponible». Hay que priorizar cuál se hace primero. |

## Evidencia
```text
$ curl -s -o /dev/null -w '%{http_code}' https://api.aportaya.161.97.85.216.sslip.io/api/v1/indicadores/operacion/servicios
401
$ curl -s -o /dev/null -w '%{http_code}' https://api.aportaya.161.97.85.216.sslip.io/api/v1/indicadores/catalogo-de-datos/resumen
401

$ npx ng test --include='src/**/*.spec.ts'
 Test Files  221 passed (221)
      Tests  1345 passed (1345)

$ npx ng build
▲ [WARNING] bundle initial exceeded maximum budget. Budget 500.00 kB was not met by 2.23 kB with a total of 502.23 kB.
Output location: …/apps/backoffice/dist/backoffice
```
Capturas en `evidencia/`: `escritorio-base-de-datos-{claro,oscuro}.png`, `escritorio-servicios-{claro,oscuro}.png`, `escritorio-menu-abajo.png`, `tablet-base-de-datos.png`, `celular-base-de-datos.png`, `celular-menu-abierto.png`, `celular-menu-abajo.png`. Los números de las capturas son **sintéticos** (los devuelve el doble).

## No cubierto
- Datos reales en el navegador: la sesión (`/sesion/refrescar`) y las dos respuestas de auditoria se simularon en Playwright con la forma del contrato (doble declarado, regla 65). Niveles cubiertos contra el doble en specs: correcto, límite (sin servicios / sin salud / sin esquemas) e inválido (500, 403).
- Usuario con solo `RESPONSABLE_SEGURIDAD`: no se comprobó si tiene `TABLERO_SISTEMAS_VER` ni `AUDITORIA_LEER`. Si no los tiene, verá el error 403 en estas dos pantallas, no datos.
- E2E Playwright de la suite (`yarn test:e2e`): no se corrió.

## Desvíos del plan
- Operaciones del sistema y Mapa de flujos van en el subgrupo «Endpoints y flujos» para respetar el tope de 8 pestañas por sección de A1.
- Además de lo planeado se tocaron: `layout/shell-financiero.ts` (nombre accesible y tooltip «Ir al inicio» en el logo), `sistemas.routes.ts` (título «Base de datos») y los specs que afirmaban el menú viejo: `nucleo/menu.spec.ts`, `layout/shell-financiero.spec.ts`, `layout/pestanas-de-seccion.spec.ts`. En `compartido/vistas-de-puerto.a11y.spec.ts` salió el caso de Base de datos, que ya no es una vista de puerto; su accesibilidad la cubre ahora `base-datos/pantalla-base-datos.spec.ts` contra HTTP.
- Dos aserciones de `servicios/pantalla-servicios.spec.ts` buscaban «arriba» en toda la página y fallaron cuando el propósito pasó a decir «está arriba ahora» (`TEST_BUG`). Ahora miran los chips de salud. La exigencia no bajó: en el caso límite todos los chips tienen que decir «sin dato».
- Entorno: el worktree nuevo no traía artefactos generados (gitignorados). Se generaron con `yarn workspace @aportaya/tokens build`, `./gradlew generarClienteAngular` y `yarn workspace @aportaya/simulado contratos`. Sin el último, `nucleo/entrada-estricta.spec.ts` falla 10/10 (`ENOENT packages/simulado/generado/*.json`), también en el árbol limpio de `test`: `ENVIRONMENT`, no producto.

## Riesgos residuales
- `PUERTO_SERVICIOS` y `PUERTO_BASE_DE_DATOS` siguen declarados y provistos (también en modo demo), pero ninguna pantalla los usa: es deuda. Retirarlos toca el paquete simulado y sus specs; queda anotado.
- Presupuesto del bundle inicial: ya se pasaba por 924 B en `test`; ahora por 2,23 kB (el menú expandido está en el bundle inicial). Es un aviso, no un error.
- La copia principal `/Users/pablo/Documents/GitHub/Pasanaku` (rama `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18`, muy atrás de `test`) tiene cambios sin commitear de otra sesión (el menú en acordeón, entre otros). **No se tocaron.** Ese acordeón es justo lo contrario de lo pedido; no hay que subirlo.

## Decisiones y ambigüedades (confirmar con Pablo)
1. **«Base y migraciones» sin migraciones.** El esquema se genera desde el modelo (`sql/aplicar.sql`); no hay migraciones versionadas que listar. La pantalla pasó a «Base de datos»: esquemas, dueño, cifras y conexión ahora.
2. **«Todo el detalle, no desplegable, scrolleable, con ícono, lo mismo para lo demás».** Se interpretó como: ningún grupo se pliega y debajo de cada sección se ven todas sus pantallas, también las de los subgrupos. Las pestañas de sección arriba de cada pantalla se mantienen.
3. **«En lugar de Inicio muestre Operación».** Se quitó la entrada «Inicio» y el menú arranca en Operación. Al inicio se vuelve con el logo, que ya era enlace. Las migas siguen diciendo «Inicio».
4. **Disponibilidad de 30 días y presupuesto de error** no se muestran en Servicios y SLO: ningún contrato los publica.
