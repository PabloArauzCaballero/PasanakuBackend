# Evidencia visual H10.S3.M1/M2 — veinte superficies

Estado: parcial; pruebas con fixtures sintéticos, ejecución local Windows. Fecha: 2026-10-13.

## Inventario conceptual del alcance del plan

`PLAN.md`, H8, párrafo “Alcance de pantallas” enumera 21 unidades visuales. La cifra cuenta individualmente los seis pasos del alta aunque se rendericen dentro de `/registro`; el candado es una capa, no ruta. El inventario H8 indica que grupos/detalle carece de ruta/contrato local, así que esa UI no se inventa.

| # | Unidad conceptual | Situación H10.S3 |
|---:|---|---|
| 1 | Portada | Matriz cubierta abajo |
| 2 | Tour | Matriz cubierta abajo |
| 3 | Ingreso | Matriz cubierta abajo |
| 4 | MFA | Matriz cubierta abajo |
| 5 | Alta — cuenta | Matriz cubierta abajo; campos vacíos, sin foco ni teclado |
| 6 | Alta — correo | Matriz cubierta abajo; OTP vacío, sin foco, teclado ni solicitud |
| 7 | Alta — documento | Matriz cubierta abajo; campos vacíos y sin capturar fotos |
| 8 | Alta — datos | Matriz cubierta abajo; nombres y fecha vacíos |
| 9 | Alta — actividad/perfil transaccional | Matriz cubierta abajo; campos iniciales vacíos y selectores cerrados |
| 10 | Alta — contrato | Matriz cubierta abajo; estado previo, sin abrir ni aceptar |
| 11 | Cámara | Matriz cubierta abajo; visor concedido y cámara sintética inerte |
| 12 | Prueba de vida | Matriz cubierta abajo; pose frontal inicial con cámara sintética |
| 13 | Subida del expediente | Matriz cubierta abajo |
| 14 | Estado de verificación | Matriz cubierta abajo |
| 15 | Bienvenida | Matriz cubierta abajo |
| 16 | Candado | Matriz cubierta abajo; bloqueado al arranque |
| 17 | Billetera/inicio — saldo y movimientos | Matriz cubierta abajo |
| 18 | Grupos — mi estado | Matriz cubierta abajo |
| 19 | Grupos — detalle | No disponible localmente: falta ruta/contrato según inventario H8 |
| 20 | Perfil | Matriz cubierta abajo |
| 21 | Ayuda | Matriz cubierta abajo; inicio recomendado y lista desplazada |

La matriz actual cubre 20 de las 21 unidades conceptuales: 228 celdas en total, 204 PNG de matriz nuevos y 24 reutilizados exactos. La carpeta de evidencia tiene actualmente 282 PNG acumulados; incluye una captura suplementaria de interacción del perfil, que no constituye una celda adicional de esta matriz. Estados vacíos se registran solo cuando el dominio los tiene; principal/cargando/error se adaptan a cada pantalla.

Este lote cubre una sola superficie (`PantallaMiEstado`), tres anchos lógicos (360, 412 y 600 dp), ambos temas y cuatro variantes visibles: principal con deuda, principal al día, carga y error parcial de aportes con la restricción cargada. El plan agrupa “vacío/cargando”; se captura carga. “Al día” es una respuesta válida sin deuda, no un `EstadoVacio` técnico. La pantalla no acepta un predicado `vacio` para `EstadoDelParticipante`, por lo que no se inventa un fixture vacío distinto.

| Ancho | Tema | Estado | Captura | Inspección visual |
|---:|---|---|---|---|
| 360 | claro | principal con deuda | `h10-grupos-mi-estado-principal-deuda-claro-360-130.png` | OK; texto y ambas acciones legibles, sin corte |
| 360 | claro | principal al día | `h10-grupos-mi-estado-principal-al-dia-claro-360-130.png` | OK; estado vacío de deuda y acciones legibles, sin corte |
| 360 | claro | carga | `h10-grupos-mi-estado-cargando-claro-360-130.png` | OK; skeleton, acciones legibles, sin corte |
| 360 | claro | error parcial | `h10-grupos-mi-estado-error-parcial-claro-360-130.png` | OK; mensaje, reintento y datos parciales legibles, sin corte |
| 360 | oscuro | principal con deuda | Reutiliza `../h8-grupos-mi-estado-deuda-oscuro-360-130-inicio.png` | Ya inspeccionada en evidencia H8 |
| 360 | oscuro | principal al día | Reutiliza `../h8-grupos-mi-estado-al-dia-oscuro-360-130.png` | Ya inspeccionada en evidencia H8 |
| 360 | oscuro | carga | Reutiliza `../h8-grupos-mi-estado-cargando-oscuro-360-130.png` | Ya inspeccionada en evidencia H8 |
| 360 | oscuro | error parcial | Reutiliza `../h8-grupos-mi-estado-error-oscuro-360-130.png` | Ya inspeccionada en evidencia H8 |
| 412 | claro | principal con deuda | `h10-grupos-mi-estado-principal-deuda-claro-412-130.png` | OK; texto y ambas acciones legibles, sin corte |
| 412 | claro | principal al día | `h10-grupos-mi-estado-principal-al-dia-claro-412-130.png` | OK; estado sin deuda y acciones legibles, sin corte |
| 412 | claro | carga | `h10-grupos-mi-estado-cargando-claro-412-130.png` | OK; skeleton, acciones legibles, sin corte |
| 412 | claro | error parcial | `h10-grupos-mi-estado-error-parcial-claro-412-130.png` | OK; mensaje, reintento y datos parciales legibles, sin corte |
| 412 | oscuro | principal con deuda | `h10-grupos-mi-estado-principal-deuda-oscuro-412-130.png` | OK; texto y ambas acciones legibles, sin corte |
| 412 | oscuro | principal al día | `h10-grupos-mi-estado-principal-al-dia-oscuro-412-130.png` | OK; estado sin deuda y acciones legibles, sin corte |
| 412 | oscuro | carga | `h10-grupos-mi-estado-cargando-oscuro-412-130.png` | OK; skeleton, acciones legibles, sin corte |
| 412 | oscuro | error parcial | `h10-grupos-mi-estado-error-parcial-oscuro-412-130.png` | OK; mensaje, reintento y datos parciales legibles, sin corte |
| 600 | claro | principal con deuda | `h10-grupos-mi-estado-principal-deuda-claro-600-130.png` | OK; contenido y acciones completos; controles ocupan el ancho disponible |
| 600 | claro | principal al día | `h10-grupos-mi-estado-principal-al-dia-claro-600-130.png` | OK; contenido y acciones completos; controles ocupan el ancho disponible |
| 600 | claro | carga | `h10-grupos-mi-estado-cargando-claro-600-130.png` | OK; skeleton y acciones completos; controles ocupan el ancho disponible |
| 600 | claro | error parcial | `h10-grupos-mi-estado-error-parcial-claro-600-130.png` | OK; mensaje, reintento y datos parciales completos; controles ocupan el ancho disponible |
| 600 | oscuro | principal con deuda | `h10-grupos-mi-estado-principal-deuda-oscuro-600-130.png` | OK; contenido y acciones completos; controles ocupan el ancho disponible |
| 600 | oscuro | principal al día | `h10-grupos-mi-estado-principal-al-dia-oscuro-600-130.png` | OK; contenido y acciones completos; controles ocupan el ancho disponible |
| 600 | oscuro | carga | `h10-grupos-mi-estado-cargando-oscuro-600-130.png` | OK; skeleton y acciones completos; controles ocupan el ancho disponible |
| 600 | oscuro | error parcial | `h10-grupos-mi-estado-error-parcial-oscuro-600-130.png` | OK; mensaje, reintento y datos parciales completos; controles ocupan el ancho disponible |

