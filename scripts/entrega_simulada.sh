#!/usr/bin/env bash
# ENTREGA SIMULADA REPRODUCIBLE (H14.S1.M5). Lo que se entrega es un recorrido que cualquiera puede repetir desde un clon limpio,
# con fixtures propios (cuentas sintéticas generadas en el momento, contratos OpenAPI y ejemplos del repositorio) y SIN ningún proveedor real.
#
#   bash scripts/entrega_simulada.sh frontend     # instala, genera, tipos, lint, pruebas, a11y y aceptación de dos usuarios en modo MOCK
#   bash scripts/entrega_simulada.sh backend      # arranque del stack por tandas (Docker) y humo por el gateway (lo corre quien opera el entorno)
#   bash scripts/entrega_simulada.sh todo         # frontend + backend
#
# Variables (todas opcionales):
#   ENTREGA_SALIDA=<dir>     dónde queda la evidencia (defecto: evidencia/entrega-simulada/<fecha-hora>, dentro del clon)
#   ENTREGA_INSTALAR=0       no corre `yarn install --immutable` (ya está instalado)
#   HEAVY_SH=<ruta>          guardián de RAM (defecto: el de la máquina del equipo). Sin él, los pasos corren directo y avisa.
#   PUERTO_BASE=5447         puertos de la aceptación (base+1 doble de API, base+2 backoffice, base+3 sitio)
#
# Cada paso pesado va por el guardián, de a uno (regla 70.1). El script NO borra nada, NO toca git y NO usa credenciales reales.
# Termina con RESUMEN.md: tabla PASS/FAIL por paso, versiones y los LÍMITES de lo que se probó.
set -u
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
QUE="${1:-frontend}"
SELLO="$(date +%Y%m%d-%H%M%S)"
SALIDA="${ENTREGA_SALIDA:-$RAIZ/evidencia/entrega-simulada/$SELLO}"
HEAVY="${HEAVY_SH:-C:/Users/Usuario/.claude/ram-guard/heavy.sh}"
PUERTO_BASE="${PUERTO_BASE:-5447}"
PY="$(command -v python3 || command -v python)"
export PYTHONUTF8=1
mkdir -p "$SALIDA"
cd "$RAIZ" || exit 2

RESUMEN="$SALIDA/RESUMEN.md"
FILAS=""
FALLOS=0
N=0

pesado() { if [ -f "$HEAVY" ]; then bash "$HEAVY" "$@"; else echo "AVISO: sin guardián de RAM ($HEAVY); corriendo directo" >&2; "$@"; fi; }

# paso <nombre> <comando...>  → log en $SALIDA/NN-nombre.log; no corta la entrega: sigue y lo anota.
paso() {
  local nombre="$1"; shift
  N=$((N + 1)); local id; id="$(printf '%02d' "$N")"
  local log="$SALIDA/$id-$nombre.log" t0 t1 rc
  t0=$(date +%s)
  echo "== [$id] $nombre"
  "$@" > "$log" 2>&1
  rc=$?
  t1=$(date +%s)
  local estado="PASS"
  if [ "$rc" -ne 0 ]; then estado="FAIL (rc=$rc)"; FALLOS=$((FALLOS + 1)); fi
  FILAS="$FILAS| $id | $nombre | $estado | $((t1 - t0)) s | \`$id-$nombre.log\` |
"
  echo "   -> $estado ($((t1 - t0)) s)"
  return 0
}

# Un comando dentro de apps/<app>, como texto para `bash -c` (las funciones no cruzan al guardián, que lanza un bash nuevo).
en_app() { echo "cd '$RAIZ/apps/$1' && $2"; }
SERIAL="$RAIZ/scripts/vitest-serial.config.mjs"

preflight() {
  echo "node $(node --version) · yarn $(yarn --version 2>/dev/null) · python $("$PY" --version 2>&1) · java $(java -version 2>&1 | head -1)"
  echo "commit: $(git rev-parse HEAD 2>/dev/null) · árbol: $(git status --porcelain 2>/dev/null | wc -l) archivos con cambios sin confirmar"
  node -e "process.exit(Number(process.versions.node.split('.')[0]) >= 22 ? 0 : 1)" || { echo "FALTA node >= 22"; return 1; }
  "$PY" -c "import sys; sys.exit(0 if sys.version_info >= (3, 10) else 1)" || { echo "FALTA python >= 3.10"; return 1; }
}

