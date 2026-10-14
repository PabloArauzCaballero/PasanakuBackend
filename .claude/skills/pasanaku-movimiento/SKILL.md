---
name: pasanaku-movimiento
description: El movimiento en la app Flutter de AportaYa — cuándo una animación está justificada y cuándo es decoración, los tokens únicos de duración, curva y resorte (`Movimiento`, `MovimientoDeInterfaz`), reducir movimiento, rendimiento (compositor, RepaintBoundary) y las piezas ya hechas (HundidoAlTocar, AparicionEscalonada, TransicionDeEje, MontoQueSube, LuzDeBoton). Usar al animar una entrada, una transición, una lista o una microinteracción, y al diagnosticar una pantalla que «se mueve sola» o janquea. Traduce al stack real la skill de Atlas `atlas-movimiento`.
---

# Movimiento en AportaYa

## 1. La regla, que es la única que importa

El movimiento contesta **dos preguntas** que la persona se hace sin darse cuenta:

> «¿de dónde salió esto?» y «¿me hizo caso?»

Una pantalla que entra deslizándose desde la derecha contesta la primera; un botón que se hunde bajo
el dedo contesta la segunda. **Todo lo que no conteste una de las dos es decoración**, y en una app de
plata la decoración se paga en segundos de espera.

Corolarios (medidos en Atlas, valen igual acá):
- **Nada se mueve solo.** Un brillo en bucle en todos los botones, una tarjeta que se mece en el
  formulario: el ojo tiene que descartarlos una y otra vez. Colgado de un toque, el mismo efecto es
  acabado. `LuzDeBoton` cruza el botón **principal de la pantalla**, espaciado (`Movimiento.brillo`
  cada `pausaDelBrillo`), nunca en todos a la vez ni en formularios largos.
- **Una tarjeta no se levanta al tocar si no se puede tocar.** La elevación es la respuesta a «esto se
  pulsa».
- **La prueba de diez segundos**: abrí la pantalla y no toques nada diez segundos. Lo que se mueva,
  justificalo o sacalo. Excepciones legítimas: la cámara en vivo, una cuenta atrás, un esqueleto
  mientras carga, el turno actual de un grupo (pulso muy suave, y no con reducir movimiento).

## 2. Los números, en un solo lugar

`packages/diseno_flutter/lib/atomos/movimiento.dart`:

| Token | Valor | Para qué |
|---|---|---|
| `Movimiento.micro` | 120 ms | Respuesta al dedo: apretar, cambio de color de estado |
| `Movimiento.entrada` | 220 ms | Algo que aparece o cambia de forma dentro de la pantalla |
| `Movimiento.pagina` | 300 ms | Una pantalla nueva que llega |
| `Movimiento.paginaDeVuelta` | 240 ms | La que se va al volver (más corta que la que llega) |
| `Movimiento.llega` | `Cubic(0.05, 0.7, 0.1, 1)` | Lo que entra (desacelera) |
| `Movimiento.sale` | `Cubic(0.3, 0, 0.8, 0.15)` | Lo que sale (acelera) |
| `Movimiento.pareja` | `easeInOutCubic` | Lo que va y vuelve en el mismo lugar |
| `Movimiento.resorte` | masa 1, rigidez 520, amortiguación 19 | Soltar un botón: pasa apenas y se asienta |
| `Movimiento.corrimiento` | 30 dp | Desplazamiento lateral de una pantalla que entra |

Reglas: **nada disparado por la persona pasa de 320 ms**; la salida es más corta que la entrada; nada
escala la pantalla entera (los objetos se desplazan y aparecen, no se inflan). Ninguna
`Duration(milliseconds: …)` literal en una pantalla: si falta un valor, se agrega acá con su porqué.
Las constantes de **reglas** (cuenta atrás de 3 s de la prueba de vida, candado de 60 s / 5 min) son
dominio, no estética, y viven en su `dominio/`.

