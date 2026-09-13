---
tags:
  - plan
  - frontend
  - diseno
  - refactor
titulo: "Refactorización visual — de Material de fábrica a AportaYa"
fecha: 2026-09-12
depende_de: [F0-M, F1-M, 20, 22]
afecta: [packages/tokens, packages/diseno_flutter, packages/ui, apps/movil, apps/web, apps/backoffice]
estado: en curso · V0 y V1 implementados, V2 con dos pantallas piloto
---

> [!success] Estado al 2026-09-12
> **Hecho:** V0.1 (fuentes empaquetadas), V0.2 (escala tipográfica generada desde la
> bóveda), V0.3 (tema completo), V0.4 (`Rueda` + `ColoresDeTurno`), V0.5 (cabeceras,
> `OpcionSeleccionable`, `Pregunta`, `Contador`, `BarraDeAccion`, `Panel`,
> `EncabezadoDeSaldo`, `Boton` con `maxLineas`), V1 (shell sin cabecera propia),
> y V2 con dos pantallas piloto: `billetera/inicio` y `pasanaku/crear-grupo`.
>
> **Dirección adoptada:** «Rueda y aguayo» —la rueda de turnos como objeto que
> organiza la interfaz— **con la paleta de AportaYa sin tocar**. El verde sigue siendo
> estructura y el naranja acción; lo que cambió es la tipografía (Bricolage Grotesque
> + Instrument Sans, empaquetadas), la escala, el tema de componentes y la composición.
>
> **También hecho (V1.3, movimiento):** apertura de marca al arrancar (el isotipo se
> escribe y se acerca, estilo Netflix), importes y puntajes que cuentan hacia arriba,
> hundido más háptica al tocar un botón, transición al cambiar de pestaña, e íconos en
> las acciones del saldo.
>
> **Falta:** el resto de V2 (las otras 38 pantallas), V3 (verificación automática) y
> V4 (Angular).

---

## 2.3 bis · La entrada — portada, ingreso y shell

La app arrancaba en `/billetera/inicio`: quien la abría por primera vez caía en el
tablero de una cuenta que no era suya, con un saldo ya puesto, sin saber qué era esto
ni cómo entrar. Ahora hay tres capas bien separadas:

| Capa | Rutas | Dónde vive | Por qué |
| --- | --- | --- | --- |
| **Entrada** | `/portada`, `/ingreso` | **Fuera** del shell (`rutasDeEntrada`) | Sin sesión no hay nada que ofrecer en una barra de pestañas: cuatro destinos a los que todavía no se puede ir |
| **Alta** | `/identidad/registro` y sus pasos | Dentro de la rama de identidad | Es un flujo con pasos, no un destino |
| **App** | las tres ramas del shell | Dentro del shell | Solo con sesión |

La **portada** (`PantallaDePortada`) explica el producto en los términos en que la
gente ya lo conoce —el pasanaku de toda la vida— y responde la única pregunta que
importa antes de pedir datos: *¿por qué te daría mi plata?* Tres promesas, ninguna de
rendimiento: **tu rueda a la vista**, **el orden se sortea y se verifica**, **la plata
la custodia el banco, no la empresa**. La `Rueda` es el héroe, no un ícono: quien ya
hizo un pasanaku la reconoce antes de leer una palabra.

**Bug de raíz que esto destapó:** `Guardias.exigirSesion` redirigía a
`/identidad/ingreso`, una ruta **que nunca existió**. Quien perdía la sesión no
llegaba al login sino a «ruta no encontrada». Ahora apunta a `/ingreso`, que existe y
está fuera del shell.

### El ingreso habla con el backend (CU-04)

El login era una fachada: guardaba el teléfono en memoria, navegaba a MFA, y el MFA
aceptaba cualquier código. Se podía «entrar» a AportaYa sin que la cuenta existiera.
El cliente Dart de identidad estaba generado y en el `pubspec` desde el principio —
solo que nadie lo llamaba.

Ahora `cu04_autenticar.dart` hace `POST /sesiones` de verdad:

