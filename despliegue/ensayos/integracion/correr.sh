#!/usr/bin/env bash
# Ensayo de integracion REAL entre servicios (carril G): levanta el conjunto minimo sobre el compose de TEST, corre los
# recorridos por HTTP con tokens reales de identidad y baja todo.
#
#   bash despliegue/ensayos/integracion/correr.sh dinero       # H8.S1.M3, H8.S1.M4, H9.S1.M5, H14.S1.M1-M3, H13.S1.M6
#   bash despliegue/ensayos/integracion/correr.sh aceptacion   # F1-F4 en modo REAL (dos usuarios: A en web, B en backoffice)
#   bash despliegue/ensayos/integracion/correr.sh dinero recorridos h8m3 h9m5   # solo esos recorridos
#
# Todo pesado va por el guardian de RAM (arranque_completo.sh y la suite de Playwright se envuelven solos). El stack se baja
# SIEMPRE al terminar, salvo con INTEGRACION_MANTENER=1. Variables utiles:
#   INTEGRACION_SALIDA   archivo JSON del informe (por omision, evidencia/carril-G/integracion-<modo>.json)
#   D3_PUERTO_PG / D3_PUERTO_PGBOUNCER / D3_PUERTO_GATEWAY   puertos propios (por omision G: 5446 / 6446 / 18086)
set -uo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(cd "$AQUI/../../.." && pwd)"
MODO="${1:-dinero}"; shift || true
export D3_PUERTO_PG="${D3_PUERTO_PG:-5446}" D3_PUERTO_PGBOUNCER="${D3_PUERTO_PGBOUNCER:-6446}" D3_PUERTO_GATEWAY="${D3_PUERTO_GATEWAY:-18086}"
EVID="${D3_EVIDENCIA:-$RAIZ/docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-G}"
export D3_EVIDENCIA="$EVID"; mkdir -p "$EVID"
export PYTHONUTF8=1
PY="${PYTHON:-python}"

case "$MODO" in
  dinero)
    SERVICIOS="grupos aportes entregas nucleo-financiero garantia cumplimiento organizador notificaciones"
    ETQ="g-dinero" ;;
  aceptacion)
    SERVICIOS="grupos notificaciones garantia transparencia aportes"
    ETQ="g-aceptacion" ;;
  *) echo "uso: correr.sh dinero|aceptacion [recorridos ...]"; exit 2 ;;
esac

bajar() { [ -z "${INTEGRACION_MANTENER:-}" ] && bash "$RAIZ/despliegue/ensayos/bajar_entorno.sh" quiet; }
trap bajar EXIT

echo "== levantando el conjunto: identidad $SERVICIOS + gateway (puerto $D3_PUERTO_GATEWAY)"
D3_MODO=conjunto D3_SEMBRAR=1 D3_MANTENER=1 D3_SOLO="$SERVICIOS" D3_ETIQUETA="$ETQ" \
  bash "$RAIZ/despliegue/ensayos/arranque_completo.sh" || { echo "el stack no levanto (ver $EVID/arranque-$ETQ.txt)"; exit 3; }

export D3_DIR="${D3_DIR:-${TEMP:-/tmp}/aportaya-d3}"
RC=0
if [ "$MODO" = dinero ]; then
  export INTEGRACION_SALIDA="${INTEGRACION_SALIDA:-$EVID/integracion-dinero.json}"
  ( cd "$AQUI" && "$PY" recorridos.py "$@" ) 2>&1 | tee "$EVID/integracion-dinero.log"
  RC=${PIPESTATUS[0]}
else
  export BASE_API="http://127.0.0.1:$D3_PUERTO_GATEWAY" EVIDENCIA_DIR="$EVID/aceptacion-real" LOG_DIR="$EVID/aceptacion-real"
  mkdir -p "$EVID/aceptacion-real"
  bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash "$RAIZ/apps/backoffice/e2e/aceptacion/scripts/aceptacion.sh" real "$@"
  RC=$?
fi
echo "== RC del ensayo: $RC"
exit $RC
