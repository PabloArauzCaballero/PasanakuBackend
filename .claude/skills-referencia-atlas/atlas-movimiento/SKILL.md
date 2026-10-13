---
name: atlas-movimiento
description: El movimiento en Atlas — Reanimated y los muelles de `tokens.ts` en la app del cliente, los resortes `linear()` de la capa web y las transiciones CSS de los portales Next. Cuándo una animación está justificada y cuándo es decoración que hay que quitar. Usar al animar una entrada, una transición de pantalla, una lista o cualquier microinteracción, y al diagnosticar una interfaz que «se mueve sola».
---

# Movimiento en Atlas

Sustituye a la guía genérica de motion, que estaba escrita para Angular (`animate.enter`,
`animate.leave`, motion.dev con guardas de SSR). Aquí no hay Angular: en la app del cliente se
anima con **Reanimated 4**, en la capa web con **transiciones y `@keyframes` CSS generadas desde
los tokens**, y en los portales Next con CSS.

## 1. La regla, que es la única que importa

El movimiento en Atlas responde a **dos preguntas** que la persona se hace sin darse cuenta:

> «¿de dónde salió esto?» y «¿me hizo caso?»

Una pantalla que entra deslizándose desde la derecha contesta la primera; un botón que se hunde bajo
el dedo contesta la segunda. **Todo lo que no conteste una de las dos es decoración**, y en una app
de dinero la decoración se paga en segundos de espera.

Corolario, y es el hallazgo que más veces se ha repetido aquí: **un elemento que se mueve sin que
nadie lo toque no comunica nada.** El destello del botón primario estuvo en bucle infinito cada 5 s
en todos los botones a la vez; la tarjeta de cuenta se mecía sola en bucle de 13 s en el acceso y en
los ocho pasos del alta. Eso es exactamente lo que hace que una interfaz se lea como una plantilla:
la pantalla se mueve sola y el ojo tiene que descartarla una y otra vez mientras se rellena un
formulario. Colgado del puntero o del dedo, el mismo efecto tiene causa y pasa a ser acabado.

Lo mismo con las superficies: **una tarjeta no se levanta al pasar el puntero.** La elevación es la
respuesta a «esto se puede pulsar», y la mayoría de las tarjetas de Atlas son la caja donde viven
dos campos. Lo que sí cambia al pasar por encima es el filo.

## 2. Movimiento reducido no es una casilla

`useReducedMotion` de Reanimated en la app, `@media (prefers-reduced-motion: reduce)` en web. Cuando
está activo, **las duraciones valen cero** y las animaciones se vuelven cambios instantáneos. Y la
regla que lo hace seguro: **el contenido nunca depende de la animación para mostrarse.** Si el
movimiento no ocurre, la pantalla está completa igual.

## 3. La app: Reanimated y los tokens

Nada de duraciones ni curvas escritas en una pantalla. Todo sale de
`src/theme/tokens.ts`:

- `motion.fast` 140 (respuesta al toque: o es inmediata o no se percibe como respuesta),
  `motion.base` 240, `motion.slow` 380 (hojas y superposiciones), `motion.stagger` 45,
  `motion.brandCut` 900.
- `easing.standard` arranca de golpe —para lo que responde al dedo—, `decelerate`, `accelerate`,
  `emphasized`.
- `spring.press` (`damping 26, stiffness 420, mass 0.7`) y los demás muelles. Están
  **sobreamortiguados a propósito**: llegan y se quedan, sin rebasar el destino. La ganancia de un
  muelle no es el rebote, es que la desaceleración no sea lineal.
- `press` para el hundimiento: escala y opacidad **juntas**. Solas se leen mal — la escala a 0.97 en
  un botón ancho casi no se ve, y la opacidad sola parece que el botón se apaga en vez de recibir el
  toque. Combinadas, el botón se lee como una superficie que CEDE.

**Por qué Reanimated y no `Animated`:** las animaciones corren en el hilo de UI. Durante una decisión
de crédito el hilo de JS está ocupado —petición, parseo, re-render— y con `Animated` sin
`useNativeDriver` eso se ve como tirones justo en el momento en que la persona más atenta está.

Utilidades ya hechas en `src/ui/motion.tsx`: `AnimatedPressable`, los envoltorios de aparición y el
`PressSurface` que da a todas las superficies el mismo temperamento. Un control que invente su propio
salto de opacidad es la inconsistencia de movimiento más repetida que ha tenido esta app.

## 4. La web de la app

`src/web/estilo.ts` genera los resortes `linear()` de la identidad (`--spring`, `--pop`, `--glide`)
y las entradas (`[data-atlas="aparece"]`, con retardo por `data-indice`). Tres cosas medidas:

- **`filter: blur()` en la atmósfera mata las pantallas grandes**: a 2.560×1.440, 14 fps con él y 61
  sin él. react-native-web repinta sin parar; la landing es un documento quieto y por eso allí no se
  nota. Los halos van como degradados radiales, sin filtro y sin deriva.
- Las animaciones de entrada sólo a partir de `TRAMO.tableta`: a 390 px la web es la app y la app ya
  tiene las suyas.
- Con `prefers-reduced-motion` se apagan **también las transiciones de hover** (`transition: none`),
  no sólo los `@keyframes`: el estado cambia, no viaja.

## 5. Los portales Next

Transiciones CSS cortas sobre `transform` y `opacity` —nunca sobre `width`, `height` o `top`, que
provocan reflow—. El Motor tiene su parte dedicada: `src/styles/parts/motion.css`. Misma regla de
siempre: si la animación no contesta «¿de dónde salió?» o «¿me hizo caso?», sobra.

## 6. Diagnóstico rápido de «se mueve sola»

1. `grep -n "infinite"` en las hojas: cualquier `animation: … infinite` es sospechoso por defecto.
2. Buscar `:hover{transform:` en superficies que no se pulsan.
3. Cargar la pantalla y NO tocar nada durante diez segundos. Lo que se mueva, justifícalo o quítalo.
4. Medir fps de verdad con `requestAnimationFrame` en Playwright si la sospecha es de rendimiento;
   «va fluido» no es una medida.

## Checklist

- [ ] Cada animación contesta «¿de dónde salió esto?» o «¿me hizo caso?».
- [ ] Nada se mueve solo: ningún `infinite` sin causa, ninguna superficie quieta que se eleve.
- [ ] Duraciones, curvas y muelles salen de los tokens; no hay números sueltos.
- [ ] Con movimiento reducido todo sigue siendo legible y completo.
- [ ] Se anima `transform`/`opacity`, no la geometría.