| Regla | Por qué |
| --- | --- |
| **El servidor decide si hace falta MFA**, nunca la pantalla | Invariante 7. El mismo endpoint atiende los dos pasos, así el backend puede pedir un factor cuando quiera —dispositivo nuevo, monto alto— sin que la app lo adivine |
| **Sin token no hay sesión** | Si el servidor responde «no hace falta otro factor» pero no manda `tokenAcceso`, es un error, no un ingreso. Entrar a medias es peor que no entrar |
| **La app no muestra el mensaje del backend** | El texto que lee una persona sale del catálogo de `dominio/errores.dart`, en voz de marca |
| **`huellaDispositivo` es de la instalación, no de la persona** | Se genera al azar y vive en el almacén seguro. No sale de un IMEI ni de nada que siga a alguien entre apps, y muere al desinstalar. Es estable entre intentos, que es lo que permite «confiar en este dispositivo» |
| **El arranque pregunta por la sesión una sola vez** | Mientras la marca se acerca. Preguntarlo en cada navegación sería tocar el llavero del teléfono a cada paso. Si leer el llavero falla, se va a la portada: se pide ingresar de nuevo, nunca al revés |

Y se agregó **cerrar sesión** al perfil, que no existía: se podía entrar y no salir.

---

## 2.4 · Movimiento — la regla que lo gobierna todo

> Esta app muestra el dinero de personas que en muchos casos no confían del todo en
> que una app tenga su plata. **Toda animación se diseña contra el peor malentendido
> que puede provocar**, no contra lo bonita que se ve.

| Animación | Qué aporta | Qué podía salir mal | Cómo se evita |
| --- | --- | --- | --- |
| **Apertura de marca** (`AperturaDeMarca`) | Da identidad al arranque | Que alguien no pueda entrar a su billetera porque el logo se colgó | Va **encima** de la app ya armada (no la demora); dura 1,4 s; se salta tocando; temporizador de respaldo a los 2 s; con «reducir movimiento» no aparece |
| **Importe que sube** (`MontoQueSube`) | El saldo se siente vivo | Ver «Bs 0,00» y creer que perdió la plata | Termina exacto; el reposo es siempre el importe real; refrescar el mismo saldo no reanima; al cambiar, cuenta **desde el anterior**, nunca desde cero; el lector de pantalla anuncia el final desde el primer frame; 650 ms; respeta «reducir movimiento». Ocho pruebas, una por forma de fallar |
| **Puntaje que sube** (`NumeroQueSube`) | Se ve que subió | Que un puntaje no numérico rompa la pantalla | Si el contrato trae algo que no es entero, se muestra tal cual: el adorno nunca tapa el dato |
| **Hundido al tocar** (`HundidoAlTocar`) | El botón acusa recibo | Que se coma el toque | `Listener`, no `GestureDetector`: no compite en la arena de gestos |
| **Cambio de pestaña** | Se entiende que uno se movió | Demorar la navegación | 180 ms, menos que sacar el dedo |

La aritmética de los importes va **siempre en centavos enteros** (`centavosDe`,
`montoDesdeCentavos`): interpolar en coma flotante haría aparecer un
`Bs 2.000,0000000002` a mitad de la cuenta.

# Refactorización visual · de Material de fábrica a AportaYa

> **Qué es este documento.** El diagnóstico de por qué la app se ve «básica» aunque
> pantalla por pantalla *tenga* el contenido de la maqueta, y el plan por fases para
> corregirlo de raíz — no parche por pantalla. Se escribe después de dos rondas de
> «ya se parece» que no se parecían: la lección es que el problema no está en las
> pantallas sino **debajo** de ellas.

---

## 0 · Resumen en cinco líneas

1. **Ninguna fuente está empaquetada.** `Fuente.display = 'Poppins'` y `cuerpo = 'Inter'`
   existen como *nombres*, pero no hay `.ttf`, ni `fonts:` en ningún `pubspec.yaml`, ni
   `@font-face` ni enlace a Google Fonts en Angular. Los tres frontends dibujan **todo**
   con la fuente del sistema (SF Pro en iOS, Roboto en Android, lo que haya en el
   navegador). La maqueta es Poppins + Inter. Solo esto ya explica la mitad de la distancia.
