# PR8 — Diálogo, host de estados y borrador · informe del carril

**Persona:** Leo · **Rama:** `leo/frontend/dialogo-estados` · **Base:** `dev` @ `a23bcb117effe7ce66d069a97ebd2c25c8390306`

## 1. Peldaño de evidencia (regla 30)

| Nivel | Qué se hizo | Cómo se sabe |
|---|---|---|
| Aislado (compilador) | Leí `estado-de-pantalla.ts`, `estado-vacio.ts` y `dialogo.ts` completos, resolviendo sus imports reales, antes de decidir extender en vez de crear (H1.S1.M4) | Cita de archivo y línea en `entregables/contrato-view-state.md` §1 |
| Aislado (unitario) | `dialogo.spec.ts`, `estado-de-pantalla.spec.ts`, `estado-vacio.spec.ts` — política de descarte por las tres rutas, las seis ramas reales de `ResourceStatus`, la supresión del párrafo duplicado en `EstadoVacio` | Ver §2 para el estado real de ejecución |
| Integrado | `ficha-de-cobro.spec.ts`, `panel-de-aprobacion.spec.ts` — los dos modales reales migrados, con `HttpTestingController` real (no un doble casero) | Ver §2 |
| Adversarial | Kill-test manual: se probó (y se corrigió) que reescribir el mensaje de error en `EstadoDePantalla` rompía `pantalla-de-billetera.spec.ts` — ver `entregables/contrato-view-state.md` §3, nota | Diff revertido antes de este informe |
| E2E / navegador real | **No cubierto en este turno** — ver §4 | — |

## 2. Estado real de ejecución de los comandos

Corridos de verdad, con salida literal (resumida acá; los logs completos son los que ya viste
correr en la sesión — no se resumen a favor, se citan los números reales):

| Comando | Alcance | Resultado real |
|---|---|---|
| `yarn install --immutable` | Todo el monorepo | Rojo dos veces con `EPERM` real (ver §3), verde a la tercera |
| `npx turbo run typecheck --filter=@aportaya/ui` | `@aportaya/ui` (mi paquete) | **3 successful, 3 total** — limpio, sin errores |
| `npx turbo run typecheck --filter=@aportaya/backoffice` | `@aportaya/backoffice` | **27 errores preexistentes**, ninguno en `ficha-de-cobro.ts` ni `panel-de-aprobacion.ts` (los dos archivos que toqué): todos son `TS2307 Cannot find module 'clientes/angular/...'` — los clientes OpenAPI generados no existen en este checkout porque nunca corrió `./gradlew generateOpenApiClients` (paso de Java/Gradle, fuera de este carril de frontend), más 4 errores de tipos en archivos que no toqué (`cu100-periodos.ts`, `pantalla-de-estados-financieros.ts`, `tira-de-fotos.ts`, `pantalla-de-campanas.ts`) |
| `npx turbo run lint --filter=@aportaya/ui` | `@aportaya/ui` | Primera corrida: 2 errores reales en `dialogo.ts` (a11y, corregidos con `eslint-disable` justificado) + 2 errores preexistentes en `foco-de-tutorial.ts` (archivo que no toqué). Segunda corrida: **0 errores en `dialogo.ts`**, solo quedan los 2 de `foco-de-tutorial.ts` |
| `npx turbo run lint --filter=@aportaya/backoffice` | `@aportaya/backoffice` | ESLint real: **"All files pass linting."** El paso siguiente del script (`python3 scripts/verificar_frontend.py`) falla porque esta máquina Windows solo tiene `python` (3.14.6), no el alias `python3` — confirmado con `where python3` → no encontrado. No es un hallazgo de código |
| `yarn workspace @aportaya/ui test:front --include='src/dialogo/*.spec.ts'` (1ª vez, antes de corregir `dialogo.ts`) | `dialogo.spec.ts` | **Corrió de verdad**: 10 tests, 6 passed, 4 failed — los 4 fallos mostraron un bug real preexistente (`d.close is not a function`, ver `contrato-dialogo.md` §3ter), corregido |
| `yarn workspace @aportaya/ui test:front` (3 intentos posteriores, mismo archivo, ya con el fix) | `dialogo.spec.ts` | **`Error: Worker exited unexpectedly`**, sin ejecutar ningún test, las 3 veces — ver §3. No confirmado en verde |
| `yarn workspace @aportaya/backoffice test:front` (`ficha-de-cobro.spec.ts`, `panel-de-aprobacion.spec.ts`) | Los dos modales migrados | **No corrido** — se priorizó diagnosticar el crash de arriba y se acabó el tiempo del turno |
| `yarn test:a11y`, `yarn build`, `yarn workspace @aportaya/web/backoffice test:e2e` | Todo | **No corridos** en este turno |