Los estados se inyectan mediante providers reemplazados; no llaman a servicios reales. El error es deliberadamente parcial (aportes falla y restricción carga correctamente). Escala de texto: 130 %. Revisé visualmente los 20 PNG nuevos: 20/20 sin recortes, desbordes ni texto ilegible. A 600 dp las dos acciones se expanden a todo el ancho disponible; no se modificó el layout porque no hay criterio de ancho máximo en este requisito. Los PNG de 360 oscuro existentes fueron producidos/inspeccionados para H8 en el mismo entorno local; la matriz no acredita equivalencia con el rasterizador de CI.

## Verificación reproducible

Generación de las 20 capturas nuevas:

```text
flutter test --concurrency=1 --update-goldens test/goldens/h10_grupos_mi_estado_visual_test.dart
00:02 +20: All tests passed!
exit_code=0
```

Repetición final sin actualizar capturas, después del último cambio al test:

```text
flutter test --concurrency=1 test/goldens/h10_grupos_mi_estado_visual_test.dart
00:02 +20: All tests passed!
exit_code=0
```

Análisis y gate móvil:

```text
dart analyze --fatal-infos test/goldens/h10_grupos_mi_estado_visual_test.dart
Analyzing h10_grupos_mi_estado_visual_test.dart...
No issues found!
exit_code=0

python scripts/verificar_frontend.py movil
TODO OK
exit_code=0
```

## Portada — estado principal estático

La portada es una pantalla estática: los estados de carga, vacío y error de contenido no aplican y no se fabrican fixtures. Escala de texto: 130 %.

| Ancho | Tema | Estado | Captura | Inspección visual |
|---:|---|---|---|---|
| 360 | claro | principal | Reutiliza `../h8-portada-claro-360-130.png` | Ya inspeccionada en evidencia H8 |
| 360 | oscuro | principal | Reutiliza `../h8-portada-oscuro-360-130.png` | Ya inspeccionada en evidencia H8 |
| 412 | claro | principal | `h10-portada-principal-claro-412-130.png` | OK; título, garantías y acciones completos |
| 412 | oscuro | principal | `h10-portada-principal-oscuro-412-130.png` | OK; título, garantías y acciones completos |
| 600 | claro | principal | `h10-portada-principal-claro-600-130.png` | OK; garantías envuelven en 2+1, sin corte |
| 600 | oscuro | principal | `h10-portada-principal-oscuro-600-130.png` | OK; garantías envuelven en 2+1, sin corte |

Se generaron cuatro PNG nuevos y se reutilizaron dos capturas H8. En la revisión de roles de color se inspeccionaron y actualizaron las cuatro H10 y las dos H8: lockup/sello, garantías, acciones secundarias y primarias quedan coherentes con acento naranja y roles neutrales; títulos/acciones completos, sin corte. `h10_portada_visual_test.dart` pasa 4/4 con y sin actualización; `h8_portada_mfa_oscuro_test.dart` pasa 4/4 con y sin actualización (las otras dos celdas MFA permanecen intactas). El análisis Dart queda limpio y el gate frontend 11/11.

## Tour — láminas estáticas

`PantallaDeTour` tiene cuatro láminas estáticas. Se capturan las cuatro en tres anchos (360 × 740, 412 × 850 y 600 × 960 dp), ambos temas, DPR 1 y texto 130 %; movimiento reducido está habilitado. Carga, vacío y error no aplican. La última lámina presenta «Empezar»; «Saltar» se desvanece y queda excluido del árbol semántico. Se reutilizan únicamente las láminas 1–3 de H8 oscuro a 360 dp: la lámina 4 difiere 126 px (0,05 %) en esta instalación y recibe baseline H10 propia.

| Lámina | Tema | 360 dp | 412 dp | 600 dp |
|---:|---|---|---|---|
| 1 | claro | `h10-tour-lamina-1-claro-360-130.png` | `h10-tour-lamina-1-claro-412-130.png` | `h10-tour-lamina-1-claro-600-130.png` |
| 1 | oscuro | Reutiliza H8 `h8-tour-oscuro-lamina-1-360-130.png` | `h10-tour-lamina-1-oscuro-412-130.png` | `h10-tour-lamina-1-oscuro-600-130.png` |
| 2 | claro | `h10-tour-lamina-2-claro-360-130.png` | `h10-tour-lamina-2-claro-412-130.png` | `h10-tour-lamina-2-claro-600-130.png` |
| 2 | oscuro | Reutiliza H8 `h8-tour-oscuro-lamina-2-360-130.png` | `h10-tour-lamina-2-oscuro-412-130.png` | `h10-tour-lamina-2-oscuro-600-130.png` |
| 3 | claro | `h10-tour-lamina-3-claro-360-130.png` | `h10-tour-lamina-3-claro-412-130.png` | `h10-tour-lamina-3-claro-600-130.png` |
| 3 | oscuro | Reutiliza H8 `h8-tour-oscuro-lamina-3-360-130.png` | `h10-tour-lamina-3-oscuro-412-130.png` | `h10-tour-lamina-3-oscuro-600-130.png` |
| 4 | claro | `h10-tour-lamina-4-claro-360-130.png` | `h10-tour-lamina-4-claro-412-130.png` | `h10-tour-lamina-4-claro-600-130.png` |
| 4 | oscuro | `h10-tour-lamina-4-oscuro-360-130.png` | `h10-tour-lamina-4-oscuro-412-130.png` | `h10-tour-lamina-4-oscuro-600-130.png` |

