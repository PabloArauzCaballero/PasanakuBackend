# Contrato del organismo `Dialogo` (`packages/ui/src/dialogo/dialogo.ts`)

## 1. Anatomía

| Parte | Obligatoria | Cardinalidad | Cómo se declara |
|---|---|---|---|
| Título | Sí | 1 | `input.required<string>() titulo` — sin él no compila (regla de tipos, no de test) |
| Contenido | Sí | 1 | `<ng-content />` (proyección por omisión, sin selector) |
| Acción de cancelar | Sí (siempre presente) | 1 | Fija: botón "fantasma" con `textoDeCancelar` (`'Cancelar'` por omisión) |
| Acción de confirmar | Sí | 1 | `input.required<string>() textoDeConfirmar` — el texto **dice la acción exacta**, no genérico |

No se exponen ranuras (`<ng-content select>`) para título ni acciones: son inputs tipados, no
proyección libre. Esto es deliberado (H3.S1.M1, "no se exponen todos los nodos internos como
ranuras"): un uso sin título no compila (`titulo` es `input.required`), y un uso sin acción de
confirmar tampoco (`textoDeConfirmar` también lo es). La única proyección libre es el cuerpo.

## 2. Foco

- **Atrapado**: nativo de `<dialog>.showModal()` (HTML Standard §4.11.4). No hay implementación
  propia de trampa de foco — reimplementarla sería duplicar lo que el navegador ya da gratis y
  peor (bugs conocidos de trampas de foco caseras con `Tab`/`Shift+Tab` en elementos dinámicos).
- **Restauración**: también nativa — `close()` devuelve el foco al elemento que tenía el foco antes
  de `showModal()`, siempre que siga en el DOM.
- **jsdom no implementa `showModal`** (comentario ya existente en el archivo, confirmado al leer el
  código): en pruebas de componente (vitest + jsdom) el diálogo se abre con el atributo `open`, sin
  el comportamiento de foco del navegador. La prueba de foco atrapado y restaurado real solo se
  puede verificar en un navegador de verdad → **E2E con Playwright**, no en la suite de componente.
  Esto se declara, no se simula (regla 00).

## 3. Política de descarte única (H3.S2)

Las tres rutas de cierre convergen en `intentarCerrar()` / `onCancelNativo()` / `onClickEnFondo()`,
las tres consultando el mismo predicado `puedeDescartar()` antes de cerrar:

| Ruta | Disparador | Si `puedeDescartar()` da `false` |
|---|---|---|
| Botón cancelar | clic → `intentarCerrar()` | no se toca `abierto`, el diálogo sigue abierto |
| `Escape` | evento nativo `cancel` (cancelable) → `onCancelNativo(evento)` | `evento.preventDefault()`: el navegador no cierra el `<dialog>` |
| Clic en el fondo | `click` en el elemento `dialog` (no en sus hijos) → `onClickEnFondo(evento)` | no se llama a `intentarCerrar()` |

`puedeDescartar` es `() => true` por omisión (diálogos sin formulario no preguntan nada). Los
consumidores con borrador pasan `confirmarDescarteSiSucio(this.sucio)`
(`packages/ui/src/dialogo/politica-de-descarte.ts`), que solo pregunta cuando `sucio()` es `true` —
así ningún diálogo sin cambios interrumpe con una confirmación innecesaria.

**Hallazgo real, no hipotético:** antes de este cambio, el clic en el fondo **no cerraba nada**: el
`<dialog>` nativo con `showModal()` no tiene descarte por clic en el backdrop incorporado, y el
componente no escuchaba ese clic. Las "tres rutas" del encargo eran dos en la práctica. Se agregó la
tercera acá, no se documentó una que ya existía.

## 3bis. El lint de accesibilidad sobre el clic en el fondo (hallazgo real, corregido)

`yarn lint` sobre `@aportaya/ui` marcó dos errores reales en `(click)="onClickEnFondo($event)"`
(`@angular-eslint/template/click-events-have-key-events` e `interactive-supports-focus`). Se
resolvió con un `eslint-disable-next-line` justificado en el propio archivo, no ignorado a ciegas:
el clic en el fondo es un cierre **suplementario**, no el único camino — `Escape` (ya manejado en
`onCancelNativo`) y el botón "Cancelar" cubren la misma acción por teclado, que es lo que exige
WCAG 2.1.1. Convertir el `<dialog>` en un elemento enfocable con `tabindex` para conformar al lint
habría sido peor accesibilidad (un contenedor modal no es un control), no mejor.

## 3ter. Hallazgo real: `close()` no tenía el mismo resguardo que `showModal()` (corregido)

Al correr `estado-de-pantalla` y `dialogo` de verdad contra `yarn workspace @aportaya/ui
test:front` se confirmó, con `new JSDOM('<dialog></dialog>')` y `typeof`, que la versión de jsdom
instalada (`28.1.0`) **no implementa ni `showModal` ni `close`** de `HTMLDialogElement` (el
comentario preexistente ya lo decía para `showModal`, pero el código solo resguardaba esa mitad).
El `effect()` del constructor llamaba `d.close()` sin el mismo resguardo condicional que
`d.showModal()`, y como `d.open` sí se actualiza por reflejo del atributo `open` (eso jsdom lo
soporta), el segundo `if` SÍ se ejecutaba y `d.close()` tiraba `TypeError: d.close is not a
function`. Esto no lo introdujo este carril: es un defecto preexistente que ningún test anterior
ejercitaba (abrir-y-después-cerrar el mismo diálogo). Lo expuso `dialogo.spec.ts` y se corrigió
con el mismo patrón que ya existía para `showModal`.

## 4. Ciclo de vida y limpieza

El `effect()` del constructor es la única suscripción del componente; no hay `setInterval`,
`addEventListener` manual sobre `window`/`document`, ni `ResizeObserver`. Angular destruye el
`effect` junto con el componente. No hay fuga de escuchas que limpiar manualmente — se verifica con
el test de "abrir y cerrar cien veces" (ver `dialogo.spec.ts`), que cuenta instancias de `<dialog>`
en el DOM antes y después, no un contador de listeners que no existen.

## 5. Q-L2 (proyección de contenido)

Verificado en la versión instalada (`@angular/core@22.1.5`): un solo `<ng-content />` sin selector
alcanza para los dos modales migrados (H4.S2) — ninguno de los dos necesita más de una región de
contenido proyectado ni contenido diferido (`@defer` dentro del diálogo). No se agregó una plantilla
diferida sin un caso real que la necesite.
