#!/usr/bin/env bash
# Espera a que el despliegue de Coolify levante los contenedores NUEVOS y reporta estado.
K=~/.ssh/id_ed25519_contabo
S="ssh -i $K -o IdentitiesOnly=yes -o BatchMode=yes -o ConnectTimeout=10 root@161.97.85.216"
for i in $(seq 1 60); do
  R=$($S 'docker ps -a --format "{{.Names}}|{{.Status}}" | grep -E "^(gateway|identidad|nucleo-financiero|inversiones|grupos|aportes|entregas|garantia|erp|notificaciones|organizador|auditoria|cumplimiento|publicidad|tarifas|transparencia|esquema)-n3wymuuo" | sed -E "s/-n3wymuuo[a-z0-9]+-[0-9T]+//"' 2>&1)
  NUEVOS=$(echo "$R" | grep -cE "Up [0-9]+ (seconds|minutes?)|Up About a minute|Exited \(0\) [0-9]+ (seconds|minutes?)")
  SANOS=$(echo "$R" | grep -c "(healthy)")
  echo "$(date +%T) nuevos=$NUEVOS sanos=$SANOS"
  if echo "$R" | grep -qE "Exited \([1-9]|Restarting|unhealthy"; then echo "PROBLEMA"; echo "$R"; exit 0; fi
  if [ "$NUEVOS" -ge 16 ] && [ "$SANOS" -ge 16 ]; then echo "TODOS SANOS"; echo "$R"; exit 0; fi
  sleep 30
done
echo TIMEOUT; echo "$R"