Se generaron e inspeccionaron visualmente 21 PNG H10 nuevos y se reutilizan 3 referencias H8 oscuras/360. En esta revisión, personas, rueda, paraguas y aro decorativos pasan a tonos neutrales; naranja queda como acento y verde solo en el tilde de comprobante verificado. Se actualizaron e inspeccionaron las cuatro capturas H8 oscuras/360. `h10_tour_visual_test.dart` pasó 24/24 con actualización y 24/24 sin actualización; `h8_tour_oscuro_test.dart` pasó 1/1 con cuatro capturas y 1/1 sin actualización. `dart analyze --fatal-infos test/goldens/h10_tour_visual_test.dart` terminó en `No issues found!`. El gate móvil `python ..\..\scripts\verificar_frontend.py movil` terminó en `TODO OK` (11/11).

## Bienvenida — cuenta nueva

`PantallaDeBienvenida` se captura con los datos por defecto de cuenta nueva (nombre vacío y bono `null`), en 360 × 740, 412 × 850 y 600 × 960 dp, claro/oscuro, DPR 1 y texto 130 %. Es una superficie estática: vacío, carga y error no aplican. El bonus monetario no está en el contrato local y se comprueba que no aparezca copy de acreditación inventado.

| Ancho | Tema | Estado | Captura | Inspección visual |
|---:|---|---|---|---|
| 360 | claro | principal | `h10-bienvenida-principal-claro-360-130.png` | OK; título y dos pasos completos |
| 360 | oscuro | principal | `h10-bienvenida-principal-oscuro-360-130.png` | OK; título y dos pasos completos; sin bono supuesto |
| 412 | claro | principal | `h10-bienvenida-principal-claro-412-130.png` | OK; título, tarjeta y pasos completos |
| 412 | oscuro | principal | `h10-bienvenida-principal-oscuro-412-130.png` | OK; título, tarjeta y pasos completos |
| 600 | claro | principal | `h10-bienvenida-principal-claro-600-130.png` | OK; contenido completo, ancho alineado |
| 600 | oscuro | principal | `h10-bienvenida-principal-oscuro-600-130.png` | OK; contenido completo, contraste legible |

Las seis capturas son H10 nuevas; la H8 oscura 360 difiere en 190 px (0,07 %), solo alrededor del contorno de la tarjeta, así que no se reutilizó ni actualizó. `h10_bienvenida_visual_test.dart` pasó **6/6** con actualización y **6/6** sin actualización; análisis Dart sin issues y gate frontend `TODO OK` (11/11). Datos sintéticos: cuenta nueva sin nombre ni bono confirmado.

## Estado de verificación — cuatro estados del expediente

`PantallaDeEstadoDeVerificacion` se presenta con cuatro estados sintéticos del contrato: pendiente, en revisión, aprobada y rechazada. Cada estado se captura en 360 × 740, 412 × 850 y 600 × 960 dp, tema claro/oscuro, DPR 1 y texto 130 %. La carga, el vacío y el error de red no son estados provistos por esta pantalla de estado final; no se fabrican. Las cuatro referencias H8 oscuras de 360 dp se comparan exactas y se reutilizan; las otras 20 imágenes son nuevas.

| Estado | Tema | Ancho | Captura | Inspección visual |
|---|---|---:|---|---|
| pendiente | claro | 360 | `h10-verificacion-pendiente-claro-360-130.png` | OK; título, caso y acciones completos |
| en revisión | claro | 360 | `h10-verificacion-en-revision-claro-360-130.png` | OK; texto y acciones completos |
| aprobada | claro | 360 | `h10-verificacion-aprobada-claro-360-130.png` | OK; estado y acciones completos |
| rechazada | claro | 360 | `h10-verificacion-rechazada-claro-360-130.png` | OK; motivo se envuelve sin corte |
| pendiente | oscuro | 360 | Reutiliza `../h8-verificacion-pendiente-oscura-360-130.png` | H8 exacta; texto y acciones visibles |
| en revisión | oscuro | 360 | Reutiliza `../h8-verificacion-en-revision-oscura-360-130.png` | H8 exacta; texto y acciones visibles |
| aprobada | oscuro | 360 | Reutiliza `../h8-verificacion-aprobada-oscura-360-130.png` | H8 exacta; estado y acciones visibles |
| rechazada | oscuro | 360 | Reutiliza `../h8-verificacion-rechazada-oscura-360-130.png` | H8 exacta; motivo y acciones visibles |
| pendiente | claro | 412 | `h10-verificacion-pendiente-claro-412-130.png` | OK; título, caso y acciones completos |
| en revisión | claro | 412 | `h10-verificacion-en-revision-claro-412-130.png` | OK; texto y acciones completos |
| aprobada | claro | 412 | `h10-verificacion-aprobada-claro-412-130.png` | OK; estado y acciones completos |
| rechazada | claro | 412 | `h10-verificacion-rechazada-claro-412-130.png` | OK; motivo en una línea |
| pendiente | oscuro | 412 | `h10-verificacion-pendiente-oscuro-412-130.png` | OK; contraste y acciones visibles |
| en revisión | oscuro | 412 | `h10-verificacion-en-revision-oscuro-412-130.png` | OK; contraste y acciones visibles |
| aprobada | oscuro | 412 | `h10-verificacion-aprobada-oscuro-412-130.png` | OK; estado y acciones visibles |
| rechazada | oscuro | 412 | `h10-verificacion-rechazada-oscuro-412-130.png` | OK; motivo en una línea |
| pendiente | claro | 600 | `h10-verificacion-pendiente-claro-600-130.png` | OK; texto y acciones completos |
| en revisión | claro | 600 | `h10-verificacion-en-revision-claro-600-130.png` | OK; texto y acciones completos |
| aprobada | claro | 600 | `h10-verificacion-aprobada-claro-600-130.png` | OK; estado y acciones completos |
| rechazada | claro | 600 | `h10-verificacion-rechazada-claro-600-130.png` | OK; motivo legible sin corte |
| pendiente | oscuro | 600 | `h10-verificacion-pendiente-oscuro-600-130.png` | OK; contraste y acciones completos |
| en revisión | oscuro | 600 | `h10-verificacion-en-revision-oscuro-600-130.png` | OK; contraste y acciones completos |
| aprobada | oscuro | 600 | `h10-verificacion-aprobada-oscuro-600-130.png` | OK; estado y acciones completos |
| rechazada | oscuro | 600 | `h10-verificacion-rechazada-oscuro-600-130.png` | OK; motivo legible sin corte |

`h10_verificacion_visual_test.dart` pasó 24/24 sin actualización. Se generaron las 20 PNG H10 nuevas en lotes seriales (claro 12/12, oscuro 412 4/4, oscuro 600 4/4); la comparación H10 contra las cuatro referencias H8 oscuras 360 dp pasó 4/4. `h8_verificacion_oscura_test.dart` pasó 1/1 testWidgets (cubre sus cuatro estados). Análisis Dart sin issues y gate frontend `TODO OK` (11/11). Fixtures UUID sintéticas; no se usan documentos ni fotos reales.

