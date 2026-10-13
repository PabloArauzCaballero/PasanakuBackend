---
name: pasanaku-diseno
description: El sistema visual de AportaYa (Pasanaku) en la app Flutter — dónde vive cada pieza (tokens generados desde packages/tokens, átomos/moléculas/organismos/móviles de packages/diseno_flutter, pantallas en apps/movil), qué gate lo mide y qué NO hay que reinventar. Punto de entrada de cualquier trabajo visual en la app móvil. Usar antes de tocar un color, un espaciado, una tipografía, un componente o una pantalla; traduce al stack real las reglas de la skill de Atlas `atlas-diseno`.
---

# El sistema visual de AportaYa (app Flutter)

Antes de nada: **AportaYa ya tiene sistema de diseño, y el gate lo mide.** El error caro no es elegir
mal un gris, es inventar un token que ya existe, pintar con un valor suelto o armar un control que ya
es un átomo. Esta skill dice dónde está cada pieza. Los fundamentos (jerarquía, composición, color,
tipografía) están en `frontend-ui-design`, `visual-hierarchy-composition`, `color-systems` y
`typography-systems`; el gate de calidad visual, en `ui-quality-review`; el pulido, en
`frontend-beautiful-ui` y `emil-design-eng`.

## 1. El stack real

| Pieza | Dónde | Qué es |
|---|---|---|
| App del participante | `apps/movil` (`aportaya_movil`) | Flutter 3.44 · Riverpod 3 · go_router 17 · Dio 5 |
| Sistema de diseño | `packages/diseno_flutter` (`aportaya_diseno`) | Tema, tokens, átomos, moléculas, organismos, piezas móviles, ilustraciones |
| Fuente de los tokens | `packages/tokens` (Node) | Genera `packages/diseno_flutter/lib/tokens/tokens.dart` con `scripts/a-dart.mjs`. **`tokens.dart` no se edita a mano** y no está versionado |
| Fuentes | `packages/diseno_flutter/lib/fuentes/` | `Bricolage` (títulos, 600/700/800) e `Instrument` (texto, 400/500/600), empaquetadas: una billetera tiene que leerse sin señal |

No hay Material «de fábrica» en las pantallas: el tema sale de `temaDesde(Tokens.claro|oscuro, …)`
(`tema.dart`). Un `Colors.green` en una pantalla es un defecto.

## 2. Los tokens, por nombre

- **Paleta cruda** (`Paleta.*`): `g900…g100` (verdes de marca), `o700…o100` (acento), `ink`, `slate`,
  `muted`, `line`, `cloud`, `crema`, `white`, y los de estado `ok/okbg`, `warn/warnbg`, `err/errbg`,
  `info/infobg`. **En pantallas no se usa la paleta cruda**: se usan los roles.
- **Roles por tema** (`Tokens.of(context)`): `bg`, `surface`, `surface2`, `text`, `text2`, `text3`,
  `border`, `brand`, `brandInk`, `brandBg`, `brandTexto`, `accent`, `accentInk`, `accentTexto`,
  `field`, `fieldBorder`, `verdeSolido`/`sobreVerdeSolido`, `rojoSolido`/`sobreRojoSolido`, y los de
  estado `ok/okBg/okTexto`, `warn/warnBg/avisoTexto`, `err/errBg/errTexto`, `info/infoBg/infoTexto`.
- **Espacio** (`Espacio.s1…s7`), **radios** (`Radios.sm, md, lg, xl, pill`), **bordes**
  (`Borde.fino, foco, desfase`), **toque** (`Tactil.minimo` = 48).
- **Tipografía** (`Tipo.*`): `display`, `cifraGrande`, `cifra`, `titulo1…3`, `boton`, `etiqueta`,
  `cuerpo`, `cuerpoFuerte`, `cuerpoChico`, `campoEtiqueta`, `ayuda`.
- **Movimiento**: `Movimiento.*` (`atomos/movimiento.dart`:
  `micro` 120, `entrada` 220, `pagina` 300, `paginaDeVuelta` 240, curvas `llega`, `sale`, `pareja`,
  `resorte`). Ver `pasanaku-movimiento`.

Roles que no se confunden (misma lógica que Atlas):
- `field` es el **hueco donde se escribe**; `surface` es una **tarjeta que se lee**. Un campo no es una
  tarjeta.
- `fieldBorder` es el contorno de un control (≥ 3:1 contra su fondo, WCAG 1.4.11); `border` separa
  filas. Usar `border` en un campo lo vuelve invisible; usar `fieldBorder` en un separador convierte
  una lista en una reja.
