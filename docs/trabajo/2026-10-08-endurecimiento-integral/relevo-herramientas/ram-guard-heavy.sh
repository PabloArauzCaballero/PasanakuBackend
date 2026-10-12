#!/usr/bin/env bash
# Guardián de RAM compartido entre agentes. Uso:  heavy.sh <comando pesado...>
# Espera a que haya >= MIN_FREE_MB libres y a que nadie tenga el candado, corre el comando, suelta el candado.
# Candado: C:/Users/Usuario/.claude/heavy.lock (directorio). Un candado de más de 30 min se considera huérfano.
LOCK="C:/Users/Usuario/.claude/heavy.lock"
MIN_FREE_MB="${MIN_FREE_MB:-3500}"
MAX_WAIT_S="${MAX_WAIT_S:-900}"

free_mb() {
  powershell.exe -NoProfile -Command "[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)" | tr -d '\r'
}
lock_age_min() {
  [ -d "$LOCK" ] || { echo 0; return; }
  echo $(( ( $(date +%s) - $(stat -c %Y "$LOCK") ) / 60 ))
}

waited=0
while true; do
  if [ -d "$LOCK" ] && [ "$(lock_age_min)" -ge 30 ]; then rmdir "$LOCK" 2>/dev/null; fi
  if [ "$(free_mb)" -ge "$MIN_FREE_MB" ] && mkdir "$LOCK" 2>/dev/null; then break; fi
  [ "$waited" -ge "$MAX_WAIT_S" ] && { echo "heavy.sh: timeout esperando RAM/candado (libres: $(free_mb) MB)" >&2; exit 75; }
  sleep 20; waited=$((waited + 20))
done
trap 'rmdir "$LOCK" 2>/dev/null' EXIT INT TERM
echo "heavy.sh: candado tomado, libres $(free_mb) MB -> $*" >&2
"$@"
