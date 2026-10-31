#!/usr/bin/env bash
# Corre un build o una suite con exclusión mutua entre agentes (regla 70.1.4):
#   bash docs/trabajo/2026-10-30-arenas-e-inicio-75/candado.sh <comando...>
# Dos candados (CLAUDE.md, 2026-10-30): uno para Gradle/Java y otro para Flutter/Dart.
# Las dos herramientas no comparten daemon, caché ni puertos; dentro de cada una sigue
# corriendo un solo build a la vez. El candado es un directorio: `mkdir` es atómico. Se
# espera por condición (que el directorio deje de existir), no por un tiempo fijo.
set -u
BASE=/private/tmp/claude-501/-Users-pablo-Documents-GitHub-Pasanaku/ec624df3-ae5d-49eb-9700-b63e3c2268a5/scratchpad
case "$*" in
  *gradle*|*java*) CANDADO=$BASE/build-java.lock ;;
  *) CANDADO=$BASE/build-flutter.lock ;;
esac
# Transición desde el candado único: si alguien todavía corre con el viejo, se lo espera.
while [ -d "$BASE/build.lock" ]; do sleep 3; done
until mkdir "$CANDADO" 2>/dev/null; do sleep 3; done
trap 'rmdir "$CANDADO"' EXIT
"$@"