2. **El tema son 30 líneas.** `packages/diseno_flutter/lib/tema.dart` fija colores y
   `fontFamily`, y nada más: sin `textTheme` (no hay escala tipográfica), sin temas de
   componente (`InputDecorationTheme`, `RadioThemeData`, `NavigationBarTheme`,
   `SnackBarTheme`, `CardTheme`, `BottomSheetTheme`, `PageTransitionsTheme`…). Cada
   `Radio`, `TextField`, `AppBar` y `SnackBar` sale como Material 3 de fábrica.
3. **La escala tipográfica no existe como token.** `tokens.json` guarda familia, colores,
   espacios y radios; no tamaños, pesos, interlineado ni tracking. La maqueta sí los fija
   (`h1 28–40/700`, `h2 24–34/700`, `h3 18/600`, cuerpo `15/1.55`, botón `14/600 display`,
   etiqueta de campo `13/600`, ayuda `12`, saldo `38/600 −.03em`). Sin ese token, cada
   pantalla adivina.
4. **Doble cabecera.** El shell pone un `AppBar` con «AportaYa» + campana, y cada pantalla
   pone *otro* título centrado debajo («Organizar un grupo», «Tu billetera»). La maqueta
   tiene una sola cabecera por pantalla.
5. **Las pantallas son listas planas de widgets.** Sin agrupación en tarjetas o secciones,
   sin jerarquía entre bloques, formularios con radios crudos y campos sin adorno,
   sin ningún estado de interacción cuidado. La maqueta agrupa todo en superficies
   (`.card`, `.saldo`, `.opcion`) con sombra y radio.

Todo lo anterior es transversal: arreglarlo en `tema.dart` + `tokens.json` +
`diseno_flutter` arregla las 40 pantallas a la vez. Arreglarlo pantalla por pantalla
—lo que se venía haciendo— no converge nunca.

---

## 1 · Diagnóstico con evidencia

| Síntoma visible | Causa raíz | Dónde | Alcance |
| --- | --- | --- | --- |
| Tipografía genérica de sistema en toda la app | Fuentes declaradas por nombre, nunca empaquetadas | `packages/diseno_flutter/pubspec.yaml` (sin `fonts:`), sin `assets/`; Angular sin `@font-face` | Los tres frontends |
| Títulos, cifras y etiquetas sin proporción entre sí | `ThemeData` sin `textTheme`; escala ausente en `tokens.json` | `tema.dart:22`, `tokens.json → primitivas.fuente` | Flutter (Angular la tiene en `estilos.css`) |
| Radios, checkboxes, campos, snackbars, tab bar «de Android» | Sin temas de componente en `ThemeData` | `tema.dart` | Flutter |
| «AportaYa» arriba y título de pantalla debajo | `ShellPrincipal` tiene `AppBar`; las pantallas también | `navegacion/shell.dart:62`, 28 pantallas con `AppBar(` | Flutter |
| Formularios crudos (ej. *Organizar un grupo*) | `GrupoRadio` = `RadioListTile` de fábrica; sin secciones ni tarjetas | `atomos/grupo_radio.dart`, `pantalla_crear_grupo.dart` | Flutter |
| La tarjeta de saldo del DS y la de la pantalla son dos cosas | `moleculas/tarjeta_saldo.dart` (plana, sin uso) vs `pantallas/billetera/tarjeta_de_saldo.dart` (degradado, la buena) | ambos | Flutter |
| Botón que corta a una línea | `Boton` fuerza `maxLines: 1` | `atomos/boton.dart` | Flutter |
| Estados de carga/vacío/error correctos pero sosos | `EstadoDePantalla` sin esqueletos, sin ilustración, sin ritmo | `organismos/estado_de_pantalla.dart` | Flutter |
| Transiciones y feedback táctil de fábrica | Sin `PageTransitionsTheme`, sin háptica, sin motion propio | `tema.dart`, shell | Flutter |

**Regla de verificación que se rompió dos veces y queda fijada:** una pantalla «se parece
a la maqueta» solo cuando hay una captura del simulador y un render real de la maqueta
**en el mismo tema** (claro con claro, oscuro con oscuro), lado a lado, y la diferencia
es de detalle. Comparar código contra código no vale. Comparar temas distintos no vale.

