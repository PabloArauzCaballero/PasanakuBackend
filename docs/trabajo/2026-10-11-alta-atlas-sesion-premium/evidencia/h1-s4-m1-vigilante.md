# H1.S4.M1 — vigilante con reloj falso

Fecha: 2026-10-12.

Verificación actual de los 14 escenarios del vigilante:

```text
cd apps/movil
flutter test --concurrency=1 --reporter expanded test/unidad/vigilante_test.dart
00:00 +14: All tests passed!
```

Se cubren cinco minutos de inactividad y reinicio por interacción, umbral de segundo plano de 60 segundos, ciclos `paused`/`inactive`/`hidden`/`detached`, relojes hacia atrás, sesión ya bloqueada y reinicio al desbloquear.

Limitación histórica: no se conservó una salida roja de la prueba antes de implementar el vigilante. La corrida verde actual no demuestra retrospectivamente ese paso de prueba-primero, por lo que H1.S4.M1 permanece A MEDIAS hasta tener una evidencia válida del criterio completo; no se reconstruye ni inventa esa salida.