## Ingreso — credenciales

Matriz de `PantallaDeSesion` a 360 × 740, 412 × 850 y 600 × 960 dp, DPR 1, ambos temas y texto 130 %. Estados visibles: formulario principal, envío/carga y rechazo de credenciales `422 AP-CU04-01`. El ingreso no tiene un estado vacío de datos. `dioSimulado` y un override sintético del provider aíslan las fixtures del backend real.

| Ancho | Tema | Estado | Captura | Inspección visual |
|---:|---|---|---|---|
| 360 | claro | principal | `h10-ingreso-principal-claro-360-130.png` | OK; formulario y pie completos |
| 360 | claro | cargando | `h10-ingreso-cargando-claro-360-130.png` | OK; progreso y acciones legibles |
| 360 | claro | error | `h10-ingreso-error-claro-360-130.png` | OK; mensaje, referencia, campos y salida al alta dentro del viewport |
| 360 | oscuro | principal | Reutiliza `../h8-ingreso-oscuro-360-130.png` | OK; misma escala/viewport; formulario y pie visibles |
| 360 | oscuro | cargando | `h10-ingreso-cargando-oscuro-360-130.png` | OK; progreso y acciones legibles |
| 360 | oscuro | error | Reutiliza `../h8-ingreso-error-oscuro-360-130.png` | OK; misma escala/viewport; error y salida accesibles |
| 412 | claro | principal | `h10-ingreso-principal-claro-412-130.png` | OK; formulario y pie completos |
| 412 | claro | cargando | `h10-ingreso-cargando-claro-412-130.png` | OK; progreso y pie legibles |
| 412 | claro | error | `h10-ingreso-error-claro-412-130.png` | OK; rechazo y campos completos |
| 412 | oscuro | principal | `h10-ingreso-principal-oscuro-412-130.png` | OK; formulario y pie completos |
| 412 | oscuro | cargando | `h10-ingreso-cargando-oscuro-412-130.png` | OK; progreso y pie legibles |
| 412 | oscuro | error | `h10-ingreso-error-oscuro-412-130.png` | OK; rechazo y campos completos |
| 600 | claro | principal | `h10-ingreso-principal-claro-600-130.png` | OK; formulario y pie completos |
| 600 | claro | cargando | `h10-ingreso-cargando-claro-600-130.png` | OK; progreso y pie legibles |
| 600 | claro | error | `h10-ingreso-error-claro-600-130.png` | OK; rechazo y campos completos |
| 600 | oscuro | principal | `h10-ingreso-principal-oscuro-600-130.png` | OK; formulario y pie completos |
| 600 | oscuro | cargando | `h10-ingreso-cargando-oscuro-600-130.png` | OK; progreso y pie legibles |
| 600 | oscuro | error | `h10-ingreso-error-oscuro-600-130.png` | OK; rechazo y campos completos |

Se inspeccionaron las 18 celdas (16 PNG nuevos y 2 capturas H8 reutilizadas). La prueba comprueba tema, escala, error traducido, rechazo del mensaje técnico y que “Crear mi cuenta” queda dentro del viewport incluso en error a 360 dp. El spinner es indeterminado: para la captura se bombea un solo frame, sin esperar que una animación continua se asiente.

Verificación con Flutter 3.44.8 (comandos ejecutados desde `apps/movil`):

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h10_ingreso_visual_test.dart
00:03 +18: All tests passed!
exit_code=0

flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h10_ingreso_visual_test.dart
00:03 +18: All tests passed!
exit_code=0

dart analyze --fatal-infos test/goldens/h10_ingreso_visual_test.dart
Analyzing h10_ingreso_visual_test.dart...
No issues found!
exit_code=0

