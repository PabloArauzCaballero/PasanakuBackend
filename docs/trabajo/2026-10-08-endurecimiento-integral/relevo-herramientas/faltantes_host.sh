#!/usr/bin/env bash
# Corre EN EL VPS, en segundo plano. Reconstruye lo que el temporizador NO rehizo
# (gateway, tarifas, transparencia, web, backoffice, movil-web), y recien si todo sale bien
# dispara el despliegue. Log: /opt/aportaya/logs/faltantes.log
set -uo pipefail
cd /opt/aportaya/repo
git fetch -q --depth 50 origin test
git checkout -f -q FETCH_HEAD
echo "[$(date +%T)] commit: $(git log --oneline -1 | cut -c1-60)"
FALLAS=0
for par in "gateway plataforma" "tarifas servicios" "transparencia servicios"; do
  echo "[$(date +%T)] construyendo $par"
  /opt/aportaya/bin/construir.sh $par >> /opt/aportaya/logs/faltantes-build.log 2>&1 || { echo "FALLA $par"; FALLAS=$((FALLAS+1)); }
done
echo "[$(date +%T)] construyendo backoffice"
docker build -f apps/backoffice/docker/Dockerfile.backoffice --build-arg CONF_NGINX=apps/backoffice/docker/nginx.desplegado.conf -t aportaya/backoffice:test . >> /opt/aportaya/logs/faltantes-build.log 2>&1 || { echo "FALLA backoffice"; FALLAS=$((FALLAS+1)); }
echo "[$(date +%T)] construyendo web"
docker build -f apps/web/docker/Dockerfile.web -t aportaya/web:test . >> /opt/aportaya/logs/faltantes-build.log 2>&1 || { echo "FALLA web"; FALLAS=$((FALLAS+1)); }
echo "[$(date +%T)] construyendo movil-web"
/opt/aportaya/bin/construir-movil-web.sh >> /opt/aportaya/logs/faltantes-build.log 2>&1 || { echo "FALLA movil-web"; FALLAS=$((FALLAS+1)); }
if [ "$FALLAS" -gt 0 ]; then echo "[$(date +%T)] SIN DESPLEGAR: $FALLAS falla(s)"; exit 1; fi
echo "[$(date +%T)] desplegando"
/opt/aportaya/bin/disparar-despliegue.sh >> /opt/aportaya/logs/faltantes.log 2>&1
echo "[$(date +%T)] FIN OK"
