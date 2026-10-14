# H10.S1.M6 — suite completa de goldens en Windows con Flutter 3.44.8

Fecha: 2026-10-14. Plataforma: Windows, Flutter 3.44.8 / Dart 3.12.2. Ejecución serial; sin regenerar snapshots.

## Comandos y resultado

En `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --timeout 60s --reporter=compact test/goldens
```

Resultado final: **79 pasaron, 189 fallaron**. Las fallas reportadas por la suite fueron comparaciones pixel a pixel contra los goldens existentes; por ejemplo, la pantalla de error de billetera difirió 296 píxeles (0,11 %) y una transición difirió 398 píxeles (0,01 %). Los artefactos de diagnóstico quedaron en `apps/movil/test/goldens/failures`. No se usó `--update-goldens` y no se aceptaron ni copiaron esos resultados sobre los baselines.

Como control acotado, los seis casos `billetera · cargando` se ejecutaron con Flutter 3.44.8 y pasaron **6/6**. Esa misma familia había mostrado diferencias al ejecutarse con Flutter 3.47.5; el control confirma que esos seis diffs eran sensibles a la versión del SDK, pero no hace pasar la suite completa ni valida el renderer macOS de CI.

## Estado y límites

H10.S1.M6 permanece **A MEDIAS**. La ejecución de toda la suite en Windows 3.44.8 sigue produciendo discrepancias y no sustituye la validación visual en el entorno macOS canónico ni autoriza regenerar baselines. Revisar las imágenes de fallo por familias, identificar cuáles son diferencias legítimas del renderer y repetir en el entorno canónico antes de cerrar la microtarea.

Para preparar Flutter 3.44.8 se ejecutó `flutter pub get --offline` (sin `--enforce-lockfile` porque el lock no coincidía con el SDK/dependencias disponibles). Esto cambió localmente `apps/movil/pubspec.lock` (`intl` 0.20.3 → 0.20.2); **ese cambio no se incluyó en el commit del PR** y requiere revisión aparte antes de conservarlo o descartarlo.
