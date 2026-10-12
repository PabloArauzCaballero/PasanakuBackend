#!/usr/bin/env bash
# Variante ligera de heavy.sh para procesos MODERADOS y cortos (p. ej. un Chromium de scraping en tramos de minutos):
# NO toma el candado exclusivo, solo espera a que haya >= MIN_FREE_MB libres (por defecto 3000) antes de lanzar el comando.
# Para cosas pesadas (gradle, docker, builds, suites completas) seguir usando heavy.sh.
MIN_FREE_MB="${MIN_FREE_MB:-3000}"; MAX_WAIT_S="${MAX_WAIT_S:-1800}"
free_mb() { powershell.exe -NoProfile -Command "[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)" | tr -d '\r'; }
waited=0
while [ "$(free_mb)" -lt "$MIN_FREE_MB" ]; do
  [ "$waited" -ge "$MAX_WAIT_S" ] && { echo "lite.sh: timeout esperando RAM (libres: $(free_mb) MB)" >&2; exit 75; }
  sleep 20; waited=$((waited + 20))
done
echo "lite.sh: libres $(free_mb) MB -> $*" >&2
"$@"
