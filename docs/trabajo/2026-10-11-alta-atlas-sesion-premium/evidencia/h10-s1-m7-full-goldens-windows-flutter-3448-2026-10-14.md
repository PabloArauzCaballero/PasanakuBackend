# H10.S1.M7 — suite completa del design system con Flutter 3.44.8 en Windows

Fecha: 2026-10-14. Entorno: Windows, Flutter 3.44.8 / Dart 3.12.2. Ejecución serial, sin `--update-goldens`.

## Preparación del SDK

El primer intento de `flutter test --no-pub` no llegó a ejecutar casos: `.dart_tool/package_config.json` seguía apuntando a `C:/src/flutter` (Flutter 3.47.5) mientras el compilador era Dart/Flutter 3.44.8. Se observó `ui.HitTestResponse` y `PlatformDispatcher.onHitTest` inexistentes en el `dart:ui` seleccionado. El comando terminó con exit code 1 tras cargar cuatro test files; se detuvo esa ejecución y no se interpretó como resultado de goldens.

`flutter pub get --offline --enforce-lockfile` no pudo satisfacer el lock fijado a paquetes más nuevos:

```text
Resolving dependencies...
Downloading packages...
  clock 1.1.2 (1.1.3 available)
< matcher 0.12.19 (was 0.12.20) (0.12.20 available)
< meta 1.18.0 (was 1.19.0) (1.19.0 available)
  stack_trace 1.12.1 (1.12.2 available)
< test_api 0.7.11 (was 0.7.12) (0.7.15 available)
< vector_math 2.2.0 (was 2.4.3) (2.4.3 available)
Would change 4 dependencies.
6 packages have newer versions incompatible with dependency constraints.
Unable to satisfy `pubspec.yaml` using `pubspec.lock`.
To update `pubspec.lock` run `flutter pub get` without
`--enforce-lockfile`.
Failed to update packages.
```

Se ejecutó `flutter pub get --offline` en `packages/diseno_flutter`; la resolución terminó correctamente e hizo esos cuatro downgrades únicamente en `packages/diseno_flutter/pubspec.lock`, para alinear el SDK. El cambio de lock queda local y requiere revisión antes de conservarlo.

## Corrida completa

Comando, desde `packages/diseno_flutter`:

```text
flutter test --no-pub --concurrency=1 --reporter=compact --timeout 60s
```

Salida final literal:

```text
00:34 +124 -8: Some tests failed.
exit_code=1
```

Las ocho diferencias fueron goldens en cuatro catálogos, ambos temas:

```text
atomos_botones_claro_isolatedDiff.png
atomos_botones_oscuro_isolatedDiff.png
atomos_indicadores_claro_isolatedDiff.png
atomos_indicadores_oscuro_isolatedDiff.png
moleculas_claro_isolatedDiff.png
moleculas_oscuro_isolatedDiff.png
organismos_claro_isolatedDiff.png
organismos_oscuro_isolatedDiff.png
```

Los archivos `masterImage`, `testImage`, `maskedDiff` e `isolatedDiff` generados/actualizados por el comparador permanecen en `packages/diseno_flutter/test/goldens/failures`. No se cambió ninguna baseline con `--update-goldens`.

## Estado y límites

H10.S1.M7 permanece **A MEDIAS**. La suite completa había pasado 132/132 en Windows con Flutter 3.47.5, pero con Flutter 3.44.8 en Windows terminó 124/132 y mostró ocho diferencias pixelarias. Aún falta validar en el runner canónico macOS y clasificar las diferencias antes de aceptar cualquier baseline. Windows con 3.44.8 no equivale al renderer macOS.

No se probó build de release, dispositivo físico ni los artefactos del paquete en una app ejecutándose. El árbol tenía goldens modificados previamente; no se restauraron ni descartaron archivos.

## Seguimiento: clasificación visual y comparación 3.47.5

El 2026-10-14 se inspeccionaron individualmente los ocho `isolatedDiff` y `maskedDiff` generados. En botones, indicadores, moléculas y organismos (claro/oscuro), las diferencias visibles se concentran en contornos finos/antialiasing de bordes; no se aprecia desplazamiento de composición ni cambio de dimensiones del catálogo. No se aprobaron ni actualizaron snapshots.

Para comprobar si el resultado dependía del SDK, se alineó temporalmente `.dart_tool` con Flutter 3.47.5 respetando el lock original del repositorio, y se repitió la suite completa sin `--update-goldens`:

```text
Flutter 3.47.5 • Dart 3.13.4
flutter pub get --offline --enforce-lockfile
Got dependencies!

flutter test --no-pub --concurrency=1 --reporter=compact --timeout 60s
00:42 +132: All tests passed!
exit_code=0
```

La evidencia conjunta permite clasificar las ocho fallas de Windows/3.44.8 como **dependientes del SDK/renderizado en Windows**, no como cambios visuales de composición que justifiquen regenerar baselines. Tras la comparación se restauró el `pubspec.lock` local exactamente a sus cuatro downgrades previos (matcher, meta, test_api y vector_math para Flutter 3.44.8) y se alineó `.dart_tool` de nuevo con `flutter pub get --offline --enforce-lockfile`; no hay diferencia neta nueva del lock causada por esta comprobación. H10.S1.M7 continúa **A MEDIAS** hasta ejecutar la suite en el runner canónico macOS/Flutter 3.44.8 y resolver allí cualquier discrepancia restante.
