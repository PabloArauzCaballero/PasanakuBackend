# Reporte — Menú lateral plegable y guardia de Sistemas que no dejaba entrar a nadie

> **AVANCE: 6 / 7 — 85,7 %.** Backoffice cerrado; la subida a TestFlight (H2.S1.M2) está corriendo y se actualiza abajo.

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/menu-plegable-guardia-sistemas-20261021` (sobre `676df14b`)
- Peldaño de evidencia alcanzado: backoffice `VERIFIED` (navegador real + tests dirigidos + regresión de módulo); TestFlight ver H2.

## Causa raíz del «toco una opción y no cambia nada»
`rutas/sistemas/guardia-rol-sistemas.ts` solo dejaba pasar `rol ∈ {PLATAFORMA, SEGURIDAD}`. Identidad emite siempre
`rol: OPERADOR` (`SesionesController.java:201`) y pone los roles reales (`ADMIN_PLATAFORMA`, `RESPONSABLE_SEGURIDAD`)
dentro de `permisos` (`EmitirAcceso.java:66-69`). Resultado: el menú mostraba Sistemas (por el permiso
`ACCESOS_ADMINISTRAR`), pero toda pantalla redirigía a `/operacion` → siempre se veía «Arquitectura y estado».
Ya estaba anotado sin corregir como H0-05 en `docs/trabajo/2026-10-16-tableros-por-rol/evidencia/H0-02-a-06.md:35`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | La guardia acepta los roles reales que vienen en `permisos`; un rol financiero sigue bloqueado | `npx ng test --include=…/guardia-rol-sistemas.spec.ts` | PASS 7/7 · `evidencia/H1.S1.M1-guardia.txt` |
| H1.S2.M1 | Menú en acordeón: solo la sección activa abierta; botón de flecha con `aria-expanded`/`aria-controls`; plegado animado; lo plegado queda `inert` | `npx ng test --include=…/menu-lateral.spec.ts` | PASS 6/6 · `evidencia/H1.S2.M1-menu.txt` |
| H1.S2.M2 | Con sesión `OPERADOR`+`ADMIN_PLATAFORMA`, Servicios/Respaldos/Accesos/Permisos/Roles/Usuarios internos abren cada una su pantalla | Playwright contra `ng serve` | PASS · capturas `escritorio-*`, `tablet-*`, `celular-*`, `final-escritorio-*` |
| H1.S2.M3 | Typecheck sin errores; lint sin fallas nuevas | `yarn typecheck`; `yarn lint` | exit 0; 2 FALLAS previas en `explicador-de-vista.component.ts` (no tocado) |
| H1.S3.M1 | Cabecera sin desborde horizontal en tablet (el menú pasa a cajón bajo 64rem; el buscador encoge antes que el título) | medición `scrollWidth-clientWidth` | 0 en 390/820/1024/1100/1280/1440 · `evidencia/H1.S3.M1-anchos.txt` |
| H2.S1.M1 | Hay 51 commits de la app móvil desde el último build registrado (202610151748) | `git log --since … origin/test -- apps/movil packages/diseno_flutter` | `evidencia/H2.S1.M1-cambios-movil.txt` |

## A medias
### H2.S1.M2 — Build y subida a TestFlight
- Qué anda: worktree limpio en `origin/test` `f7bab6ac`, Flutter 3.44.8, clientes Dart presentes, contratos idénticos a HEAD.
- Qué no anda: todavía no terminó (`scripts/testflight_ios.sh` en curso).
- Qué falta exactamente: que el script termine con `Upload succeeded`; si la firma pide la cuenta de Xcode, la tiene que correr Pablo.
- Dónde quedó: worktree `scratchpad/movil`, log `scratchpad/testflight.log`.

## Pendiente
Ninguna.

## Evidencia
```text
$ npx ng test --include='src/app/layout/**/*.spec.ts' --include='src/app/rutas/sistemas/**/*.spec.ts' --include='src/app/rutas/configuracion/**/*.spec.ts'   (worktree = contenido exacto del commit)
 Test Files  41 passed (41)
      Tests  134 passed (134)
```
Resto en `evidencia/`: `H1.S1.M1-guardia.txt`, `H1.S2.M1-menu.txt`, `H1.S3.M1-anchos.txt`, `regresion-front.txt`, `H2.S1.M1-cambios-movil.txt`, capturas `.png`.

## No cubierto
- Navegación contra el backend real de TEST con un usuario `ADMIN_PLATAFORMA` real: la sesión se simuló interceptando `/sesion/refrescar` en el navegador (doble declarado, regla 65). Lo que falta verificar en TEST: ingresar con el operador real y tocar 3 pantallas de Sistemas.
- E2E Playwright de la suite (`yarn test:e2e`) no se corrió.
- Lector de pantalla sobre el botón de plegar (solo se verificó `aria-*` por test y teclado por `inert`).

## Desvíos del plan
- Se agregó H1.S3.M1 (cabecera en tablet), descubierto en la prueba visual.
- Los estilos del menú se movieron a `layout/menu-lateral.css` para cumplir el tope de 200 líneas del lint.
- **Edición concurrente**: durante la sesión otro agente (Gemini) editó el mismo árbol sin commitear — barra lateral clara en `shell-financiero.{ts,css}`, colores del menú en `menu-lateral.css`, y `packages/ui` (tabla, estado de pantalla, estilos). El commit de este trabajo se armó en un worktree limpio con **solo** los cambios propios; el trabajo de Gemini sigue sin commitear en el árbol principal.

## Riesgos residuales
- `yarn test:front` en el árbol compartido: 4 rojos en Configuración (tarifario, mensajería, reportes) causados por los cambios sin commitear de Gemini en `packages/ui/src/tabla-de-datos`. En el contenido de este commit pasan (`evidencia/regresion-front.txt`).
- Las pantallas de Sistemas que leen `recurso.value()` en estado de error lanzan dentro de un `computed` (visto en TEST con 401 en `/permisos` y `/roles`).
- Con la cabecera a 1100 px el texto del buscador se corta («Busca…»).

## Decisiones y ambigüedades
- «Comprimí las opciones» → se interpretó como acordeón con la sección activa abierta. Confirmar con Pablo.
- «Detalles y animaciones de los botones» → se pulió el menú (flecha que gira, plegado animado, respuesta al pulsar) y la cabecera; `ap-boton` ya tenía hover/pulsado/carga. Pablo debe señalar qué otras pantallas le parecen flojas.
- Requisito cambiado: los tests que exigían «siempre abiertas, sin botón de colapso» se reemplazaron por el requisito pedido hoy.
- No se reusó `ap-acordeon`: su cabecera es un título en tarjeta y acá la cabecera tiene que seguir siendo enlace.
