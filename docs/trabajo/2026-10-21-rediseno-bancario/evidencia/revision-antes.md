# Revisión del ANTES (H0.S3.M8)

Revisión hecha mirando las capturas, no describiendo el código. La base son las hojas de
contacto por flujo (estado «datos», 360 dp, claro) y las hojas de los 5 estados × 2 temas
de las pantallas de billetera. Rúbrica: `ui-quality-review`. Lenguaje de referencia:
`frontend-beautiful-ui`, `apple-design` y las apps bancarias hermanas (PLAN §2).

Datos de las capturas: **sintéticos**, sacados de los ejemplos del contrato (`packages/simulado`).
Textos como «glosa», «nombre» o «montoAporte moneda» son valores de ejemplo del contrato,
no defectos de la pantalla.

## Hallazgos transversales (afectan a toda la app)

| # | Hallazgo | Dónde se ve | Lo resuelve |
|---|---|---|---|
| T1 | **Sin jerarquía de profundidad.** Fondo gris plano, tarjetas blancas con borde de 1 px y sombras casi nulas: todo pesa lo mismo y nada es protagonista | Todas | H2.S2 (elevación) · H3.S1.M1 · H4.S2.M1 |
| T2 | **El saldo es texto suelto.** La cifra más importante de una app bancaria no tiene contenedor, ni color, ni gesto para ocultarla | billetera/inicio | H4.S2.M2 · H6.S1.M1 |
| T3 | **Un botón apagado se lee como texto.** «Aportar», «Crear grupo», «Guardar», «Proponer el cambio», «Solicitar el retiro», «Ingresar» y «Confirmar» apagados quedan como texto gris sin forma de botón | grupos, perfil, entrada | H4.S1.M1 (estado apagado con forma y motivo) |
| T4 | **El naranja es el color de toda acción**, incluida «Pedir mi cupo» o «Activar mi cuenta»: no distingue urgencia de dinero de una acción común | Todas | H2.S1.M3 (decisión A2) |
| T5 | **Barra de pestañas básica:** pegada abajo, sin profundidad, y la etiqueta «Movimientos» sale **cortada** («Movimien…») en 360 dp | Shell, todas las pantallas con pestañas | H4.S3.M4 · H6.S4.M7 (pestaña QR) |
| T6 | **Íconos de Material mezclados** (rellenos, contorneados y redondeados) y de trazo fino que se pierde en 360 dp | Todas | H4.S1.M9 (Lucide) |
| T7 | **Cabeceras planas:** un título suelto arriba, sin cabecera grande, sin acción de volver visible al abrir la ruta directo y sin cambio al desplazar | Todas | H4.S2.M6 · H3.S1.M2 |
| T8 | **Esqueleto de carga casi invisible** en tema claro: en «cargando» la pantalla parece vacía o rota | billetera/inicio y todas las que cargan | H4.S1.M6 |
| T9 | **Estados vacíos mudos o de caja plana:** un párrafo dentro de una tarjeta con borde, sin ilustración ni acción | billetera (pasanakus, movimientos), transparencia, notificaciones | H4.S3.M3 |
| T10 | **Estado de error correcto en contenido** (dice qué pasó, da referencia y «Volver a intentar») pero visualmente suelto en el centro, sin ilustración | Todas en «error» | H4.S3.M3 |
| T11 | **Tema oscuro:** el verde casi negro funciona, pero las tarjetas apenas se separan del fondo y el naranja claro de los botones pierde peso | Todas en oscuro | H2.S1 · H2.S2.M1 (borde claro en oscuro) |
| T12 | **Tipografía de un solo registro:** Instrument en todo; Bricolage está empaquetada y no se usa. Las cifras no son tabulares | Todas | H2.S3.M1 |

## Por pantalla

### Billetera
| Pantalla | Hallazgo principal |
|---|---|
| inicio | T2, T5 y T8. «Tus pasanakus» vacío es una caja sin acción. Las acciones son un botón naranja grande más dos secundarios cuadrados, no las acciones circulares de una app bancaria. La línea «En custodia…» queda huérfana debajo de los botones |
| recargar | Formulario de monto sin protagonismo: el monto se escribe en un campo común. Sin montos sugeridos |
| retirar | Igual que recargar; la cuenta destino no se presenta como tarjeta |
| transferir | Sin selección de contacto con avatar; formulario plano |
| extracto | Lista sin agrupación visual por día ni avatar por tipo de movimiento |

