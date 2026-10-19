#!/usr/bin/env bash
# Corre build_runner en cada cliente Dart declarado en pubspec.yaml. Los .g.dart son
# generados y están gitignored: sin este paso, ningún carril de pantallas compila.
set -euo pipefail
cd "$(dirname "$0")/../../../clientes/dart"
for servicio in identidad nucleo-financiero tarifas grupos aportes transparencia entregas garantia organizador notificaciones cumplimiento; do
  echo "== $servicio =="
  # Los pubspec.yaml son generados (OpenAPI) y no tienen lockfile: el 2026-10-19 salió
  # source_helper 1.4.0, que json_serializable no compila («createSortedFieldSet is
  # imported from both…»), y frenó el despliegue de TEST. Se fija la pareja que anda,
  # sin tocar lo generado.
  printf '%s\n' 'dependency_overrides:' '  json_serializable: 6.14.1' '  source_helper: 1.3.13' > "$servicio/pubspec_overrides.yaml"
  (cd "$servicio" && dart pub get && dart run build_runner build --delete-conflicting-outputs)
done
