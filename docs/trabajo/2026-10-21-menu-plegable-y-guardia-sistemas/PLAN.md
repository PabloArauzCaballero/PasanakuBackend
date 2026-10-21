# Plan — Menú lateral plegable y guardia de Sistemas que no deja entrar a nadie

- Fecha: 2026-10-21 · Repos afectados: Pasanaku (apps/backoffice) · Predecesor: `2026-10-16-tableros-por-rol` (hallazgo H0-05)
- Resultado observable: un operador con rol de catálogo `ADMIN_PLATAFORMA` o `RESPONSABLE_SEGURIDAD` hace click en cualquier opción de **Sistemas** y ve esa pantalla (no Operación); el menú lateral muestra abierta solo la sección activa y las demás se pliegan/despliegan con un botón.
- Kill-test: sesión con `rol: OPERADOR` + `permisos: [ADMIN_PLATAFORMA, ACCESOS_ADMINISTRAR]`, click en «Permisos internos» → si la URL termina en `/operacion`, no está hecho.

## Hallazgos (fase 1)
- `rutas/sistemas/guardia-rol-sistemas.ts:12-20`: admite solo `rol ∈ {PLATAFORMA, SEGURIDAD}`; si no, redirige a `/operacion`.
- `servicios/identidad/.../web/SesionesController.java:201`: el claim `rol` es siempre `ROL_DE_OPERADOR`/`ROL_DE_PARTICIPANTE`, nunca un rol del catálogo.
- `servicios/identidad/.../aplicacion/EmitirAcceso.java:66-69`: los códigos de rol vigentes (`ADMIN_PLATAFORMA`, …) viajan dentro de `permisos`.
- `sql/60_semillas/10-roles-y-permisos.sql:5,10`: los roles reales son `ADMIN_PLATAFORMA` y `RESPONSABLE_SEGURIDAD`.
- Ya anotado sin corregir en `docs/trabajo/2026-10-16-tableros-por-rol/evidencia/H0-02-a-06.md:35` (H0-05).
- Reproducción: dev local y TEST navegan bien con `rol: PLATAFORMA` falso; con el rol real (`OPERADOR`) la guardia redirige → síntoma «todas las opciones muestran lo mismo».
- `layout/menu-lateral.ts`: todas las secciones siempre abiertas (commit `cdbe02d6`); `menu-lateral.spec.ts:36-55` fija ese requisito viejo.

## Alcance
- IN: `rutas/sistemas/guardia-rol-sistemas.ts` (+ spec), `layout/menu-lateral.ts` (+ spec).
- OUT: backend de identidad (el claim `rol` no se cambia), `ap-boton` (ya tiene hover/press/carga), las pantallas de Sistemas, el error de `recurso.value()` en estado de error (ver Riesgos).
- Ambigüedades registradas:
  1. «comprimí las opciones» → supuesto: menú en acordeón, sección activa abierta, resto plegado, plegado manual con botón. Confirmar con Pablo.
  2. «detalles finales y animaciones de los botones» es amplio → supuesto: se pulen los del menú lateral (plegado animado, chevron, feedback de pulsación). Lo demás se anota, no se toca. Confirmar con Pablo qué pantallas le parecen más flojas.
  3. Cambio de requisito: los tests «siempre abiertas / sin botones de colapso» se reemplazan por el requisito nuevo pedido hoy.

## H1 — Sistemas navegable y menú compacto
**CA:** Dado un operador ADMIN_PLATAFORMA, cuando hace click en cualquier pantalla de Sistemas, entonces ve esa pantalla; y el menú solo despliega la sección en la que está.
**DoD:** typecheck + `test:front` de los specs tocados en verde + verificación en navegador con capturas (3 viewports, claro/oscuro).
**Estado:** HECHO

