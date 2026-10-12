#!/usr/bin/env bash
# Q·DD-8 sobre Kafka real: se escribe en el outbox de identidad (como lo hace CU-01) un `usuario_registrado` SINTETICO para
# las cuentas sembradas (el alta real pide un OTP de correo que no se escribe en el log). El relevo del outbox lo publica y
# nucleo-financiero lo consume. Dos veces el mismo evento no abre dos billeteras.
q() { docker exec -i aportaya-pg psql -X -tA -U pasanaku -d pasanaku -c "$1"; }
UB=$(q "select id from identidad.usuario where codigo_publico='SINT-E2E-B'")
echo "billeteras del usuario B antes: $(q "select count(*) from nucleo_financiero.cuenta_billetera where usuario_id='$UB' and grupo_id is null")"
EV=$(python3 -c "import uuid;print(uuid.uuid4())"); COR=$(python3 -c "import uuid;print(uuid.uuid4())")
for i in 1 2; do   # el MISMO evento (mismo id) insertado/relevado dos veces = entrega duplicada de Kafka
  if [ $i = 1 ]; then
    q "insert into identidad.evento_dominio (id,tipo,version,agregado,agregado_id,payload,metadatos,correlation_id,ocurrido_en,estado,intentos) values ('$EV','identidad.usuario_registrado','1','usuario','$UB','{\"usuarioId\":\"$UB\",\"nivelSolicitado\":\"BASICO\"}','{\"tema\":\"aportaya.identidad.usuario_registrado\"}','$COR',now(),'PENDIENTE',0)" >/dev/null
  else
    # reentrega: se vuelve a marcar PENDIENTE el mismo evento para que el relevo lo publique otra vez con el mismo id
    q "update identidad.evento_dominio set estado='PENDIENTE', publicado_en=null, intentos=0, proximo_intento_en=null where id='$EV'" >/dev/null
  fi
  for t in $(seq 1 40); do [ "$(q "select estado from identidad.evento_dominio where id='$EV'")" = PUBLICADO ] && break; sleep 1; done
  sleep 3
  echo "entrega $i: outbox=$(q "select estado from identidad.evento_dominio where id='$EV'") · billeteras del usuario B=$(q "select count(*) from nucleo_financiero.cuenta_billetera where usuario_id='$UB' and grupo_id is null") · marcas de consumo=$(q "select count(*) from nucleo_financiero.evento_consumido where evento_id='$EV'" 2>/dev/null || echo n/d)"
done
echo "estado de la billetera: $(q "select estado||' '||moneda from nucleo_financiero.cuenta_billetera where usuario_id='$UB' and grupo_id is null limit 1")"
