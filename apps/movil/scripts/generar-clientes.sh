#!/usr/bin/env bash
# Corre build_runner en cada cliente Dart declarado en pubspec.yaml. Los .g.dart son
# generados y están gitignored: sin este paso, ningún carril de pantallas compila.
set -euo pipefail
cd "$(dirname "$0")/../../../clientes/dart"
for servicio in identidad nucleo-financiero tarifas grupos aportes transparencia entregas garantia organizador notificaciones cumplimiento; do
  echo "== $servicio =="
  # Los pubspec.yaml son generados (OpenAPI) y piden `json_serializable: ^6.9.3` sin
  # lockfile: el 2026-10-19 salió la 6.15.0, que no compila con source_helper y frenó
  # el despliegue de TEST. Se fija la última que anda, sin tocar lo generado.
  printf '%s\n' 'dependency_overrides:' '  json_serializable: 6.14.1' > "$servicio/pubspec_overrides.yaml"
  (cd "$servicio" && dart pub get && dart run build_runner build --delete-conflicting-outputs)
done
