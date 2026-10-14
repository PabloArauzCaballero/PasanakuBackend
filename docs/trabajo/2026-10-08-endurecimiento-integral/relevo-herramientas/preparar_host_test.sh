#!/usr/bin/env bash
# Corre EN EL VPS (via ssh ... 'bash -s' < este archivo). Tres cosas, en orden:
#  1) frena el temporizador y la corrida en curso (estaba construyendo desde el arbol VIEJO,
#     porque `git checkout FETCH_HEAD` fallaba por los clientes generados modificados);
#  2) deja el repo del host en el commit nuevo y corrige el checkout del script (-f);
#  3) EXPANDE el esquema de la base real (el build genera jOOQ introspeccionando la base VIVA:
#     sin las tablas nuevas, inversiones y los cambios de grupos/nucleo no compilan).
# No toca datos existentes (el esquema solo se expande; una sola transaccion). No usa ROLES_CLAVE:
# la credencial de svc_inversiones la pone despues el job de esquema del compose con BD_CLAVE.
set -euo pipefail
echo "== 1) frenar temporizador y corrida en curso"
systemctl stop aportaya-autodespliegue.timer || true
pkill -f reconstruir-si-cambio.sh || true
pkill -f "docker build --network=host -f despliegue/Dockerfile" || true
sleep 3
echo "== 2) repo del host al commit nuevo"
cd /opt/aportaya/repo
git fetch -q --depth 50 origin test
git checkout -f -q FETCH_HEAD
echo "HEAD: $(git log --oneline -1 | cut -c1-70)"
test -d servicios/inversiones || { echo "ERROR: falta servicios/inversiones"; exit 2; }
sed -i 's/git checkout -q FETCH_HEAD/git checkout -f -q FETCH_HEAD/' /opt/aportaya/bin/reconstruir-si-cambio.sh
grep -n 'checkout -f' /opt/aportaya/bin/reconstruir-si-cambio.sh | head -2
echo "== 3) esquema: imagen y trabajo contra la base real"
set -a; . /opt/aportaya/postgres/.env; set +a
docker build -q -f despliegue/Dockerfile.esquema -t aportaya/esquema:test . >/dev/null
docker run --rm --network aportaya-interna \
  -e PGHOST=aportaya-postgres -e PGUSER=pasanaku -e PGPASSWORD="$POSTGRES_PASSWORD" -e PGDATABASE=pasanaku \
  aportaya/esquema:test 2>&1 | tail -40
echo "== 4) reanudar temporizador (reconstruye los servicios sobre la base ya expandida)"
systemctl start aportaya-autodespliegue.timer
echo "LISTO. Vigila con: curl -s -o /dev/null -w '%{http_code}\n' https://api.aportaya.161.97.85.216.sslip.io/actuator/health"