python ..\..\scripts\verificar_frontend.py movil
TODO OK
exit_code=0
```

La aserción geométrica del pie también pasó aisladamente para error a 360 dp (1/1); está incluida en las 18 repeticiones anteriores.

## Billetera — saldo

Matriz de `PantallaDeSaldo` a 360 × 740, 412 × 850 y 600 × 960 dp, DPR 1, ambos temas y texto 130 %. Fixtures sintéticas: principal con cuatro movimientos, carga pendiente del saldo, cuenta en cero con feed vacío y error de saldo HTTP 503. `dioSimulado` evita toda llamada real.

| Ancho | Tema | Estado | Captura | Inspección visual |
|---:|---|---|---|---|
| 360 | claro | principal | `h10-billetera-principal-claro-360-130.png` | OK; acciones apiladas con etiquetas completas; lista desplazable |
| 360 | claro | cargando | `h10-billetera-cargando-claro-360-130.png` | OK; cabecera y skeleton legibles |
| 360 | claro | vacio | `h10-billetera-vacio-claro-360-130.png` | OK; saldo cero y aviso completos |
| 360 | claro | error | `h10-billetera-error-claro-360-130.png` | OK; error 503, referencia y reintento completos |
| 360 | oscuro | principal | `h10-billetera-principal-oscuro-360-130.png` | OK; acciones apiladas con etiquetas completas; lista desplazable |
| 360 | oscuro | cargando | `h10-billetera-cargando-oscuro-360-130.png` | OK; cabecera y skeleton legibles |
| 360 | oscuro | vacio | `h10-billetera-vacio-oscuro-360-130.png` | OK; saldo cero y aviso completos |
| 360 | oscuro | error | `h10-billetera-error-oscuro-360-130.png` | OK; error 503, referencia y reintento completos |
| 412 | claro | principal | `h10-billetera-principal-claro-412-130.png` | OK; acciones apiladas con etiquetas completas; lista desplazable |
| 412 | claro | cargando | `h10-billetera-cargando-claro-412-130.png` | OK; cabecera y skeleton legibles |
| 412 | claro | vacio | `h10-billetera-vacio-claro-412-130.png` | OK; saldo cero y aviso completos |
| 412 | claro | error | `h10-billetera-error-claro-412-130.png` | OK; error 503, referencia y reintento completos |
| 412 | oscuro | principal | `h10-billetera-principal-oscuro-412-130.png` | OK; acciones apiladas con etiquetas completas; lista desplazable |
| 412 | oscuro | cargando | `h10-billetera-cargando-oscuro-412-130.png` | OK; cabecera y skeleton legibles |
| 412 | oscuro | vacio | `h10-billetera-vacio-oscuro-412-130.png` | OK; saldo cero y aviso completos |
| 412 | oscuro | error | `h10-billetera-error-oscuro-412-130.png` | OK; error 503, referencia y reintento completos |
| 600 | claro | principal | `h10-billetera-principal-claro-600-130.png` | OK; acciones y cuatro movimientos completos |
| 600 | claro | cargando | `h10-billetera-cargando-claro-600-130.png` | OK; cabecera y skeleton legibles |
| 600 | claro | vacio | `h10-billetera-vacio-claro-600-130.png` | OK; saldo cero y aviso completos |
| 600 | claro | error | `h10-billetera-error-claro-600-130.png` | OK; error 503, referencia y reintento completos |
| 600 | oscuro | principal | `h10-billetera-principal-oscuro-600-130.png` | OK; acciones y cuatro movimientos completos |
| 600 | oscuro | cargando | `h10-billetera-cargando-oscuro-600-130.png` | OK; cabecera y skeleton legibles |
| 600 | oscuro | vacio | `h10-billetera-vacio-oscuro-600-130.png` | OK; saldo cero y aviso completos |
| 600 | oscuro | error | `h10-billetera-error-oscuro-600-130.png` | OK; error 503, referencia y reintento completos |

Las 24 capturas se abrieron. Se detectó truncamiento de las tres acciones en 360 y 412 dp; `AccionesDeSaldo` ahora las apila bajo 420 dp, y la prueba verifica que ninguna etiqueta exceda sus líneas. A 600 dp se conserva la fila. En viewports compactos, la parte inferior de movimientos continúa dentro del `ListView` desplazable; no se observó overflow ni solapamiento.

Verificación con Flutter 3.44.8: golden update 24/24 y repetición sin update 24/24; regresión `pantalla_de_saldo_test.dart` 9/9; análisis Dart limpio y gate frontend 11/11 (`TODO OK`).

No cubierto para H10.S3: las superficies restantes del alcance, error del feed de movimientos, backend/datos reales, dispositivos físicos y rasterizador canónico de CI. El alcance conceptual de 21 unidades está enumerado en `PLAN.md`, H8, párrafo “Alcance de pantallas”; no equivale a 21 rutas: seis son pasos de una ruta de alta, candado es una capa y el inventario H8 informa que grupos/detalle carece de ruta/contrato local. H10.S3.M1 permanece parcial.

### Subida del expediente

Matriz de `PantallaDeSubidaDelExpediente`: en curso y error de red, tres anchos × dos temas, texto 130 %, DPR 1. Fixture de fotos sintéticas; no se invocan red, cámara ni backend.

| Estado | Tema | 360 dp | 412 dp | 600 dp |
|---|---|---|---|---|
| En curso | claro | `h10-subida-en-curso-claro-360-130.png` | `h10-subida-en-curso-claro-412-130.png` | `h10-subida-en-curso-claro-600-130.png` |
| En curso | oscuro | Reutiliza `../h8-subida-oscura-360-130.png` | `h10-subida-en-curso-oscuro-412-130.png` | `h10-subida-en-curso-oscuro-600-130.png` |
| Error de red | claro | `h10-subida-error-red-claro-360-130.png` | `h10-subida-error-red-claro-412-130.png` | `h10-subida-error-red-claro-600-130.png` |
| Error de red | oscuro | `h10-subida-error-red-oscuro-360-130.png` | `h10-subida-error-red-oscuro-412-130.png` | `h10-subida-error-red-oscuro-600-130.png` |

La referencia H8 del error de red difiere en 148 píxeles (0,06 %) y no se modifica; ese estado usa baseline H10. Update y corrida normal pasaron 12/12; se inspeccionaron los 11 PNG nuevos. Detalle de inspección y límites en `LEIDO.md`.

```powershell
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h10_subida_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded test/goldens/h10_subida_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\dart.bat' analyze --fatal-infos test/goldens/h10_subida_visual_test.dart lib/pantallas/identidad/pantalla_subida_del_expediente.dart lib/pantallas/identidad/fila_de_subida.dart
python ..\..\scripts\verificar_frontend.py movil
```

### Prueba de vida — pose frontal inicial

`PantallaDePruebaDeVida` muestra pose frontal, miniaturas pendientes e instrucción manual con cámara falsa silenciosa. Tres tamaños (360 × 740, 412 × 850, 600 × 960 dp), claro/oscuro, texto 130 %, DPR 1. Se deshabilitan animaciones y se fija el reloj para mantener el estado inicial; no se inyectan rostros, no se pulsa «Tomar ahora» y el fake falla si se solicita foto. El estado oscuro/360 coincide exactamente con `h8-prueba-vida-oscura-frente-360-130.png`; se reutiliza sin copiar ni regenerar. Las otras cinco celdas tienen PNG H10 nuevos.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-prueba-vida-inicial-claro-360-130.png` | OK; instrucciones, marco, miniaturas y pie legibles |
| 360 | oscuro | H8 `h8-prueba-vida-oscura-frente-360-130.png` (reutilizada exacta) | OK; comparación pixelaria exacta |
| 412 | claro | `h10-prueba-vida-inicial-claro-412-130.png` | OK; contenido completo |
| 412 | oscuro | `h10-prueba-vida-inicial-oscuro-412-130.png` | OK; contenido completo |
| 600 | claro | `h10-prueba-vida-inicial-claro-600-130.png` | OK; contenido completo |
| 600 | oscuro | `h10-prueba-vida-inicial-oscuro-600-130.png` | OK; contenido completo |

Update/repetición de las cinco capturas H10: 5/5 ambas; H8 exacta en repetición: 1/1. Tests widget/unidad de vida: 39/39; análisis Dart sin issues; gate móvil `TODO OK` (11/11). Actualización segura — saltea la referencia H8 para nunca sobrescribirla:

```powershell
flutter test --no-pub --dart-define=OMITIR_H8_EN_ACTUALIZACION=true --update-goldens --concurrency=1 test/goldens/h10_prueba_vida_visual_test.dart
flutter test --no-pub --concurrency=1 test/goldens/h10_prueba_vida_visual_test.dart
```

### Cámara de identidad — visor concedido

