# Revisión de goldens

Cada regeneración de goldens se mira entera, en hojas antes|después, antes de commitear
(regla 95.7.2). Acá queda qué se vio y qué hallazgo dejó.

## 2026-10-21 · H4 (tarjeta héroe, acciones circulares, botones en píldora)

Base: en `dev` (7752e1b) los 18 goldens del paquete y los 274 de la app pasaban en Linux con
Flutter 3.44.8. Todo cambio de píxeles es del rediseño.

| Lote | PNG | Visto | Resultado |
|---|---|---|---|
| `packages/diseno_flutter/test/goldens` | 19 | sí, 7 hojas por grupo | Cambio intencional: verde de marca, píldoras, superficies y fondo nuevos. Sin cortes. |
| `apps/movil/test/goldens/imagenes` | 12 | sí | Idem. |
| `docs/trabajo/2026-10-11-alta-atlas-sesion-premium/evidencia/**` | 276 | sí, 16 hojas | Idem; héroe del saldo en las vistas de billetera. |

Artefacto conocido, no defecto: en los goldens `flutter_test` pinta las sombras como bloques
sólidos grises (`debugDisableShadows`). Las capturas de `intermedio-h4/` usan sombras reales.

### Hallazgos (ninguno es regresión del rediseño)

| # | Dónde | Qué se ve | Desde cuándo | Va a |
|---|---|---|---|---|
| G1 | `h10-ayuda-lista-*` | Cada tarjeta de la lista repite un «Comenzar» primario a todo el ancho: cinco acciones principales en una pantalla. | Antes, en naranja | H6 (Ayuda): acción secundaria o enlace por tarjeta |
| G2 | `h10-grupos-mi-estado-error-parcial-*` | «Volver a intentar» y «Invitar a alguien» son los dos primarios. | Antes | H6.S2 |
| G3 | `h10-grupos-mi-estado-principal-deuda-*` | Con deuda vigente, la única acción principal es «Invitar». Falta la urgencia de pago si hay ruta. | Antes | H6.S2 (A2: urgencia naranja) |
| G4 | `billetera-40-movimientos-200` | A 200 % el concepto del movimiento se trunca a «Movi…». | Antes | **Corregido** en H4.S2.M4: el importe baja de renglón y el concepto se ve entero |
| G5 | `h8-subida-error-oscura-320-200` | A 320 px y 200 %, «Continuar» tapa «Repetir foto del anverso». | Antes | Fuera de alcance (identidad). Anotado |
| G6 | `billetera-360-200` | A 200 % las etiquetas «Aportar Recargar Retirar» quedan muy juntas, sin tocarse. | Nuevo (círculos) | Aceptado; se revisa con H4.S2.M3 |

## 2026-10-21 · H4.S2.M4 (`FilaDeMovimiento` única, avatar redondo, signo siempre)

10 PNG cambiados (4 del paquete, 6 de la app), mirados en una hoja antes|después: avatar circular
con Lucide, «+»/«−» explícitos, y a 200 % el concepto entero con el importe debajo (G4 corregido).