### Grupos
| Pantalla | Hallazgo principal |
|---|---|
| mi-estado | «Tus grupos» es una fila con avatar «G» y un chip; sin rueda, sin progreso del ciclo ni monto. «Organizar un grupo nuevo» es un botón fantasma |
| mi-puntaje | **Llama a `GET /reputacion/puntaje`, que no está en ningún contrato** de `packages/simulado`: con el doble solo puede mostrar el error «No encontramos lo que buscabas». Registrado como hallazgo de contrato |
| nuevo | El mejor formulario de la app (opciones en tarjeta con check). El botón «Crear grupo» apagado tiene el problema T3 |
| pedir-cupo | Correcta; el botón naranja compite con nada. Falta resumen del grupo que se pide |
| invitar | Ilustración de bandeja genérica; el enlace y el QR no se muestran como algo compartible |
| invitar-numero | Segmentado «Enlace / SMS / WhatsApp» correcto; el botón «Invitar» apagado tiene el problema T3 |
| solicitudes | Tarjeta de solicitud correcta pero plana; el avatar de iniciales no tiene color |
| retiro | Formulario sin contexto de consecuencias (cuánto se devuelve) arriba |
| transparencia | Vacío con ícono de bandeja: no explica qué son los «bloques sellados» |
| permuta | Un campo de texto y un botón apagado: no muestra qué turnos se intercambian |
| sorteo-turno | «Turno 0 de 0» y hashes crudos: el momento más emocional del pasanaku (saber tu turno) no tiene rueda ni celebración |
| sorteo-verificar | Muro de texto con hashes; correcto para auditoría pero sin jerarquía (resultado primero, detalle técnico plegable) |
| aportar | **El segmentado de medio de pago corta «Transferencia» en dos líneas («Transferen / cia») en 360 dp.** «Aportar» apagado (T3) |
| reclamo-nuevo | Texto de «todavía no podemos»; sin ilustración ni salida alternativa clara |
| escanear | Recuadro negro sin marco de guía ni animación de escaneo |
| unirse | La tarjeta de invitación es un bloque de texto; sin monto destacado, periodicidad ni avatares de miembros |
| enlace-de-grupo | Sin sesión, la ruta pública lleva al ingreso. El ingreso no dice que viene de una invitación |

### Entrada
| Pantalla | Hallazgo principal |
|---|---|
| portada | Correcta y con marca; los tres «chips» de beneficios se leen como botones |
| tour | Ilustración de pintor plana, indicador de puntos chico |
| ingreso | «Ingresar» apagado (T3); el aviso de custodia es letra chica gris |
| mfa | Celdas OTP correctas; «Confirmar» apagado (T3) |

### Alta y KYC
| Pantalla | Hallazgo principal |
|---|---|
| registro / verificacion-basica | Fases en texto chico; tarjetas con borde dentro de tarjetas (T1) |
| pautas | Buenas láminas «correcto / incorrecto»; el segmentado naranja es el único naranja no urgente (T4) |
| camara / prueba-de-vida | Con permiso concedido, en un dispositivo sin cámara **la pantalla no atrapa el `StateError`** de `CamaraEnVivoPlugin.iniciar` (hallazgo de producto, fuera de alcance). Se fotografiaron los permisos denegado y bloqueado |
| subida | Lista de documentos con guiones «—»: sin estado visual por documento (pendiente, subiendo, listo) |
| estado / verificacion-de-sesion | Reloj de arena de Material grande y solo; «Caso 3a1b05ac» suelto |
| contrato | Correcto para lectura; las casillas apagadas hasta deslizar se leen como desactivadas sin motivo visible |
| bienvenida | Pantalla casi vacía, con la tarjeta de bienvenida perdida en el medio. Momento ideal para el checklist de activación (H7.S2.M1). Dice «AportaYa» mientras el título de la app es «Aporteo» (A5) |
| identidad-requerida | Check grande genérico; «Volver» como botón contorneado al mismo nivel que la acción |
| verificar-identidad | Carrusel de capturas correcto; íconos de pose muy finos (T6) |