`PantallaDeCamara` con cámara sintética inerte; no se toma foto, no se usan plugins/dispositivo ni archivos de identidad. Consejos plegados. Viewports 360 × 740, 412 × 850 y 600 × 960 dp, ambos temas, texto 130 %, DPR 1. No hay golden H8 compatible, así que las seis capturas son H10 nuevas.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-camara-concedida-claro-360-130.png` | OK; pista, consejos y controles sin solaparse |
| 360 | oscuro | `h10-camara-concedida-oscuro-360-130.png` | OK; pista, consejos y controles sin solaparse |
| 412 | claro | `h10-camara-concedida-claro-412-130.png` | OK; contenido completo |
| 412 | oscuro | `h10-camara-concedida-oscuro-412-130.png` | OK; contenido completo |
| 600 | claro | `h10-camara-concedida-claro-600-130.png` | OK; contenido completo |
| 600 | oscuro | `h10-camara-concedida-oscuro-600-130.png` | OK; contenido completo |

El test comprueba tamaño completo del visor, que los consejos sigan a la cabecera sin solapamiento y que el botón quede en la zona inferior; también falla si se llama a `tomarFoto`. En la revisión de roles se inspeccionaron las seis imágenes y se actualizaron sus baselines: las acciones usan un único acento naranja, texto/superficies mantienen contraste y la cámara sintética no captura. H10 update/repetición: 6/6 ambas; test widget de respaldo: 5/5; `dart analyze` limpio; gate móvil `TODO OK` (11/11). Para repetir desde `apps/movil`:

```powershell
flutter test --no-pub --concurrency=1 test/goldens/h10_camara_visual_test.dart
flutter test --no-pub --concurrency=1 test/widget/pantalla_camara_de_respaldo_test.dart
dart analyze test/goldens/h10_camara_visual_test.dart lib/pantallas/identidad/pantalla_camara.dart lib/pantallas/identidad/pasos_alta/encabezado_y_consejos_de_camara.dart
python ..\..\scripts\verificar_frontend.py movil
```

### Alta — cuenta inicial vacía

Subestado `PasoAlta.cuenta` directo en `PantallaDeRegistro`: 360 × 740, 412 × 850 y 600 × 960 dp; ambos temas; texto 130 %, DPR 1. Fixture con `dioSimulado()` y `AlmacenEnMemoria()`. Los campos están vacíos y sin foco; los ejemplos visibles son placeholders sintéticos del producto. No se abre teclado, no se pulsa «Continuar» y no hay llamada de red.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-alta-cuenta-vacia-claro-360-130.png` | OK; formulario inicia legible y continúa por scroll |
| 360 | oscuro | H8 `h8-alta-oscuro-paso-1-360-130.png` (reutilizada exacta) | OK; comparación pixelaria exacta |
| 412 | claro | `h10-alta-cuenta-vacia-claro-412-130.png` | OK; primer viewport completo, contenido restante desplazable |
| 412 | oscuro | `h10-alta-cuenta-vacia-oscuro-412-130.png` | OK; contraste y campos vacíos visibles |
| 600 | claro | `h10-alta-cuenta-vacia-claro-600-130.png` | OK; formularios completos por scroll vertical |
| 600 | oscuro | `h10-alta-cuenta-vacia-oscuro-600-130.png` | OK; contraste y formulario legibles |

La aserción valida `PasoAlta.cuenta`, teléfono/correo/clave vacíos, ningún `EditableText` enfocado y teclado oculto. H10 update/repetición: 5/5 y 6/6; H8 oscuro/360 exacto: 1/1; `dart analyze` limpio; gate móvil `TODO OK` (11/11). Actualización segura que evita escribir sobre H8:

### Alta — actividad/perfil transaccional inicial vacío

Subestado `PasoAlta.actividad` en `PantallaDeRegistro`, avanzando el notifier de manera sintética hasta el quinto paso: 360 × 740, 412 × 850 y 600 × 960 dp; claro/oscuro, texto 130 %, DPR 1. `dioSimulado()` y `AlmacenEnMemoria()`. Origen de fondos, actividad y monto vacíos; selectores cerrados, sin foco ni teclado. No se pulsa «Continuar» ni se llama al backend.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-alta-actividad-vacia-claro-360-130.png` | OK; campos y acción completos, sin solapamiento |
| 360 | oscuro | H8 `h8-alta-oscuro-paso-5-360-130-scroll-restablecido.png` (reutilizada exacta) | OK; comparación pixelaria exacta |
| 412 | claro | `h10-alta-actividad-vacia-claro-412-130.png` | OK; tarjeta de confianza y acción completas |
| 412 | oscuro | `h10-alta-actividad-vacia-oscuro-412-130.png` | OK; contraste, campos y acción completos |
| 600 | claro | `h10-alta-actividad-vacia-claro-600-130.png` | OK; formulario y tarjeta dentro del viewport |
| 600 | oscuro | `h10-alta-actividad-vacia-oscuro-600-130.png` | OK; formulario y tarjeta dentro del viewport |

Prueba: `flutter test --no-pub test/goldens/h10_alta_actividad_visual_test.dart test/widget/paso_perfil_transaccional_test.dart` (11/11); la actualización generó cinco PNG H10 y omitió explícitamente la referencia H8; repetición normal comparó las seis celdas. `dart analyze --fatal-infos` limpio; gate móvil `TODO OK` (11/11).

### Alta — paso previo al contrato

Subestado `PasoAlta.contrato` de `PantallaDeRegistro`: 360 × 740, 412 × 850 y 600 × 960 dp; claro/oscuro, texto 130 %, DPR 1; fixture sintético con Dio y almacén en memoria. La captura es antes de abrir `/identidad/contrato`: muestra aviso de último paso y «Continuar», sin navegación, consentimiento ni envío.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-alta-contrato-previo-claro-360-130.png` | OK; texto y botón completos |
| 360 | oscuro | H8 `h8-alta-oscuro-paso-6-360-130-scroll-restablecido.png` (reutilizada exacta) | OK; comparación pixelaria exacta |
| 412 | claro | `h10-alta-contrato-previo-claro-412-130.png` | OK; mensaje y acción íntegros |
| 412 | oscuro | `h10-alta-contrato-previo-oscuro-412-130.png` | OK; contraste y acción completos |
| 600 | claro | `h10-alta-contrato-previo-claro-600-130.png` | OK; texto y acción completos |
| 600 | oscuro | `h10-alta-contrato-previo-oscuro-600-130.png` | OK; contraste y acción completos |

`flutter test --no-pub test/goldens/h10_alta_contrato_visual_test.dart test/widget/paso_contrato_de_alta_test.dart`: 8/8; H8 comparado exacto y omitido de actualización; cinco PNG H10 nuevos. `dart analyze --fatal-infos` limpio. No se llama `onIrAlContrato` ni se monta la ruta legal.

### Alta — correo inicial, OTP vacío

