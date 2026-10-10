#!/usr/bin/env bash
# Diagnostico de SECURITY DEFINER sobre una base desechable con el esquema real (5439).
#   bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash despliegue/diagnosticos/diagnostico_definer.sh
# 1) aplica sql/aplicar.sql en un PostgreSQL de Docker; 2) corre security_definer.sql; 3) corre la
# PRUEBA DE DERIVA PROPUESTA (deriva_security_definer.sql) contra el estado actual: tiene que FALLAR,
# porque describe el estado deseado y hoy no se cumple; esa falla es la linea base de la correccion.
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$AQUI/../.." && pwd)"
PUERTO="${PUERTO:-5439}"; CT="${CONTENEDOR:-aportaya-pg-s2-definer}"; INICIE_DOCKER=0
sql() { MSYS_NO_PATHCONV=1 docker exec -i "$CT" psql -q -U pasanaku -d pasanaku "$@"; }
limpiar() { docker rm -f -v "$CT" >/dev/null 2>&1; [ "$INICIE_DOCKER" = 1 ] && { docker desktop stop >/dev/null 2>&1 || taskkill //IM "Docker Desktop.exe" //F >/dev/null 2>&1; }; }
trap limpiar EXIT
if ! docker info >/dev/null 2>&1; then
  INICIE_DOCKER=1; ("/c/Program Files/Docker/Docker/Docker Desktop.exe" >/dev/null 2>&1 &)
  for _ in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 5; done
  docker info >/dev/null 2>&1 || { echo "Docker no levanto"; exit 3; }
fi
docker rm -f -v "$CT" >/dev/null 2>&1
MSYS_NO_PATHCONV=1 docker run -d --name "$CT" -e POSTGRES_DB=pasanaku -e POSTGRES_USER=pasanaku -e POSTGRES_PASSWORD=pasanaku \
  -e POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=C" -p "127.0.0.1:${PUERTO}:5432" \
  -v "$REPO/sql:/repo/sql:ro" -v "$REPO/despliegue/compose/init:/docker-entrypoint-initdb.d:ro" \
  postgres:16 -c max_connections=100 >/dev/null || exit 4
for _ in $(seq 1 90); do
  docker exec "$CT" pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && sleep 3 \
    && docker exec "$CT" pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && break
  sleep 3
done
sql -v ON_ERROR_STOP=1 -f /repo/sql/aplicar.sql >/dev/null 2>&1 || { echo "aplicar.sql fallo"; exit 5; }
echo "== DIAGNOSTICO (seccion | clave | valor ...)"
sql -tA -F'|' < "$AQUI/security_definer.sql" | tee "${TMPDIR:-${TEMP:-/tmp}}/definer_diagnostico.txt"
# Lista PROPUESTA (candidatas): las DEFINER cuyo cuerpo cruza de esquema. Es el punto de partida de la revision humana.
PERMITIDAS=$(awk -F'|' '$1=="D5" && $6=="t" {print $2}' "${TMPDIR:-${TEMP:-/tmp}}/definer_diagnostico.txt" | paste -sd, -)
echo
echo "== LISTA EXPLICITA PROPUESTA (candidatas que cruzan esquema): $(echo "$PERMITIDAS" | tr ',' '
' | grep -c .) funciones"
echo "$PERMITIDAS" | tr ',' '
' | sed 's/^/   /'
echo
echo "== PRUEBA DE DERIVA PROPUESTA contra el estado ACTUAL (debe fallar: describe el estado deseado)"
sql -tA -F'|' -v permitidas="$PERMITIDAS" < "$AQUI/deriva_security_definer.sql"
exit 0