---

## 2 · El plan de diseño (lo que se decide una vez)

### 2.1 · Color — se conserva la marca, se usa mejor

> **Decisión del dueño del producto (2026-09-12): la paleta de AportaYa no se toca.**
> Verde `g600` estructura, naranja `o500` acción, crema de fondo, y los semánticos tal
> como están en la bóveda. Los «colores de aguayo» que distinguen personas dentro de
> una rueda salen de esa misma paleta (`ColoresDeTurno`), no de una nueva.

La paleta no es el problema: **verde `g600` = estructura, naranja `o500` = acción, un
solo botón naranja por pantalla** es la regla de oro documentada en
`docs/Views/Sistema-Diseno/README.md` y ya está en `tokens.json` con contraste AA
verificado. Lo que falta es *usarla con jerarquía*:

| Rol | Claro | Uso que hoy no se hace |
| --- | --- | --- |
| Fondo de página | crema `#F6F4EC` | ✔ ya |
| Superficie de tarjeta | blanco `#FFFFFF` + `sombra1` | casi nada agrupa en tarjetas |
| Superficie hundida (campo, chip, opción no elegida) | nube `#F3F6F2` | los campos van sobre crema con borde gris |
| Estructura fuerte (tarjeta de saldo, cabecera de detalle) | degradado `g500→g600→g800` | solo en `TarjetaDeSaldo` |
| Acción única | `o500` sobre `#3A1E02` | ✔ pero botones sin peso tipográfico |
| Tinta / pizarra / musgo | `#10231A` / `#38473F` / `#647169` | títulos y cuerpo salen del mismo gris |

> Si más adelante se decide cambiar la paleta de verdad, el lugar es
> `docs/Views/Sistema-Diseno/estilos.css` (la bóveda) → `tokens.json` → generadores. No
> se toca desde una pantalla. Este plan **no** cambia la paleta.

### 2.2 · Tipografía — Bricolage Grotesque para lo que se mira, Instrument Sans para lo que se lee

> **Implementado.** Poppins e Inter estaban nombradas en los tokens pero nunca
> empaquetadas, así que la app dibujaba con la fuente del sistema. Se reemplazan por
> Bricolage Grotesque (display y cifras: tiene el ancho y el contraste que una cifra
> grande necesita) e Instrument Sans (cuerpo), las dos OFL, instanciadas a pesos
> estáticos con `fontTools` y empaquetadas en `packages/diseno_flutter/lib/fuentes/`.
> Se declaran en el `pubspec` del paquete **y** en el de la app: una familia declarada
> solo por el paquete se registra como `packages/aportaya_diseno/Bricolage` y
> `Tipo` la pide por su nombre pelado.

Se empaquetan (licencia OFL) en `packages/diseno_flutter/assets/fuentes/` y se declaran
en su `pubspec.yaml` para que la app las herede; en Angular se auto-hospedan en
`packages/ui/src/fuentes/` con `@font-face` (`font-display: swap`). Sin Google Fonts en
tiempo de ejecución: una billetera no depende de un CDN para ser legible.

Escala nueva en `tokens.json → primitivas.tipo` (nombres del sistema, no de Material):

| Token | Familia | Tamaño / interlineado / tracking / peso | Equivale en maqueta |
| --- | --- | --- | --- |
| `cifraGrande` | Poppins | 38 / 1.0 / −0.03em / 600 · `tabular-nums` | `.saldo__m` |
| `cifra` | Poppins | 22 / 1.1 / −0.02em / 600 · tabular | `.pin input`, KPIs |
| `titulo1` | Poppins | 28 / 1.15 / −0.02em / 700 | `h1` móvil |
| `titulo2` | Poppins | 22 / 1.2 / −0.02em / 700 | `h2` móvil |
| `titulo3` | Poppins | 18 / 1.25 / −0.01em / 600 | `h3` |
| `boton` | Poppins | 14 / 1.0 / 0 / 600 | `.btn` |
| `etiqueta` | Poppins | 11.5 / 1.2 / +0.07em / 600 | `.saldo__l`, `.badge` |
| `cuerpo` | Inter | 15 / 1.55 / 0 / 400 | `body` |
| `cuerpoChico` | Inter | 13 / 1.45 / 0 / 400 | `.help`, `.chip` |
| `campoEtiqueta` | Inter | 13 / 1.3 / 0 / 600 | `.field-label` |
| `ayuda` | Inter | 12 / 1.4 / 0 / 400 | `.help` |
| `mono` | JetBrains Mono | 12 / 1.4 / +0.04em / 500 | `.hash`, `.codigo` |

