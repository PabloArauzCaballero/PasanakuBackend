# Crítica de las pantallas tal como están hoy (después de H2–H4)

Método: `roast-my-ui` (diez pasadas) + `fintech-ux` + `mobile-app-ui-design`, sobre los goldens
`h10-*-claro-412-130` (fuentes reales) del 2026-10-21. Hojas: `hoy-1` (portada, ingreso, inicio,
mi estado) y `hoy-2` (verificación, alta, ayuda, perfil).

## Veredicto

Solo Inicio recibió el lenguaje bancario. Las otras pantallas siguen siendo un documento: título,
párrafo, campos y un botón abajo, sin un objeto protagonista. Cambió el color, no la composición.
Por eso «se ve igual».

## Tres cosas que no se pueden dejar de ver

1. 🔥🔥🔥 **«Estado de tu aporte» esconde la plata en un párrafo.** La deuda («Bs 1.240,50») va
   dentro de una oración, y «Bs 4,44» (lo que hay que aportar, `porAportar`) aparece suelto,
   sin etiqueta visible: solo el lector de pantalla sabe qué es. La acción principal es
   «Invitar a alguien», no ponerse al día. Es la pantalla de dinero con la jerarquía rota.
   Arreglo: tarjeta de estado como protagonista, con la cifra etiquetada en grande, el chip de
   estado y la restricción como línea propia. Si existe la ruta, el pago va en urgencia
   naranja (A2) e «Invitar» baja a secundaria.
2. 🔥🔥 **Las pantallas de estado son un reloj de arena sobre una hoja vacía.** En
   verificación (en revisión, pendiente, rechazada, aprobada) y bienvenida, el texto queda
   arriba a la izquierda, hay un vacío de unos 400 px y el botón queda abajo. No dicen dónde
   está el trámite.
   Arreglo: una composición común de estado. Ilustración en un círculo tintado, título
   centrado, los pasos del trámite (Recibido → En revisión → Listo) con el actual marcado, y
   el número de caso en un chip.
3. 🔥🔥 **El perfil no tiene a nadie adentro.** No hay avatar, nombre ni estado de
   verificación. El «Correo» es una caja gris vacía que parece rota. «Dar de baja tu cuenta»
   es un botón rojo a todo el ancho, al mismo nivel que todo lo demás: la acción irreversible
   es la segunda más visible de la pantalla.
   Arreglo: cabecera de identidad (avatar con iniciales, nombre y chip de verificación) y
   lista agrupada estilo iOS (Seguridad, Ayuda, Sesión). «Dar de baja» va como fila de texto
   roja al final, detrás de una confirmación.

## Crítica completa

- 🔥🔥 **Inicio, «Tus pasanakus» vacío**: un recuadro con una oración. Un estado vacío es la
  oportunidad de vender la primera acción (`mobile-app-ui-design`). Va como tarjeta con
  ilustración y la acción «Crear un grupo» o «Unirme con un enlace».
- 🔥 **Inicio, cabecera**: escanear y avisos son dos baldosas cuadradas pegadas. Van como
  círculos separados, del mismo lenguaje que las acciones.
- 🔥🔥 **Inicio, barra de pestañas**: es la vieja, y a 360 px dice «Movimien…». Está planificada
  como barra flotante con la pestaña QR central (A6).
- 🔥🔥 **Ingreso**: es un formulario sin un momento de marca. El logo queda chico arriba, la
  custodia es una nota al pie y «Ingresar» deshabilitado se ve gris claro sobre gris claro.
  Arreglo: banda verde arriba con el saludo y el logo, y los campos en una hoja que se monta
  encima.
- 🔥 **Portada**: el ícono de la app está sobre una sombra en bloque y las tres garantías son
  píldoras centradas, apiladas una bajo otra (lo genérico del punto 8). El elemento propio ya
  existe y es la tarjeta héroe verde: la portada debería mostrarla.
- 🔥🔥 **Ayuda**: cuatro controles de filtro apilados (buscador, select, cuatro chips en dos
  filas) antes del primer tutorial, y cada tarjeta repite un primario a todo el ancho (G1).
  Arreglo: buscador y una fila de chips con desplazamiento; acción por tarjeta como enlace.
- 🔥 **Alta**: la etiqueta de fase en mayúsculas, la barra de progreso de una línea y las
  secciones con ícono de 12 px hacen ver la pantalla como un formulario administrativo.
  Arreglo: progreso en cuatro segmentos y títulos de sección con jerarquía real.
- 🔥 **Consistencia**: todo es tarjeta blanca con el mismo radio y la misma sombra. Falta una
  segunda superficie: la banda o tarjeta verde para lo protagonista de cada pestaña.

## Lo que sí funciona (proteger)

- La tarjeta héroe verde del saldo: es el elemento propio de la app. Hay que extenderlo, no
  diluirlo.
- La voz, rioplatense y honesta («Ningún tutorial mueve tu plata ni confirma nada por vos»).
- La garantía de custodia como frase entera.
- Bricolage en los títulos, que da carácter sin gritar.

## Orden de arreglo (impacto / esfuerzo)

1. Mi estado: tarjeta de estado con la cifra etiquetada, y urgencia si hay ruta de pago.
2. Inicio: estado vacío de pasanakus con acción, cabecera en círculos y barra flotante con QR.
3. Perfil: cabecera de identidad, lista agrupada y «Dar de baja» abajo.
4. Pantallas de estado (verificación ×5, bienvenida): una composición común con pasos.
5. Ingreso: banda de marca con una hoja encima.
6. Portada: muestra de la tarjeta héroe y garantías en fila.
7. Ayuda: filtros compactos y acción por tarjeta como enlace.
8. Alta: progreso por segmentos y jerarquía de secciones.
