# Barrido de botones del backoffice contra el backend real

Fecha 2026-10-24 · backend local en el contenedor de la nube (15 servicios + gateway, PostgreSQL, Kafka, Redis) ·
`ng serve` en desarrollo contra el gateway · Chromium · 1440×900 · inicio de sesión real por la pantalla
(segundo factor `000000` del perfil `local`) · scripts: `botones-reales.mjs`, `recorrido-real.mjs`.

## Qué hace
En cada pantalla estática que el rol puede abrir, pulsa hasta 40 controles (botones, enlaces, pestañas,
chips, interruptores, selects, `summary`) y por cada uno registra las peticiones a la API con estado ≥ 400 y los
errores de consola. Si abre un diálogo lo cierra con Escape. No pulsa lo que ejecuta una acción de
confirmación o irreversible (lista `SALTAR` del script).

## Resultado por rol (primera pasada, `botones/*.json`)
| Rol | Rutas abiertas | Controles vistos | Pulsados |
|---|---|---|---|
| Admin (SOPORTE + ADMIN_PLATAFORMA) | 43 / 57 | 1235 | 327 |
| CONTABILIDAD | 15 / 57 | 124 | 102 |
| TESORERIA | 15 / 57 | 125 | 108 |
| OFICIAL_CUMPLIMIENTO | 25 / 57 | 197 | 164 |
| RESPONSABLE_RIESGOS | 22 / 57 | 162 | 141 |
| OPERADOR_PUBLICIDAD | 10 / 57 | 45 | 32 |

## Clasificación de los 57 hallazgos únicos
| Clase | Qué es | Veredicto |
|---|---|---|
| Paginadores `Anterior` / `Siguiente` (≈ 36 casos) | «no se pudo pulsar» | **No es defecto**: están deshabilitados con una sola página. Se comprobó con `isDisabled()` en la segunda pasada (`botones2/`). |
| 7 chips de estado de `/cumplimiento/uif/reportes` y otros controles sueltos | «no se pudo pulsar» | **No es defecto**: artefacto del barrido (el DOM se rehace tras cada pulsación). Pulsados uno por uno, los 7 disparan `GET /uif/reportes?estados=…` con 200 y alternan `aria-pressed`. «Dar de alta anunciante» también se pulsa bien. |
| `GET /identidad/verificaciones` → 403 (rol RESPONSABLE_RIESGOS: `/cumplimiento`, `/cumplimiento/verificaciones`, tarjeta del tablero, `/ayuda`) | La ruta se le abre y la API se lo niega; la pantalla muestra el error con «Volver a intentar», que no arregla nada | **Defecto abierto** (hallazgo 9 del plan): alinear el permiso de la ruta con el de la API. |
| `GET /grupos/solicitudes` y `/grupos/solicitudes-escaladas` → 404 (admin) | Rutas que el frontend llama y ningún servicio publica | **Defecto conocido** (H4.B del plan predecesor). |

No apareció ningún error de consola, ningún 5xx ni ningún 422 en los controles pulsados, después de las
correcciones de H3.

## Lo que NO cubre (sin esta línea el resultado se leería como total)
- Tope de 40 controles por pantalla; el administrador vio 1235 y pulsó 327.
- No se pulsan confirmaciones ni acciones irreversibles; las mutaciones se probaron aparte y solo tres por la API
  (alta de usuario interno, asignación de roles —rechazada por la regla de separación de funciones, correcta—,
  apertura de ejercicio fiscal). No se ejercitó `UI → petición → respuesta → persistencia → recarga → UI` en ninguna.
- Rutas con parámetro (fichas por id: casos, alertas, actas, reclamos, billetera, periodo, campaña, operación…).
- Solo escritorio 1440×900 y tema claro; sin móvil, tablet ni tema oscuro; sin teclado ni lector de pantalla.
- Las capturas **no tuvieron la doble revisión de la regla 35**: lo visual queda en `VERIFIED_FUNCTIONAL_ONLY`.
- `/tablero/comercial` y `/publicidad/moderacion` no las abre ninguno de los seis roles probados.
- Contra el backend **local** con datos de desarrollo; nada se probó contra TEST.
