#!/usr/bin/env bash
K=~/.ssh/id_ed25519_contabo
S="ssh -i $K -o IdentitiesOnly=yes -o BatchMode=yes -o ConnectTimeout=10 root@161.97.85.216"
INI=$($S 'date +%Y-%m-%dT%H:%M:%S')
for i in $(seq 1 120); do
  R=$($S "awk -v ini='$INI' 'substr(\$0,2,19)>ini' /opt/aportaya/logs/autodespliegue.log | grep -E '^\[2026' | tail -n 5 | cut -c1-150; echo SHA:\$(cut -c1-8 /opt/aportaya/ultimo-construido)" 2>&1)
  echo "$R" | grep -qE "FALLA construyendo|SIN DESPLEGAR|listo en|SHA:aa27284" && { echo "$R"; exit 0; }
  sleep 30
done
echo TIMEOUT; echo "$R"
