# Carril F4 · índice de evidencia (cuarta vuelta)

Peldaño por área: web y backoffice **TESTED** (unitarias, a11y, e2e con doble) y **VERIFIED_FUNCTIONAL_ONLY** en lo que corre contra
Prism o el doble con estado; ninguna corrió contra el backend real. Flutter: **WRITTEN** (no ejecutado).

| Archivo | Qué es |
|---|---|
| `1-generarClientes.log` | `./gradlew generarClientes`: BUILD SUCCESSFUL, 39 tareas up-to-date (los clientes ya reflejaban los YAML vigentes) |
| `a-simulado.log`, `b-simulado.log` | contratos-a-json + ejemplos + inyectar + tsc + eslint + vitest (258 pruebas, 4 archivos) |
| `b-backoffice.log` → `c-backoffice.log` | tipos, `ng lint`, gate de la casa, 457 pruebas + 47 a11y (`c-` es la corrida final) |
| `b-web.log` → `d-web.log` | tipos, lint, gate, 74 pruebas + 8 a11y (`d-` es la final) |
| `c-e2e-backoffice.log` | Playwright del backoffice (Prism + `ng serve`, 1 worker, 0 reintentos): 105 pasan |
| `c-e2e-web.log` | Playwright del sitio sobre `ng serve`: 28 pasan, 1 falla por ENTORNO (necesita el build SSR + Prism) |
| `d-build-web.log`, `d-e2e-web-ssr.log` | `ng build` del sitio (falló antes del arreglo de `app.routes.server.ts`; ahora RC=0) y e2e de verificación pública/SEO sobre el SSR: 8 pasan |
| `d-build-backoffice.log` | `ng build` del backoffice: RC=0, con aviso de presupuesto del bundle inicial (+9,66 kB) |
| `aceptacion-mock/` | suite de dos usuarios en modo mock, **axe estricto**: 21 pasan (1 «DESEADO» esperado), 166 capturas (3 viewports × 2 temas), 0 violaciones, 0 desbordes |
| `2-rutas-del-frontend-vs-contratos.md` | inventario de rutas del frontend contra los OpenAPI: 50 usadas, 31 con contrato, 19 sin contrato (todas nombradas en `scripts/verificar_rutas_frontend.py`) |
| `E-H1-03-ADR-051-propuesta-garantia-comision-autogestion.md` | ADR propuesto con antes/después, consecuencias y lo ABIERTO |
| `entrega-simulada-corrida/`, `e-entrega-simulada.log` | corrida del propio `scripts/entrega_simulada.sh frontend` (sin `yarn install`) |
| `capturas-e2e/` | capturas de los e2e del backoffice y del sitio (las corridas `f4_visual`) |

## Clasificación de los rojos hallados (regla 80.4)

| Prueba | Clase | Causa | Acción |
|---|---|---|---|
| `matriz-de-roles` (2 rojos pendientes de catálogo) | DATA/producto | `RECLAMO_VER` y `SOLICITUD_INGRESO_*` ya están sembrados; la lista de «pendientes» y `ver:operacion` no los reflejaban | `secciones.ts` y la matriz alineadas; lista vacía; prueba nueva de quién los recibe |
| `f2-posiciones-y-roles` · ADMIN_PLATAFORMA | TEST_BUG derivado | esperaba el catálogo anterior: ADMIN recibe `RECLAMO_VER` y `SOLICITUD_INGRESO_*` y ve «Operación» | expectativa actualizada con la razón; `prohibida` pasa a `/contabilidad` |
| `caso-de-cumplimiento` · consola sin errores | TEST_BUG heredado | tras la integración del arranque con sesión restaurada, `POST /sesion/refrescar` da 404 en Prism y ensucia la consola | `prepararArranque` (401 limpio) y el 401 esperado se descuenta, no se ignora |
| `sistemas-sin-contrato` · sin sesión → `/ingreso` | TEST_BUG heredado | el arranque pasaba por `/arranque` sin el doble del refresco | `prepararArranque` + regex con `?volverA=` |
| `web-verificacion` en `ng serve` | ENVIRONMENT | necesita el build SSR y Prism; la config por defecto intenta `@scalar/mock-server`, que no está instalado | corrida con build + Prism propios: pasa (`d-e2e-web-ssr.log`) |
| `ng build` del sitio | PRODUCT_BUG | la ruta `invitacion/:grupoId/:invitacionId` caía en el prerender de `**` sin `getPrerenderParams` | entrada `RenderMode.Client` + `noindex` en `app.routes.server.ts` y prueba |

## Hallazgos que NO son de cliente (a quién)

- **erp**: `pagarFacturaDeProveedor` exige `autorizadoPor` (R-CTB-05) en el cuerpo y el cliente no lo manda por decisión de seguridad.
  Contra el backend real es 400. Arreglo de fondo: derivarlo del token y quitar el campo del contrato. Fijado en
  `apps/backoffice/src/app/nucleo/entrada-estricta.spec.ts`. Dueño: erp + seguridad.
- **grupos**: `AP-CU68-06/08/09/10` se reutilizan con significados distintos según la operación (resolver, sustituir, decidir); el texto del cliente
  cubre ambos sentidos. Dueño: grupos.
- **Contratos faltantes**: 19 rutas que el frontend llama y ningún OpenAPI publica (ver el inventario).
- **Ejemplo del mock**: `aprobarRetiro.rechazo` respondía 404 con `AP-CU11-10`; el contrato lo declara 422 (corregido).