### H1.S1 — Guardia de Sistemas con roles reales
**CA:** Dado `permisos` con `ADMIN_PLATAFORMA` o `RESPONSABLE_SEGURIDAD`, cuando matchea `/sistemas`, entonces entra; un rol financiero sigue bloqueado.
**DoD:** spec de la guardia en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | La guardia acepta los códigos de rol reales que vienen en `permisos` | ADMIN_PLATAFORMA/RESPONSABLE_SEGURIDAD entran; ORGANIZADOR y sin sesión no | `npx ng test --include=src/app/rutas/sistemas/guardia-rol-sistemas.spec.ts` → 7/7 PASS (`evidencia/H1.S1.M1-guardia.txt`) | HECHO |

### H1.S2 — Menú en acordeón
**CA:** Dado el menú, cuando estoy en una sección, entonces solo sus pantallas se ven; el botón con chevron pliega/despliega cualquier sección con `aria-expanded` correcto.
**DoD:** spec del menú en verde + capturas.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Secciones plegables: abierta la activa, botón chevron por sección con hijos, animación de altura | Solo la sección activa muestra hijos; el botón alterna `aria-expanded` | `npx ng test --include=src/app/layout/menu-lateral.spec.ts` → 6/6 PASS (`evidencia/H1.S2.M1-menu.txt`) | HECHO |
| H1.S2.M2 | Verificación visual del menú (escritorio/tablet/celular, claro/oscuro) y navegación real en Sistemas | Capturas inspeccionadas; click en Permisos internos muestra Permisos internos con sesión `OPERADOR`+`ADMIN_PLATAFORMA` | Capturas inspeccionadas: `escritorio-claro/oscuro.png`, `tablet-*.png`, `celular-*.png`, `final-escritorio-*.png`; con `rol: OPERADOR`+`ADMIN_PLATAFORMA` las 6 pantallas probadas abren su h1 | HECHO |
| H1.S2.M3 | Typecheck y lint del backoffice | Sin errores nuevos | `yarn typecheck` exit 0; `yarn lint`: solo 2 FALLAS previas en `explicador-de-vista.component.ts` (no tocado) | HECHO |

### H1.S3 — Cabecera sin desborde en tablet (descubierto en H1.S2.M2)
**CA:** Dado un ancho de 820 px, cuando abro cualquier pantalla, entonces la cabecera no se pisa con las migas ni la página se desplaza en horizontal.
**DoD:** medición `scrollWidth == clientWidth` a 820, 1024, 1100 y 1440 px + capturas.
**Estado:** HECHO

Hallazgo: a 820 px la cabecera pide ~734 px y tiene 529 (`.vista` queda en 0 px, documento 1014 > 809). El menú pasa a cajón recién bajo 40rem (`layout/shell-financiero.ts:23`). Alcance agregado: `layout/shell-financiero.ts`, `layout/shell-financiero.css`.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | El menú pasa a cajón bajo 64rem y el buscador puede encogerse | Sin desplazamiento horizontal en 820/1024/1100/1440 | desborde=0 en 390/820/1024/1100/1280/1440 (`evidencia/H1.S3.M1-anchos.txt`) | HECHO |

## H2 — TestFlight si hay cambios nuevos en la app móvil
**CA:** Si `apps/movil` o `packages/diseno_flutter` cambiaron desde el último build subido, se sube uno nuevo; si no, se declara que no hacía falta.
**DoD:** comparación de commits + salida de la subida.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Detectar cambios de la app móvil desde el último build | Lista de commits o «ninguno» | `git log --since=<build 202610151748> origin/test -- apps/movil packages/diseno_flutter` → hay commits nuevos (`evidencia/H2.S1.M1-cambios-movil.txt`) | HECHO |
| H2.S1.M2 | Construir y subir a TestFlight si hay cambios | Build aceptado por App Store Connect | salida de la subida | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Pantallas de Sistemas que leen `recurso.value()` en estado de error lanzan en un `computed` (visto con 401 en TEST) | Consola con error; posible vista congelada | Se anota en el reporte, fuera de alcance |
| Credenciales/firma de TestFlight no disponibles en esta máquina | H2 no se completa | Se declara con evidencia |
