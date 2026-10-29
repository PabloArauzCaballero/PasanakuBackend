# Plan — Sistemas con datos reales y menú lateral fijo

- Fecha: 2026-10-28 · Repos afectados: Pasanaku (solo `apps/backoffice`) · Rama: `claude/sistemas-datos-reales-y-menu-fijo-20261028` sobre `origin/test` (`ec4e9ad0`)
- Predecesor: `2026-10-21-menu-plegable-y-guardia-sistemas` (acordeón) y A1 `2026-10-26-backoffice-listados-y-navegacion` (recorte del menú)
- Resultado observable: una persona con rol de sistemas abre **Sistemas › Servicios y SLO** y **Sistemas › Base de datos** y ve datos reales que contesta `auditoria`, no el estado vacío. Encuentra en el menú, con su ícono, **Operaciones del sistema** (metadata de cada endpoint real), **Mapa de flujos** y **Catálogo de datos** (metadata de la base). El menú lateral no se pliega: cada sección muestra todas sus pantallas con ícono, el panel se desplaza hacia abajo, el logo AportaYa lleva al inicio y la lista arranca en «Operación».
- Kill-test: abrir `/sistemas/base-de-datos` y ver «todavía no hay datos para mostrar» → NO está hecho.

## Hallazgos (descubrimiento)
- Las 9 pantallas de Sistemas leen un puerto que en producción resuelve `adaptador-fuente-no-disponible.ts` (siempre rechaza). Por eso todas salen vacías (`dominio/proveedor-fuentes.compartido.ts`).
- **Ya existe backend real** en `servicios/auditoria/src/main/resources/openapi/auditoria.yaml`:
  - `GET /indicadores/operacion/servicios` (`consultarSaludDeServicios`): readiness de cada servicio, que incluye la base (`SaludDeLaBase.java`). Ya lo usa `rutas/operacion/estado/dominio/estado-del-sistema.ts`.
  - `GET /indicadores/catalogo-de-datos/resumen` (`obtenerResumenDelCatalogo`): esquemas con servicio dueño, tablas, columnas, relaciones, append-only y datos personales. Ya lo usa `rutas/tablero/catalogo/dominio.ts`.
- El esquema **no tiene migraciones versionadas**: se genera desde el modelo (`sql/aplicar.sql`, «Generado por scripts/generar_ddl.py»; aplicado por `despliegue/aplicar-esquema.sh`). No hay tabla de migraciones que listar.
- Despliegues, Respaldos, Proveedores, Outbox, Webhooks, Revisión de accesos e Incidentes **no tienen contrato** (`entregables/brecha-observabilidad.md`).
- Operaciones del sistema (`/sistemas/operaciones`), Mapa de flujos (`/sistemas/flujos`) siguen ruteadas pero salieron del menú en A1. El Catálogo de datos está bajo Configuración.
- El logo ya es enlace a `/tablero` (`layout/shell-financiero.ts:46`).

## Alcance
- IN: pantallas `sistemas/servicios` y `sistemas/base-datos` (y sus specs); `nucleo/menu.ts`; `layout/menu-lateral.ts` (y su spec); `sistemas/textos.ts`.
- OUT: backend nuevo para las 7 pestañas sin contrato; los puertos y el paquete simulado (quedan como están); el cambio sin commitear de la copia principal (`/Users/pablo/Documents/GitHub/Pasanaku`, rama vieja), que no se toca.
- Ambigüedades registradas:
  1. «Base y migraciones» sin migraciones: la pantalla muestra **el esquema por servicio + si su servicio alcanza la base ahora**. Supuesto mío; confirmar con Pablo.
  2. «Sistemas fijo, todo el detalle scrolleable con ícono, lo mismo para lo demás»: interpreto que ningún grupo se pliega y que debajo de cada sección se listan todas sus pantallas (incluidas las de los subgrupos Integraciones y Personal y accesos) con su ícono. El panel se desplaza. Confirmar con Pablo.
  3. «En lugar de Inicio muestre Operación»: se quita la entrada «Inicio» del menú (el logo cumple esa función) y la lista arranca en Operación. Las migas siguen diciendo «Inicio». Confirmar con Pablo.