## 3. Reducir movimiento no es una casilla

`MediaQuery.disableAnimationsOf(context)`. Cuando está activo, **las duraciones valen cero** y las
animaciones son cambios instantáneos. Lo que lo hace seguro: **el contenido nunca depende de la
animación para mostrarse** (si el movimiento no ocurre, la pantalla está completa igual). Lo que es
información (una cuenta atrás, un progreso) se muestra igual, sin transición.

## 4. Cómo se anima en Flutter, sin janquear

- **Animá lo que el compositor mueve barato**: `Transform` (escala, traslación, rotación), `Opacity`
  vía `FadeTransition`/`AnimatedOpacity`. **No** animes `width`/`height`/`padding` de algo con hijos
  pesados en cada cuadro (relayout); si hace falta, `AnimatedSize` en piezas chicas.
- `RepaintBoundary` alrededor de lo que se repinta seguido (cámara, contorno de la cara, brillo, una
  cifra que cuenta) para que no arrastre al resto.
- `BackdropFilter` (desenfoque) es caro: solo en barras pequeñas, **nunca** a pantalla completa sobre
  la cámara.
- Resortes: `SpringSimulation(Movimiento.resorte, desde, hasta, velocidad)` con
  `AnimationController.animateWith`; las animaciones **se interrumpen** desde donde están (no saltan al
  inicio): usar el valor actual del controlador como `desde` (`apple-design`).
- Implícitas cuando alcanza (`AnimatedScale`, `AnimatedOpacity`, `TweenAnimationBuilder`); explícitas
  (`AnimationController`) cuando hay secuencia, interrupción o resorte.
- Medir: `flutter run --profile` + overlay de rendimiento; «va fluido» no es una medida.

## 5. Piezas ya hechas (usarlas)

`HundidoAlTocar` (presión: escala + resorte), `AparicionEscalonada` (entrada de bloques),
`TransicionDeEje` (pantallas en eje compartido), `TransicionDeMarca` (las de entrada), `AperturaDeMarca`
(arranque), `MontoQueSube` / `NumeroQueSube` (cifras), `LuzDeBoton` (brillo del principal),
`SuperficieViva` (superficie que responde al toque), `Esqueleto` (carga). Un control que inventa su
propio salto de opacidad es la inconsistencia más repetida: el mismo temperamento en toda la app.

## 6. Catálogo de lo que SÍ se anima en AportaYa

| Momento | Animación | Valores |
|---|---|---|
| Tocar cualquier cosa tocable | Hundido (escala 0,97) + soltar con resorte | `micro` / `resorte` |
| Pantalla que llega | Eje compartido, 30 dp + fundido | `pagina` / `llega` |
| Bloques de una pantalla nueva | Escalonado 40 ms, 220 ms cada uno, máx. 6 bloques | `entrada` / `llega` |
| Error de credencial | Temblor horizontal 3 × 6 dp | 300 ms total |
| Cambio de paso del alta | Eje horizontal + barra de fase que se llena | `pagina` |
| Foto tomada | Destello 80 + 420 ms, háptico medio, miniatura «pop» | — |
| Cuenta creada | Círculo de marca con resorte + tilde que se dibuja | 360 ms |
| Saldo | Cifra que sube | `contador` |
| Hoja que aparece | La del sistema; sube con el teclado | `entrada` |
| Candado | Fundido 220 ms + marca 0,92→1 con resorte; sale en 160 ms | — |

## Checklist

- [ ] Cada animación contesta «¿de dónde salió?» o «¿me hizo caso?».
- [ ] Nada se mueve solo sin causa (prueba de diez segundos hecha).
- [ ] Duraciones, curvas y resortes salen de `Movimiento`; ninguna literal en la pantalla.
- [ ] Con reducir movimiento todo está completo y legible.
- [ ] Se anima transform/opacidad; `RepaintBoundary` donde corresponde; medido en `--profile`.
- [ ] Se interrumpe desde donde está.
