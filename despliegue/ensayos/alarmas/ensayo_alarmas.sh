#!/usr/bin/env bash
# Ensayo de INYECCION DE INCIDENTES para las alarmas de despliegue/alertas/reglas.yml (H13.S1.M5).
#
#   bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash despliegue/ensayos/alarmas/ensayo_alarmas.sh
#
# PostgreSQL desechable en Docker (127.0.0.1:${PUERTO}, 5439 por omision), con el esquema real.
# Fases: (0) en orden no suena nada; (1) se inyectan los cuatro incidentes y suenan las cuatro,
# cada una con su responsable por rol y el cuerpo de notificacion que `notificaciones` espera;
# (2) una alarma abierta no se repite en el registro; (3) se responde segun el procedimiento y las
# cuatro se resuelven, con su duracion; (4) sin nadie a quien avisar la alarma falla en voz alta.
# Todo dato es sintetico. Sale con 0 solo si las cinco fases se cumplen.
set -u

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$AQUI/../../.." && pwd)"
PUERTO="${PUERTO:-5439}"
CT="${CONTENEDOR:-aportaya-pg-s2-alarmas}"
IMAGEN="${IMAGEN:-postgres:16}"
TMP="${TMP:-${TEMP:-/tmp}/ensayo-alarmas-s2}"
REGISTRO="$TMP/alarmas.jsonl"
mkdir -p "$TMP"; : > "$REGISTRO"
FALLAS=0
INICIE_DOCKER=0
PY="${PYTHON:-python}"

ms() { echo $(( $(date +%s%N) / 1000000 )); }
paso() { echo; echo "== $*"; }
falla() { echo "FALLA: $*"; FALLAS=$((FALLAS + 1)); }
sql() { MSYS_NO_PATHCONV=1 docker exec -i "$CT" psql -v ON_ERROR_STOP=1 -q -tA -U pasanaku -d pasanaku "$@"; }
alarmas() { PYTHONUTF8=1 "$PY" "$REPO/despliegue/alertas/evaluar_alarmas.py" \
  --psql "docker exec -i $CT psql -U pasanaku -d pasanaku" --registro "$REGISTRO" "$@"; }

