> **AVANCE: 182 / 213 — 85,4 %.** (incluye 3 microtareas H0 adicionales; alcance del prompt original: 179 / 210 — 85,2 %.)

# Reporte — Alta Atlas, sesión y experiencia móvil

- Fecha: 2026-10-13 · Plan: [PLAN.md](./PLAN.md) · Rama: `codex/alta-atlas-sesion-premium-2026-10-11`
- Peldaño de evidencia alcanzado: `TESTED` para los flujos enumerados abajo; `WRITTEN/REVIEWED` para lo que sigue parcial. No equivale a QA visual en hardware.

## COMPLETADO

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H4.S5.M5 | Bloque de formulario integrado y localizado en el paso de perfil | Suite dirigida indicada en [evidencia](./evidencia/cierre-parcial-2026-10-12.md) | PASS; 5 pruebas de perfil dentro de 32 |
| H0.S1.M4 | Tokens Dart generados, octava fachada semántica de elevación, sincronía y gate frontend | [evidencia](./evidencia/h0-s1-m4.md) | PASS; clases 8, hashes iguales, pruebas 10/10 + 24/24, análisis y gate limpios |
| H2.S1.M2 | Clasificación de todos los errores actualmente registrados en la tabla de reproducción | [evidencia H2](./evidencia/h2-reproduccion.md#h2s1m2-auditoría-de-clasificación) | PASS de clasificación; 1/1 fila `ENVIRONMENT`, vinculada a hipótesis nueva; M1/M3 siguen parciales |
| H5.S4.M2 | Pautas públicas de foto, siete parejas de ejemplos correctos/incorrectos e ilustraciones vectoriales propias | [pruebas y captura](./evidencia/h5-s4-m2.md) | PASS; ruta anónima 2/2 y golden 360×800 inspeccionado, sin excepciones de layout |
| H4.S5.M3 | Seis pasos reorganizados en cuatro fases; correo renombrado; datos preservados al retroceder y el POST conserva su payload | [evidencia del refactor](./evidencia/h4-s5-m3-refactor.md) | PASS; pruebas del flujo dirigidas en serie, incluida actividad 5/5 y selector 6/6 |
| H4.S5.M6 | Error remoto visible arriba y desplazamiento comprobado en los siete pasos del alta | `flutter test --concurrency=1 --reporter expanded --timeout 60s test/identidad/error_alta_desplazamiento_test.dart` | PASS; siete pasos; alerta de contrato duplicada eliminada |
| H4.S5.M7 | Tarjeta de confianza integrada al final del bloque y ubicación verificada | Suite dirigida indicada en evidencia | PASS; cubierto por prueba widget del perfil |
| H4.S5.M8 | Protección de pantalla en fases de cuenta/identidad y login | `flutter test --concurrency=1 --reporter expanded --timeout 60s test/identidad/proteccion_alta_ingreso_test.dart` | PASS; 2/2 con puerto falso |
| H5.S1.M2 | No queda ninguna salida «Escribir a mano» en el alta; la galería permanece disponible ante permiso denegado | `git grep -n -i -E 'escribirAMano|a mano' apps/movil/lib` + test de tarjeta | PASS; grep sin coincidencias y 2/2 pruebas |
| H5.S2.M5 | El botón secundario de capturas abre pautas y navega al contenido de fotos de rostro | `flutter test test/widget/paso_capturas_test.dart --reporter expanded` | PASS; contenido de pautas, selección de «Foto de frente» y scroll verificados |
| H5.S2.M3 | Puntos del carrusel saltan a la lámina, tienen área 48×48 dp y muestran estado completo con `t.ok` | `flutter test --reporter expanded test/widget/paso_capturas_test.dart` | PASS; 3/3; color semántico verificado para los cinco puntos |
| H5.S2.M1 | Cinco láminas, insignia 0→1, avance al reverso y estado de éxito a 5/5 verificados | `flutter test --reporter expanded test/widget/paso_capturas_test.dart` | PASS; 3/3; el caso completo también valida `Tono.ok` |
| H5.S2.M4 | Imagen local usa decode cache de tamaño de pantalla, reemplazo gapless y hueco visible con archivo ausente | `flutter test --reporter expanded test/widget/tarjeta_de_captura_ampliada_test.dart` | PASS; 1/1; cache 720 px a 360 dp/DPR 2 y fallback visible |
| H5.S2.M2 | Carrusel 0,86 con peek; lámina 300 dp a viewport 360 dp y ampliación Hero desde el toque a la imagen | [evidencia + golden](./evidencia/h5-s2-m2.md) | PASS; tests de carrusel, Hero, zoom y cierre; captura inspeccionada |
| H9.S1.M1 | Skill de diseño cotejada con tokens, componentes, catálogo y reglas; referencia falsa a `MovimientoDeInterfaz` corregida | [evidencia H9](./evidencia/h9-verificacion-de-skills.md) | PASS; discrepancia corregida contra `movimiento.dart` |
| H9.S1.M3 | Skill de movimiento cotejada con duraciones, curvas y piezas reales | [evidencia H9](./evidencia/h9-verificacion-de-skills.md) | PASS; valores y seis piezas confirmados |
| H9.S1.M4 | Inventario de identidad, catálogo y criterio de 200 líneas cotejados con el checkout | [evidencia H9](./evidencia/h9-verificacion-de-skills.md) | PASS; 31/31 nombres presentes en la ruta indicada; gate verificado |
| H9.S1.M5 | §95.8 enumera skills D2 y las cuatro adaptaciones Pasanaku D4 | `git diff -- .claude/rules/95-frontend.md` | PASS; lista actualizada |
| H9.S1.M6 | Espejo de skills sincronizado por rutas y contenido | Comparación recursiva de 289 rutas + SHA-256 | PASS; 289/289 idénticos |
| H10.S1.M1 | Análisis estático de móvil sin avisos | `dart analyze --fatal-infos` | PASS; No issues found |
| H10.S1.M2 | Formato de todos los archivos `lib` y `test` de móvil verificado | `dart format --output=none --set-exit-if-changed lib test` | PASS; 383 archivos, 0 cambios pendientes |
| H10.S1.M3 | Gates estructurales y de tokens del frontend | `python scripts/verificar_frontend.py movil` | PASS; TODO OK |
| H10.S1.M4 | Regresión integral dentro de la baseline H0 | `flutter test --concurrency=1 --reporter expanded test/unidad test/widget test/contrato test/identidad test/pasanaku --timeout 60s` | 566 PASS; único fallo es el timer conocido de `guardian_de_sesion_test.dart` presente en H0 |
| H10.S1.M5 | Suite de accesibilidad | `flutter test --concurrency=1 test/a11y` | PASS; 4/4 |
| H10.S4.M2 | Rechazo de redirección externa con `volver` | `flutter test --concurrency=1 --reporter expanded test/unidad/acceso_a_rutas_test.dart` | PASS; 4/4, incluida ruta externa rechazada y destino privado conservado |
| H10.S4.M6 | Amenazas y riesgos residuales documentados | Esta sección §Seguridad | Controles, evidencia disponible y verificaciones pendientes separados explícitamente |
| H3.S1.M7 | Semántica del selector, foco inicial en título, atrás y retorno de foco | Dos tests widget del paquete + `flutter test --concurrency=1 --reporter expanded test/a11y` | PASS; 15/15 paquete y 6/6 a11y; evidencia [H3.S1.M7](./evidencia/h3-s1-m7.md) |
| H4.S5.M4 | Cabecera de fase Atlas con progreso animado | `flutter test --concurrency=1 --reporter expanded test/identidad/componentes_de_fase_test.dart` | PASS; 4/4; golden 360×800 revisado: [captura](./evidencia/h4-s5-m4-cabecera-fase-2-360.png) |
| H4.S1.M3 | Ejemplos de campos del alta y del ingreso; hint de contraseña completo | [pruebas rojas/verdes y análisis](./evidencia/h4-s1-m3-verificacion-2026-10-13.md) | PASS; 4/4 + 4/4; análisis dirigido sin incidencias |
| H0.S1.M3 | Baseline del SDK del repo instalada en paralelo, sin sustituir la global | `C:\src\flutter-3.44.8\flutter\bin\flutter.bat --version` | Flutter 3.44.8 / Dart 3.12.2; archivo contrastado con SHA oficial; [salida](./evidencia/h0-s1-m3-flutter-3.44.8.md) |
| H10.S1.M6 | Suite completa de goldens de la app, revalidada serialmente con Flutter 3.47.5 | [salida histórica y revalidación actual](./evidencia/h10-app-goldens-flutter-3.47.5-2026-10-13.md) | A MEDIAS; 52/268 PASS y 216/268 FAIL por diffs golden, sin `--update-goldens`; falta clasificar los PNG de diferencias y validar/revisar en runner canónico |
| H10.S1.M7 | Suite de `diseno_flutter` con Flutter 3.47.5 | [revalidación local y baselines no canónicas](./evidencia/h10-goldens-flutter-3.44.8-2026-10-12.md) | A MEDIAS; 118/118 PASS en Windows, pero las cinco baselines nuevas requieren verificación en el runner canónico Linux |
| H8.S1.M3 | Barrido de alta, billetera y perfil con entradas extremas y texto al 200 % | [evidencia y seis capturas revisadas](./evidencia/h8-break-ui.md) | Suite actualizada PASS 6/6; prueba de 40 movimientos comprueba las 40 filas al 200 % y captura el extremo inferior de la lista |
| H4.S3.M5 | Decisión visual sobre icono de CI propio | [dos capturas y decisión](./evidencia/h4-s3-icono.md) | PASS; `badge_outlined` se reconoce como documento, se descarta dibujar una variante redundante |
| H10.S3.M4 | Texto grande 200 % en ingreso, alta y billetera | [7 capturas inspeccionadas](./evidencia/h10-texto-200.md) | PASS; 3 tests, hint, rótulo y monto corregidos; scroll sin excepciones de layout |
| H10.S3.M3 | Rúbrica final del gate visual | [evidencia posterior](./evidencia/h8-rubrica-despues.md) | PASS del criterio literal: 0 críticos, 0 mayores; tres suites dirigidas y análisis/gate limpios |
| H10.S1.M8 | Verificación backend/Gradle | Evidencia de bloqueo backend | BLOQUEADO; PostgreSQL está activo solo en 5432 (el build requiere 5433), no hay credenciales de prueba disponibles, Docker daemon inaccesible y memoria libre insuficiente para el heap configurado |
| H8.S1.M1 | Auditoría de movimiento (solo lectura) en alta, billetera y perfil | [evidencia H8](./evidencia/h8-oportunidades.md) | PASS; 7 oportunidades priorizadas, 5 candidatos rechazados explícitamente, sin cambios de código durante auditoría |
| H8.S1.M2 | Rúbrica visual previa y revisión anti-slop de las superficies disponibles | [evidencia y capturas](./evidencia/h8-rubrica-antes.md) | Auditoría HECHA; veredicto visual RECHAZADA por hallazgos mayores; 20/21 unidades disponibles, sin cambios de UI |
| H8.S4.M5 | Rúbrica `ui-quality-review` posterior a ajustes | [comparación antes/después](./evidencia/h8-rubrica-despues.md) | PASS del gate de severidad: 0 críticos y 0 mayores en 20 superficies disponibles; límites físicos y de cobertura explícitos |
| H8.S2.M1 | `Movimiento` documentado como fuente Flutter y duraciones estéticas de pantalla centralizadas | [evidencia](./evidencia/h8-s2-m1-movimiento.md) | PASS; grep deja solo cuatro tiempos de dominio; 23 pruebas dirigidas |
| H8.S3.M1 | Hundido de controles y destinos de pestaña completos; accesibilidad y hit target | [evidencia](./evidencia/h8-s3-m1.md) | PASS; 11 pruebas de movimiento, 1 dirigida de barra, 24 de comportamiento y 9 rutas |
| H9.S1.M2 | Skill de UX de sesión contrastada y corregida contra rutas, biometría, cierre, tokens y haptics reales | [evidencia H9](./evidencia/h9-verificacion-de-skills.md) | PASS; cinco afirmaciones falsas/generalizadas corregidas; 0 afirmaciones falsas restantes en el alcance verificado |
| H5.S3.M1 | Rótulos de repetición distinguen las cinco caras, tanto en captura como en subida | `flutter test test/unidad/textos_de_captura_test.dart` | PASS; 3 pruebas (incluye copy de vida) |
| H5.S3.M5 | Reemplazo, abandono, limpieza tras subida e impresión de captura sin ruta privada | Suite dirigida de privacidad/subida | PASS; 15 pruebas de esos grupos dentro de 32 |
| H5.S4.M1 | Hoja previa muestra las pautas iOS y espera a que se cierre antes de abrir el escáner | Suite dirigida de hoja/escáner | PASS; 1 prueba de hoja |
| H6.S1.M1 | Goldens de las tres poses (300×400) en claro y oscuro, revisados visualmente | `flutter test test/widget/silueta_de_cara_test.dart --reporter expanded` | PASS; 6 goldens dentro de 5 pruebas |
| H6.S1.M2 | Pintor vectorial, velo y guías de las tres poses | Misma suite de silueta | PASS |
| H6.S1.M3 | Tokens de cámara iguales en ambos temas | `tokens.json` + `silueta_de_cara_test.dart` | PASS; roles y colores comprobados |
| H6.S1.M4 | Interpolación de color/grosor y estado con movimiento reducido | Misma suite de silueta | PASS |
| H6.S3.M1 | Casos widget del flujo con cámara falsa y resultado verificado | `flutter test test/widget/prueba_de_vida_test.dart --reporter expanded` | PASS; 10/10 |
| H6.S3.M3 | Copy de poses, cuenta, foto tomada y final documentado con tests | Suite dirigida de textos de captura | PASS |
| H6.S3.M4 | Disparo, rechazo/repetición, miniaturas y destello del flujo | `flutter test test/widget/prueba_de_vida_test.dart --reporter expanded` | PASS; 10/10 |
| H6.S3.M5 | Avance automático entre tres poses y mapa devuelto al tocar Continuar | `flutter test test/widget/prueba_de_vida_test.dart --reporter expanded` | PASS; 10/10 |
| H6.S4.M1 | Tocar selfie pendiente o ya capturada selecciona desde esa pose o solo la repetición | `test/widget/prueba_de_vida_test.dart` (posesDesde) | PASS; helper cubre pendientes y repetición |
| H6.S4.M2 | Round-trip de prueba de vida: envía las poses pendientes, registra el mapa devuelto y avanza el carrusel | `flutter test test/widget/paso_capturas_test.dart --reporter expanded` | PASS; 3/3 |
| H6.S3.M6 | Miniaturas semánticas, estado repetible, estado pendiente y ancho de caché verificados | `flutter test test/unidad/ciclo_de_pose_test.dart test/widget/miniaturas_de_vida_test.dart test/widget/aviso_permiso_prueba_de_vida_test.dart --reporter compact` | PASS; `+16: All tests passed!` |
| H6.S3.M7 | Copy/acciones de permiso denegado y bloqueado verificados | Misma suite de ciclo/miniaturas/permisos | PASS; `+16: All tests passed!` |
| H8.S3.M7 | Animación de saldo, feed escalonado (máximo ocho), esqueleto brillante y refresco con haptic falso | Suites dirigidas de billetera y `MontoQueSube` | PASS; 9/9 y 10/10; límites backend/dispositivo/historial descritos en [evidencia](./evidencia/h8-s3-m7.md) |

### H8.S3.M7 — billetera (criterio UI HECHO)
- Qué anda: `GET /billetera/{cuentaId}/movimientos` lee el libro bajo contexto RLS; la UI muestra concepto, fecha y monto, escalona únicamente ocho filas a 30 ms y agrupa las siguientes. Saldo/secciones aparecen sin retraso global; `MontoQueSube` usa duración 220 ms y curva compartida `Movimiento.llega`. Tests widget: billetera 9/9 y átomo 10/10; análisis estático dirigido y generación servidor/cliente pasan.
- Límites fuera del criterio de esta microtarea: prueba real del endpoint bajo PostgreSQL/jOOQ, haptic en dispositivo y cursor antes de afirmar que la lista cubre historial ilimitado. No se afirma integración E2E, haptic físico ni historial ilimitado.
- Dónde quedó: [evidencia H8.S3.M7](./evidencia/h8-s3-m7.md), `apps/movil/lib/pantallas/billetera/` y `packages/diseno_flutter/lib/atomos/esqueleto_brillante.dart`.

## A MEDIAS

### H5.S4.M3 — Cámara de respaldo
- Qué anda: la cámara de respaldo cubre visor, marco ID-1, consejos superpuestos, captura y cancelación; sus pruebas widget pasan. H5.S4.M2 queda cerrado con ruta pública probada y golden 360×800 inspeccionado.
- Qué no anda: no hay captura revisada en emulador/dispositivo ni prueba de cámara física, permisos y errores reales.
- Qué falta exactamente: probar permisos/errores de cámara y revisar la composición en un dispositivo.
- Dónde quedó: `apps/movil/lib/pantallas/identidad/{pantalla_camara.dart,guia_de_encuadre.dart}`; pautas: [evidencia H5.S4.M2](./evidencia/h5-s4-m2.md).

### H6.S3.M8 — Rendimiento de vida
- Qué anda: el visor, la silueta y la cuenta atrás están aislados para repintado; el flujo automatizado y sus pruebas usan cámara falsa.
- Qué falta exactamente: medir en Android de gama media que la cuenta atrás no supere 16 ms por cuadro. Esta validación física no se considera satisfecha por los tests widget/unitarios.
- Dónde quedó: `apps/movil/lib/pantallas/identidad/` y [evidencia H6.S3.M9](./evidencia/h6-s3-m9.md).

### QA integral
- Qué anda: `dart analyze --fatal-infos` queda limpio; formato de los 383 archivos de `lib` y `test` verificado; gate estructural TODO OK; accesibilidad móvil 4/4; la regresión `unidad/widget/contrato/identidad/pasanaku` termina 566 PASS y un único fallo de timer ya presente en H0. H10.S1.M1–M5 están HECHO.
- Qué no anda: con Flutter 3.44.8, `apps/movil/test/goldens` termina 5 PASS y 15 FAIL por comparación pixelaria; los 5 nuevos golden de estrés H8 pasan y los 15 baselines preexistentes continúan discrepando. `packages/diseno_flutter` termina 99 PASS y 15 FAIL, que se reproducen en `test/goldens` (3 PASS, 15 FAIL). Tras corregir el ticker de `HundidoAlTocar`, los fallos de imagen permanecen; no se regeneraron baselines preexistentes. La geometría funcional del selector con teclado ya está corregida y sus 6 pruebas pasan.
- Qué falta exactamente: ejecutar goldens en el rasterizador/plataforma canónicos y actualizar o aceptar sus baselines tras revisión; ejecutar compilación/integración backend cuando haya PostgreSQL/Docker y memoria suficiente. Gradle no se lanzó: PostgreSQL/Docker no están disponibles y la configuración pide 3 GiB de heap, por encima de la memoria libre observada.
- Dónde quedó: [resultados Flutter 3.44.8](./evidencia/h10-goldens-flutter-3.44.8-2026-10-12.md), evidencia de cierre y directorios `apps/movil/test/goldens/failures` y `packages/diseno_flutter/test/goldens/failures` (salidas generadas por tests).

### Otras microtareas parciales
- Qué anda: las bases de H0–H7 y H1/H2/H3/H4 tienen implementación y evidencia previa en `evidencia/`.
- Qué no anda: el estado oficial conserva 20 microtareas `A MEDIAS`, listadas por ID al ejecutar `plan_status.py`; entre ellas sesión, geometría móvil, perfil, profundidad/color visual y validación visual/física.
- Qué falta exactamente: atender cada ID que aparece en la salida del contador; ver “Pendiente” para dependencias externas.
- Dónde quedó: ver las secciones citadas en [PLAN.md](./PLAN.md), `NOTAS.md` y archivos `evidencia/h0-*` a `evidencia/h7-*`.

### H8.S3.M9 — perfil
- Qué anda: «Cambiar contraseña» y «Ver la guía otra vez» son filas navegables con `HundidoAlTocar`, ripple, semántica de botón y área ≥48 dp. El cierre muestra carga y bloquea duplicados; la confirmación de baja conserva hoja inferior, acción peligrosa y cancelar.
- Qué se verificó: TDD rojo/verde, 3 pruebas de widget H8, a11y 6/6, análisis sin incidencias, gate móvil TODO OK, ocho goldens/referencias del perfil pasaron y siete capturas fueron inspeccionadas (incluida una fila presionada).
- Límites: no se inventaron opciones ni estados de perfil; la captura de baja/carga y una pasada física con lector de pantalla quedan fuera de esta microtarea.
- Dónde quedó: [evidencia H8.S3.M9](./evidencia/h8-s3-m9.md), `fila_de_accion_de_perfil.dart`, `pantalla_perfil.dart` y pruebas del perfil.

### H8.S4.M1 — profundidad visual
- Qué anda: `Tarjeta` y `TarjetaDeCaptura` usan elevación tokenizada de dos capas; los radios revisados usan `Radios.*`, no hay `BoxShadow` literal en pantallas y la tarjeta se inspeccionó en claro/oscuro. Golden focalizado 4/4 con y sin actualización; análisis Dart limpio.
- Qué no anda: las suites completas de goldens siguen teniendo fallos históricos de rasterizador documentados en H10; esto no invalida la suite focalizada que prueba el requisito.
- Qué falta exactamente: nada para el DoD local de H8.S4.M1. La reproducibilidad pixel a pixel entre Flutter/Windows y el rasterizador Linux histórico no se declara cubierta.
- Dónde quedó: [evidencia H8.S4.M1](./evidencia/h8-s4-m1.md), `apps/movil/lib/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart`, `apps/movil/test/widget/paso_capturas_test.dart` y `packages/diseno_flutter/lib/moleculas/tarjeta.dart`.

### H8.S4.M3 — color por rol
- Qué anda: matriz ampliada de texto, controles, snackbars y los pares de `ColorScheme` pasa en ambos temas; se ajustaron los roles naranja/verde y la dirección del degradado para conservar contraste. Al incluir `onError/error` se detectó 4,38:1 en claro; el tema ahora empareja `onError` blanco con `rojoSolido` y conserva `err` para indicadores/bordes. Node 3/3, catálogo a11y + comportamiento Flutter previo 38/38, a11y actual 14/14, goldens actuales de catálogo 14/14 sin actualizar y análisis Dart dirigido limpio.
- Qué no anda: las 20 unidades visuales disponibles ya tienen una captura representativa por tema revisada (40 PNG); se halló verde en acciones/enlaces secundarios y texto destacado, y rojo en la acción destructiva de Perfil. Esto contradice la regla literal y no se aprueba por inferencia. La unidad 21, grupos/detalle, no tiene pantalla/ruta local. El gate estático de tamaño se corrigió con extracción de helpers; H8/H10 goldens móviles mostraron diferencias pixelarias en Flutter 3.47.5 que requieren comparación con el rasterizador canónico y no se actualizaron.
- Qué falta exactamente: tras el cotejo estático de billetera, perfil, ingreso y subida, aplicar literalmente «verdes/rojos solo para estado» a sus usos accionables o registrar una decisión de producto/diseño con excepciones explícitas; extender el cotejo al resto de superficies/estados. Las 228 observaciones H10 de legibilidad no sustituyen la clasificación cromática. Ejecutar la comprobación Vitest de bóveda cuando Vitest esté disponible.
- Dónde quedó: [evidencia H8.S4.M3](./evidencia/h8-s4-m3.md) (revisión color-focalizada de 40 capturas, 20 unidades en claro/oscuro), [cotejo estático widget-token](./evidencia/h8-s4-m3-auditoria-roles-2026-10-13.md) y pruebas de `packages/tokens`.

### H8.S4.M4 — modo oscuro
- Qué anda: la matriz H10 complementaria cubre 20 de las 21 unidades visuales conceptuales existentes con 228/228 celdas inspeccionadas individualmente, incluidas variantes oscuras a 360/412/600 dp; el inventario y cada observación por celda están enlazados en `evidencia/h10-visual/INDICE.md` y `LEIDO.md`. También hay capturas dirigidas H8 de ingreso, portada, MFA inicial, las cuatro láminas del tour, bienvenida sin bono, perfil vacío, centro de ayuda (inicio/lista) y los cuatro estados de verificación oscuros a 360 × 740 dp con texto 130 %; portada también en claro. Se corrigieron y revisaron los seis pasos del alta, la pose frontal inicial de prueba de vida, cuatro estados de «Grupos / mi estado» y subida en curso/error sintético, con fixtures locales. Para cámara concedida, las tres capturas H10 de `PantallaDeCamara` (360/412/600 dp, texto 130 %) usan visor sintético; las capturas oscuras de permiso denegado/bloqueado tampoco inician sensor ni toman foto. La fixture de subida detectó y permitió corregir overflow horizontal de 21 px y título truncado a 200 %. Los goldens dirigidos y el análisis/gate documentados en sus evidencias pasan.
- Qué no anda: grupos/detalle no tiene ruta/pantalla ni contrato GET local, por lo que no puede producirse su captura; no se inventa UI. Hasta resolver el alcance de esa unidad no se puede certificar «0 textos ilegibles» para todo el alcance conceptual, y H8.S4.M4 permanece A MEDIAS. Las capturas usan fixtures sintéticas y Windows; no acreditan hardware, permisos del SO, backend ni baselines canónicas Linux. Las limitaciones de estados/tamaños adicionales y navegación integrada son cobertura ampliada, no hallazgos visuales pendientes en las 228 celdas inspeccionadas.
- Evidencia añadida el 13-10: estado de permiso denegado revisado visualmente en tres tamaños y dos temas; suite sin actualización 6/6 PASS y análisis dirigido limpio. Ver [cámara denegada](./evidencia/h8-camara-denegada-visual.md).
- Evidencia añadida el 13-10: vistas sintéticas de subida a 360 × 740 dp/130 % (una cara en curso/cuatro pendientes y fallo de red con reintento) y error en 320 × 640 dp/200 %. Las capturas detectaron overflow horizontal de 21 px y título truncado; se corrigió el wrapping de acciones y de la barra. Golden 2/2 con y sin actualización, widget 2/2, unidad 10/10, análisis limpio y gate frontend TODO OK 11/11. Ver [subida oscura](./evidencia/h8-subida-oscura-360-130.md).
- Evidencia añadida el 13-10: estados sintéticos oscuros de carga y error/reintento de «Grupos / mi estado» a 360 × 740 dp/130 %; goldens del caso 4/4, incluidos los estados deuda/al día previos. El botón de retry lleva al estado al día. Ver [evidencia de grupos](./evidencia/h8-grupos-mi-estado-oscuro.md).
- Evidencia añadida el 13-10: rechazo sintético `422 AP-CU04-04` en MFA oscuro a 360 × 740 dp/130 %; el mensaje de código inválido se lee completo y las seis casillas quedan visibles. La fixture focalizada pasa 1/1; no prueba MFA válido ni navegación.
- Evidencia añadida el 13-10: ingreso oscuro con rechazo sintético `422 AP-CU04-01` a 360 × 740 dp/130 %; conserva campos y acciones, contraseña enmascarada, sin recorte. Golden 2/2, análisis limpio y gate frontend TODO OK 11/11; sin teclado ni haptics reales.
- Evidencia añadida el 13-10: variante oscura del feed de billetera con 40 movimientos y texto 200 %; filas/importes visibles legibles y sin overflow. Suite H8 break-ui 6/6 con/sin update, análisis limpio y gate frontend TODO OK 11/11. No es validación del backend ni de todas las celdas de la matriz H10.S3.
- Qué falta exactamente: resolver con producto/arquitectura el alcance de grupos/detalle sin ruta/contrato y, si se implementa, añadir su captura oscura inspeccionada. Mantener documentadas como limitaciones separadas la validación física, backend y equivalencia del rasterizador canónico; no ampliar el DoD por estados/tamaños extra sin criterio de producto.
- Dónde quedó: [evidencia del alta oscura](./evidencia/h8-s4-m4-alta-oscura.md), [golden de prueba de vida](./evidencia/h8-prueba-vida-oscura-frente-360-130.png), [grupos / mi estado](./evidencia/h8-grupos-mi-estado-oscuro.md), [centro de ayuda](./evidencia/h8-ayuda-oscura.md), [estado de verificación](./evidencia/h8-verificacion-oscura.md), `apps/movil/test/goldens/h8_alta_oscura_test.dart`, `apps/movil/test/goldens/h8_prueba_vida_oscura_test.dart`, `apps/movil/test/goldens/h8_grupos_mi_estado_oscuro_test.dart`, `apps/movil/test/goldens/h8_centro_ayuda_oscuro_test.dart`, `apps/movil/test/goldens/h8_verificacion_oscura_test.dart`, [evidencia de ingreso](./evidencia/h8-s4-m4-ingreso-oscuro.md), [portada/MFA](./evidencia/h8-s4-m4-portada-mfa-oscuro.md), [tour](./evidencia/h8-s4-m4-tour-oscuro.md), [bienvenida](./evidencia/h8-s4-m4-bienvenida-oscuro.md), [perfil](./evidencia/h8-s4-m4-perfil-oscuro.md) e [inventario](./evidencia/h8-s4-m4-inventario.md).

### H10.S3.M1 — matriz visual de pantallas del alcance
- Qué anda: hay evidencia para veinte unidades del alcance en 360, 412 y 600 dp, claro/oscuro y texto 130 %: grupos, portada, billetera, ingreso, MFA, Tour, Bienvenida, Verificación, Subida, Perfil, Candado, Ayuda, Cámara, Prueba de vida y seis subestados de alta. El detalle por celda, fixture y estado está en [INDICE.md](./evidencia/h10-visual/INDICE.md) y [LEIDO.md](./evidencia/h10-visual/LEIDO.md). Total: 228 celdas, 204 PNG nuevos y 24 reutilizados. Alta/datos: update 5/5, repetición 8/8 con widget y H8 exacto; Alta/documento: update/repetición 6/6 y widget de capturas 10/10, análisis limpio. H8 documento no se reutiliza por diferencia conocida de 1.056 píxeles (0,40 %).
- Qué no anda: la matriz sigue parcial y aún no valida rasterizador canónico, backend real ni dispositivo. “Al día” no es un EstadoVacio técnico de grupos; carga/vacío/error no aplican a la portada estática ni existe un estado vacío de datos para ingreso. Las referencias H8 de subida/error de red (148 px, 0,06 %), Perfil (444 px, 0,17 %) y Ayuda inicial (1.286 px, 0,48 %) difieren con Flutter 3.44.8 y no se regeneraron. H8 del PLAN.md enumera 21 unidades visuales conceptuales, contando por separado los seis pasos del alta; no son 21 rutas (alta es un wizard, candado un overlay) y grupos/detalle carece de ruta/contrato local según el inventario H8.
- Qué falta exactamente: la unidad conceptual grupos/detalle, que no puede capturarse localmente porque el inventario H8 confirma que falta ruta/contrato; no inventar su UI. Ya no quedan subestados iniciales del registro en esta matriz. H10.S3.M1 sigue parcial por ese alcance no disponible y por validación en dispositivo/rasterizador canónico.
- Dónde quedó: [índice e inventario de las 21 unidades](./evidencia/h10-visual/INDICE.md), [lectura por captura](./evidencia/h10-visual/LEIDO.md), y los tests `h10_grupos_mi_estado_visual_test.dart`, `h10_portada_visual_test.dart`, `h10_billetera_visual_test.dart`, `h10_ingreso_visual_test.dart`, `h10_mfa_visual_test.dart`, `h10_tour_visual_test.dart`, `h10_bienvenida_visual_test.dart`, `h10_verificacion_visual_test.dart`, `h10_camara_visual_test.dart`, `h10_prueba_vida_visual_test.dart`, `h10_alta_cuenta_visual_test.dart`. MFA ahora presenta el envío con el botón estándar y deshabilita las casillas durante la verificación; la billetera apila acciones bajo 420 dp, el pie del ingreso queda dentro del viewport a 360 dp incluso en error, y ambas pantallas de cámara usan su paleta oscura para mantener el contraste de las acciones sobre el visor.

### H10.S3.M2 — inspección de capturas H10
- Qué anda: se abrieron e inspeccionaron individualmente las 228 celdas de veinte superficies y se dejó una observación por celda (156 filas por archivo + 72 celdas en matrices por estado/tema/ancho); la auditoría de cobertura no encontró faltantes ni duplicados. No se observó error de layout no intencional. La parte de la tarjeta que asoma en Alta/documento es un indicio de carrusel; CI/fecha siguen por scroll. Los estados son sintéticos y no se activa cámara, permiso, envío ni datos reales. Las referencias H8 reutilizadas se registran por nombre; las H8 que difieren permanecen intactas y no se reutilizan. H10.S3.M2 cumple su DoD y queda HECHO.
- Qué no anda: el grupo/detalle no tiene ruta/contrato local y, por tanto, no existe una captura que inspeccionar; la matriz H10.S3.M1 permanece parcial. La lectura visual local no acredita dispositivo físico ni rasterizador de CI.
- Qué falta exactamente: nada dentro del alcance de las 228 capturas existentes para M2; registrar y abrir cada captura nueva si se amplía la matriz y reabrir la microtarea de UI si aparece una anomalía.
- Dónde quedó: [LEIDO.md](./evidencia/h10-visual/LEIDO.md) contiene 228 observaciones verificadas; [INDICE.md](./evidencia/h10-visual/INDICE.md) registra 21 unidades conceptuales, las matrices y sus límites.

## PENDIENTE

| IDs / frente | Estado | Qué lo destraba |
|---|---|---|
| H4.S2.M4 | A MEDIAS | Prueba de geometría 360/412 dp 4/4 y captura H10 a 412 dp/130 % 1/1 tras alinear cajas; falta evidencia visual en dispositivo real para los anchos indicados ([detalle](./evidencia/h4-s2.txt)) |
| H4.S5.M1 | A MEDIAS | La regresión ampliada actual pasa 4/4 y verifica las cuatro fases/cabeceras y el retorno; falta conservar la salida roja histórica requerida por el DoD TDD ([evidencia](./evidencia/h4-s5-m1-verificacion-2026-10-13.md)) |
| H1.S4.M1 | A MEDIAS | La suite actual pasa 14/14, pero no se conservó la salida roja previa que exige el criterio de prueba-primero; evidencia [H1.S4.M1](./evidencia/h1-s4-m1-vigilante.md) |
| H1.S6.M2–M5 | A MEDIAS | M2 ya bifurca cookie WEB/cuerpo móvil y tiene expectativas web; ejecutar sus pruebas y las de M3–M5 con BD PostgreSQL/jOOQ disponibles. Intento actual detenido antes de compilar en `generateJooq` (127.0.0.1:5433 rechazó); ver [límite actualizado](./evidencia/h1-s6-backend-bloqueado.md) |
| H1.S6.M8 | BLOQUEADO | Configurar/obtener entorno de DB/servicios de integración; dueño: responsable de infraestructura/backend |
| H2.S1.M1/M3 | A MEDIAS | Repetir los recorridos funcionales y registrar errores/capturas cuando se disponga del backend indicado y un dispositivo |
| H3.S1.M3 | A MEDIAS | Geometría del teclado verificada en simulación; falta captura física que confirme el comportamiento con teclado del sistema |
| H3.S3.M3 | BLOQUEADO | Resolver la dependencia/criterio documentado en el propio ID del plan; dueño: responsable de identidad |
| H5.S4.M3; H6.S3.M8 | A MEDIAS | Captura/cámara y medición de rendimiento requieren emulador o dispositivo; las microtareas de subida y privacidad H5.S3.M2–M4/H6.S3.M9 ya están HECHO según sus criterios automatizados |
| H8.S4.M3/M4 | A MEDIAS | Resolver política de color por producto y completar el alcance oscuro que carece de ruta local |
| H6.S2.M3 | TODO | Medir umbrales con 10 altas reales en teléfono y registrar el antes/después; no sustituible por cámara fake |
| H10.S1.M6 | A MEDIAS | El barrido global previo a los últimos retoques dio 52/268 PASS y 216 diffs; la captura H10 puntual de datos a 412 dp pasó 1/1 después de corregir espaciado. Falta volver a barrer, clasificar el resto y verificar las referencias en el runner canónico ([evidencia](./evidencia/h10-app-goldens-flutter-3.47.5-2026-10-13.md)) |
| H10.S1.M7 | A MEDIAS | Ejecutar `flutter test` en el runner canónico Linux y confirmar las cinco baselines de marca; la única distro WSL presente es `docker-desktop` y Docker no está activo |
| H10.S1.M8 | BLOQUEADO | Restaurar PostgreSQL/Docker y disponer de memoria suficiente para Gradle |
| 10 microtareas TODO restantes de H1–H10 | TODO | Implementar en orden según dependencias del plan; incluye trabajo pendiente de producto, backend y validación física |

| H8.S2.M2 | Preferencia de movimiento reducido y duraciones visuales centralizadas; widgets afectados se asientan sin transición | [evidencia](./evidencia/h8-s2-m2.md) | PASS; suites de paquete/app y análisis estático limpios |
| H8.S2.M3 | Política háptica semántica centralizada en la app; confirmaciones solo en acciones y eventos de éxito permitidos | [evidencia](./evidencia/h8-s2-m3.md) | PASS; pruebas de canal/captura, análisis estático y gate frontend |
| H8.S3.M2 | Entrada de portada por cuatro bloques y brillo primario demorado | [evidencia](./evidencia/h8-s3-m2.md) | PASS; 10 pruebas de portada, 10 de movimiento, accesibilidad y análisis estático |
| H8.S3.M3 | Sacudida de credenciales y haptic ligero solo para `AP-CU04-01`; suprimida con movimiento reducido | [evidencia](./evidencia/h8-s3-m3.md) | PASS; análisis y 12 pruebas de haptics/sesión/autenticación |
| H8.S3.M4 | Transición horizontal entre pasos, progreso de fase y tildes de validez animadas | [evidencia](./evidencia/h8-s3-m4.md) | PASS; 11 pruebas de alta, 19 de diseño, análisis y gate frontend |
| H8.S3.M5 | Confirmación visual de alta con tilde dibujada, movimiento reducido y haptic semántico | [evidencia](./evidencia/h8-s3-m5.md) | PASS; 13 pruebas de diseño, 6 de haptic/alta, análisis y gate frontend |
| H8.S3.M6 | Pop de foto y contador de capturas animado al guardar/repetir | [evidencia](./evidencia/h8-s3-m6.md) | PASS; 3 pruebas de capturas, 23 de movimiento/interacción, análisis y gate frontend |
| H8.S3.M7 | Billetera: brillo, refresh, feed real y escalonado de movimientos | [evidencia](./evidencia/h8-s3-m7.md) | HECHO para el criterio UI; límites backend/dispositivo/historial documentados en evidencia |
| H8.S3.M8 | Pulso del turno actual y progreso animado de aportes | [evidencia](./evidencia/h8-s3-m8.md) | PASS; 18 pruebas de comportamiento, movimiento reducido y análisis limpios |
| H8.S3.M9 | Filas de perfil con hundido, cierre de sesión cargando, hoja de confirmación de baja | [evidencia](./evidencia/h8-s3-m9.md) | HECHO; TDD, navegación/semántica, estados y capturas dirigidas PASS |
| H8.S3.M10 | Entrada de vacíos/errores y giro de reintento | [evidencia](./evidencia/h8-s3-m10.md) | PASS; 21 pruebas de comportamiento, 7 de saldo, análisis y gate limpios |
| H8.S3.M11 | Entrada/salida animada del candado con movimiento reducido | [evidencia](./evidencia/h1-s5-revision-visual.md) | PASS; prueba dedicada de duración, salida y `disableAnimations` |
| H5.S3.M4 | Estados de subida por cara y umbral lento de 10 s | [evidencia](./evidencia/h5-s3-m4-h5-s4-m2.txt) | PASS; 10 unidad + 2 widget |
| H5.S3.M3 | Rechazo de foto con opción de usarla igual | [evidencia](./evidencia/h5-s3-m3.md) | PASS; escáner 5 + cámara de respaldo 5 |
| H6.S3.M9 | Privacidad y limpieza de fotos del alta | [evidencia](./evidencia/h6-s3-m9.md) | PASS; 11 widget + 5 unidad |
| H8.S3.M12 | Icono activo de pestaña con fundido cruzado y rebote | [evidencia](./evidencia/h8-s3-m12.md) | PASS; 24 pruebas de comportamiento, 9 pruebas de rutas y análisis limpios |
| H8.S4.M2 | Roles tipográficos, cifra tabular y altura de línea desde tokens | [evidencia](./evidencia/h8-s4-m2.md) | PASS; 24 pruebas de comportamiento y análisis limpios |

## Evidencia

- Salidas dirigidas, análisis, primera suite integral e incidencias de temporizadores: [evidencia/cierre-parcial-2026-10-12.md](./evidencia/cierre-parcial-2026-10-12.md).
- Otras evidencias por hito: directorio [`evidencia/`](./evidencia/).
- Contador al cierre de esta actualización: `python .claude/hooks/plan_status.py --path docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md` → `182/213`, `85.4%`; `TODO=10`, `HECHO=182`, `A MEDIAS=18`, `BLOQUEADO=3`. Los 210 IDs del prompt original están todos en la copia de trabajo; 179/210 (85.2%) están HECHO. Las 3 filas suplementarias H0 también están HECHO.

## Seguridad

Esta sección distingue controles presentes de controles cuya eficacia aún no se verificó en el entorno indicado. “Implementado” no significa aprobado para producción.

| Amenaza | Control y ubicación | Prueba / resultado | Riesgo residual |
|---|---|---|---|
| Robo de refresh token móvil | El cliente consume y rota el refresh del contrato móvil (`apps/movil/lib/pantallas/identidad/dominio/cu04_autenticar.dart`, `refrescador.dart`); el backend contempla rotación y revocación de familia por reutilización (H1.S6). | El intento Gradle más reciente no compiló ni ejecutó tests: `generateJooq` no pudo conectar con PostgreSQL en `127.0.0.1:5433`; Docker CLI está presente pero no hay contenedores activos ni compose local de base disponible ([límite de verificación](./evidencia/h1-s6-backend-bloqueado.md)). | Reutilización y revocación no están verificadas; H1.S6.M3/H10.S4.M5 siguen pendientes. Un access JWT robado puede valer hasta su expiración (máximo configurado: 15 min); el backend no ofrece revocación inmediata global comprobada. |
| Redirección externa manipulando `volver` | `destinoTrasIngresar` acepta solo rutas internas privadas y rechaza `//`/destinos no permitidos (`apps/movil/lib/navegacion/acceso_a_rutas.dart:46-56`). | Reejecutada para H10.S4.M2: 4/4 PASS, incluida redirección externa rechazada ([salida original](./evidencia/h1-s2-m6.txt)). | El caso automatizado está cubierto; siguen pendientes la verificación de logs, dispositivo, filesystem y backend descritas abajo. |
| Captura de pantalla o exposición de datos de identidad | La protección de pantalla se activa en fases de cuenta/identidad y se desactiva al salir (`comportamiento_de_pantalla_del_alta.dart:38-46`); prueba con puerto falso: 2/2 PASS. | Automatización del adaptador, no captura del sistema operativo. | H10.S4.M3 sigue TODO: falta evidencia en Android físico de que ingreso, alta y capturas quedan bloqueados/negros. |
| Persistencia accidental de fotos o cuadros faciales | El stream de cámara se procesa en memoria; el archivo nace de captura explícita. Rechazo, abandono y limpieza posterior a la subida borran temporales ([evidencia](./evidencia/h6-s3-m9.md)); suites: 11 widget + 5 unidad PASS. | Pruebas automatizadas y revisión del flujo local; no se inspeccionó filesystem en un dispositivo Android. | H10.S4.M4 sigue TODO: falta listar el directorio temporal real antes/después del alta. Fallos del sistema de archivos pueden diferir del fake. |
| Filtración de credenciales o datos personales en logs | La regla 90.6 prohíbe registrar tokens/OTP/credenciales en claro; el código móvil debe revisarse también en runtime. | Búsqueda estática revisada: el único `developer.log` solo informa ausencia de `tokenRefresco`, sin valores sensibles ([evidencia](./evidencia/h10-s4-m1-estatica.md)). No se lanzó AVD con ~2.37 GiB libres. | No se afirma ausencia de datos sensibles en logs de ejecución; H10.S4.M1 sigue A MEDIAS hasta probar `flutter logs` durante el alta en Android. |
| Logout o refresh fallido deja sesión utilizable | Cliente invalida el estado local; el backend busca revocar sesión/familia mediante `DELETE /sesion` (evidencia y límite H1.S6). | Tests de servicio e integración no ejecutados por falta de DB/Gradle funcional. | Si el DELETE no llega, el access puede seguir válido hasta su expiración; la familia refresh podría permanecer utilizable hasta expiración/revocación. H1.S6.M2–M5 continúan A MEDIAS y M8 BLOQUEADO. |

**Pendiente para considerar cerrada la verificación de seguridad:** escaneo y `flutter logs` (H10.S4.M1), evidencia física Android (M3), inspección filesystem real (M4) y test backend de reuso de refresh (M5). Esta documentación no sustituye esas pruebas.

## No cubierto

- No se verificó un build Android/iOS, cámara física, biometría/cámara del sistema ni perfil de rendimiento en dispositivo.
- No se repitió después del último cambio la suite completa del frontend ni los tests de servicios de identidad/backend; estos requieren el stack Gradle/DB y siguen parciales/bloqueados.
- Se revisó la matriz sintética de 20/21 unidades disponibles en claro/oscuro a 360/412/600 dp y 130 %; grupos/detalle no tiene ruta/contrato. No hubo desktop nativo ni dispositivo físico. La corrida golden actual para contrastar contra masters quedó interrumpida (21 PASS/201 FAIL, seis sin ejecutar) y no actualizó baselines; ver [rúbrica previa](./evidencia/h8-rubrica-antes.md).
- No se hizo push, PR, merge ni despliegue.

## Desvíos del plan

- Se ejecutaron microtareas en paralelo con hasta cuatro agentes por pedido explícito, cuidando RAM; las pruebas pesadas se ejecutaron serialmente.
- El plan decía trabajar microtarea por microtarea, pero el volumen y los frentes independientes llevaron a agrupar lectura/cambios y pruebas dirigidas. Cada estado `HECHO` se mantiene condicionado a evidencia registrada.
- Se añadió inyección de reloj al controlador de vida, se corrigió el alto de las filas del selector y se dividió el paso de cuenta para conservar el gate de 200 líneas.
- El SDK disponible es Flutter 3.47.5 / Dart 3.13.4, no Flutter 3.44.x como baseline solicitado; se mantuvo el entorno instalado.
- Esa nota registraba la situación inicial; después se instaló Flutter 3.44.8/Dart 3.12.2 en paralelo en `C:\src\flutter-3.44.8`, conservando el SDK global 3.47.5. Evidencia actual: [H0.S1.M3](./evidencia/h0-s1-m3-flutter-3.44.8.md).

## Riesgos residuales

- El flujo automatizado de vida pasa con fakes; faltan validaciones con cámara física, rendimiento y privacidad end-to-end.
- Backend de sesión tiene tests nuevos no ejecutados con Gradle/DB; el análisis del agente registró un posible deadlock por orden de locks entre refresh y logout.
- El upload/limpieza best-effort y las cámaras quedan sin ensayo en dispositivos reales; fallas de permisos y ciclo de vida pueden diferir del fake.
- 32 microtareas no están `HECHO`; este reporte no afirma finalización total.
- El checkout conserva cambios ajenos o previos, incluyendo `docs/Views/AportaYa-Maqueta.html`; no se revirtieron ni limpiaron.

## Decisiones y ambigüedades

- Se mantuvo la rama `codex/alta-atlas-sesion-premium-2026-10-11` y se evitó push/PR/merge/deploy según las restricciones del plan.
- El test automatizado de vida conserva su reloj de sistema por defecto en producción; la inyección se expone para determinismo en test. Falta confirmar con la persona responsable de QA si la sincronización esperada debe observar `FOTO 2` antes o después de completar los 1100 ms de la animación.
- Las listas de tareas AportaYa/Atlas continúan usando los textos y umbrales transcritos por el plan; criterios normativos y de onboarding deben confirmarse por producto/compliance antes de lanzamiento.
- La cámara de respaldo permanece como alternativa local; falta decidir con producto si habilitar galería para la captura de carnet fuera de la pantalla de permiso.
