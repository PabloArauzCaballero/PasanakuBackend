# Plan — Rediseño visual bancario de la app móvil (nivel «Meru»)

- Fecha: 2026-10-21 · Rama: `claude/rediseno-bancario` (desde `dev` @ `7752e1b`)
- Repos afectados: `PasanakuFrontend` (solo `apps/movil`, `packages/diseno_flutter`, `packages/tokens`,
  `docs/Views/Sistema-Diseno/estilos.css` y este directorio)
- Predecesores: `2026-10-15-visual-bancario` (identidad institucional), `2026-10-11-alta-atlas-sesion-premium`
  (alta y sesión), `2026-10-19-app-arreglos-del-telefono`
- **Resultado observable:** quien abre la app en el celular ve una app de banca digital premium.
  - El saldo es protagonista sobre una tarjeta con profundidad.
  - Las acciones rápidas son circulares, con iconografía coherente.
  - Los movimientos son legibles, con avatar de categoría.
  - La barra de pestañas es flotante y translúcida.
  - Las transiciones tienen física de resorte.
  - Los textos dan confianza.
  - Todo esto vale en las ~45 pantallas, en claro y oscuro, a 360/412/600 dp y en cada estado
    (cargando, datos, vacío, error).
- **Kill-test:** abrir la galería comparativa `evidencia/galeria.html`. Si una pantalla cualquiera del
  «después» puede confundirse con su «antes», o se ve como Material de fábrica (AppBar plano, tarjeta
  blanca con borde, `ListTile` crudo), el trabajo NO está hecho.

---

## 0. Cómo se trabaja (obligatorio, no negociable)

### 0.1 Capturas de TODO el trabajo (pedido explícito de la persona usuaria)

Cada pantalla y cada componente tocado se captura **antes** de tocarlo y **después** de tocarlo.
Sin la captura revisada la microtarea no pasa a `HECHO` (regla 95.7 y regla 30).

| Dimensión | Valores | Notas |
|---|---|---|
| Viewport | **360 × 800**, **412 × 915**, **600 × 960** dp | Teléfono chico, teléfono común y tableta chica. Los fija `pasanaku-diseno` §5 |
| Tema | **claro**, **oscuro** | Ambos, siempre (regla 95.5.4) |
| Estado | **cargando**, **datos**, **vacío**, **error**, más **sin permiso** y **sin conexión** donde existan | Regla 95.2 |
| Variante de estrés | **peor caso** (`break-ui`): nombre largo, monto de 9 dígitos, 0 ítems, 300 ítems | Una por pantalla con datos |
| Movimiento | **video MP4 o GIF** de cada animación nueva, más 3 cuadros clave (inicio, medio, fin) | Con y sin «reducir movimiento» |

**Convención de nombres:**

```
evidencia/<antes|despues>/<flujo>/<pantalla>__<estado>__<360|412|600>__<claro|oscuro>.png
evidencia/movimiento/<pieza>__<normal|reducido>.mp4
evidencia/movimiento/<pieza>__<t0|t50|t100>.png
evidencia/componentes/<antes|despues>/<nivel>/<componente>__<claro|oscuro>.png
```

**Tres mecanismos de captura** (de más reproducible a más real):

1. **Arnés de capturas en Flutter**, el principal. Es `apps/movil/test/capturas/rediseno/`:
   - Hace `testWidgets` de cada pantalla con `ProviderScope(overrides:)` y datos **sintéticos
     declarados** (regla 97.6).
   - Usa las **fuentes reales** cargadas (Instrument y Bricolage vía `FontLoader`), no la fuente de
     prueba en bloques de los goldens actuales.
   - Recorre `tester.view.physicalSize` por viewport y `ThemeMode` por tema, y escribe el PNG con
     `matchesGoldenFile` o `RepaintBoundary.toImage`.
   - Es determinista y re-ejecutable: el «antes» y el «después» salen del mismo arnés.
2. **App web real + Playwright:**
   - `flutter build web` contra el mock `yarn dev:mock`.
   - Recorrido navegado de verdad con Chromium (`/opt/pw-browsers`), 1 worker (regla 70.3).
   - Prueba que las rutas, el router y las transiciones funcionan juntas, no solo cada widget aislado.
   - Los videos de movimiento salen de acá (`recordVideo`).
3. **Teléfono real** (fuera del alcance de este contenedor): queda como verificación pendiente para
   la persona, con un checklist en el `REPORTE.md`.