Subestado `PasoAlta.correo` con datos de contacto en blanco: tres anchos (360 × 740, 412 × 850 y 600 × 960 dp), ambos temas, texto 130 %, DPR 1. Fixture Dio simulado/almacén en memoria. No hay código, destino verificado ni foco; el teclado permanece oculto, no se solicita ni verifica código y «Continuar» queda deshabilitado. El texto de destino conserva la elipsis del estado sin dirección; H8 oscuro/360 se reutilizó solo tras comparación exacta.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-alta-correo-inicial-claro-360-130.png` | OK; seis celdas OTP y acciones completas |
| 360 | oscuro | H8 `h8-alta-oscuro-paso-2-360-130.png` (reutilizada exacta) | OK; comparación pixelaria exacta |
| 412 | claro | `h10-alta-correo-inicial-claro-412-130.png` | OK; texto, OTP y tarjeta de confianza visibles |
| 412 | oscuro | `h10-alta-correo-inicial-oscuro-412-130.png` | OK; contraste y acciones completos |
| 600 | claro | `h10-alta-correo-inicial-claro-600-130.png` | OK; contenido y acciones completos |
| 600 | oscuro | `h10-alta-correo-inicial-oscuro-600-130.png` | OK; contraste y contenido completos |

`flutter test --no-pub test/goldens/h10_alta_correo_visual_test.dart`: actualización segura 5/5 y repetición total 6/6; `test/identidad/estructura_del_alta_test.dart`: 4/4; análisis dirigido sin problemas; gate móvil `TODO OK` (11/11). El test comprueba que los seis OTP estén vacíos y sin foco; no contiene correo de persona real.

### Alta — datos personales iniciales vacíos

Subestado `PasoAlta.datos`: 360 × 740, 412 × 850 y 600 × 960 dp; claro/oscuro, texto 130 %, DPR 1. Fixture sintético `dioSimulado()` + `AlmacenEnMemoria()` y estado inicial vacío; no se ingresa información personal, no se enfoca ningún campo ni abre el selector de fecha. A 360/412 dp los controles inferiores siguen por scroll; a 600 dp el formulario y su resumen/acción quedan completos.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-alta-datos-inicial-claro-360-130.png` | OK; etiquetas/campos legibles, contenido sigue por scroll |
| 360 | oscuro | H8 `h8-alta-oscuro-paso-4-360-130-scroll-restablecido.png` (reutilizada exacta) | OK; comparación pixelaria exacta |
| 412 | claro | `h10-alta-datos-inicial-claro-412-130.png` | OK; primeros campos y fecha legibles; desplazable |
| 412 | oscuro | `h10-alta-datos-inicial-oscuro-412-130.png` | OK; contraste y contenido desplazable |
| 600 | claro | `h10-alta-datos-inicial-claro-600-130.png` | OK; campos, resumen y acción completos |
| 600 | oscuro | `h10-alta-datos-inicial-oscuro-600-130.png` | OK; contraste, resumen y acción completos |

`flutter test --no-pub test/goldens/h10_alta_datos_visual_test.dart test/widget/paso_datos_test.dart`: actualización H10 segura 5/5; repetición con H8 y widget 8/8. `dart analyze --fatal-infos` limpio; gate móvil `TODO OK` (11/11). H8 se omitió expresamente al actualizar.

### Alta — documento inicial, sin fotos

Subestado `PasoAlta.documento`: 360 × 740, 412 × 850 y 600 × 960 dp; ambos temas, texto 130 %, DPR 1. Fixture con Dio simulado y almacén en memoria. CI, expedición y vencimiento vacíos; cinco caras sin captura; no se da foco, no se abre selector/calendario ni se acciona «Tomar foto», pautas o envío. Sin cámara, permisos ni dispositivo al renderizar.

| Ancho | Tema | Captura | Inspección visual |
|---:|---|---|---|
| 360 | claro | `h10-alta-documento-inicial-claro-360-130.png` | OK; anverso/acciones visibles; campos siguen por scroll |
| 360 | oscuro | `h10-alta-documento-inicial-oscuro-360-130.png` | OK; contraste conservado; vista inicial desplazable |
| 412 | claro | `h10-alta-documento-inicial-claro-412-130.png` | OK; anverso y botón completos; CI empieza abajo |
| 412 | oscuro | `h10-alta-documento-inicial-oscuro-412-130.png` | OK; contraste, carril y anverso completos |
| 600 | claro | `h10-alta-documento-inicial-claro-600-130.png` | OK; campos CI/expedición empiezan visibles |
| 600 | oscuro | `h10-alta-documento-inicial-oscuro-600-130.png` | OK; contraste y primer viewport sin overflow |

Se generaron seis PNG H10 propios; no se reutiliza H8 porque `h8-alta-oscuro-paso-3-360-130.png` presentó 1.056 píxeles de diferencia (0,40 %) con Flutter 3.47.5 y queda intacto. El carrusel deja ver deliberadamente parte de la siguiente tarjeta para sugerir desplazamiento; el anverso activo queda completo. `flutter test --no-pub test/goldens/h10_alta_documento_visual_test.dart test/widget/paso_capturas_test.dart`: 10/10; `dart analyze --fatal-infos` limpio; gate móvil `TODO OK` (11/11).

```powershell
flutter test --no-pub --dart-define=OMITIR_H8_EN_ACTUALIZACION=true --update-goldens --concurrency=1 test/goldens/h10_alta_cuenta_visual_test.dart
flutter test --no-pub --concurrency=1 test/goldens/h10_alta_cuenta_visual_test.dart
```

### Centro de ayuda

Matriz de `PantallaCentroDeAyuda` con tres tutoriales sintéticos: inicio con recomendado y lista desplazada hasta el último tutorial × 360 × 740, 412 × 850 y 600 × 960 dp × claro/oscuro; texto 130 %, DPR 1. No se lanzan tutoriales ni se inventan carga/error.

| Estado | Tema | 360 dp | 412 dp | 600 dp |
|---|---|---|---|---|
| Inicio | claro | `h10-ayuda-inicio-claro-360-130.png` | `h10-ayuda-inicio-claro-412-130.png` | `h10-ayuda-inicio-claro-600-130.png` |
| Inicio | oscuro | `h10-ayuda-inicio-oscuro-360-130.png` | `h10-ayuda-inicio-oscuro-412-130.png` | `h10-ayuda-inicio-oscuro-600-130.png` |
| Lista desplazada | claro | `h10-ayuda-lista-claro-360-130.png` | `h10-ayuda-lista-claro-412-130.png` | `h10-ayuda-lista-claro-600-130.png` |
| Lista desplazada | oscuro | `h10-ayuda-lista-oscuro-360-130.png` | `h10-ayuda-lista-oscuro-412-130.png` | `h10-ayuda-lista-oscuro-600-130.png` |

H8 oscuro/360 difiere en la captura inicial (1.286 píxeles, 0,48 %); no se reutilizaron ni alteraron sus PNG. Las 12 capturas H10 pasan con/sin actualización; análisis limpio, regresiones de widget/a11y/unidad 18/18 y gate móvil 11/11. Revisión visual en `LEIDO.md`.

```powershell
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h10_ayuda_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded test/goldens/h10_ayuda_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\dart.bat' analyze --fatal-infos test/goldens/h10_ayuda_visual_test.dart lib/pantallas/soporte/pantalla_centro_de_ayuda.dart lib/pantallas/soporte/textos.dart
python ..\..\scripts\verificar_frontend.py movil
```

### Candado — capa de bloqueo al arranque

