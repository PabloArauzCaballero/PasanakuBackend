#!/usr/bin/env bash
# Carril Q: Docker (si hace falta) + Postgres DESECHABLE puerto 5454 + Gradle con los argumentos + limpieza. Un solo proceso pesado.
REPO="C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-cierre"
SP="C:/Users/Usuario/AppData/Local/Temp/claude/C--Users-Usuario-Downloads-Entrypoint-GitHUb-Pasanaku/c468f89e-ca7f-4172-b7d5-74080b60e5a8/scratchpad/q"
cd "$REPO" || exit 2
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"
export JAVA_TOOL_OPTIONS="-Xmx1g -XX:+UseSerialGC"
INICIE_DOCKER=0
if ! docker info >/dev/null 2>&1; then
  INICIE_DOCKER=1
  echo "== levantando Docker Desktop"
  ("/c/Program Files/Docker/Docker/Docker Desktop.exe" >/dev/null 2>&1 &)
  for i in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 5; done
  docker info >/dev/null 2>&1 || { echo "Docker no levanto"; exit 3; }
fi
echo "== libres: $(powershell.exe -NoProfile -Command '[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)' | tr -d '\r') MB"
CT=aportaya-pg-q
docker rm -f $CT >/dev/null 2>&1
MSYS_NO_PATHCONV=1 docker run -d --name $CT -e POSTGRES_DB=pasanaku -e POSTGRES_USER=pasanaku -e POSTGRES_PASSWORD=pasanaku \
  -e POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=C" -p 127.0.0.1:5454:5432 \
  -v "$REPO:/repo:ro" -v "$REPO/despliegue/compose/init:/docker-entrypoint-initdb.d:ro" \
  postgres:16 -c max_connections=300 -c shared_buffers=128MB -c timezone=America/La_Paz >/dev/null || { echo "no pude crear $CT"; exit 4; }
for i in $(seq 1 60); do docker exec $CT pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && sleep 3 && docker exec $CT pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && break; sleep 3; done
MSYS_NO_PATHCONV=1 docker exec -i $CT psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku -q -f /repo/sql/aplicar.sql > "$SP/aplicar.log" 2>&1
echo "== aplicar.sql RC: $?"; tail -3 "$SP/aplicar.log"
export BD_URL_ADMIN=jdbc:postgresql://127.0.0.1:5454/pasanaku
./gradlew.bat --max-workers=2 -Dorg.gradle.parallel=false -Dorg.gradle.jvmargs='-Xmx1536m -XX:MaxMetaspaceSize=512m' "$@"
RC=$?
./gradlew.bat --stop >/dev/null 2>&1
docker rm -f $CT >/dev/null 2>&1
if [ "$INICIE_DOCKER" = 1 ]; then docker desktop stop >/dev/null 2>&1 || taskkill //IM "Docker Desktop.exe" //F >/dev/null 2>&1; fi
echo "== RC gradle: $RC"
exit $RC