`a-dart.mjs` genera una clase `Tipo` con `TextStyle` por token, y `tema.dart` arma el
`TextTheme` de Material mapeando (`displayMedium ← cifraGrande`, `headlineSmall ← titulo1`,
`titleLarge ← titulo2`, `titleMedium ← titulo3`, `labelLarge ← boton`, `labelSmall ←
etiqueta`, `bodyLarge/Medium ← cuerpo`, `bodySmall ← cuerpoChico`). Así los widgets que
ya usan `texto.titleMedium` mejoran sin tocarlos; los nuevos piden `Tipo.of(context).cifra`.

Regla de escritura que se aplica de paso: **sin mayúsculas sostenidas salvo `etiqueta`**
(la maqueta las usa solo ahí); nada de «WORD — fragmento»; títulos en frase
(«Organizar un grupo», no «ORGANIZAR UN GRUPO»).

### 2.3 · Composición — una cabecera, superficies, ritmo

```
┌──────────────────────────────┐
│ Hola, Pablo            🔔 ●  │  ← cabecera propia de la pantalla (no del shell)
│ Tu billetera                 │     saludo pequeño en Inter, título en Poppins
├──────────────────────────────┤
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐  │  ← AccionesRapidas: cuadrados nube, ícono verde
│ │ ↓  │ │ ↑  │ │ 🏦 │ │ 🎟 │  │
│ └────┘ └────┘ └────┘ └────┘  │
│ Recargar Aportar Retirar Vales│
├──────────────────────────────┤
│ ╔══════════════════════════╗ │  ← TarjetaSaldo (degradado + manchas), r-xl
│ ║ ✓ SALDO DISPONIBLE   ◌   ║ │
│ ║ Bs 1.240,00              ║ │     cifraGrande, tabular
│ ║ En custodia de …         ║ │
│ ║ [Ver aportes pend.][Mov.]║ │     una acción naranja, una fantasma
│ ║ RETENIDO   PUESTO EN PAS.║ │
│ ╚══════════════════════════╝ │
├──────────────────────────────┤
│ Próximo aporte          Ver › │  ← sección: título3 + enlace
│ ┌──────────────────────────┐ │
│ │ Grupo Vecinos · 15 sep   │ │  ← tarjeta blanca, sombra1, r-lg
│ │ Bs 200,00        [Aportar]│ │
│ └──────────────────────────┘ │
│ Movimientos recientes   Ver › │
│ ┌──────────────────────────┐ │
│ │ ↓ Recarga QR   +Bs 500,00│ │  ← FilaDeMovimiento dentro de tarjeta
│ │ ↑ Aporte       −Bs 200,00│ │
│ └──────────────────────────┘ │
├──────────────────────────────┤
│  ⌂ Inicio  ☷ Grupos  ⇄ Mov ◯ │  ← tab bar: activo píldora g100 + ícono g600
└──────────────────────────────┘
```

Principios que valen para las 40 pantallas:

- **Una cabecera por pantalla.** El shell deja de dibujar `AppBar`; cada pantalla de nivel
  superior trae la suya (`CabeceraDeInicio` con saludo + campana; `CabeceraDeSeccion`
  con volver + título + acción para las interiores). Alineación a la izquierda, no centrada.
- **Todo bloque de contenido vive en una superficie.** Tarjeta blanca con `sombra1` y
  `r-lg` para grupos de información; nube `surface2` para lo interactivo no elegido;
  degradado verde solo para la cabecera de dinero. Nada suelto sobre la crema salvo
  títulos de sección.