### Perfil
| Pantalla | Hallazgo principal |
|---|---|
| perfil | No hay tarjeta de identidad (nombre, avatar, nivel). «Dar de baja tu cuenta» es un botón rojo sólido al mismo nivel que «Cerrar sesión»: una acción destructiva con demasiado peso |
| verificacion | Selector de fecha en tres campos apilados; «Guardar fecha» apagado (T3) |
| dispositivos | Casilla suelta y botón naranja: sin lista de dispositivos con ícono |
| verificacion-profunda | Segmentado «No / Sí» correcto; «Continuar» apagado (T3) |
| contrasena | Dos campos y «Guardar» apagado (T3); sin indicador de fuerza |
| baja | La alerta está bien; falta la lista de consecuencias concretas |

### Sistema
| Pantalla | Hallazgo principal |
|---|---|
| notificaciones | Vacío con ícono de bandeja (T9) |
| ayuda | Bien organizada, pero densa: filtros en chips más un selector repiten la misma función |

## Componentes (catálogo de `diseno_flutter`, fuentes reales)

| Grupo | Hallazgo |
|---|---|
| Organismos | **`BannerDePauta` desborda 22 px a la derecha en 360 dp** con las fuentes reales (`organismos/banner_de_pauta.dart:44`, `Row`). Los goldens no lo ven porque usan la fuente de bloques. Lo corrige H4.S3 |
| Moléculas | `TarjetaSaldo`, `AccionesRapidas`, `FilaDeMovimiento` e `ItemPasanaku` ya existen pero planos: son la base a re-pintar en H4.S2 (no se crean gemelos) |
| Átomos | Botón apagado sin forma (T3); íconos de Material mezclados (T6) |

## Recorrido real en la web (Playwright, contra el simulado)

| Hallazgo | Evidencia | Lo resuelve |
|---|---|---|
| **La pestaña «Movimientos» no lleva a Movimientos si venís de otra pestaña**: aterriza en Inicio | `web/antes/09-movimientos__412.png` · `navegacion/shell.dart:107-111` | H6.S4.M8 |
| **Los campos del ingreso exponen el placeholder como nombre accesible** (`71234567`, `Mínimo 8 caracteres`), no la etiqueta; varios botones salen sin nombre en la semántica | `herramientas/recorrer_web.cjs` (se ubican por tipo) | H8.S2.M1 |
| El tutorial guiado tapa Inicio con un velo oscuro la primera vez: bien resuelto, pero el foco verde sobre el saldo resalta un bloque sin forma (T2) | `web/antes/06-tutorial__412.png` | H4.S2.M2 |
| La app consulta `GET /api/v1/version`, que no está en los contratos (404 contra el simulado) | consola del recorrido | Fuera de alcance (contrato) |
| Sin idioma del navegador, Flutter web no arranca («Incorrect locale information provided») | `herramientas/recorrer_web.cjs` fija `es-BO` | Fuera de alcance (anotado) |
| El build web incluye las fuentes de `widgetbook` (Poppins): el catálogo viaja dentro de la app | log del servidor estático | Fuera de alcance (anotado) |

## Hallazgos fuera de alcance (anotados, no arreglados — regla 00 §3)

1. `CamaraEnVivoPlugin.iniciar` lanza `StateError` sin cámara y `_PantallaDeCamaraState._inicializar` no lo atrapa.
2. `_PantallaDeRegistroState.dispose` llama a `AltaNotifier.abandonar()`, que modifica un provider mientras el árbol se desmonta. Riverpod lo rechaza en debug cada vez que alguien sale del registro navegando. Lo mismo pasa en `verificar-identidad`.
3. `_PantallaDeSubidaDelExpedienteState.dispose` usa el provider en un callback posterior al cuadro; falla si la app entera se desmonta.
4. `mi-puntaje` consume `GET /reputacion/puntaje`, que no figura en los contratos de `packages/simulado`.
