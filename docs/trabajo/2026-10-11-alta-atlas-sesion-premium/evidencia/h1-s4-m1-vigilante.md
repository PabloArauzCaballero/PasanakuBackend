# H1.S4.M1 — vigilante con reloj falso

Fecha: 2026-10-12.

Verificación actual de los 14 escenarios del vigilante:

```text
cd apps/movil
flutter test --concurrency=1 --reporter expanded test/unidad/vigilante_test.dart
00:00 +14: All tests passed!
```

Se cubren cinco minutos de inactividad y reinicio por interacción, umbral de segundo plano de 60 segundos, ciclos `paused`/`inactive`/`hidden`/`detached`, relojes hacia atrás, sesión ya bloqueada y reinicio al desbloquear.

Revalidación actual (Windows, Flutter 3.44.8; 2026-10-14):

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/unidad/vigilante_test.dart
00:00 +0: loading test/unidad/vigilante_test.dart
00:00 +0: no bloquea antes de cinco minutos sin interacción
00:00 +1: bloquea por inactividad al cumplir cinco minutos
00:00 +2: reinicia el límite con una interacción y bloquea al cumplirlo
00:00 +3: volver antes de 60 segundos no bloquea
00:00 +4: volver después de 60 segundos bloquea por segundo plano
00:00 +5: inactive y resumed no cuenta como salida pero sí revisa inactividad
00:00 +6: dos paused seguidos conservan la primera hora de salida
00:00 +7: un reloj hacia atrás al revisar bloquea por seguridad
00:00 +8: un reloj hacia atrás al volver bloquea por segundo plano
00:00 +9: no vuelve a solicitar bloqueo cuando ya está bloqueada
00:00 +10: no revisa inactividad mientras la app está fuera
00:00 +11: desbloqueado reinicia el reloj de inactividad
00:00 +12: hidden se trata como salida a segundo plano
00:00 +13: detached no cuenta como salida
00:00 +14: All tests passed!
EXIT_CODE=0

dart analyze --fatal-infos test/unidad/vigilante_test.dart lib/dominio/vigilante_de_sesion.dart
Analyzing vigilante_test.dart, vigilante_de_sesion.dart...
No issues found!
EXIT_CODE=0
```

Limitación histórica: no se conservó una salida roja de la prueba antes de implementar el vigilante. La corrida verde actual no demuestra retrospectivamente ese paso de prueba-primero, por lo que H1.S4.M1 permanece A MEDIAS hasta tener una evidencia válida del criterio completo; no se reconstruye ni inventa esa salida.

Auditoría de historia Git (2026-10-14, solo lectura):

```text
git show --stat --oneline 0922bade -- apps/movil/lib/dominio/vigilante_de_sesion.dart apps/movil/test/unidad/vigilante_test.dart
0922bade wip: alta atlas y sesion premium al 85%
 apps/movil/lib/dominio/vigilante_de_sesion.dart |  70 ++++++++++
 apps/movil/test/unidad/vigilante_test.dart      | 173 ++++++++++++++++++++++++
 2 files changed, 243 insertions(+)

git cat-file -e 0922bade^:apps/movil/lib/dominio/vigilante_de_sesion.dart
fatal: path 'apps/movil/lib/dominio/vigilante_de_sesion.dart' exists on disk, but not in '0922bade^'
```

El padre de ese commit no contiene la implementación; el commit introduce simultáneamente
la implementación y la prueba. Esto es evidencia del estado anterior, pero no de que la
prueba se ejecutara en rojo antes de escribir el código. No se corrió una reproducción
artificial contra el padre ni se atribuye a ella una salida histórica; M1 sigue `A MEDIAS`.