- **Formularios por secciones.** Cada pregunta es una tarjeta: etiqueta arriba
  (`campoEtiqueta`), control debajo, ayuda debajo del control. Las opciones excluyentes
  (`Semanal / Quincenal / Mensual`) son **tarjetas seleccionables** (`OpcionSeleccionable`,
  la `.opcion` de la maqueta: borde `fieldBorder`, elegida → borde `g600` + fondo `g100`
  + check), no `RadioListTile`.
- **Ritmo vertical fijo:** `s5` entre secciones, `s3` entre tarjetas de la misma sección,
  `s2` dentro de una tarjeta. Márgenes laterales `s4`.
- **Una sola acción naranja visible** por pantalla; el resto, verde o fantasma. Si hay
  acción principal de formulario, va **anclada abajo** (`BarraDeAccion` sobre `surface`,
  borde superior, safe-area), no perdida al final del scroll.
- **Movimiento solo cuando responde a la persona:** transición de página deslizante
  iOS/desvanecida Android (`PageTransitionsTheme`), hoja inferior que sube, check que se
  dibuja al confirmar, esqueleto que respira mientras carga. Ninguna animación de
  entrada «porque sí». `reduceMotion` respetado.
- **Estados feos con la misma calidad que el bueno:** vacío con ilustración de trazo
  (`marca-trazo`) + una frase + una acción; error con qué pasó y qué hacer; carga con
  esqueleto de la *forma* de la pantalla, no un girador centrado.

---

## 3 · Fases

Cada fase cierra con el gate del repo (`lint`, `typecheck`, `test:front`, `test:a11y`,
goldens revisados a mano) **y** con la comparación lado a lado en el mismo tema. Ninguna
fase toca una pantalla antes de que la fase de abajo esté cerrada, porque cada una
mejora las de arriba gratis.

### V0 · Cimientos (`packages/tokens` + `packages/diseno_flutter` + `packages/ui`) — micro-PR a paquetes congelados

| # | Trabajo | Entrega |
| --- | --- | --- |
| V0.1 | Empaquetar Poppins (400/500/600/700), Inter (400/500/600) y JetBrains Mono (500) en `diseno_flutter/assets/fuentes/` y `ui/src/fuentes/`; `pubspec.yaml` con `fonts:`; `@font-face` en `estilos.css`. Licencias OFL junto a los archivos. | La app muestra Poppins/Inter en el simulador; captura de prueba adjunta al PR |
| V0.2 | `tokens.json → primitivas.tipo` con la escala de §2.2; extender `a-dart.mjs` (clase `Tipo` + `TextStyle`) y `a-css.mjs` (custom properties `--t-*`); `pruebas/tokens-contra-boveda` cubre los nuevos valores contra `estilos.css` | `tokens.dart` y `tokens.css` regenerados, prueba en verde |
| V0.3 | `tema.dart` completo: `textTheme` desde `Tipo`; temas de `AppBar`, `NavigationBar`, `InputDecoration` (relleno `field`, borde `fieldBorder`, foco `g600` 2px, radio `md`, ayuda `ayuda`), `Radio/Checkbox/Switch` (`g600`), `ElevatedButton/OutlinedButton/TextButton` (forma `md`, altura `Tactil.minimo`, `boton`), `Card` (`surface`, `sombra1`, `lg`), `SnackBar` (flotante, `ink`, `md`), `BottomSheet` (`xl` arriba, asa), `Dialog`, `Chip`, `Divider`, `ListTile`, `ProgressIndicator`, `PageTransitionsTheme` (Cupertino en iOS, fade-through en Android), `splashFactory` discreta | Widgetbook (`lib/catalogo.dart`) muestra cada componente al lado de su captura del `Sistema-Diseno/*.html` |
| V0.4 | `Boton` con `maxLines` (2 por defecto en `primario`/`secundario` anchos); `BotonDeDosLineas` de la pantalla se borra | Golden del átomo con texto largo |
| V0.5 | `TarjetaSaldo` del DS absorbe la de la pantalla (degradado, manchas, `r-xl`, dos acciones, cifras); `pantallas/billetera/tarjeta_de_saldo.dart`, `mancha_radial.dart`, `cifra.dart` se mueven al paquete | Un solo widget, golden claro/oscuro |
| V0.6 | Nuevos: `OpcionSeleccionable` (la `.opcion`), `SeccionDeFormulario` (tarjeta con etiqueta/control/ayuda), `BarraDeAccion` (CTA anclado), `CabeceraDeInicio`, `CabeceraDeSeccion`, `EsqueletoDePantalla` (formas por tipo: lista, tarjeta, formulario), `IlustracionDeEstado` (trazo, 4 motivos) | Goldens por componente |