Matriz de `CapaDeBloqueo` bloqueada por arranque, fixture de proveedor biométrico sin confirmar, 360 × 740, 412 × 850 y 600 × 960 dp; ambos temas, texto 130 %, DPR 1. Solo se representa el estado bloqueado confirmado por fuente/test; no se inventan éxito biométrico, desbloqueo, sesión vencida ni navegación.

| Tema | 360 dp | 412 dp | 600 dp |
|---|---|---|---|
| claro | `h10-candado-inicial-claro-360-130.png` | `h10-candado-inicial-claro-412-130.png` | `h10-candado-inicial-claro-600-130.png` |
| oscuro | `h10-candado-inicial-oscuro-360-130.png` | `h10-candado-inicial-oscuro-412-130.png` | `h10-candado-inicial-oscuro-600-130.png` |

Los PNG de H1 no se reutilizan: no corresponden a los mismos altos ni fijan escala 130 %. Update y repetición normal: 6/6; análisis limpio, regresiones widget/unidad relacionadas 20/20 y gate móvil 11/11. Se inspeccionaron las seis capturas, sin cortes ni solapamientos.

```powershell
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h10_candado_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded test/goldens/h10_candado_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\dart.bat' analyze --fatal-infos test/goldens/h10_candado_visual_test.dart lib/pantallas/identidad/capa_de_bloqueo.dart lib/pantallas/identidad/capa_de_bloqueo_vista.dart
python ..\..\scripts\verificar_frontend.py movil
```

### Perfil

Matriz del perfil inicial vacío a 360 × 740, 412 × 850 y 600 × 960 dp, ambos temas, texto 130 %, DPR 1. No se escribe correo ni se invoca guardar, navegación, cierre de sesión o baja.

| Tema | 360 dp | 412 dp | 600 dp |
|---|---|---|---|
| claro | `h10-perfil-inicial-claro-360-130.png` | `h10-perfil-inicial-claro-412-130.png` | `h10-perfil-inicial-claro-600-130.png` |
| oscuro | `h10-perfil-inicial-oscuro-360-130.png` | `h10-perfil-inicial-oscuro-412-130.png` | `h10-perfil-inicial-oscuro-600-130.png` |

El H8 oscuro/360 difiere en 444 píxeles (0,17 %) con Flutter 3.44.8; no se modifica ni reutiliza. Las seis capturas H10 propias pasaron con actualización y sin actualización (6/6 cada una) y fueron inspeccionadas. El test widget vecino de cierre de sesión pasó 1/1; análisis Dart limpio; gate móvil `TODO OK` (11/11). Más límites en `LEIDO.md`.

```powershell
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h10_perfil_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded test/goldens/h10_perfil_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\dart.bat' analyze --fatal-infos test/goldens/h10_perfil_visual_test.dart lib/pantallas/identidad/pantalla_perfil.dart
python ..\..\scripts\verificar_frontend.py movil
```

### MFA — segundo factor

Matriz de `PantallaDeMfa` a 360 × 740, 412 × 850 y 600 × 960 dp, DPR 1, ambos temas y texto 130 %. Se cubren principal, carga y error sintético `422 AP-CU04-04`; el estado vacío no aplica a un desafío OTP. Dos capturas oscuras de 360 dp son reutilizaciones exactas de H8; las otras 16 se generan aquí. La carga usa el botón del sistema de diseño y deshabilita las seis casillas, sin indicador suelto de pantalla.

| Ancho | Tema | Estado | Captura | Inspección visual |
|---:|---|---|---|---|
| 360 | claro | principal | `h10-mfa-principal-claro-360-130.png` | OK; seis casillas y ayuda dentro del viewport |
| 360 | claro | cargando | `h10-mfa-cargando-claro-360-130.png` | OK; botón y rótulo completos; casillas deshabilitadas |
| 360 | claro | error | `h10-mfa-error-claro-360-130.png` | OK; seis dígitos, mensaje completo en dos líneas |
| 360 | oscuro | principal | Reutiliza `../h8-mfa-oscuro-360-130.png` | H8; captura exacta y compatible |
| 360 | oscuro | cargando | `h10-mfa-cargando-oscuro-360-130.png` | OK; botón y rótulo completos; casillas deshabilitadas |
| 360 | oscuro | error | Reutiliza `../h8-mfa-error-oscuro-360-130.png` | H8; captura exacta y compatible |
| 412 | claro | principal | `h10-mfa-principal-claro-412-130.png` | OK; seis casillas y ayuda legibles |
| 412 | claro | cargando | `h10-mfa-cargando-claro-412-130.png` | OK; botón y rótulo completos |
| 412 | claro | error | `h10-mfa-error-claro-412-130.png` | OK; error en una línea; casillas completas |
| 412 | oscuro | principal | `h10-mfa-principal-oscuro-412-130.png` | OK; seis casillas y ayuda legibles |
| 412 | oscuro | cargando | `h10-mfa-cargando-oscuro-412-130.png` | OK; botón y rótulo completos |
| 412 | oscuro | error | `h10-mfa-error-oscuro-412-130.png` | OK; error en una línea; casillas completas |
| 600 | claro | principal | `h10-mfa-principal-claro-600-130.png` | OK; casillas distribuidas sin corte |
| 600 | claro | cargando | `h10-mfa-cargando-claro-600-130.png` | OK; botón ocupa el ancho disponible |
| 600 | claro | error | `h10-mfa-error-claro-600-130.png` | OK; seis dígitos y error completos |
| 600 | oscuro | principal | `h10-mfa-principal-oscuro-600-130.png` | OK; casillas distribuidas sin corte |
| 600 | oscuro | cargando | `h10-mfa-cargando-oscuro-600-130.png` | OK; botón ocupa el ancho disponible |
| 600 | oscuro | error | `h10-mfa-error-oscuro-600-130.png` | OK; seis dígitos y error completos |

Las 16 capturas nuevas se abrieron una por una; no observé clipping, solapamiento ni texto ilegible. El error envuelve correctamente en claro a 360 dp. La carga muestra el estado de envío según el componente `Boton` y los campos OTP quedan deshabilitados. Pruebas Flutter 3.44.8: golden update 18/18 y repetición 18/18; H8 MFA focalizado 2/2 PASS. Análisis Dart dirigido sin issues y gate móvil 11/11 (`TODO OK`). Comandos reproducibles desde `apps/movil`:

```powershell
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h10_mfa_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded test/goldens/h10_mfa_visual_test.dart
& 'C:\src\flutter-3.44.8\flutter\bin\flutter.bat' test --no-pub --concurrency=1 --reporter expanded --plain-name MFA test/goldens/h8_portada_mfa_oscuro_test.dart
dart analyze --fatal-infos test/goldens/h10_mfa_visual_test.dart lib/pantallas/identidad/pantalla_mfa.dart ../../packages/diseno_flutter/lib/atomos/campo_o_t_p.dart
python ..\..\scripts\verificar_frontend.py movil
```