limpiar() {
  docker rm -f -v "$CT" >/dev/null 2>&1
  [ "$INICIE_DOCKER" = 1 ] && { docker desktop stop >/dev/null 2>&1 || taskkill //IM "Docker Desktop.exe" //F >/dev/null 2>&1; }
}
trap limpiar EXIT

echo "ENSAYO DE ALARMAS · $(date -u +%Y-%m-%dT%H:%M:%SZ) · contenedor $CT · puerto $PUERTO"
if ! docker info >/dev/null 2>&1; then
  INICIE_DOCKER=1; paso "levantando Docker Desktop"
  ("/c/Program Files/Docker/Docker/Docker Desktop.exe" >/dev/null 2>&1 &)
  for _ in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 5; done
  docker info >/dev/null 2>&1 || { echo "Docker no levanto"; exit 3; }
fi

paso "F0 base desechable con el esquema real y los catalogos"
docker rm -f -v "$CT" >/dev/null 2>&1
MSYS_NO_PATHCONV=1 docker run -d --name "$CT" -e POSTGRES_DB=pasanaku -e POSTGRES_USER=pasanaku -e POSTGRES_PASSWORD=pasanaku \
  -e POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=C" -p "127.0.0.1:${PUERTO}:5432" \
  -v "$REPO/sql:/repo/sql:ro" -v "$REPO/despliegue/compose/init:/docker-entrypoint-initdb.d:ro" \
  "$IMAGEN" -c max_connections=100 -c shared_buffers=128MB >/dev/null || { echo "no pude crear $CT"; exit 4; }
for _ in $(seq 1 90); do
  docker exec "$CT" pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && sleep 3 \
    && docker exec "$CT" pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && break
  sleep 3
done
sql -f /repo/sql/aplicar.sql >"$TMP/aplicar.log" 2>&1 || { tail -5 "$TMP/aplicar.log"; exit 5; }
# Los catalogos se siembran SIN ON_ERROR_STOP: son de otro carril y pueden estar a mitad de cambio. Lo que este
# ensayo necesita (los roles) se verifica abajo; los errores de siembra se informan, no se tapan.
sql -v ON_ERROR_STOP=0 -f /repo/sql/60_semillas/sembrar.sql >"$TMP/semillas.log" 2>&1
echo "   siembra de catalogos: $(grep -c 'ERROR' "$TMP/semillas.log") error(es)"; grep 'ERROR' "$TMP/semillas.log" | head -3 | sed 's/^/   /'
# HALLAZGO: sql/60_semillas/10-roles-y-permisos.sql:22 falla con «value too long for type character varying(60)» (el nombre del rol
# SERVICIO_INVERSIONES supera rol.nombre VARCHAR(60)) y, al ser un solo INSERT, NINGUN rol queda sembrado. Es de otro carril; para que
# el ensayo no dependa de ello se crean SOLO los 4 roles que usa, con los codigos del catalogo (sinteticos para esta base desechable).
echo "   roles sembrados por el catalogo: $(sql -c 'select count(*) from identidad.rol')"
sql -c "INSERT INTO identidad.rol (codigo, nombre, ambito, es_sistema) VALUES ('TESORERIA','Tesoreria','GLOBAL',true),('OFICIAL_CUMPLIMIENTO','Oficial de cumplimiento','GLOBAL',true),('SOPORTE','Soporte','GLOBAL',true),('RESPONSABLE_SEGURIDAD','Responsable de seguridad','GLOBAL',true),('ADMIN_PLATAFORMA','Administrador de plataforma','GLOBAL',true) ON CONFLICT (codigo) DO NOTHING"

echo "-- personal sintetico con su rol (el catalogo de roles es el sembrado; ADMIN_PLATAFORMA queda SIN persona a proposito)"
sql <<'SQL'
INSERT INTO identidad.usuario (id, codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
SELECT uuid_in(md5('s2:' || r)::cstring), left('S2' || r, 12), 'Sintetico', r, '+000000' || lpad(i::text, 6, '0'),
       DATE '1990-01-01', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', now()
  FROM (VALUES (1, 'TESORERIA'), (2, 'OFICIAL_CUMPLIMIENTO'), (3, 'SOPORTE'), (4, 'RESPONSABLE_SEGURIDAD')) v(i, r);
-- quien otorga el rol es OTRA persona (ck_asignacion_no_autoasignada: nadie se asigna a si mismo, R-SEG-07)
INSERT INTO identidad.usuario (id, codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES (uuid_in(md5('s2:otorgante')::cstring), 'S2OTORGANTE', 'Sintetico', 'Otorgante', '+000000999999', DATE '1990-01-01', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', now());
INSERT INTO identidad.asignacion_rol (usuario_id, rol_id, ambito, otorgada_por, otorgada_en)
SELECT uuid_in(md5('s2:' || r.codigo)::cstring), r.id, 'GLOBAL', uuid_in(md5('s2:otorgante')::cstring), now()
  FROM identidad.rol r WHERE r.codigo IN ('TESORERIA','OFICIAL_CUMPLIMIENTO','SOPORTE','RESPONSABLE_SEGURIDAD');
SQL

paso "F1 todo en orden: no debe sonar nada"
alarmas; RC=$?
[ "$RC" = 0 ] && echo "   OK rc=0" || falla "F1: con todo en orden la salida debia ser 0 y fue $RC"

paso "F2 INYECCION de los cuatro incidentes"
T_INYECCION=$(ms)
sql <<'SQL'
-- Dos cuentas de custodia activas (datos sinteticos; numero cifrado = relleno, no una cuenta real).
INSERT INTO nucleo_financiero.cuenta_custodia (id, tipo, entidad_financiera, numero_cuenta_cifrado, version_llave, numero_enmascarado,
       moneda, saldo_segun_banco, saldo_segun_libro, fecha_saldo, contrato_referencia, es_principal, estado, abierta_en) VALUES
 ('c0570d1a-0000-4000-8000-000000000001','CUENTA_RECAUDADORA','Entidad sintetica','relleno-s2',1,'****0001','BOB',900,900,now(),'S2-CONTRATO-1',true,'ACTIVA',current_date),
 ('c0570d1a-0000-4000-8000-000000000002','CUENTA_ENCAJE','Entidad sintetica','relleno-s2',1,'****0002','BOB',0,0,now(),'S2-CONTRATO-2',false,'ACTIVA',current_date);
-- ALR-01 brecha de caja: hay 900 en custodia y 1000 de dinero electronico emitido. (La cuenta 2 queda SIN conciliar: ALR-04.)
INSERT INTO nucleo_financiero.conciliacion_custodia (cuenta_custodia_id, fecha, saldo_dinero_electronico, saldo_custodia, saldo_en_transito, cumple_encaje, estado)
VALUES ('c0570d1a-0000-4000-8000-000000000001', current_date, 1000, 900, 0, false, 'DESCUADRADA');
-- ALR-02 atraso: una obligacion que vencio y salio de gracia hace 15 dias. Se inserta sin los padres
-- (grupo, periodo, cupo, participante): replica desactiva las FK SOLO en esta sesion de ensayo.
BEGIN;
SET LOCAL session_replication_role = replica;
INSERT INTO aportes.obligacion_aporte (grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado, moneda, monto_pagado,
       monto_recargo, monto_condonado, monto_cubierto_garantia, estado, fecha_vencimiento, fecha_fin_gracia, dias_mora)
VALUES (uuid_in(md5('s2:grupo')::cstring), uuid_in(md5('s2:periodo')::cstring), uuid_in(md5('s2:cupo')::cstring), uuid_in(md5('s2:part')::cstring),
        'APORTE_PERIODICO', 100, 'BOB', 0, 0, 0, 0, 'EN_MORA', current_date - 20, current_date - 15, 15);
COMMIT;
-- ALR-03 cola detenida: un evento pendiente hace una hora y otro TOMADO por un relevo que murio.
INSERT INTO nucleo_financiero.evento_dominio (tipo, agregado, agregado_id, payload, correlation_id, ocurrido_en, estado)
VALUES ('s2.inyectado', 'asiento', uuid_in(md5('s2:ev1')::cstring), '{}', uuid_in(md5('s2:c1')::cstring), now() - interval '1 hour', 'PENDIENTE');
INSERT INTO aportes.evento_dominio (tipo, agregado, agregado_id, payload, correlation_id, ocurrido_en, estado, tomado_en, tomado_por)
VALUES ('s2.inyectado', 'pago', uuid_in(md5('s2:ev2')::cstring), '{}', uuid_in(md5('s2:c2')::cstring), now() - interval '2 hours', 'TOMADO', now() - interval '2 hours', 'relevo-que-murio');
SQL
T_DETECTA0=$(ms)
alarmas --json > "$TMP/f2.txt" 2>&1; RC=$?
T_DETECTA1=$(ms)
grep '^\[ALR\|^AVISO' "$TMP/f2.txt" | sed 's/^/   /'
[ "$RC" = 3 ] || falla "F2: debian sonar alarmas (rc=3) y la salida fue $RC"
for r in ALR-01 ALR-02 ALR-03 ALR-04; do
  grep -q "\[$r\].*DISPARADA" "$TMP/f2.txt" || falla "F2: la regla $r NO sono"
done
echo "   deteccion (inyeccion -> alarma): $(( T_DETECTA1 - T_INYECCION )) ms"

paso "F3 el responsable por rol, el escalamiento y el cuerpo de la notificacion"
"$PY" - "$REGISTRO" "$REPO" <<'PY' || FALLAS=$((FALLAS + 1))
import json, sys, pathlib, uuid, yaml
registro, repo = sys.argv[1], pathlib.Path(sys.argv[2])
alarmas = [json.loads(l) for l in open(registro, encoding="utf-8") if l.strip()]
entrada = yaml.safe_load((repo / "servicios/notificaciones/src/main/resources/openapi/notificaciones.yaml").read_text(encoding="utf-8"))["components"]["schemas"]["EntradaNotificacion"]
esperadas = set(entrada["required"])
roles = {"ALR-01": ("responsable", "TESORERIA"), "ALR-02": ("responsable", "SOPORTE"),
         "ALR-03": ("escalamiento", "RESPONSABLE_SEGURIDAD"), "ALR-04": ("responsable", "TESORERIA")}
malo = 0
for a in alarmas:
    nivel, rol = roles[a["regla"]]
    ok = a["nivel"] == nivel and a["avisado_a_rol"] == rol and len(a["destinatarios"]) == 1
    for n in a["notificaciones"]:
        ok = ok and set(n) == esperadas and uuid.UUID(n["destinatarioId"]) and isinstance(n["datos"], dict) \
            and all(isinstance(v, str) for v in n["datos"].values())
    ok = ok and (repo / a["runbook"]).is_file()
    print(f"   {a['regla']} {a['severidad']:8} nivel={a['nivel']:12} rol={a['avisado_a_rol']:22} runbook existe={(repo / a['runbook']).is_file()} cuerpo valido={ok}")
    malo += 0 if ok else 1
print(f"   EntradaNotificacion exige {sorted(esperadas)}; alarmas registradas: {len(alarmas)}")
sys.exit(1 if malo or len(alarmas) != 4 else 0)
PY

paso "F4 una alarma abierta NO se repite en el registro (se informa como vigente)"
alarmas > "$TMP/f4.txt" 2>&1; RC=$?
grep '^\[ALR' "$TMP/f4.txt" | sed 's/^/   /'
[ "$RC" = 3 ] || falla "F4: debian seguir sonando (rc=3), rc=$RC"
[ "$(grep -c 'alarma_disparada' "$REGISTRO")" = 4 ] || falla "F4: el registro se duplico"
[ "$(grep -c 'VIGENTE' "$TMP/f4.txt")" = 4 ] || falla "F4: las cuatro debian figurar como vigentes"

paso "F5 RESPUESTA segun los procedimientos (docs/operacion/alarmas/*) y resolucion"
T_RESP0=$(ms)
sql <<'SQL'
-- ALR-01 (procedimiento: reponer fondos y re-conciliar): la entidad repone 100 y se concilia de nuevo.
UPDATE nucleo_financiero.cuenta_custodia SET saldo_segun_banco = 1000, saldo_segun_libro = 1000 WHERE id = 'c0570d1a-0000-4000-8000-000000000001';
INSERT INTO nucleo_financiero.conciliacion_custodia (cuenta_custodia_id, fecha, saldo_dinero_electronico, saldo_custodia, saldo_en_transito, cumple_encaje, estado, ejecutada_en)
VALUES ('c0570d1a-0000-4000-8000-000000000001', current_date + 1, 1000, 1000, 0, true, 'CUADRADA', now() + interval '1 second');
-- ALR-04 (procedimiento: ejecutar la conciliacion que falta): se concilia la segunda cuenta.
INSERT INTO nucleo_financiero.conciliacion_custodia (cuenta_custodia_id, fecha, saldo_dinero_electronico, saldo_custodia, saldo_en_transito, cumple_encaje, estado)
VALUES ('c0570d1a-0000-4000-8000-000000000002', current_date, 0, 0, 0, true, 'CUADRADA');
-- ALR-02 (procedimiento: cobrar o llevar a debido proceso): el participante paga.
UPDATE aportes.obligacion_aporte SET monto_pagado = monto_esperado, estado = 'PAGADO', fecha_pago_efectivo = now() WHERE dias_mora = 15;
-- ALR-03 (procedimiento: reiniciar el relevo; NUNCA marcar a mano): el relevo vuelve y publica lo pendiente.
UPDATE nucleo_financiero.evento_dominio SET estado = 'PUBLICADO', publicado_en = now() WHERE tipo = 's2.inyectado';
UPDATE aportes.evento_dominio SET estado = 'PUBLICADO', publicado_en = now(), tomado_en = NULL, tomado_por = NULL WHERE tipo = 's2.inyectado';
SQL
alarmas > "$TMP/f5.txt" 2>&1; RC=$?
T_RESP1=$(ms)
grep '^\[ALR' "$TMP/f5.txt" | sed 's/^/   /'
[ "$RC" = 0 ] || falla "F5: respondidas las cuatro la salida debia ser 0 y fue $RC"
[ "$(grep -c 'alarma_resuelta' "$REGISTRO")" = 4 ] || falla "F5: faltan resoluciones en el registro"
echo "   respuesta + verificacion: $(( T_RESP1 - T_RESP0 )) ms"

paso "F6 sin nadie a quien avisar la alarma FALLA en voz alta (rc=4), no calla"
sql -c "UPDATE identidad.asignacion_rol SET revocada_en = now(), motivo_revocacion = 'ensayo: sin responsables'"
sql -c "INSERT INTO nucleo_financiero.conciliacion_custodia (cuenta_custodia_id, fecha, saldo_dinero_electronico, saldo_custodia, saldo_en_transito, cumple_encaje, estado, ejecutada_en) VALUES ('c0570d1a-0000-4000-8000-000000000001', current_date + 2, 1000, 800, 0, false, 'DESCUADRADA', now() + interval '1 minute')"
alarmas > "$TMP/f6.txt" 2>&1; RC=$?
grep '^\[ALR' "$TMP/f6.txt" | sed 's/^/   /'
[ "$RC" = 4 ] && echo "   OK rc=4 (alarma sin destinatario)" || falla "F6: debia salir 4 y salio $RC"

paso "REGISTRO DE RESPUESTA (alarmas.jsonl, resumido)"
"$PY" - "$REGISTRO" <<'PY'
import json, sys
for l in open(sys.argv[1], encoding="utf-8"):
    e = json.loads(l)
    if e["tipo"] == "alarma_disparada":
        print(f"   {e['detectada_en'][11:19]} DISPARADA {e['regla']} {e['severidad']:8} conteo={e['conteo']} avisado_a={e['avisado_a_rol']} procedimiento={e['runbook']}")
    else:
        print(f"   {e['resuelta_en'][11:19]} RESUELTA  {e['regla']} duracion={e['duracion_s']} s")
PY
echo "fallas: $FALLAS"
[ "$FALLAS" = 0 ] && echo "ENSAYO DE ALARMAS: PASS" || echo "ENSAYO DE ALARMAS: FAIL"
cp "$REGISTRO" "$TMP/alarmas-final.jsonl" 2>/dev/null
exit $((FALLAS > 0))
