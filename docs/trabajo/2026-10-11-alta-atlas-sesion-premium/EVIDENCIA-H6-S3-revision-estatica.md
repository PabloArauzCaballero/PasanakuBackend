# H6.S3 — evidencia de implementación (revisión estática)

Fecha: 2026-10-12. Esta nota no sustituye la suite ni la medición en dispositivo.

## Implementación visible en código

- `pantalla_prueba_de_vida.dart` conserva el fondo negro y compone cámara, silueta,
  guía, cuenta, miniaturas y pie como overlays separados.
- `controlador_prueba_de_vida.dart` avanza `CicloDePose` con un ticker de 100 ms;
  las lecturas del detector también avanzan el mismo ciclo. Ya no usa plazo de 20 s
  ni una bandera de respaldo manual: «Tomar ahora» permanece visible.
- El ciclo usa 3 s de pose sostenida, la interfaz anuncia el resultado de la captura
  y espera `avisoDeFotoTomada` (1100 ms) antes de cambiar la pose o finalizar.
- La toma detiene primero la escucha del detector, dispara háptico/destello de 500 ms,
  obtiene la foto de `CamaraEnVivo`, aplica el control local y ofrece repetir cuando
  falla. El archivo rechazado se elimina sin registrar su ruta.
- `miniaturas_de_vida.dart` pide `Image.file(cacheWidth: 228)` para cuadros de 76 dp;
  los estados tienen etiquetas accesibles y únicamente «Repetir» es accionable.
- El permiso de la prueba muestra el copy de Atlas y no ofrece galería. El lector ML
  consume bytes del cuadro en memoria mediante `InputImage.fromBytes`; no se observa
  escritura del video en los archivos revisados.

## Pruebas agregadas/ajustadas (no ejecutadas)

- `prueba_de_vida_test.dart`: cuenta/pose, permiso concedido/denegado/bloqueado,
  progreso automático, manual visible y archivo rechazado borrado con acción de repetir.
- `miniaturas_de_vida_test.dart`: semántica de los tres estados, botón solo para
  rechazada y `cacheWidth`.
- `aviso_permiso_prueba_de_vida_test.dart`: copy y ausencia de acceso a galería.
- `ciclo_de_pose_test.dart`: el tic conserva una lectura fresca durante la cuenta.
- `textos_de_captura_test.dart`: copy exacto por pose y fase.

## Límites de DoD

- M1: no se corrió Flutter ni analyzer por la restricción de RAM; no se declara verde.
  La suite integrada debe comprobar compilación y comportamiento antes de cerrar M2–M7.
- M8: las `RepaintBoundary` están en visor/silueta/cuenta y no se añadió `BackdropFilter`,
  pero falta `flutter run --profile`, dispositivo Android de gama media y captura de
  evidencia de cuadros. Sigue pendiente por hardware.
- M9: hay prueba escrita para eliminación de una foto rechazada, pero no fue ejecutada.
  La ausencia de escritura de frames es evidencia estática del adaptador ML, no una
  inspección del directorio temporal en runtime; la verificación de privacidad completa
  queda pendiente de suite/dispositivo.
