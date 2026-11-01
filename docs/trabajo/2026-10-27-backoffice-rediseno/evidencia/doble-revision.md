# Doble revisión de capturas (regla 35)

## Primera pasada (quien implementó)

Capturas de las 44 rutas del menú a 1280 claro (`cap/v2`, `cap/v3`), revisadas una por una. Defectos encontrados y su estado:

| Pantalla | Defecto | Estado |
|---|---|---|
| Todas | Doble relleno (`main` + `.pantalla`), cada vista empezaba en otra x | Corregido |
| Todas | «Notas de la vista» repetía la descripción de la cabecera | Pasó a ⓘ en la cabecera fija |
| Todas | Título 4 veces (cabecera fija, pestaña, H1, caption) | Migas «Sección › Pantalla», caption oculto en listados |
| 17 | Códigos internos en el eyebrow (CU-28, D-15…) y en textos (R-PUB-05, R-CTB-05, CU-34…) | Quitados del texto visible |
| Todas | `router-outlet` contaba como fila de la grilla: hueco fantasma | `router-outlet { display: none }` |
| Desembolsos, Reversos, Reclamos, Custodia | Identificadores partidos letra a letra (fila de 200 px) | Una línea con «…» y `title` |
| Operación | «4,44· moneda no registrada» sin espacio | Corregido |
| Cumplimiento, Contabilidad, Publicidad, Sistemas | «1 alertas», «1 órdenes», «1 anunciantes» | `nombreSegunCantidad` con irregulares |
| Verificación de identidad | Enums crudos (PENDIENTE, PERFIL_IZQUIERDO) | Etiquetas legibles (también en el CSV) |
| Reportes UIF | «4.44» sin formato | Agrupado como el resto |
| Sistemas (14) | Con la carga caída las cifras decían «0 caídos» | «—» y «Sin datos» |
| Bitácora, Catálogos operativos | Fecha ISO cruda | `ap-fecha` (La Paz) |
| Sistemas | Banner vacío sumaba un hueco | `:empty { display: none }` |
| Publicidad | «Cargar 100 más» verde relleno compitiendo con la acción principal | Fantasma chico |
| 16 vistas | «Actualizar» verde relleno al lado del primario negro | Fantasma |
| Desempeño, Liquidación | Vacío metido 24 px | Sin doble relleno |
| Tablero | Chips estirados a todo el ancho | `justify-self: start` |

## Segunda pasada (adversarial, revisor independiente `visual-reviewer`)

Veredicto del revisor: **RECHAZADA, 58/100**, sobre `cap/final-*`. Hallazgos y qué se hizo:

| Sev. | Hallazgo | Respuesta |
|---|---|---|
| BLOQUEANTE | Usuarios internos a 375: «Nuevo usuario interno» cortado en el borde | Medido: `scrollWidth = 375`, la página no desborda; el botón estaba en un carril desplazable. Igual se cambió: las acciones de la cabecera se envuelven en el celular. Re-capturado (`despues-375/sistemas__usuarios-internos.png`) |
| BLOQUEANTE | Catálogo de datos: «Sensibilidad» tapada por la columna fija | **Sigue abierto.** Se probó acotar el ancho de la columna de acciones; empeoró (los botones se superponían) y se revirtió. Se agregó sombra de desplazamiento en el borde de toda tabla más ancha que su marco. Queda en Riesgos |
| MAYOR | Barra lateral corta debajo de 900 px en capturas de página completa | Corregido: la columna oscura es el fondo de la grilla |
| MAYOR | Migas cortadas («C… › Compras») | Corregido: «Sección › Pantalla»; el buscador cede ancho primero |
| MAYOR | Columnas recortadas sin señal de desplazamiento | Sombra de borde en tablas desplazables (estilo global de `ui`) |
| MAYOR | Usuarios internos 1280: «Segundo factor»/«Estado» tapados por 5 acciones | **Sigue abierto** (misma causa que el Catálogo). Ya pasaba antes de este trabajo |
| MAYOR | Primario negro y secundario verde relleno conviven | Parcial: «Actualizar» y «Cargar más» pasaron a fantasma. «Volver a intentar» y «Ver ejercicios anteriores» siguen verdes: es la paleta del sistema de diseño (A3) |
| MAYOR | Catálogo: 392 en la cifra y 391 en la tabla | No corregido: viene del catálogo sintético, fuera de alcance |
| MAYOR | Anunciantes: «Cargar 100 más» + paginación «1 de 1» | No corregido: dos mecanismos que ya existían (lotes por cursor + página local). Decisión de producto |
| MENOR | 1 px de alto de más en todas las páginas | Corregido (`scrollHeight` 900 en 900) |
| MENOR | «1 de 1 expedientes» | Corregido |
| MENOR | «Indicador no disponible» estirado | Corregido |
| MENOR | Resto (títulos de pestaña ≠ H1, mismo ícono, etc.) | Anotados, sin cambio |

Re-captura posterior a las correcciones: `despues-1280/`, `despues-oscuro/`, `despues-375/`, `despues-768/`. La segunda pasada **no se repitió** sobre la re-captura final: el peldaño visual queda en `VERIFIED_FUNCTIONAL_ONLY` para las dos pantallas con hallazgos abiertos (Catálogo de datos y Usuarios internos) y `ACEPTABLE CON RESERVAS` para el resto, según la nota del revisor.

## Segunda pasada repetida sobre la versión final (2026-10-28, `visual-reviewer`, independiente)

44 rutas × 1280 claro, 1280 oscuro, 375 claro, 375 oscuro, más error 503 forzado (1280 y 375 oscuro), menú «Más» y
pestaña Columnas del catálogo. Cuatro rondas más un cierre; cada ronda sobre capturas nuevas, juzgando por la captura.

| Ronda | Puntaje | Rechazadas por el frontend | Qué quedó abierto |
|---|---|---|---|
| 1 | 66 | 31 | tablas cortadas a 1280, ceros en rojo, concordancia del título de error, jerga, plurales, enums crudos, calendario invisible en oscuro |
| 2 | 79 | 8 | regresiones del corte de línea: identificadores partidos, fechas en 3–4 renglones |
| 3 | 80 | 5 | `min-width: 16ch` general volvió a desbordar tablas; columna fija tapando «Estado» |
| 4 | 86 | 1 | pestaña Columnas del catálogo: «Confianza» cortada |
| Cierre | 89 | **0** | — |

Nota final: **ninguna pantalla RECHAZADA por el frontend**; todas ACEPTABLE CON RESERVAS (menores abiertos: migas y
pestañas truncadas a 1280, «September» en el campo de mes nativo, «Limpiar»/«Quitar filtros», alturas de tarjetas del
tablero, foco del menú «Más», tabla acotada sin indicio de que sigue).

Rechazadas **solo por decisión del dueño** (no del frontend): las 7 pantallas de Sistemas sin fuente (accesos, base de
datos, despliegues, outbox, proveedores, respaldos, webhooks). Para escalar: rojo semántico en alertas reales
(Descuadre, Vencido, Crítica, Bloqueado, Rechazar), códigos «CU-xx» en el panel «Cómo funciona esta pantalla» (los
exigen tests vigentes) y «(regla 91.1.2)», que viene en el dato del catálogo.

No cubierto: estado de carga (esqueleto), foco por teclado en el menú «Más», panel de explicación abierto, la línea de
confianza con un valor real (el simulado no la trae).