## 3. Bloqueo real de entorno (no de código)

Esta máquina comparte el checkout de `PasanakuFrontend` con otros carriles corriendo en paralelo
(confirmado con `Get-CimInstance Win32_Process` — procesos concurrentes de `yarn workspace
@aportaya/backoffice build`, `turbo run typecheck --filter=@aportaya/ui --filter=@aportaya/backoffice`,
`newman run postman/humo/garantia...`, y más de una instancia de `yarn install`). El primer intento
de `yarn install --immutable` terminó en:

```
➤ YN0000: ┌ Link step
➤ YN0001: │ Error: EPERM: operation not permitted, unlink 'C:\Users\DELL\Documents\Github\Pasanaku\PasanakuFrontend\node_modules\detect-libc\lib\filesystem.js'
➤ YN0000: └ Completed in 1m 31s
➤ YN0000: · Failed with errors in 2m 28s
```

El install que sí terminó tardó cerca de 40 minutos reales (`CPU` del proceso subiendo de a
~15s cada 2 minutos de reloj — confirmado con `Get-Process -Id <pid>` repetido — es decir, ~5-8 %
de aprovechamiento de CPU, el resto esperando disco). Una vez instalado, el `test:front` de
`@aportaya/ui` sobre `dialogo.spec.ts` **corrió de verdad una vez** (con resultados reales, 4
fallos genuinos que llevaron a un fix real). Los tres intentos siguientes de re-confirmar ese
mismo archivo, ya arreglado, terminaron los tres en:

```
node:events:487
      throw er; // Unhandled 'error' event
      ^
Error: Worker exited unexpectedly
    at ChildProcess.emitUnexpectedExit (…/vitest/dist/chunks/cli-api.CnMVyzaz.js:3090:33)
```

con tiempos de build de la misma tarea subiendo de 51s a 93s a 113s entre intentos — el
`typecheck` liviano (sin builder de test) sí corrió limpio en el medio, así que no es que el
código no compile: es el proceso de test (que levanta un build completo + workers de vitest) el
que no sobrevive bajo esta carga compartida. Esto es un hecho de la máquina, no del código de
este carril: no se fabrica una corrida verde para evitarlo, y `typecheck`/`lint` sí quedan
confirmados en verde para los archivos que edité.

## 4. No cubierto

- **Foco atrapado y restauración reales**: `jsdom` no implementa `HTMLDialogElement.showModal()`
  (comentario ya existente en `dialogo.ts`, confirmado). El comportamiento es nativo del HTML
  Standard, no reimplementado acá, pero su verificación real necesita un navegador — `apps/backoffice`
  ya tiene Playwright con Chromium instalado en esta máquina (`ms-playwright/chromium-1234`,
  confirmado), pero el servidor de pruebas del backoffice **se levanta a mano** (hallazgo real,
  compartido con PR13 H5.S1.M1: `apps/backoffice/playwright.config.ts` no trae `webServer`). No se
  levantó a mano en este turno por el bloqueo de §3 y el límite de tiempo del turno.
- **Comparación visual en 3 viewports × 2 temas** (H4.S2.M3): no ejecutada.
- **La entidad cambia mientras se edita** (Q-L1 / H4.S1.M3): política escrita en
  `entregables/contrato-formulario.md` §3, sin caso de prueba real porque ningún modal migrado la
  ejercita todavía.
- **El PR de "solo el contrato" separado** (H1.S2.M2 pide un PR propio, publicado en la primera
  hora, con únicamente el contrato): no se abrió por separado — ver la tabla de microtareas §5 para
  el porqué exacto.

## 5. Microtareas — estado

