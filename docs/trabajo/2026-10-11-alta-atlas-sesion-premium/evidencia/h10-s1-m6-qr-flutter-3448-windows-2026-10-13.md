# H10.S1.M6 - aclaracion de los seis snapshots de billetera cargando

Fecha: 2026-10-13. Skill aplicada: `visual-regression-testing`; no se actualizaron baselines.

La revalidacion anterior en Windows/Flutter 3.47.5 habia reportado seis diffs limitados a `billetera cargando` (claro/oscuro en 360/412/600 dp), con un icono de QR en la cabecera. Se repitio solo ese subconjunto con Flutter 3.44.8 / Dart 3.12.2, el SDK del job canonico, aun en Windows:

```text
C:\src\flutter-3.44.8\flutter\bin\flutter.bat test --no-pub --concurrency=1 --reporter expanded --name 'billetera.*cargando' test/goldens/h10_billetera_visual_test.dart
00:01 +6: All tests passed!
EXIT_CODE=0
```

Las seis capturas pasan sin diferencias con Flutter 3.44.8 en esta plataforma. Esto localiza los seis diffs anteriores en la diferencia de version de SDK observada (no prueba que Windows sea equivalente a macOS). En el barrido exploratorio de las 24 celdas de billetera con Flutter 3.44.8/Windows pasaron 6 y fallaron 18 comparaciones visuales; no se declara verde la suite completa ni se aceptan sus baselines.

H10.S1.M6 sigue `A MEDIAS`: falta correr el conjunto global completo y el gate del runner canonico macOS (`.github/workflows/ci.yml`, Flutter 3.44.8). La corrida dirigida tampoco sustituye ese gate.