**Galería comparativa** (publicada, privada: https://claude.ai/artifact/Qr6hRkXnRFJpDS4vzqXLK5): `evidencia/galeria.html` (las herramientas viven en `herramientas/` de este trabajo: `comprimir_capturas.py` cuantiza a 256 colores, 57 → 20 MB en el «antes») muestra el antes y el después lado a lado por
pantalla, con filtros por flujo, tema, viewport y estado. Se publica como página para revisarla
desde el celular. Se regenera con `scripts/galeria_capturas.mjs` (script nuevo, solo lee
`evidencia/`).

**Mirar cada captura es parte del DoD.** Por cada lote se escribe en `evidencia/revision-<lote>.md`
lo que se vio y lo que se corrigió (rúbrica `ui-quality-review`).

### 0.2 Skills que se leen antes de tocar código (AGENTS.md) y para qué

| Grupo | Skills | Uso en este trabajo |
|---|---|---|
| Casa, diseño Flutter | `pasanaku-diseno`, `pasanaku-ui-componentes`, `pasanaku-movimiento`, `pasanaku-app-movil-ux`, `flutter-theming`, `flutter-development`, `flutter-testing` | Dónde vive cada pieza, gates, límites de 200 líneas, sesión y teclado |
| Fundamentos visuales | `frontend-ui-design`, `visual-hierarchy-composition`, `color-systems`, `typography-systems`, `iconography-imagery`, `frontend-design-system`, `frontend-beautiful-ui`, `frontend-design`, `web-design-guidelines`, `improve-ui` | Jerarquía, paleta, tipografía, íconos, sombras en capas |
| Apple y pulido | **`apple-design`** (resortes, gestos, materiales, tipografía óptica), **`emil-design-eng`** (detalles invisibles), `mobile-native`, `mobile-ux-design` | Física de la interfaz, hojas, tab bar translúcida |
| Animación | **`animate`**, **`animation-vocabulary`**, **`find-animation-opportunities`**, **`improve-animations`**, **`review-animations`**, **`fixing-motion-performance`**, `frontend-motion` | Diseñar, nombrar, auditar y medir cada movimiento |
| Calidad y estados | `frontend-ux-states`, `frontend-forms-ux`, `frontend-accessibility`, `accessibility-testing`, `fixing-accessibility`, `break-ui`, `ui-quality-review`, `visual-proof`, `visual-regression-testing` | Cuatro estados, AA, peor caso, rúbrica, prueba visual |
| Redacción y crecimiento (instaladas hoy) | **`product-marketing`**, **`copywriting`**, **`copy-editing`**, **`marketing-psychology`**, **`onboarding`**, **`signup`**, **`cro`**, **`referrals`**, **`marketing-ideas`**, **`aso`** | Voz de marca, microcopy, activación, invitaciones, capturas de tienda |
| Proceso | `milestone-planning`, `evidence-and-verification`, `scope-discipline`, `rationalization-guard`, `work-report-md`, `finish-your-turn` | Plan, peldaños, reporte |

Cada checkpoint registra qué skills se leyeron (AGENTS.md).

### 0.3 Restricciones que el rediseño respeta

- **Cero literales** de color, espacio, radio, tipografía o duración en pantallas. Todo lo nuevo
  entra como token en `packages/tokens/tokens.json` + `docs/Views/Sistema-Diseno/estilos.css`
  (el test `tokens-contra-boveda` exige que coincidan), con su contraste medido en ambos temas.
- **Reusar antes que crear** (regla 95.1). Se re-pinta `Tarjeta`, `Boton`, `TarjetaSaldo`,
  `AccionesRapidas` y las demás. No se crean gemelas.
- **Dinero:** el rediseño **no toca** cálculos, DTOs ni formateo de importes. `Monto` se re-estiliza
  pero sigue usando decimal exacto y moneda (regla 91).
- **Datos de captura:** sintéticos y declarados como tales (por ejemplo «Ana Prueba», Bs 1.250,00).
  Ningún dato real en capturas (regla 90.2).
- **Movimiento:** nada disparado por la persona dura más de 320 ms, todo respeta reducir movimiento
  y se anima solo `transform`/`opacity` (`pasanaku-movimiento`).
- **200 líneas por archivo** en `apps/movil/lib`. El gate es `scripts/verificar_frontend.py`.
- **No se agrega ninguna dependencia** sin ADR (regla 95.1.4 y 90.4.2).

---

## 1. Hallazgos (fase 1, con ruta)

**Hechos:**
- **Tokens:**
  - La fuente es `packages/tokens/tokens.json`, espejada en `docs/Views/Sistema-Diseno/estilos.css`.
  - Se generan con `scripts/a-dart.mjs` y `a-css.mjs` y se copian a
    `packages/diseno_flutter/lib/tokens/tokens.dart`.
  - Las salidas no están versionadas y faltan en el contenedor.
  - **`tokens.css` también lo consume la web Angular.**
- **Paleta y roles actuales:**
  - Verdes g900–g100, naranja o700–o100 como color de acción y fondo `#F4F6F5`.
  - Superficie blanca con borde de 1px y sombras muy bajas.
  - Display y cuerpo en **Instrument**. **Bricolage** está empaquetada y nunca se usa.
- **Tema:** `packages/diseno_flutter/lib/tema.dart`, con `temaDesde()`, Material 3 y `Card` con
  borde. Se cablea en `apps/movil/lib/app_real.dart:45-47`, con `ThemeMode.system`.
- **Inventario:** 53 átomos, 33 moléculas, 21 organismos y 19 móviles, con catálogo Widgetbook en
  `lib/catalogo/`.
- **Lo que se ve «básico»:**
  - `EncabezadoDeSaldo` es texto plano; antes era una tarjeta con degradado.
  - `Tarjeta` es una caja blanca con borde.
  - `Casilla`, `Interruptor`, `FilaDeCuota` y `PanelBienvenida` son `ListTile` de Material.
  - 30 pantallas usan el `AppBar` de fábrica y solo 2 usan `Cabecera`.
  - Íconos: solo `Icons.*` de Material (~185 usos) que mezclan filled, outlined y rounded.
  - No hay vidrio ni desenfoque.
- **Navegación:**
  - `navegacion/shell.dart` muestra `BarraPestanas` con 5 pestañas: Inicio, Grupos, Movimientos,
    Perfil y Ayuda.
  - Las rutas viven en `navegacion/rutas.dart`, `pantallas/*/rutas.dart` y `rutas_de_entrada.dart`.
- **Pruebas que se rompen:**
  - 3 goldens con 21 PNG en `diseno_flutter`.
  - 38 archivos de goldens en la app, que escriben 310 PNG en
    `docs/trabajo/2026-10-11-alta-atlas-sesion-premium/evidencia/`.
  - El job de CI `app-goldens` corre en Linux y `goldens-macos.yml` regenera las bases.
- **Entorno:** Flutter **no** está instalado. Se puede descargar el 3.44.8 (HTTP 206 desde
  `storage.googleapis.com`), `pub.dev` responde y hay Chromium disponible.

**Desconocidos:**
- Cómo se ve Meru por dentro. La búsqueda no devolvió capturas de su interfaz.
- Si las pantallas cargan sin backend con el mock `packages/simulado`.

**Hipótesis (a verificar en H0):**
- El arnés con fuentes reales produce capturas legibles en Linux.
- `flutter build web` funciona con las dependencias de cámara y ML Kit (degradan en web según
  `pasanaku-app-movil-ux` §7).

---

## 2. Dirección de arte propuesta: «Banca Verde Profunda»

Lenguaje de la fintech premium (Meru, Nubank, Revolut, Apple Wallet), traducido a la marca existente.
No se copia ninguna marca ajena: se toma el **lenguaje**, no los activos.

| Eje | Hoy | Propuesta |
|---|---|---|
| **Saldo** | Texto suelto en tinta | **Tarjeta héroe** con degradado verde profundo de 2 paradas, ruido sutil de 2 %, cifra en Bricolage 44 con `tabularFigures`, ojo para ocultar, chip de variación y brillo diagonal al tocar |
| **Acciones** | 4 tiles con ícono | 4 **círculos de 56 dp** con ícono lineal y etiqueta debajo (Recargar, Enviar, Aportar, QR). Se hunden con resorte |
| **Superficies** | Blancas con borde 1px | Sin borde en claro. **Sombra en 2 capas** (contacto + ambiente) y radio 20. En oscuro, superficie un tono más clara y borde de 1px al 6 % de blanco |
| **Fondo** | `#F4F6F5` plano | Claro `#F5F7F6` con **halo** radial de marca detrás de la tarjeta héroe. Oscuro casi negro verdoso `#07130D` |
| **Color de acción** | Naranja `o700` | **Ambigüedad A2.** Propuesta: verde de marca como primario y naranja solo para «pagar / aportar ahora» (urgencia). Se decide con la persona |
| **Tipografía** | Instrument en todo | **Bricolage** para cifras y títulos de pantalla (display, 700) e **Instrument** para el texto. Cifras tabulares y tracking negativo en display |
| **Íconos** | Material mezclado | **Lucide** (`lucide_icons_flutter` 3.1.20): un solo set lineal de trazo uniforme y redondeado, el de las fintech modernas. Contenedor circular con tinte por categoría |
| **Cabeceras** | `AppBar` de fábrica | `Cabecera` grande estilo iOS (título de 28 que se encoge a 17 al desplazar, con desenfoque al pasar contenido por detrás) |
| **Barra de pestañas** | Barra pegada abajo | **Flotante, translúcida** (vidrio con `BackdropFilter`), indicador en píldora que se desliza con resorte y pestaña central «QR» destacada |
| **Movimientos** | Filas planas | Avatar circular por tipo (aporte, cobro, recarga), monto alineado a la derecha con signo y color semántico + ícono (nunca solo color), y agrupación por día con encabezado fijo |
| **Grupos** | Lista | **Tarjetas de grupo** con la rueda de turnos en miniatura, barra de progreso del ciclo y avatares apilados de participantes |
| **Estados** | Ilustraciones de pintor | Esqueletos con brillo, vacíos con ilustración y acción, errores con referencia y reintento |
| **Movimiento** | Básico | Resortes de `apple-design`: entrada escalonada, cifra que rueda, tarjeta compartida (Hero) de lista a detalle, hoja con banda elástica y celebración al aportar |

**Anatomía de la pantalla de Inicio** (la más importante; las demás heredan su lenguaje):

```
┌ safe area ─────────────────────────────┐
│ (avatar) Hola, Ana          (🔔•) (⚙)  │ ← cabecera con saludo según la hora y campana con punto
│ ╭──────────────────────────────────╮   │
│ │ Saldo disponible          (👁)   │   │ ← tarjeta héroe: degradado g700→g900, radio 28
│ │ Bs 1.250,00                      │   │   cifra Bricolage 44 tabular, rueda al cambiar
│ │ ▲ Bs 150 este mes · Verificada ✓ │   │   chip de variación y chip de nivel KYC
│ ╰──────────────────────────────────╯   │
│  (＋)     (↗)      (◎)      (⌗)        │ ← 4 acciones circulares de 56 dp
│ Recargar  Enviar  Aportar   QR         │
│ ┌ Tu próximo aporte ───────────────┐   │ ← tarjeta de urgencia: fecha, monto y botón naranja
│ │ Grupo Amigos · vence en 3 días    │   │
│ └──────────────────────────────────┘   │
│ Tus grupos                    Ver todo │ ← carrusel horizontal de tarjetas de grupo
│ [■ rueda · 4/10 · ●●●+6] [■ …]        │
│ Movimientos                   Ver todo │
│ (◉) Aporte Grupo Amigos   −Bs 150,00  │
│ (◉) Recarga QR            +Bs 500,00  │
╰─ ( Inicio  Grupos  [QR]  Movim.  Perfil ) ─╯ ← barra flotante de vidrio
```

---

## 3. Alcance

- **IN:**
  - Tokens (paleta, roles, elevación, radios, tipografía, movimiento), `tema.dart` y los átomos,
    moléculas, organismos y móviles de `diseno_flutter`.
  - Todas las pantallas de `apps/movil/lib/pantallas/**` y el shell.
  - Microcopy de esas pantallas y ARB/strings.
  - Goldens y arnés de capturas, capturas antes y después, galería, `REPORTE.md` y PR.
- **OUT:**
  - Lógica de negocio, providers de datos, contratos de API, cálculos de dinero y sesión
    (`Sesion`, `Vigilante`).
  - Backend, web Angular y backoffice, salvo el efecto inevitable de tokens compartidos (ver A3).
  - Landing, ícono de la app y splash nativo, que quedan como propuesta en H8.
  - Dependencias nuevas sin ADR.
- **Ambigüedades registradas** (supuesto tomado → a quién confirmar: la persona usuaria y Pablo):
  - **Decisiones confirmadas por la persona usuaria el 2026-10-21** (delegó en «lo más recomendado»):
  - **A1, referencia (RESUELTA):** lenguaje de las apps bancarias hermanas (Meru, Nubank, Revolut,
    Ualá, Mercado Pago, Apple Wallet). Ver §2. Se recalibra si llegan capturas de Meru.
  - **A2, color primario (RESUELTA):** verde de marca como primario y naranja solo para la urgencia
    de pago («Aportar ahora», vencimientos).
  - **A3, `tokens.css` compartido (RESUELTA):** se permite que el cambio de roles alcance a la web y
    al backoffice. Se capturan antes y después (H2.S5.M1, H8.S3.M2).
  - **A4, íconos (RESUELTA):** **Lucide**, con `lucide_icons_flutter` fijado en **3.1.20**
    (publicado el 2026-09-15, licencia ISC, ~290 mil descargas por mes y API `IconData`
    compatible con `Icon`). Phosphor quedó descartado: sin publicar desde 2024-05. Hugeicons quedó
    descartado: API no `IconData`. Se registra en ADR-053 (H1.S2.M2).
  - **A5, nombre visible:** «Aporteo» (`app_real.dart`) o «AportaYa» (`app.dart`). Sigue abierta;
    no se toca (no es diseño).
  - **A6, pestaña central QR (RESUELTA):** sí. Pestañas: Inicio · Grupos · **QR** · Movimientos ·
    Perfil. Ayuda pasa a una fila del Perfil y a un botón de la cabecera.
  - **A7, bases de goldens:** se regeneran en Linux (CI). Las de macOS las regenera
    `goldens-macos.yml`.
  - **A8, «Transferir» en Inicio (ABIERTA):** la pantalla `billetera.transferir` existe pero
    ninguna vista la enlaza. Las apps hermanas ponen «Enviar» entre las acciones rápidas; acá no
    se expone sin confirmación (mover plata entre personas toca la regla 91.5). Supuesto:
    Aportar · Recargar · Retirar. Confirmar con Pablo.
  - **A9, `TarjetaSaldo` vs `EncabezadoDeSaldo` (ABIERTA):** la tarjeta héroe vive en
    `EncabezadoDeSaldo`, que es lo que usa la app; `TarjetaSaldo` quedó solo en el catálogo con el
    estilo viejo. Dos componentes para el mismo saldo viola 95.1.3. Supuesto: se retira
    `TarjetaSaldo` (o queda como alias del héroe) en H4.S2.M2. Confirmar con Pablo antes de borrar.
  - **A10, tipo de movimiento en la billetera (ABIERTA):** `MovimientoBilletera` solo trae `sentido`
    (sin tipo: aporte, recarga, entrega). La fila muestra entra/sale y no adivina el tipo por la
    glosa (regla 97.5.2). Si el contrato suma el tipo, la fila ya lo acepta (`tipo:`). Confirmar
    con el dueño de `nucleo-financiero`.
  - **A11, pagar desde «Estado de tu aporte» (ABIERTA):** la ruta de pago
    `/pasanaku/obligaciones/:obligacionId/aportar` necesita un id que el estado del participante no
    trae, y ninguna pantalla la enlaza (solo llega por aviso). Sin ese dato no se ofrece «Poner al
    día» (no se inventa la ruta). Hace falta que `aportes` exponga las obligaciones abiertas con id.
    Confirmar con el dueño de `aportes`.
  - **A12, filtros de Ayuda en carrusel (ABIERTA):** la crítica propone una fila de chips con scroll
    horizontal; un test existente exige lo contrario («ChipsDeFiltro se acomodan en varias líneas,
    nunca en carrusel») y el tema es un campo de selección. Se mantiene el requisito y solo se
    reduce el ruido (un primario por pantalla). Confirmar con Pablo si el requisito sigue vigente.
  - **A13, montos sugeridos en Recargar (ABIERTA):** Bs 50 / 100 / 200 / 500 son atajos de
    comodidad, no parámetros del negocio: no hay mínimo ni tope inferidos (regla 97.5.4). Si
    producto quiere otros montos o ninguno, es una lista en un solo lugar. Confirmar con Pablo.
  - **A15, Mi puntaje sin escala (ABIERTA):** el contrato devuelve `puntaje` como cadena y
    `nivelDeConfianza` sin máximo, umbrales ni factores. Un medidor circular o una escalera de
    etapas inventaría esos valores (regla 97.5.4). Confirmar con transparencia si exponen la escala.
  - **A14, Extracto (RESUELTA 2026-10-21):** el contrato del extracto no trae la lista, pero la billetera ya sirve los últimos 100 movimientos; el detalle sale de ahí recortado al período y avisa si la lectura pudo quedar corta. El «certificado» se comparte como texto (período y saldo); un PDF firmado sería decisión de producto.

---

## H0 — Entorno y capturas del «ANTES»
**CA:** Dado el repo en `dev`, cuando se corre el arnés, entonces existe el catálogo completo de capturas
«antes» de todas las pantallas y componentes, en 3 viewports × 2 temas × estados, y se puede abrir la galería.
**DoD:** `flutter --version` → 3.44.8 · `flutter test test/capturas/rediseno` exit 0 · conteo de PNG pegado
· galería abre · `revision-antes.md` escrito.
**Estado:** TODO

### H0.S1 — Entorno
| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H0.S1.M1 | Instalar Flutter 3.44.8 en `/opt/flutter` | `flutter --version` imprime 3.44.8 | `flutter --version` → `Flutter 3.44.8` | HECHO |
| H0.S1.M2 | Generar los tokens | `tokens.dart` existe | `yarn workspace @aportaya/tokens build && yarn workspace @aportaya/diseno-flutter build` exit 0 · `ls packages/diseno_flutter/lib/tokens/tokens.dart` | HECHO |
| H0.S1.M3 | Resolver dependencias de la app y el paquete | `pub get` sin error | `flutter pub get` en ambos → exit 0 | HECHO |
| H0.S1.M4 | Fijar la línea base de calidad | Se conoce el estado previo | `dart analyze --fatal-infos` + `flutter test` + `python3 scripts/verificar_frontend.py movil` → salidas pegadas en `evidencia/linea-base.txt` | HECHO |

### H0.S2 — Arnés de capturas
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H0.S2.M1 | Cargador de fuentes reales para tests (`test/capturas/fuentes.dart`) | Un texto de prueba se ve en Instrument, no en bloques | `flutter test test/capturas/fuentes_test.dart` + captura mirada | HECHO |
| H0.S2.M2 | Matriz de viewport × tema (`test/capturas/matriz.dart`) | Una pantalla genera 6 PNG | `flutter test …/matriz_test.dart` → 6 archivos | HECHO |
| H0.S2.M3 | Datos sintéticos declarados por pantalla (`test/capturas/datos_sinteticos.dart`) | Cada override devuelve datos, vacío o error | Test unitario de los fixtures en verde | HECHO |
| H0.S2.M4 | Inventario de pantallas del arnés (lista cerrada contra las rutas) | Toda ruta registrada tiene su entrada o una exclusión justificada | Test que compara el inventario con `rutas.dart` → PASS | HECHO |
| H0.S2.M5 | Recorrido web con Playwright (`herramientas/capturas-web/`) | Navega Inicio → Grupos → Movimientos y captura | `node herramientas/capturas-web/recorrer.mjs` exit 0 + PNG | HECHO |
| H0.S2.M6 | Grabador de movimiento (video de Playwright más extracción de cuadros con ffmpeg) | Sale un MP4 y 3 cuadros de una transición | Archivo MP4 > 0 bytes + 3 PNG | HECHO |
| H0.S2.M7 | Generador de galería `scripts/galeria_capturas.mjs` | `galeria.html` lista antes y después por pantalla con filtros | `node scripts/galeria_capturas.mjs` → HTML abierto y mirado | HECHO |

### H0.S3 — Capturas «antes» (una microtarea por flujo)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H0.S3.M1 | Entrada: portada, tour (4), ingreso, MFA | Matriz completa | conteo de PNG = pantallas × 6 × estados | HECHO |
| H0.S3.M2 | Alta y KYC: registro (6 pasos), pautas, cámara, prueba de vida, subida, estado, contrato, bienvenida | ídem | ídem | HECHO |
| H0.S3.M3 | Billetera: inicio, recargar, retirar, transferir, extracto | ídem | ídem | HECHO |
| H0.S3.M4 | Grupos: mi estado, puntaje, nuevo, pedir cupo, invitar (+número), solicitudes, retiro, transparencia, permuta, sorteo (2), aportar, escanear, unirse, enlace | ídem | ídem | HECHO |
| H0.S3.M5 | Perfil: perfil, verificación, dispositivos, verificación profunda, contraseña, baja | ídem | ídem | HECHO |
| H0.S3.M6 | Notificaciones, ayuda, candado, splash, pantallas de bloqueo y configuración inválida | ídem | ídem | HECHO |
| H0.S3.M7 | Componentes: el catálogo Widgetbook completo por nivel | Una captura por componente y tema | conteo de PNG | HECHO |
| H0.S3.M8 | Revisión del «antes» con la rúbrica `ui-quality-review`, con la lista de problemas por pantalla | `evidencia/revision-antes.md` con ≥ 1 hallazgo por pantalla | Archivo escrito | HECHO |

---

## H1 — Dirección de arte, voz y decisiones
**CA:** Dado el «antes» revisado, cuando la persona abre el brief y la lámina de dirección, entonces puede
aprobar o corregir paleta, tipografía, íconos, color de acción y navegación antes de que se toque código.
**DoD:** brief y lámina capturados · ADR-053 escrito · ambigüedades A1–A7 respondidas o con supuesto registrado.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contexto de producto (`.agents/product-marketing.md`, skill `product-marketing`): audiencia, promesa, voz rioplatense con voseo y lo que no se promete | Documento con las 8 secciones de la skill | Archivo en disco | HECHO |
| H1.S1.M2 | Guía de voz y microcopy (`copywriting` + `copy-editing` + `ux-writing-microcopy`): glosario, verbos de botón, errores sin culpa y prohibiciones de muletillas de IA | Tabla «decimos / no decimos» con ≥ 30 entradas | Archivo `voz.md` | HECHO |
| H1.S1.M3 | Mapa de psicología aplicada (`marketing-psychology`): confianza, transparencia del pozo, progreso del ciclo, aversión a la pérdida en mora (sin manipular) | Principio → pantalla → elemento | Archivo `psicologia.md` | HECHO |
| H1.S2.M1 | Lámina de dirección de arte: paleta, tipos, sombras, radios, íconos e Inicio en alta fidelidad (HTML estático con los tokens propuestos) | Se ve en claro y oscuro | 2 capturas de la lámina + revisión | HECHO |
| H1.S2.M2 | ADR-053 «Rediseño bancario»: decisiones de paleta, tipografía, íconos, navegación y efecto sobre la web | ADR con contexto, opciones, decisión y consecuencias | Archivo en `docs/Arquitectura/` | HECHO |
| H1.S2.M3 | Confirmación de A1–A7 con la persona | Cada una con respuesta o supuesto | Tabla actualizada en este plan | HECHO |

---

## H2 — Tokens nuevos
**CA:** Dado el ADR aprobado, cuando se regeneran los tokens, entonces existen los roles, la elevación, los radios,
la tipografía y el movimiento nuevos, en claro y oscuro, con contraste AA medido.
**DoD:** `yarn workspace @aportaya/tokens test` (incluye contraste y espejo con `estilos.css`) en verde · `tokens.dart`
regenerado · lámina de tokens capturada.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Paleta: extender verdes (g950, g850) y neutros fríos para el fondo oscuro | Primitivas nuevas en JSON y CSS | test espejo PASS | DESCARTADO (la paleta existente alcanza: héroe g600→g900 y oscuro con hex derivado; decidido en H2) |
| H2.S1.M2 | Roles nuevos: `heroInicio`, `heroFin`, `heroTexto`, `heroTexto2`, `halo`, `vidrio`, `vidrioBorde`, `superficieElevada`, `iconoContenedor` por tono | Existen en claro y oscuro | test de contraste PASS (texto ≥ 4.5:1 y no texto ≥ 3:1) | HECHO |
| H2.S1.M3 | Rol de acción según A2 | `accent` y `brand` con la semántica decidida | test PASS + captura de botones | HECHO |
| H2.S2.M1 | Elevación en 4 niveles con sombra en 2 capas (`e1` fila, `e2` tarjeta, `e3` héroe, `e4` hoja) y equivalentes oscuros con borde | `Elevacion.*` generado | test generador PASS | HECHO |
| H2.S2.M2 | Radios: agregar `r2xl` 28 y la regla de anidado (interior = exterior − padding) | Token y documentación | test PASS | HECHO |
| H2.S3.M1 | Tipografía: `Tipo.saldo` (Bricolage 44/700, tabular, −2 % de tracking), `tituloPantalla` (Bricolage 28/700), `tituloCompacto` 17 y `cifraFila` (tabular) | Estilos generados con `fontFeatures` | test PASS + lámina tipográfica capturada | HECHO |
| H2.S4.M1 | Movimiento: tokens de resorte (`resorteSuave`, `resorteRapido`, `resorteRebote`) y escalonado (`paso` 30 ms) en `movimiento.dart` | Con su porqué en comentario | test unitario de valores | HECHO |
| H2.S5.M1 | Efecto sobre la web (A3): capturar la web antes y después del cambio de tokens | Lista de roles que cambiaron con captura | 4 capturas web + nota | HECHO |

---

## H3 — Tema global
**CA:** Dado el tema nuevo, cuando cualquier pantalla usa los componentes Material por defecto, entonces ya se ven con
el lenguaje nuevo (sin bordes de fábrica, cabeceras grandes, hojas redondeadas y transición con resorte).
**DoD:** test `roles_de_color_*` y del tema en verde · capturas de una pantalla «testigo» antes y después.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | `CardTheme`: sin borde en claro, elevación `e2`, radio `r2xl` | Tarjeta testigo cambia | golden testigo regenerado y mirado | HECHO |
| H3.S1.M2 | `AppBarTheme` → estilo cabecera grande, transparente y con desenfoque al desplazar | Captura con contenido debajo | captura 3 vp × 2 temas | HECHO |
| H3.S1.M3 | `BottomSheetTheme`, `DialogTheme` y `SnackBarTheme` en el lenguaje nuevo | Captura de cada uno | 6 capturas | A MEDIAS (código y tests en verde; faltan las capturas de hoja y diálogo, que salen del catálogo en H4.S3.M8) |
| H3.S1.M4 | Transición de página con resorte (`TransicionDeEje` actualizada) | Video de la transición | MP4 normal + reducido | TODO |
| H3.S1.M5 | Fondo con halo para pantallas raíz (`FondoDeMarca` parametrizado) | Captura | 6 capturas | TODO |

---

## H4 — Componentes del sistema de diseño
**CA:** Dado el catálogo Widgetbook, cuando se recorre cada nivel, entonces cada componente tocado se ve con el lenguaje
nuevo en claro y oscuro, conserva su API (o se barren todos sus usos) y pasa a11y.
**DoD:** `flutter test` del paquete en verde · goldens del paquete regenerados y **mirados** · captura antes y después
por componente · `test/a11y` PASS.
**Estado:** TODO

### H4.S1 — Átomos
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | `Boton`: altura 56, radio píldora, variantes primario/secundario/fantasma/peligro/urgencia, hundido con resorte y brillo solo en el principal | 7 estados capturados | golden `atomos_botones_*` | HECHO |
| H4.S1.M2 | `BotonIcono` circular de 44/56 con contenedor tintado | Área ≥ 48 | a11y PASS + captura | HECHO |
| H4.S1.M3 | `Monto` / `MontoQueSube`: Bricolage tabular con decimales más chicos y en `text2`, rodillo de dígitos al cambiar | Video del rodillo | MP4 + test de formateo intacto | TODO |
| H4.S1.M4 | `Avatar` con anillo de estado y `AvataresApilados` (parámetro de `Avatar`, no componente nuevo, salvo justificación) | Captura de 1, 3 y 9+ | captura | TODO |
| H4.S1.M5 | `ChipEstado` con ícono + texto + tono (nunca solo color) | 4 tonos × 2 temas | captura | TODO |
| H4.S1.M6 | `Esqueleto` con brillo diagonal de baja frecuencia (se apaga con reducir movimiento) | Video | MP4 normal + reducido | TODO |
| H4.S1.M7 | `Casilla` e `Interruptor` sin `ListTile` de fábrica | Captura | golden `atomos_seleccion_*` | TODO |
| H4.S1.M8 | Campos (`Campo`, `CampoMonto`, `CampoOTP`): relleno suave, foco con anillo de marca y monto grande centrado | Captura de los estados | golden `atomos_campos_*` | HECHO (2026-10-29, tanda de pulido: relleno `surface2`, `BordeConHalo` con el token `halo` y anillo de error; golden `atomos_campos_*` regenerado; 3 pruebas en `campo_relleno_y_halo_test` con mutante; el monto grande ya estaba centrado en `CampoMontoGrande`) |
| H4.S1.M9 | Iconografía: agregar `lucide_icons_flutter: 3.1.20` (con ADR-053), mapa único `IconosDeMarca` (sustantivo → `LucideIcons.*`) en `diseno_flutter` y barrido de los ~185 `Icons.*` | `git grep -n "Icons\." apps/movil/lib` sin `Icons.` de Material (salvo excepciones listadas) | grep pegado + capturas | A MEDIAS (anda: dependencia fijada, `IconosDeMarca` con 22 sustantivos, fuente cargada en las capturas; falta: barrido de los 91 `Icons.` que quedan en `apps/movil/lib` y la lista de excepciones) |

### H4.S2 — Moléculas
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S2.M1 | `Tarjeta`: niveles `plana`/`elevada`/`heroe`/`vidrio` por variante semántica | Las 4 capturadas | golden `moleculas_*` | TODO |
| H4.S2.M2 | `TarjetaSaldo` → tarjeta héroe (degradado, ruido, ojo, chips y brillo al tocar) | Captura + video del brillo | 6 capturas + MP4 | A MEDIAS (anda: la tarjeta héroe con degradado, brillo radial, ojo que oculta y chips, en `EncabezadoDeSaldo`; falta: ruido de textura, brillo al tocar con su MP4 y resolver A9 con `TarjetaSaldo`) |
| H4.S2.M3 | `AccionesRapidas` → círculos de 56 dp con etiqueta y entrada escalonada | Captura + video | idem | A MEDIAS (anda: círculos de 56 dp con la acción destacada en verde sólido, test de roles; falta: entrada escalonada y su MP4, que dependen de H5) |
| H4.S2.M4 | `FilaDeMovimiento`: avatar por tipo, monto con signo y color + ícono, y subtítulo con hora | Captura de los tipos | captura | HECHO |
| H4.S2.M5 | `ItemPasanaku` → **tarjeta de grupo** con rueda mini, progreso del ciclo y avatares apilados | Captura en datos y peor caso | captura | TODO |
| H4.S2.M6 | `Cabecera` grande que se compacta al desplazar (reemplaza el `AppBar` de fábrica) | Video del colapso | MP4 | A MEDIAS (anda: `PantallaConCabecera` con 4 pruebas (compacta, sin contenido debajo, un encabezado para el lector, título largo al 200 %), MP4 claro/oscuro en `evidencia/movimiento/cabecera`, y turno, verificar sorteo y transparencia migradas; falta: migrar las otras 19 pantallas con `AppBar`, franja por franja) |
| H4.S2.M7 | `Alerta`, `Seccion`, `Pestanas`, `BarraDePasos`, `ChipsDeFiltro`, `TarjetaKPI` y `RielDeTurnos` re-estilizados | Una captura por componente | golden `moleculas_*` | A MEDIAS (anda: BotonDeCabecera en círculo, píldora de pestañas; falta Alerta, Seccion, Pestanas, BarraDePasos, ChipsDeFiltro, TarjetaKPI y RielDeTurnos) |

### H4.S3 — Organismos y móviles
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S3.M1 | `EncabezadoDeSaldo` vuelve a usar la tarjeta héroe | Captura | golden `pantalla_de_saldo_*` | HECHO |
| H4.S3.M2 | `ListaDeMovimientos` con encabezados de día fijos | Video al desplazar | MP4 | TODO |
| H4.S3.M3 | `EstadoVacio`, `EstadoError` y `EstadoDePantalla` con ilustración, título, explicación y acción | 3 estados × 2 temas | golden `organismos_*` | A MEDIAS (anda: EstadoVacio con círculo de marca, título, ícono propio y acción, y EstadoDePantalla lo propaga; falta: EstadoError con el mismo tratamiento y las capturas de los tres estados × 2 temas) |
| H4.S3.M4 | `BarraPestanas` flotante de vidrio con indicador en píldora con resorte (más pestaña QR si A6 = sí) | Video del cambio de pestaña | MP4 normal + reducido | A MEDIAS (anda: cápsula flotante con píldora que crece, QR central levantado, etiquetas del mismo tamaño y test; falta: indicador que se desliza con resorte entre pestañas y su MP4 normal/reducido, que van con H5) |
| H4.S3.M5 | `HojaDeConfirmacion` con agarradera, banda elástica y fondo atenuado | Video | MP4 | TODO |
| H4.S3.M6 | `CelebracionDeExito` para aporte pagado (confeti breve ≤ 900 ms, solo en éxito de dinero, apagado con reducir movimiento) | Video | MP4 | TODO |
| H4.S3.M7 | `TecladoNumerico` con teclas circulares y háptico de selección | Captura | captura | TODO |
| H4.S3.M8 | Catálogo Widgetbook actualizado con todas las variantes nuevas | Recorrido completo | capturas `componentes/despues/` | TODO |

---

## H5 — Motion design (Apple + Emil + skills de animación)
**CA:** Dado el inventario de movimiento, cuando la persona usa la app, entonces cada animación contesta «¿de dónde
salió?» o «¿me hizo caso?», dura ≤ 320 ms si la dispara la persona, usa resortes donde hay gesto y desaparece con
reducir movimiento sin perder contenido.
**DoD:** `evidencia/movimiento/` con un video por pieza, normal y reducido · auditoría `review-animations` escrita ·
perfil de rendimiento sin cuadros > 16 ms en la pantalla de Inicio (`flutter run --profile`, o DevTools en web).
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Auditoría con `find-animation-opportunities` sobre todas las pantallas, como tabla «momento → propósito → pieza» | ≥ 1 decisión por flujo (animar o no) | `movimiento/oportunidades.md` | TODO |
| H5.S1.M2 | Vocabulario común (`animation-vocabulary`): nombre exacto de cada efecto usado | Glosario | archivo | TODO |
| H5.S2.M1 | Entrada escalonada de Inicio (héroe → acciones → secciones, 30 ms de paso) | Video | MP4 + 3 cuadros | A MEDIAS (anda desde 0922bade: las filas del feed entran con 30 ms de paso; el héroe y las secciones no esperan, por decisión fijada en `pantalla_de_saldo_test` — además la guía mide el héroe apenas existe y quedaría corrida; falta: el MP4 y los 3 cuadros) |
| H5.S2.M2 | Rodillo de cifra del saldo al refrescar | Video | MP4 | TODO |
| H5.S2.M3 | Hero compartido: tarjeta de grupo → detalle del grupo | Video | MP4 | TODO |
| H5.S2.M4 | Indicador de pestaña con resorte | Video | MP4 | A MEDIAS (anda: una sola píldora que se desliza con resorte crítico (k 322, c 35,9), interrumpible, y llega de una vez con movimiento reducido; 3 tests con mutante y 28 cuadros en `evidencia/movimiento/barra`; falta: el MP4 normal/reducido que pide el DoD) |
| H5.S2.M5 | Hoja interrumpible con gesto (arrastrar, soltar y rebote; `apple-design` §momentum) | Video | MP4 | TODO |
| H5.S2.M6 | Arrastrar para refrescar personalizado (marca que gira) | Video | MP4 | TODO |
| H5.S2.M7 | Ocultar o mostrar el saldo con desenfoque y fundido | Video | MP4 | HECHO (2026-10-29: 4 pruebas con mutante, incluida la regresión de la cifra que se recreaba; MP4 normal y reducido, claro y oscuro, en `evidencia/movimiento/saldo-oculto`) |
| H5.S2.M8 | Éxito de aporte: check que se dibuja, háptico `mediumImpact` y celebración | Video | MP4 | TODO |
| H5.S2.M9 | Prueba de diez segundos en cada pantalla raíz: nada se mueve solo salvo esqueleto y turno actual | Video de 10 s por pantalla raíz | 5 MP4 + nota | TODO |
| H5.S3.M1 | `fixing-motion-performance`: `RepaintBoundary` en héroe y barra, sin animar layout | Perfil sin jank | salida del perfil pegada | TODO |
| H5.S3.M2 | Revisión final `review-animations` + `improve-animations` | Hallazgos corregidos | `movimiento/revision.md` | TODO |
| H5.S3.M3 | Política de hápticos aplicada por el puerto `proveedores/haptica.dart` (`pasanaku-app-movil-ux` §4) | `git grep HapticFeedback apps/movil/lib/pantallas` vacío | grep pegado | TODO |
| H5.S3.M4 | **Agregada en H0.S1.M4 (bug de la línea base):** el candado se resincroniza cuando «reducir movimiento» cambia con la capa animándose (`didChangeDependencies`). Hoy queda con opacidad 0 | Dado el candado entrando, cuando se activa reducir movimiento, entonces la capa se ve completa en el mismo cuadro | `flutter test test/widget/capa_de_bloqueo_animacion_test.dart` → PASS | HECHO |

---

## H6 — Pantallas (rediseño por flujo)
Cada microtarea de pantalla tiene el **mismo DoD**:
1. `dart analyze --fatal-infos` y `verificar_frontend.py movil` en verde.
2. Tests de la pantalla en verde.
3. **Capturas «después» de la matriz completa** (3 vp × 2 temas × estados + peor caso), **mirada** y
   anotada en `revision-<flujo>.md`.
4. Comparación con su «antes» en la galería.

**Estado general:** TODO

### H6.S1 — Inicio y billetera (primero: es la cara de la app)
| ID | Pantalla | Cambios clave | Estado |
|---|---|---|---|
| H6.S1.M1 | `/billetera/inicio` | Anatomía §2: saludo, héroe, acciones, próximo aporte, carrusel de grupos y últimos movimientos | A MEDIAS (anda: cabecera con Ayuda y avisos en círculos, héroe, acciones, vacío de pasanakus con crear/entrar y últimos movimientos con la fila nueva; falta: tarjeta «próximo aporte» (A11) y carrusel de grupos) |
| H6.S1.M2 | `/billetera/recargar` | Monto grande centrado con teclado propio, chips de montos sugeridos, QR en tarjeta y estado de espera honesto («esperando confirmación», regla 91.3) | A MEDIAS (anda: cabecera única con volver (se corrigió el título duplicado), monto grande centrado con atajos y estado de espera honesto con etapas (91.3); falta: teclado propio y QR en tarjeta) |
| H6.S1.M3 | `/billetera/retirar` | Ídem, más la cuenta destino enmascarada en tarjeta y el aviso de enfriamiento | A MEDIAS (anda: monto grande y confirmación «instruido» con etapas y monto neto; falta: cuenta destino enmascarada en tarjeta y aviso de enfriamiento, que dependen de CU-18) |
| H6.S1.M4 | `/billetera/transferir` | Contacto con avatar, monto y hoja de confirmación con resumen | A MEDIAS (anda: cabecera única, monto grande y confirmación; falta: contacto con avatar y hoja de confirmación con resumen) |
| H6.S1.M5 | `/billetera/extracto` | Filtros en chips, lista agrupada por día y tarjeta para descargar el certificado | HECHO — filtros Todos/Entradas/Salidas, lista por día (lectura del libro recortada al período), comprobante y «Compartir certificado de saldo» como texto; ver `evidencia/h6-s1-m5-extracto.txt` |

### H6.S2 — Grupos (el corazón del pasanaku)
| ID | Pantalla | Cambios clave | Estado |
|---|---|---|---|
| H6.S2.M1 | `/pasanaku/mi-estado` | Tarjetas de grupo, resumen de compromisos y restricciones como alerta con acción | A MEDIAS (anda: título grande, tarjeta de grupo con la cifra por período, tarjeta de aporte con cada cifra nombrada y restricción como alerta; falta: acción de pago en la alerta (A11) y el resumen de compromisos de todos los grupos) |
| H6.S2.M2 | `/mi-puntaje` | Medidor circular de confianza, escalera de etapas y qué sube el puntaje | HECHO en lo que el contrato permite (tarjeta, puntaje, nivel, origen, vacío «Recién empezás»; `evidencia/h6-s2-m2-puntaje.txt`). Medidor, escalera y «qué sube» son DECISION_REQUIRED (A15): sin escala del negocio se inventarían umbrales (regla 97.5.4) |
| H6.S2.M3 | `/grupos/nuevo` | Asistente por pasos con `BarraDePasos`, vista previa de la tarjeta del grupo y monto grande | TODO |
| H6.S2.M4 | `/grupos/:id/invitar` (+número) | Tarjeta compartible con QR (`referrals`: mensaje listo para WhatsApp, beneficio para ambos solo si existe en el negocio) | TODO |
| H6.S2.M5 | `/grupos/:id/solicitudes`, `pedir-cupo` | Tarjetas de solicitud con avatar, nivel y acciones | HECHO — tarjeta con avatar, puntaje y cupos como chips, mensaje entre comillas, Aceptar como primario; Pedir cupo con etapas de espera y selector de cupos (`evidencia/h6-s2-m5-solicitudes.txt`). «Nivel» se muestra como puntaje: no hay escala (A15) |
| H6.S2.M6 | `/grupos/:id/transparencia` | Pozo del grupo como héroe y libro de movimientos legible (lectura del mayor, sin cálculo nuevo) | TODO |
| H6.S2.M7 | `/sorteos/:id/turno`, `verificar` | Rueda protagonista, momento del sorteo con movimiento y verificación explicada | TODO |
| H6.S2.M8 | `/turnos/permuta`, `/grupos/:id/retiro` | Comparación lado a lado y hoja de confirmación | TODO |
| H6.S2.M9 | `/obligaciones/:id/aportar` | Desglose claro, botón de urgencia, espera honesta y celebración al confirmarse | A MEDIAS (anda: importe fijo como cifra protagonista, canales en chips con ícono, «Aportar» en naranja de urgencia y confirmación en palabras con el estado de la obligación; falta: desglose del cobro, celebración y espera honesta con webhook) |
| H6.S2.M10 | `/pasanaku/escanear`, `/unirse/:codigo`, `/g/:codigo` | Visor de QR con marco animado y tarjeta de vista previa del grupo antes de unirse | TODO |

### H6.S3 — Entrada, alta y KYC
| ID | Pantalla | Cambios clave | Estado |
|---|---|---|---|
| H6.S3.M1 | `/portada` | Héroe de marca, promesa en una línea (`copywriting`) y CTA claro | A MEDIAS (anda: banda verde de marca a sangre con el sello y la palabra, promesa y garantías, puertas abajo; falta: mostrar la tarjeta héroe como vista previa, revisar el copy con `copywriting` y el recorrido web) |
| H6.S3.M2 | `/tour` (4) | Ilustraciones reestilizadas, indicador de puntos con resorte y texto por beneficio | TODO |
| H6.S3.M3 | `/ingreso`, `/identidad/mfa` | Campos nuevos, biometría como botón destacado y OTP en celdas | A MEDIAS (anda: ingreso con banda de marca verde, logo, saludo y titular que se corre con el teclado; falta: MFA con celdas nuevas y biometría como botón destacado) |
| H6.S3.M4 | `/registro` (6 pasos) | Fricción mínima según `signup`, progreso visible y ayuda en contexto | A MEDIAS (anda: cabecera de fase con jerarquía real: eyebrow de marca, título grande y barra más gruesa; falta: fricción mínima según `signup`, ayuda en contexto y los seis pasos rediseñados) |
| H6.S3.M5 | Pautas, cámara, prueba de vida y subida | Marcos de guía claros sobre fondo oscuro y estados de permiso (los 3) | TODO |
| H6.S3.M6 | Estado, contrato y bienvenida | Bienvenida con checklist de activación (`onboarding`: primer grupo, primera recarga) | A MEDIAS (anda: estado de verificación ×5 con círculo de tono, etapas Recibido/En revisión/Listo y caso, y bienvenida con tarjeta verde y pasos numerados; falta: checklist de activación con primer grupo y primera recarga (`onboarding`) y contrato) |

### H6.S4 — Perfil, notificaciones, ayuda y sistema
| ID | Pantalla | Cambios clave | Estado |
|---|---|---|---|
| H6.S4.M1 | `/identidad/perfil` | Tarjeta de identidad con nivel KYC y secciones agrupadas tipo Ajustes de iOS | A MEDIAS (anda: tarjeta de verificación con escudo y chip de estado con tono, correo, lista agrupada con íconos, cerrar sesión secundario y «Dar de baja» como fila roja al final; falta: avatar/nombre de la persona (el contrato solo trae el correo) y nivel KYC en escalera) |
| H6.S4.M2 | Verificación, verificación profunda, dispositivos y contraseña | Filas agrupadas, chips de estado y sin `Card`/`ListTile` crudos | TODO |
| H6.S4.M3 | `/identidad/baja` | Pantalla sobria, consecuencias claras y confirmación con hoja | TODO |
| H6.S4.M4 | `/notificaciones/bandeja` | Agrupación por día, no leídas con punto y vacío que orienta | TODO |
| H6.S4.M5 | `/soporte/ayuda` | Buscador, temas en tarjetas y tutoriales | A MEDIAS (anda: solo el tutorial recomendado lleva la acción principal; falta: buscador y temas en tarjetas, A12) |
| H6.S4.M6 | Candado, splash, bloqueo de plataforma y configuración inválida | Candado con marca y biometría; los errores de sistema con referencia | TODO |
| H6.S4.M7 | Shell: barra flotante y, si A6 = sí, pestaña central QR | Navegación capturada y E2E de las pestañas | HECHO |
| H6.S4.M8 | **Agregada en H0.S2.M5 (bug real):** desde otra rama, la pestaña «Movimientos» llama a `goBranch(0, initialLocation: true)` y aterriza en `/billetera/inicio`, no en `/billetera/extracto` (`navegacion/shell.dart:107-111`). Visto en el recorrido web, captura `web/antes/09-movimientos__412.png` | Test de widget: desde Grupos, tocar Movimientos deja la ubicación en `/billetera/extracto` · recorrido web con la captura correcta | HECHO |

---

## H7 — Redacción y crecimiento dentro de la app (skills de marketing)
**CA:** Dado el rediseño, cuando una persona nueva recorre la app, entonces cada texto dice qué pasa y qué hacer en voz
de la marca, y existen los puntos de crecimiento del producto, sin prometer nada que el negocio no ofrezca.
**DoD:** barrido de textos con `copy-editing` (tabla antes → después) · capturas de las pantallas de crecimiento ·
cero cifras de prueba social inventadas (regla 00.2).
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H7.S1.M1 | Barrido de microcopy de todas las pantallas (botones, vacíos, errores, confirmaciones) | Tabla con ≥ 1 mejora por pantalla | `textos-antes-despues.md` | TODO |
| H7.S1.M2 | Errores accionables: qué pasó, qué hacer y referencia (`error-handling-contract`) | Ninguno crudo | grep de mensajes + capturas de error | TODO |
| H7.S2.M1 | Checklist de activación en Inicio para personas nuevas (`onboarding`: completar identidad, primer grupo, primera recarga) | Se oculta al completarse | capturas en 3 progresos | TODO |
| H7.S2.M2 | Invitación viral (`referrals` y `marketing-ideas` #93): tarjeta del grupo para compartir con QR y vista previa del enlace | Capturas de la tarjeta compartible | captura | TODO |
| H7.S2.M3 | «Mi turno llegó»: tarjeta celebratoria compartible cuando cobrás tu pozo (`marketing-ideas` #106, al estilo resumen anual) | Captura | captura | TODO |
| H7.S2.M4 | Propuesta de capturas de tienda (`aso`): 6 láminas con el «después» y titulares | Láminas en `evidencia/aso/` | 6 PNG | TODO |
| H7.S3.M1 | Lista de ideas de crecimiento priorizadas (`marketing-ideas`, formato de la skill), como propuesta y no implementadas | Documento con 5 ideas | `crecimiento.md` | TODO |

---

## H8 — QA, regresión y gates
**CA:** Dado todo lo anterior, cuando se corre la pirámide de cierre, entonces todo está en verde, los goldens nuevos
están mirados y no hay regresión funcional.
**DoD:** salidas literales pegadas en `evidencia/qa/`.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H8.S1.M1 | Análisis y formato | 0 avisos | `dart analyze --fatal-infos` + `dart format --set-exit-if-changed` | TODO |
| H8.S1.M2 | Gate de frontend | PASS | `python3 scripts/verificar_frontend.py movil` | TODO |
| H8.S1.M3 | Tests del paquete y de la app | Verde | `flutter test` (ambos) pegado | TODO |
| H8.S1.M4 | Goldens regenerados y **mirados uno por uno** | Lista de goldens con «visto» | `flutter test --update-goldens` + `revision-goldens.md` | TODO |
| H8.S2.M1 | Accesibilidad: tamaño táctil, contraste y semántica | PASS | `flutter test test/a11y` | TODO |
| H8.S2.M2 | Peor caso (`break-ui`) en todas las pantallas con datos | Hallazgos corregidos | capturas `peor-caso` + lista | TODO |
| H8.S2.M3 | Rúbrica `ui-quality-review` final por flujo con capturas delante | Puntaje ≥ objetivo en cada eje | `revision-final.md` | TODO |
| H8.S3.M1 | Recorrido web real con Playwright (todas las pestañas, consola sin errores) | 0 errores de consola | log pegado | TODO |
| H8.S3.M2 | Capturas web del backoffice afectado por tokens (A3) | Sin roturas | capturas | TODO |

---

## H9 — Galería, reporte y entrega
**CA:** Dado el trabajo cerrado, cuando la persona abre la galería en el celular, entonces ve el antes y el después de
cada pantalla y componente, y el reporte dice con honestidad qué está hecho, qué está a medias y qué falta.
**DoD:** galería publicada (enlace) · `REPORTE.md` con avance en la primera línea · PR abierto contra `dev` con capturas.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H9.S1.M1 | Galería final generada y publicada | Enlace abre en el celular | URL + captura de la galería | TODO |
| H9.S1.M2 | `REPORTE.md` (regla 40) | 3 secciones + evidencia + no cubierto | `report_gate.py` no bloquea | TODO |
| H9.S1.M3 | PR a `dev` con las capturas más importantes embebidas | PR abierto | URL del PR | TODO |
| H9.S1.M4 | Checklist de verificación en teléfono real para la persona (lo que el contenedor no puede probar: cámara, biometría, hápticos, rendimiento real) | Lista entregada | sección en el reporte | TODO |

---

## Orden de ejecución y entregas parciales

> **Reorden del 2026-10-21 (pedido de la persona usuaria: «hay cosas que antes se veían mal y
> siguen igual»).** La crítica `evidencia/roast-pantallas-hoy.md` mostró que H2–H4 cambiaron color
> y piezas, pero no la composición de las pantallas. Desde acá se ejecuta **H6 antes de terminar
> H4 y H5**, en este orden, y cada pantalla construye en el momento la pieza de H4 que necesite:
> 1. H6.S2.M1 Mi estado · 2. H6.S1.M1 + H6.S4.M7 Inicio y barra · 3. H6.S4.M1 Perfil ·
> 4. H6.S3.M6 Pantallas de estado · 5. H6.S3.M3 Ingreso · 6. H6.S3.M1 Portada ·
> 7. H6.S4.M5 Ayuda · 8. H6.S3.M4 Alta.
> Cada una cierra con su antes|después en 3 viewports × 2 temas, mirado.


1. **H0** (antes) → **H1** (dirección; A2, A3, A4 y A6 ya resueltas el 2026-10-21) → **H2** → **H3** →
   **H4** → **H6.S1 Inicio** → *entrega parcial con galería* → H5 en paralelo con el resto de **H6** →
   **H7** → **H8** → **H9**.
2. Cada flujo cerrado se commitea y se empuja con sus capturas. La galería se actualiza en cada entrega.
3. Total de microtareas: **124** (H0: 19 · H1: 6 · H2: 8 · H3: 5 · H4: 24 · H5: 14 · H6: 28 ·
   H7: 7 · H8: 9 · H9: 4; `plan_status.py` da la cifra oficial). El avance se calcula solo como
   `HECHO / total`.

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| `flutter build web` falla por cámara o ML Kit | Sin recorrido web | Las capturas salen del arnés de widgets (mecanismo 1). La web queda como mecanismo 2 declarado |
| Goldens dependen de la plataforma (79/268 diffs en Windows, ítem «golden diffs» de PENDIENTES.md) | CI rojo en otra plataforma | Bases regeneradas en Linux como CI. macOS vía `goldens-macos.yml`, declarado en el reporte |
| Cambiar roles compartidos altera el backoffice (A3) | Regresión visual en la web | Roles nuevos con nombre nuevo, más capturas de la web antes y después |
| `BackdropFilter` caro en gama baja | Jank en la barra de vidrio | `RepaintBoundary`, desenfoque acotado y fondo sólido de respaldo en gama baja o con reducir transparencia |
| Volumen de capturas (~1.500 PNG) | Repositorio pesado | Capturas en `evidencia/` comprimidas con `pngquant` si está disponible. La galería es la vista principal |
| Sin teléfono real en el contenedor | Cámara, biometría y hápticos sin verificar | Checklist H9.S1.M4, declarado como «No cubierto» |
| Sin capturas de Meru | Dirección mal calibrada | Lámina H1.S2.M1 para aprobar antes de tocar componentes |