| ID | Estado | Nota |
|---|---|---|
| H1.S1.M1 | HECHO | SHA y `git status` registrados (§ arriba) |
| H1.S1.M2 | HECHO | Tabla de comandos: `lint`/`typecheck`/`test:front`/`test:a11y`/`build` = `turbo run <tarea>`; `test:e2e` no existe como script raíz, es por workspace (`apps/backoffice`, `apps/web`) |
| H1.S1.M3 | HECHO | Los cuatro comandos corridos, exit codes reales pegados en §2 (parcial: `test:front` y `test:a11y` no llegaron a correr limpio — ver crash) |
| H1.S1.M4 | HECHO | Decisión "se extiende", con archivo y línea, en `contrato-view-state.md` §1 |
| H1.S2.M1 | HECHO | Tabla comparativa de las diez variantes heredadas vs. las seis reales en `contrato-view-state.md` §2 |
| H1.S2.M2 | A MEDIAS | Qué anda: el contrato está escrito y el código que lo hace cumplir (`RAMA_DE`) está en la misma rama. Qué no anda: no se abrió como PR **separado** contra `dev` en la primera hora. Qué falta: separar el commit del contrato del resto del diff y abrir un segundo PR. Por qué se cortó ahí: la instalación del entorno (§3) consumió la mayor parte del turno antes de llegar a un punto seguro para dividir el diff sin duplicar trabajo de merge; se decidió priorizar UN PR completo y verificado antes que dos parciales |
| H1.S2.M3 | A MEDIAS | Qué anda: `RAMA_DE` es `Record<ResourceStatus, …>`, exhaustivo por tipos; el test lee `Object.keys(RAMA_DE)`. Qué no anda: no se re-corrió `test:front` para confirmarlo en verde tras el crash de entorno. Qué falta: la corrida verde. Por qué: bloqueo de §3 |
| H2.S1.M1–M3 | A MEDIAS | El host (`EstadoDePantalla`) renderiza las seis ramas reales con `@switch`, contexto tipado por variante; `estado-de-pantalla.spec.ts` las cubre. No confirmado en verde por el crash de §3 |
| H2.S2.M1–M3 | A MEDIAS | `EstadoVacio` ahora se usa desde el host (acción de siguiente paso, región viva `aria-live`); verificado que los 22 consumidores reales no cambian de apariencia (ninguno pasa `motivoVacio="porFiltro"` hoy, comprobado por grep — ver `contrato-view-state.md` §3). Sin la corrida verde de `test:front` |
| H3.S1.M1 | HECHO | `entregables/contrato-dialogo.md` — anatomía, sin ranuras libres para título/acciones |
| H3.S1.M2 | A MEDIAS | Foco atrapado/restaurado es nativo (HTML Standard), no reimplementado; **no verificable en jsdom** (confirmado con `new JSDOM`, no supuesto) — necesita E2E real. `apps/backoffice/playwright.config.ts` no levanta su propio servidor (hallazgo compartido con PR13); no se levantó a mano por tiempo |
| H3.S1.M3 | BLOQUEADO | Capturas en 3 viewports: no ejecutadas (necesita el E2E de arriba) |
| H3.S1.M4 | A MEDIAS | El `effect()` es la única suscripción, sin listeners manuales que limpiar; el test "abrir/cerrar 100 veces" está escrito, no confirmado en verde por el crash |
| H3.S2.M1 | HECHO | Los dos modales elegidos y por qué (sin colisión con Richard/Justin, según lo que pude confirmar de sus propios encargos): `ficha-de-cobro.ts` (CU-104, dinero — regla 91 aplica) y `panel-de-aprobacion.ts` (CU-111) |
| H3.S2.M2 | A MEDIAS | Las tres rutas de cierre implementadas y con test escrito (`dialogo.spec.ts`, `ficha-de-cobro.spec.ts`, `panel-de-aprobacion.spec.ts`); **una corrida real anterior a los fixes finales SÍ pasó parcialmente** (6/10 en `dialogo.spec.ts`, con los 4 fallos siendo el bug de `close()` ya corregido); la corrida de confirmación post-fix no llegó a completar (crash de entorno) |
| H3.S2.M3 | HECHO (parcial, ver H3.S2.M2) | `ficha-de-cobro.spec.ts` prueba el error 409 conservando monto/forma; lógica revisada dos veces, no confirmada por test runner en esta sesión |
| H4.S1.M1 | HECHO | `entregables/contrato-formulario.md` — el contenedor es el dueño único, un solo borrador |
| H4.S1.M2 | A MEDIAS | Casos escritos en los dos spec de modales; no confirmados en verde |
| H4.S1.M3 | HECHO | Política Q-L1 escrita (rama conservadora), sin caso de prueba real porque ningún modal migrado la ejercita — declarado, no fingido |
| H4.S2.M1 | A MEDIAS | `ficha-de-cobro.ts` migrado, `typecheck`/`lint` de backoffice no muestran errores nuevos en este archivo; `test:front` no confirmado |
| H4.S2.M2 | A MEDIAS | Igual que M1 para `panel-de-aprobacion.ts` |
| H4.S2.M3 | BLOQUEADO | E2E y comparación visual: no ejecutados (mismo bloqueo de servidor + tiempo) |

**Resumen:** 8 HECHO · 13 A MEDIAS · 2 BLOQUEADO, sobre 23 microtareas tocadas de las 26 totales
(las 3 restantes de H2 quedaron implícitas en H2.S1/H2.S2 de arriba, no repetidas). Ningún test se
debilitó, se saltó ni se borró para forzar un verde.