- El ejemplo de un campo (placeholder) va en `textoDeEjemplo` —más apagado que `text3`—: un ejemplo con
  la forma exacta del dato en el tono del texto se lee como un valor ya escrito.

**Un token nuevo se agrega en `packages/tokens`** (con su contraste medido en claro y oscuro) y se
regenera. Nunca un `Color(0x…)` en `apps/movil` ni en una pieza de `diseno_flutter` (regla 95.1.5).

## 3. Las piezas que ya existen (buscar antes de crear)

`atomos/`: `Boton` (+variantes), `BotonIcono`, `BotonFlotante`, `Campo`, `CampoBusqueda`,
`CampoContrasena`, `CampoMonto`, `CampoOTP`, `CampoCodigo`, `CampoDeSeleccion` (abre la hoja con
buscador), `Casilla`, `Interruptor`, `GrupoRadio`, `ChipEstado`, `ChipElegible`, `SelectorSegmentado`,
`Esqueleto`, `Girador`, `Progreso`, `Rueda`, `BarraDePuntos`, `Marca`, `Logotipo`, `PalabraDeMarca`,
`Monto`, `MontoQueSube`, `NumeroQueSube`, `HundidoAlTocar`, `LuzDeBoton`, `SuperficieViva`, `Avatar`,
`CodigoQR`, `CuentaEnmascarada`, `Fecha`, `Punto`, `EstrellasCalificacion`.
`moleculas/`: `Alerta`, `Tarjeta`, `Seccion`, `Cabecera*`, `BarraDeAccion`, `BarraDePasos`,
`CampoDeFecha`, `AparicionEscalonada`, `Acordeon`, `Pestanas`, `Pregunta`, `OpcionSeleccionable`,
`OpcionConCosto`, `FilaDeCotejo`, `FilaDeMovimiento`, `TarjetaSaldo`, `TarjetaKPI`, `RielDeTurnos`, …
`organismos/`: `EstadoError`, `EstadoVacio`, `EstadoDePantalla`, `EncabezadoDeSaldo`,
`ListaDeMovimientos`, `ListaDeRequisitos`, `PanelBienvenida`, `CalendarioDeCuotas`, …
`moviles/`: `AperturaDeMarca`, `TransicionDeMarca`, `TransicionDeEje`, `FondoDeMarca`, `PanelDeMarca`,
`HojaDeConfirmacion`, `NotificacionEmergente`, `TecladoNumerico`, `BarraPestanas`, `ComoEnUnTelefono`,
tutoriales (`GloboDeTutorial`, `FocoDeTutorial`, anclas).

El catálogo vivo está en `packages/diseno_flutter/lib/catalogo/` (widgetbook): **mirá ahí antes de
crear**.

## 4. Reglas de la casa que no son teoría

- **Selects**: solo `CampoDeSeleccion`; abre **siempre** una hoja con buscador que sube con el teclado
  (Pablo, 2026-10-11). El gate prohíbe `DropdownButton*`, `DropdownMenu` y `showDatePicker` sueltos.
- **Fechas**: solo `CampoDeFecha`.
- **200 líneas por archivo** en `apps/movil/lib` (gate). Un widget que crece se parte.
- **Sin `print`/`debugPrint`.** Sin datos de personas en logs (regla 90.2).
- **Voz**: español rioplatense con voseo, sin culpar a la persona, con la acción que corrige.
- **Una sola ayuda por campo**, junto a su etiqueta; dentro de la hoja de un select se repite.
- **Un botón apagado dice por qué** (debajo, como instrucción, no como alarma).
- **Sin capturas de pantalla** en ingreso, alta y capturas del carnet (`proteccion_pantalla`).

## 5. Antes de dar una pantalla por terminada

1. `dart analyze --fatal-infos`, `dart format`, `python3 scripts/verificar_frontend.py movil`.
2. `flutter test` dirigido + `test/a11y` + goldens si tocaste un átomo (regenerados **y mirados**).
3. Capturas reales a 360, 412 y 600+ dp, en claro y oscuro, en los estados relevantes (`visual-proof`).
4. Rúbrica `ui-quality-review` con las capturas delante, no describiendo la pantalla.

## Checklist

- [ ] Usé tokens existentes; si faltaba uno, lo agregué en `packages/tokens` con su contraste.
- [ ] Busqué la pieza en `diseno_flutter` (y en el catálogo) antes de crear una.
- [ ] Ningún color, espacio, radio, duración ni tipografía literal en la pantalla.
- [ ] Funciona en claro y oscuro; lo vi en capturas.
- [ ] Pasé el gate y la rúbrica con evidencia.
