# H6.S2.M3 — teléfono real no disponible

- Fecha: 2026-10-13.
- Criterio literal: validar `JuezDePose` con al menos 10 altas de prueba en un teléfono Android real, variando luz y características del rostro; registrar resultados y cualquier ajuste en `h6-umbrales.md` y `NOTAS.md`.
- Comando ejecutado desde `apps/movil`: `flutter devices`.
- Salida: `Found 2 connected devices: Windows (desktop) … windows-x64 …; Chrome (web) … web-javascript …`.
- Verificación no sustitutiva de la lógica automatizada: `flutter test --no-pub --concurrency=1 --reporter compact test/unidad/ciclo_de_pose_test.dart` → `All tests passed!` (13/13).
- Integridad del contador/evidencia: `python .claude/hooks/plan_status.py --self-test` → 31 PASS, 0 FAIL.
- Resultado: no hay un teléfono Android físico conectado. La cámara fake, Windows/Chrome y un emulador no sustituyen las 10 altas reales que exige el criterio.
- Decisión: estado `BLOQUEADO` por dependencia de dispositivo físico. No se inventaron mediciones y no se modificó ningún umbral.
- Para reanudar: conectar un teléfono Android real; ejecutar 10 altas representativas (incluyendo luz interior/de día, lentes y barba); registrar condición, veredicto y falsos rechazos en `h6-umbrales.md`; ajustar solo si los datos lo justifican y registrar antes/después en `NOTAS.md`.
