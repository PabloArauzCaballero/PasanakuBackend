#!/usr/bin/env bash
# Humo sobre el stack ya arriba (arranque_completo.sh lo llama; tambien se corre solo).
# Comprueba, contra el gateway publicado en 127.0.0.1:${D3_PUERTO_GATEWAY}:
#   1 · readiness UP de cada servicio (por dentro) y liveness/readiness del gateway
#   2 · una ruta PUBLICA llega a identidad a traves del gateway y responde con error de CONTRATO
#   3 · una ruta PROTEGIDA por cada prefijo de cada servicio responde 401 (ruteada y con el servicio vivo)
#   4 · inversiones ruteada (401, no 404/502/503)
# Sale 0 solo si todo coincide. Un 404 es ruta que no existe, 502/503/504 es servicio caido.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
G="http://127.0.0.1:${D3_PUERTO_GATEWAY}"
FALLAS=0
chk() { # nombre esperado obtenido
  if [ "$2" = "$3" ]; then echo "PASS  $1 -> $3"; else echo "FAIL  $1 -> $3 (esperado $2)"; FALLAS=$((FALLAS+1)); fi
}
code() { curl -s -o /dev/null -m 20 -w '%{http_code}' "$@"; }
# `servicio prefijo` por cada prefijo de rutas.yml (el uri precede al predicado Path).
prefijos() {
  awk '/uri: http:\/\//{ sub(/.*http:\/\//,""); sub(/:8080.*/,""); h=$0 }
       /Path=/{ p=$0; sub(/.*Path=/,"",p); n=split(p,a,","); for(i=1;i<=n;i++){ sub(/\/\*\*$/,"",a[i]); print h" "a[i] } }'     "$RAIZ/plataforma/gateway/src/main/resources/rutas.yml" | sort -u
}
# Con D3_SERVICIOS_ARRIBA ("a b c") solo se prueba lo de esos servicios (modo tandas).
arriba() { [ -z "${D3_SERVICIOS_ARRIBA:-}" ] && return 0; case " $D3_SERVICIOS_ARRIBA " in *" $1 "*) return 0;; esac; return 1; }

echo "--- 1. readiness de cada servicio (por dentro) y gateway"
for s in "${SERVICIOS_NEGOCIO[@]}"; do
  arriba "$s" || continue
  r="$(readiness_directo "$s")"
  chk "readiness $s" UP "$(echo "$r" | sed -n 's/.*"status":"\([A-Z_]*\)".*/\1/p' | head -1)"
done
echo "--- 1b. /actuator/health completo (la sonda de base pega a PostgreSQL por PgBouncer con el rol svc_<servicio>)"
for s in "${SERVICIOS_NEGOCIO[@]}"; do
  arriba "$s" || continue
  h="$(health_directo "$s")"
  chk "health+base $s" UP "$(echo "$h" | sed -n 's/^{"status":"\([A-Z_]*\)".*/\1/p' | head -1)"
done
chk "gateway liveness" 200 "$(code "$G/actuator/health/liveness")"
chk "gateway readiness" 200 "$(code "$G/actuator/health/readiness")"

if arriba identidad; then
echo "--- 2. ruta publica (identidad + PgBouncer + base) a traves del gateway"
c="$(code -X POST -H 'Content-Type: application/json' -d '{"identificador":"nadie@ejemplo.invalid","clave":"incorrecta-0000"}' "$G/api/v1/sesiones")"
case "$c" in 400|401|422) echo "PASS  POST /api/v1/sesiones rechaza por contrato -> $c";; *) echo "FAIL  POST /api/v1/sesiones -> $c (esperado 400/401/422)"; FALLAS=$((FALLAS+1));; esac
c="$(code -X POST -H 'Content-Type: application/json' -H 'Idempotency-Key: humo-d3-0001' -d '{}' "$G/api/v1/usuarios/verificaciones/correo")"
case "$c" in 400|422|202) echo "PASS  POST /api/v1/usuarios/verificaciones/correo es publica -> $c";; *) echo "FAIL  verificaciones/correo -> $c (esperado 400/422/202)"; FALLAS=$((FALLAS+1));; esac
fi

echo "--- 3. ruta protegida por cada prefijo del gateway: sin token -> 401 (o 200 si es publica por contrato)"
while read -r svc p; do
  arriba "$svc" || continue
  c="$(code "$G$p/humo-d3")"
  # /verificar es publico por contrato (verificacion publica de transparencia): no pide sesion.
  esperado=401; [ "$p" = /api/v1/verificar ] && esperado=200
  chk "GET $p/humo-d3 [$svc]" "$esperado" "$c"
done < <(prefijos)

if arriba inversiones; then
echo "--- 4. inversiones ruteada"
chk "GET /api/v1/inversiones/productos sin token" 401 "$(code "$G/api/v1/inversiones/productos")"
fi
echo "--- 5. ruta inexistente"
chk "GET /api/v1/no-existe" 404 "$(code "$G/api/v1/no-existe")"

echo "RESUMEN humo: $FALLAS falla(s)"
[ "$FALLAS" -eq 0 ]