### V1 · Shell (`apps/movil/lib/navegacion/`)

| # | Trabajo |
| --- | --- |
| V1.1 | `ShellPrincipal` sin `AppBar`; la campana pasa a `CabeceraDeInicio` de cada rama (Inicio, Grupos, Perfil) con el `Badge` de `noLeidasProvider` |
| V1.2 | `BarraPestanas` al estilo maqueta: píldora `g100` detrás del activo, ícono `g600`, etiqueta `cuerpoChico` 600, altura 64 + safe-area, sin sombra superior, borde `border` de 1px |
| V1.3 | Transiciones por plataforma desde el tema; `HapticFeedback.selectionClick` al cambiar de pestaña; `lightImpact` al confirmar dinero |
| V1.4 | `errorBuilder` del enrutador con `IlustracionDeEstado` (nunca pantalla blanca ni rojo de Flutter) |

### V2 · Pantallas por dominio (`apps/movil/lib/pantallas/`), en este orden

Cada pantalla se rehace **componiendo** V0, no dibujando. Criterio de cierre por pantalla:
captura simulador claro + oscuro junto al render de la maqueta claro + oscuro (y del
`Moviles.html` cuando aplica), más el estado adverso.

| Orden | Dominio · pantalla | Qué cambia sobre todo |
| --- | --- | --- |
| 1 | **billetera/inicio** | Cabecera propia, secciones «Próximo aporte» y «Movimientos recientes» (hoy no existen; la maqueta las tiene debajo de la tarjeta), `TarjetaSaldo` del DS |
| 2 | billetera/extracto | Lista dentro de tarjetas agrupadas por día, filtros como chips píldora, resumen de período arriba |
| 3 | billetera/recargar · retirar · transferir | `TecladoNumerico` del DS a pantalla completa, monto en `cifraGrande`, `BarraDeAccion`, hoja de confirmación `xl` |
| 4 | **pasanaku/crear_grupo** | `SeccionDeFormulario` + `OpcionSeleccionable` para frecuencia y orden de turnos; `BarraDePasos` arriba; CTA anclado |
| 5 | pasanaku/mi_estado · turno · aportar | `RielDeTurnos` protagonista, `CalendarioDeCuotas` en tarjeta, `EstadoDeMora` como `Alerta` no como texto |
| 6 | pasanaku/pedir_cupo · invitar · permuta · retiro · reclamo | Formularios por secciones; hoja de confirmación para lo irreversible |
| 7 | pasanaku/transparencia · verificar_sorteo · mi_puntaje | `PanelSorteo`, `MedidorDeRango`, hash en `mono` dentro de tarjeta copiable |
| 8 | identidad/bienvenida · tour · registro (5 pasos) · mfa · sesion | `PanelBienvenida` con ilustración de trazo, `BarraDePasos`, `CampoOTP` grande, biometría como opción con ícono |
| 9 | identidad/perfil · dispositivos · contrato · baja | Lista en tarjetas con `ListTile` temado; contrato con barra de progreso de lectura |
| 10 | notificaciones/bandeja | `ItemDeNotificacion` en tarjeta por día, no leídas con punto `o500` |

### V3 · Verificación continua