frontend() {
  paso preflight preflight
  if [ "${ENTREGA_INSTALAR:-1}" = "1" ]; then paso instalar pesado yarn install --immutable; fi
  paso tokens pesado yarn workspace @aportaya/tokens build
  paso contenido-sitio pesado yarn workspace @aportaya/web contenido
  paso simulado-build pesado yarn workspace @aportaya/simulado build
  paso simulado-tipos pesado bash -c "cd '$RAIZ/packages/simulado' && npx tsc -p tsconfig.json"
  paso simulado-lint pesado bash -c "cd '$RAIZ/packages/simulado' && npx eslint src scripts"
  paso simulado-pruebas pesado bash -c "cd '$RAIZ/packages/simulado' && npx vitest run"
  paso rutas-vs-contratos "$PY" "$RAIZ/scripts/verificar_rutas_frontend.py" --tabla
  for app in backoffice web; do
    paso "$app-tipos" pesado bash -c "$(en_app "$app" "npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit")"
    paso "$app-lint" pesado bash -c "$(en_app "$app" "npx ng lint && $PY ../../scripts/verificar_frontend.py $app")"
    paso "$app-pruebas" pesado bash -c "$(en_app "$app" "npx ng test --runner-config='$SERIAL' --include='src/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts' --no-watch")"
    paso "$app-a11y" pesado bash -c "$(en_app "$app" "npx ng test --runner-config='$SERIAL' --include='src/**/*.a11y.spec.ts' --no-watch")"
  done
  # Aceptación con DOS usuarios (A en el sitio web, B en el backoffice) contra el DOBLE con estado. axe estricto: una violación falla.
  # La corrida levanta y baja sus propios servidores en PUERTO_BASE+1..+3 y verifica que los puertos queden libres.
  export EVIDENCIA_DIR="$SALIDA/aceptacion-mock" LOG_DIR="$SALIDA/aceptacion-mock" AXE_ESTRICTO=1 PUERTO_BASE
  mkdir -p "$EVIDENCIA_DIR"
  paso aceptacion-mock pesado bash "$RAIZ/apps/backoffice/e2e/aceptacion/scripts/aceptacion.sh" mock
}

backend() {
  # El stack completo NO entra en 4 GB de Docker Desktop: arranque_completo.sh lo levanta por tandas y se re-lanza solo bajo el guardián.
  paso backend-arranque-por-tandas env D3_MODO=tandas D3_ETIQUETA="entrega-$SELLO" bash "$RAIZ/despliegue/ensayos/arranque_completo.sh"
  paso backend-bajar bash "$RAIZ/despliegue/ensayos/bajar_entorno.sh"
}

case "$QUE" in
  frontend) frontend ;;
  backend) backend ;;
  todo) frontend; backend ;;
  *) echo "uso: $0 frontend|backend|todo"; exit 64 ;;
esac

{
  echo "# Entrega simulada — $SELLO"
  echo
  echo "- Alcance pedido: \`$QUE\` · commit: \`$(git rev-parse HEAD 2>/dev/null)\` · cambios sin confirmar: $(git status --porcelain 2>/dev/null | wc -l) archivos"
  echo "- node $(node --version) · yarn $(yarn --version 2>/dev/null) · $("$PY" --version 2>&1)"
  echo "- Resultado: **$([ "$FALLOS" -eq 0 ] && echo "TODOS LOS PASOS EN VERDE" || echo "$FALLOS PASO(S) EN ROJO")**"
  echo
  echo "| # | Paso | Estado | Tiempo | Salida literal |"
  echo "|---|---|---|---|---|"
  printf "%s" "$FILAS"
  echo
  echo "## Límites (qué NO prueba esta entrega)"
  echo
  echo "- Toda la parte web corre contra el **doble con estado** y Prism (\`VERIFIED_FUNCTIONAL_ONLY\`): no es integración con los servicios reales."
  echo "- La aceptación en modo \`real\` no corre: \`sembrarReal\` (apps/backoffice/e2e/aceptacion/apoyo/siembra-real.ts) no está implementado."
  echo "- Flutter (apps/movil) no entra en este recorrido: sus pruebas se corren en una máquina con el SDK."
  echo "- No hay lector de pantalla real: la accesibilidad es axe-core + teclado + capturas inspeccionadas."
  echo "- Sin proveedor de pagos, banco, SIN ni UIF reales: cualquier parámetro comercial o fiscal es sintético (\`apto_produccion=false\`)."
  echo "- Los secretos de este recorrido son sintéticos y se generan en el momento; nada de esto sirve en un entorno real."
} > "$RESUMEN"
echo "Resumen: $RESUMEN"
exit "$FALLOS"
