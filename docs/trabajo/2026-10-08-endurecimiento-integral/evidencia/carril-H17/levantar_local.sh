#!/usr/bin/env bash
# Ensayo LOCAL (Linux) de integracion real: los servicios como procesos del host contra PostgreSQL y Kafka reales
# (contenedores aportaya-pg :5433 y aportaya-kafka :9092). Secretos SINTETICOS generados al vuelo en /tmp; no salen del contenedor.
# Uso: levantar_local.sh up | down | estado
D=/tmp/claude-0/e2e; mkdir -p "$D"
RAIZ=/home/user/PasanakuBackend
declare -A PUERTO=( [identidad]=18081 [grupos]=18082 [tarifas]=18083 [nucleo-financiero]=18084 [aportes]=18085 [organizador]=18086 [cumplimiento]=18087 [notificaciones]=18088 [garantia]=18095 )
ORDEN=(identidad tarifas cumplimiento organizador notificaciones aportes garantia grupos nucleo-financiero)
[ -f $D/secretos.env ] || { umask 077; cat > $D/secretos.env <<ENV
SEGURIDAD_PIMIENTA=$(openssl rand -hex 32)
INVITACIONES_CLAVE=$(openssl rand -hex 32)
QR_INTERNO_CLAVE=$(openssl rand -hex 32)
WEBHOOK_SECRETO=$(openssl rand -hex 32)
ARCHIVOS_CLAVE=$(openssl rand -hex 16)
ENV
}
set -a; . $D/secretos.env; set +a
u() { echo "http://127.0.0.1:${PUERTO[$1]}"; }
arrancar() {
  local s=$1
  ( export SERVER_PORT=${PUERTO[$s]} SPRING_PROFILES_ACTIVE=local BD_URL=jdbc:postgresql://127.0.0.1:5433/pasanaku BD_CLAVE=pasanaku \
      KAFKA_URL=127.0.0.1:9092 JWKS_URI=$(u identidad)/.well-known/jwks.json JAVA_TOOL_OPTIONS="-Xmx400m -XX:+UseSerialGC" \
      URL_IDENTIDAD=$(u identidad) URL_GRUPOS=$(u grupos) URL_TARIFAS=$(u tarifas) URL_APORTES=$(u aportes) URL_ENTREGAS=http://127.0.0.1:18099 \
      URL_ORGANIZADOR=$(u organizador) URL_CUMPLIMIENTO=$(u cumplimiento) URL_NOTIFICACIONES=$(u notificaciones) \
      URL_GARANTIA=$(u garantia) URL_TRANSPARENCIA=http://127.0.0.1:18096 \
      ARCHIVOS_BUCKET=aportaya-archivos ARCHIVOS_URL=http://127.0.0.1:19000 ARCHIVOS_USUARIO=aportaya NOTIFICATION_EMAIL_PROVIDER=local \
      BASE_URL_PUBLICA=http://localhost SIN_NIT_EMISOR=1234567890 CUENTA_PUENTE_CUSTODIA=00000000-0000-0000-0000-0000000000c0 \
      APORTAYA_CORS_ORIGENES=http://localhost CUENTAS_LOGICAS_APTO_PRODUCCION=false \
      ADMISION_HUMANA_OBLIGATORIA=${ADMISION_HUMANA_OBLIGATORIA:-false} SUSTITUCION_AUTOMATICA_ADMIN=false;
    nohup java -jar $RAIZ/servicios/$s/build/libs/$s-1.0.0.jar > $D/$s.log 2>&1 & echo $! > $D/$s.pid )
}
case "${1:-estado}" in
  up) for s in "${ORDEN[@]}"; do arrancar $s; done
      for s in "${ORDEN[@]}"; do
        for i in $(seq 1 90); do [ "$(curl -s -o /dev/null -w '%{http_code}' $(u $s)/actuator/health)" = 200 ] && break; sleep 2; done
        echo "$s $(curl -s -o /dev/null -w '%{http_code}' $(u $s)/actuator/health)"
      done ;;
  reiniciar) shift; for s in "$@"; do [ -f $D/$s.pid ] && kill $(cat $D/$s.pid) 2>/dev/null; sleep 3; arrancar $s; for i in $(seq 1 90); do [ "$(curl -s -o /dev/null -w '%{http_code}' $(u $s)/actuator/health)" = 200 ] && break; sleep 2; done; echo "$s $(curl -s -o /dev/null -w '%{http_code}' $(u $s)/actuator/health)"; done ;;
  down) for s in "${ORDEN[@]}"; do [ -f $D/$s.pid ] && kill $(cat $D/$s.pid) 2>/dev/null; rm -f $D/$s.pid; done ;;
  estado) for s in "${ORDEN[@]}"; do echo "$s $(curl -s -o /dev/null -w '%{http_code}' -m 3 $(u $s)/actuator/health)"; done ;;
esac