| # | Trabajo |
| --- | --- |
| V3.1 | `scripts/comparar_maqueta.py`: sirve la maqueta con `<meta charset>`, la renderiza con Playwright en claro y oscuro a 393×852, captura el simulador con `simctl` en los dos temas, y arma una hoja de contacto `informes/visual/<pantalla>.png` (maqueta | app | diff resaltado). Falla si el diff estructural supera un umbral; el humano decide con la hoja a la vista |
| V3.2 | Golden por pantalla × tema × estado (éxito, vacío, error, carga) en `apps/movil/test/goldens/`; regla: un golden no se actualiza sin mirar el diff |
| V3.3 | `verificar_frontend.py` suma: ninguna pantalla con `AppBar(` fuera de `CabeceraDeSeccion`; ningún `RadioListTile`/`Radio(` fuera del DS; ningún `TextStyle(fontSize:` fuera de `tokens.dart`; ningún `Colors.`/`Color(0x` (ya está). **La regla del `AppBar` se escribe cuando V2 cierre**: hoy fallaría en las 26 pantallas que todavía no se rehicieron, y un gate que falla siempre deja de leerse |
| V3.4 | Una pasada `test:a11y` por pantalla con `TextScaler` 1.3 y 2.0 (la maqueta se lee a 200 %; la app debe también) |

### V4 · Angular (`apps/web`, `apps/backoffice`) — después de que Flutter cierre

Mismas fuentes auto-hospedadas (V0.1), misma escala `--t-*` (V0.2), y una pasada por
`packages/ui` para que cada organismo compartido (`Monto`, `Vale`, `RielDeTurnos`,
`VerificadorDeSorteo`…) lea la escala en vez de `1.3em` a mano. Las pantallas de
Angular se comparan contra el `AportaYa-Maqueta.html` de escritorio con el mismo script.

---

## 4 · Reglas del proceso

- **`packages/tokens` y `packages/diseno_flutter` están congelados**: V0 entra como
  micro-PRs con ficha (qué, por qué, captura antes/después), uno por fila de la tabla, en
  ramas `pablo/feature/V0.n-…`. Nadie los edita desde una pantalla.
- **Mismo tema o no cuenta.** Toda captura de verificación dice en el nombre el tema
  (`…-claro.png`, `…-oscuro.png`) y va junto a su par de la maqueta.
- **Se muestra, no se afirma.** Un PR de V2 sin la hoja de contacto de V3.1 no se revisa.
- **Nada de literales**: si un valor no existe como token, primero se agrega a
  `tokens.json` (y a la bóveda si tampoco está ahí), después se usa.
- **200 líneas por archivo** se mantiene; V0.6 existe en parte para que las pantallas
  compongan y no crezcan.
- **Android espera.** Hasta que iOS cierre V2.1–V2.4, no se abre el emulador Android.

---

## 5 · Orden de ataque propuesto y primer corte

**Semana 1 — V0.1, V0.2, V0.3, V1.1, V1.2.** Solo con esto, sin tocar ninguna pantalla,
la app cambia de fuente, de escala, de campos, de radios, de tab bar y pierde la doble
cabecera. Es el mayor salto visual por hora invertida y es donde se decide si el
rumbo es el correcto **antes** de rehacer 40 pantallas.

**Semana 2 — V0.4–V0.6 + V2.1 (billetera/inicio) + V2.4 (crear grupo)** como las dos
pantallas piloto: una de lectura con dinero, una de formulario. Si esas dos pasan la
hoja de contacto, el resto de V2 es mecánico.

**Semanas 3–4 — resto de V2 en el orden de la tabla, V3 en paralelo.**

**Después — V4 Angular.**

---

## 6 · Decisiones abiertas (las toma el dueño del producto)

| # | Pregunta | Recomendación |
| --- | --- | --- |
| D1 | ¿Se mantiene la paleta verde/naranja de la bóveda o se rediseña? | **Mantener.** Es la marca, tiene contraste verificado y la maqueta entera está pintada con ella. Si se cambia, es un proyecto aparte que empieza en `estilos.css`, no acá |
| D2 | ¿Fuentes empaquetadas o `google_fonts` en tiempo de ejecución? | **Empaquetadas.** Offline-first, sin flash de fuente, sin dependencia de red para leer un saldo |
| D3 | ¿Cabecera del shell se elimina (cada pantalla la suya) o se vuelve `SliverAppBar` grande compartida? | **Eliminar del shell.** Es lo que hace la maqueta y da libertad a Inicio (saludo) vs. interiores (volver + título) |
| D4 | ¿Se acomete Angular en paralelo o después? | **Después.** Un solo frente visual a la vez; V0.1/V0.2 ya dejan la base lista |
