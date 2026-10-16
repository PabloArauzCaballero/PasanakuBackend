# H8.S4.M4 — cámara de identidad sin permiso

Estado de esta porción: **capturada e inspeccionada**. No cierra H8.S4.M4 globalmente.

## Alcance

`PantallaDeCamara` para el anverso del carnet en oscuro, 360 × 740 dp, escala de texto 130 % y DPR 1, en los dos estados locales de permiso que cambian el mensaje/acción:

- [Permiso denegado](./h8-camara-identidad-denegada-oscura-360-130.png): «Necesitamos tu cámara», «Permitir cámara» y galería.
- [Permiso bloqueado](./h8-camara-identidad-bloqueada-oscura-360-130.png): indicación de abrir ajustes y galería.

La prueba inyecta `CamaraEnVivo` sintética; comprueba una consulta de permiso, cero inicios de sensor y cero fotos. No toca botones, plugins, cámara, permisos del sistema, ajustes, archivos de identidad, red ni backend. Al desmontar se libera la fixture. No es validación en dispositivo.

## Revisión visual

Las dos imágenes se inspeccionaron después de generarlas. Título, aviso, acciones y cancelar se leen completos; no hay recorte ni solapamiento. «Tomar foto» permanece visible pero deshabilitado bajo el aviso de permiso; no se accionó. La galería se muestra, pero no se abre.

## Verificación

- Update-goldens y repetición sin update de `flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_camara_identidad_permiso_oscuro_test.dart`: 2/2 PASS en ambas corridas.
- `dart analyze --fatal-infos test/goldens/h8_camara_identidad_permiso_oscuro_test.dart`: `No issues found!`.
- `python ..\..\scripts\verificar_frontend.py movil`: `TODO OK`, gate 11/11.

Falta ampliar tamaños/estados y validar permisos/cámara reales. La evidencia sintética mide distribución y legibilidad únicamente.
