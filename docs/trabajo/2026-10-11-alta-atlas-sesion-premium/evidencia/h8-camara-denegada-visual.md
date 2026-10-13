# H8.S4.M4 — cámara denegada: revisión visual claro/oscuro

Fecha: 2026-10-13. Estado: evidencia parcial; no cierra H8.S4.M4.

Se regeneró la suite dirigida después de corregir el fixture para cargar las fuentes reales de la app. La inspección visual cubrió el estado de permiso de cámara denegado en tres viewports —móvil, tableta y escritorio— y dos temas:

- `apps/movil/test/goldens/imagenes/escanear_denegado_movil_claro.png`
- `apps/movil/test/goldens/imagenes/escanear_denegado_tableta_claro.png`
- `apps/movil/test/goldens/imagenes/escanear_denegado_escritorio_claro.png`
- `apps/movil/test/goldens/imagenes/escanear_denegado_movil_oscuro.png`
- `apps/movil/test/goldens/imagenes/escanear_denegado_tableta_oscuro.png`
- `apps/movil/test/goldens/imagenes/escanear_denegado_escritorio_oscuro.png`

En las seis capturas se leen el título, la explicación, el aviso de permiso, la acción “Abrir ajustes”, la etiqueta/campo de enlace y el botón “Usar este enlace”. No observé texto recortado ni superposiciones; el aviso mantiene contraste visible en oscuro y claro. El contenido se adapta de móvil a anchos mayores sin desbordamiento horizontal.

Verificación final, desde `apps/movil`, sin regenerar goldens:

```powershell
flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/pantalla_escanear_golden_test.dart
```

Resultado: **6/6 PASS** (móvil, tableta y escritorio, claro y oscuro).

```powershell
dart analyze --fatal-infos test/goldens/pantalla_escanear_golden_test.dart
```

Resultado: **No issues found!**

Límites: la suite cubre solo el permiso denegado y usa cámara/ajustes simulados; no demuestra permiso real, apertura de ajustes del sistema ni comportamiento en dispositivo. Las imágenes se generaron en Windows y no se consideran baselines canónicas Linux. Las capturas antiguas de esta suite usaban glifos de fuente de prueba y no se usan como evidencia de legibilidad.