## H1 — Sistemas muestra datos reales y la metadata se encuentra
**CA:** Dado un operador con rol de sistemas, cuando abre Servicios y SLO o Base de datos, entonces ve las filas que contestó `auditoria`. Si `auditoria` falla, ve el error accionable, nunca datos inventados.
**DoD:** specs dirigidas en verde + build de producción + verificación en el navegador contra backend real (o BLOQUEADO con evidencia).
**Estado:** HECHO (contra doble; falta verificar en TEST con sesión real)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Servicios y SLO lee `/indicadores/operacion/servicios` | Spec: con respuesta HTTP de 2 servicios pinta ambos con su salud; con 500, error; con 403, sin permiso | `npx ng test --include=**/sistemas/servicios/**` PASS | HECHO (6/6, `evidencia/H1.S1.M1-servicios.txt`) |
| H1.S1.M2 | Base de datos lee resumen del catálogo + salud | Spec: pinta un esquema por fila con servicio dueño, tablas y «alcanza la base» | `npx ng test --include=**/sistemas/base-datos/**` PASS | HECHO (`evidencia/H1.S1.M2-base-datos.txt`) |
| H1.S1.M3 | Operaciones, Mapa de flujos y Catálogo de datos visibles bajo Sistemas | `menu-y-guardias.spec.ts` y spec del menú PASS con las nuevas entradas | `npx ng test --include=**/nucleo/**` PASS | HECHO (agrupadas en «Endpoints y flujos», `evidencia/H2.S1.M1-menu.txt`) |

## H2 — Menú lateral fijo
**CA:** Dado cualquier operador, cuando mira el menú, entonces ningún grupo se pliega, cada sección lista todas sus pantallas con ícono, el panel se desplaza y no existe la entrada «Inicio» (el logo lleva a `/tablero`).
**DoD:** spec del menú + prueba visual en 3 viewports.
**Estado:** HECHO (contra doble; falta verificar en TEST con sesión real)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Grupos sin plegar, pantallas listadas con ícono, sin «Inicio» | Spec: no hay `aria-expanded` en rótulos; existen los enlaces de cada pantalla visible; no hay «Inicio» | `npx ng test --include=**/layout/menu-lateral*` PASS | HECHO (`evidencia/H2.S1.M1-menu.txt`) |
| H2.S1.M2 | Regresión del backoffice + build | Suite del backoffice sin fallas nuevas; build OK | `npx ng test` + `npx ng build` | HECHO (1345/1345; build OK con aviso de presupuesto previo) |
| H2.S1.M3 | Prueba visual | Capturas escritorio/tablet/celular mirándolas | Playwright | HECHO contra doble declarado (`evidencia/*.png`) |

## Desvíos registrados durante la ejecución
- Operaciones del sistema y Mapa de flujos van en el subgrupo «Endpoints y flujos» para respetar el tope de ocho pestañas por sección (A1, `nucleo/menu.spec.ts`).
- Se tocaron además `layout/shell-financiero.ts` (nombre accesible del logo) y los specs que afirmaban el menú viejo (`nucleo/menu.spec.ts`, `layout/shell-financiero.spec.ts`, `layout/pestanas-de-seccion.spec.ts`, `sistemas/compartido/vistas-de-puerto.a11y.spec.ts`).

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin credenciales de TEST para verificar contra backend real | Peldaño queda en TESTED | Declararlo; verificar con `ng serve` contra el gateway que haya |
| `menu-y-guardias.spec.ts` asume la forma del menú | Regresión | Correrlo y ajustar el test solo si el requisito cambió (lo cambió el usuario) |
