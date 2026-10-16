#!/usr/bin/env bash
# Corre la suite de aceptación con DOS usuarios (A en apps/web, B en apps/backoffice). Se lanza SIEMPRE por el guardián de RAM:
#   bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash apps/backoffice/e2e/aceptacion/scripts/aceptacion.sh [mock|real] [filtro-de-playwright...]
# Levanta los servidores de desarrollo de web y backoffice (uno por puerto, puerto base 5445), corre Playwright con UN worker y UN Chromium,
# y al terminar (también si falla) baja TODO lo que levantó y verifica que los puertos quedaron libres.
set -u
MODO="${1:-mock}"; shift || true
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
BASE=${PUERTO_BASE:-5445}
export ACEPTACION_MODO="$MODO" PUERTO_BASE="$BASE" NG_CLI_ANALYTICS=false
export BASE_API="${BASE_API:-http://127.0.0.1:$((BASE+1))}" BASE_BO="${BASE_BO:-http://127.0.0.1:$((BASE+2))}" BASE_WEB="${BASE_WEB:-http://127.0.0.1:$((BASE+3))}"
PBO=$(echo "$BASE_BO" | sed 's/.*://'); PWEB=$(echo "$BASE_WEB" | sed 's/.*://'); PAPI=$(echo "$BASE_API" | sed 's/.*://')
LOG="${LOG_DIR:-$RAIZ/docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-E3}"; mkdir -p "$LOG"

dueno() { powershell.exe -NoProfile -Command "(Get-NetTCPConnection -LocalPort $1 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess" | tr -d '\r'; }
bajar() { local p; p=$(dueno "$1"); [ -n "$p" ] && taskkill.exe //PID "$p" //T //F >/dev/null 2>&1; true; }
limpiar() { bajar "$PBO"; bajar "$PWEB"; [ "$MODO" = mock ] && bajar "$PAPI"; }
trap limpiar EXIT INT TERM

for p in "$PBO" "$PWEB"; do
  [ -n "$(dueno "$p")" ] && { echo "aceptacion.sh: el puerto $p ya está ocupado (pid $(dueno "$p")); no se reutiliza." >&2; exit 70; }
done

espera() { # espera($1=url, $2=segundos) -> 0 si responde
  local t=0; until curl -s -o /dev/null --max-time 3 "$1"; do sleep 3; t=$((t+3)); [ $t -ge "$2" ] && return 1; done; return 0
}

echo "== backoffice (ng serve) en $BASE_BO, proxy -> $BASE_API"
( cd "$RAIZ/apps/backoffice" && exec npx ng serve --port "$PBO" --host 127.0.0.1 --proxy-config e2e/aceptacion/proxy.aceptacion.mjs ) >"$LOG/servidor-bo-$MODO.log" 2>&1 &
echo "== web (ng serve) en $BASE_WEB"
( cd "$RAIZ/apps/web" && exec npx ng serve --port "$PWEB" --host 127.0.0.1 ) >"$LOG/servidor-web-$MODO.log" 2>&1 &
espera "$BASE_BO/ingreso" 420 || { echo "backoffice no arrancó"; tail -20 "$LOG/servidor-bo-$MODO.log"; exit 71; }
espera "$BASE_WEB/" 420 || { echo "web no arrancó"; tail -20 "$LOG/servidor-web-$MODO.log"; exit 72; }
# Calentamiento: la primera carga de Vite optimiza dependencias (se ve en el log del servidor) y no debe contarse contra ninguna prueba.
curl -s -o /dev/null --max-time 120 "$BASE_BO/ingreso"; curl -s -o /dev/null --max-time 120 "$BASE_WEB/invitacion/g/i"

RC=0
echo "== suite de dos usuarios (backoffice + doble/real), modo $MODO"
( cd "$RAIZ/apps/backoffice" && npx playwright test -c e2e/aceptacion/playwright.aceptacion.config.ts "$@" ) 2>&1 | tee "$LOG/corrida-bo-$MODO.log"
[ "${PIPESTATUS[0]}" -ne 0 ] && RC=1
echo "== lado A en el sitio web"
( cd "$RAIZ/apps/web" && npx playwright test -c pruebas/e2e/aceptacion/playwright.aceptacion.config.ts ) 2>&1 | tee "$LOG/corrida-web-$MODO.log"
[ "${PIPESTATUS[0]}" -ne 0 ] && RC=1
limpiar
for p in "$PBO" "$PWEB" "$PAPI"; do [ -n "$(dueno "$p")" ] && echo "AVISO: el puerto $p sigue ocupado" >&2; done
exit $RC
