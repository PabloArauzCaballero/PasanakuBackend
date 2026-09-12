#!/usr/bin/env bash
# Corre build_runner en cada cliente Dart declarado en pubspec.yaml. Los .g.dart son
# generados y están gitignored: sin este paso, ningún carril de pantallas compila.
set -euo pipefail
cd "$(dirname "$0")/../../../clientes/dart"
for servicio in identidad nucleo-financiero tarifas grupos aportes transparencia entregas garantia organizador notificaciones; do
  echo "== $servicio =="
  (cd "$servicio" && dart pub get && dart run build_runner build --delete-conflicting-outputs)
done
