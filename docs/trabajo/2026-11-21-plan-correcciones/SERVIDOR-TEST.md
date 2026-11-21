# Pasanaku · TEST — lo que tiene que correr tu sesión local (con acceso SSH al VPS)

> Pegale este archivo entero a tu sesión local de Claude. Está escrito para ella. Versión: 2026-11-21, 23:00 UTC.

## Situación

- `test` tiene hasta la promoción **#469**. `dev` tiene bastante más, y **la próxima promoción no puede salir hasta que
  el servidor tenga sus secretos**: sin ellos, `aportes`, `entregas` e `identidad` no arrancan.
- En TEST **sorteo, entrega e inversiones fallan hoy** por falta de credencial de servicio. Viene de antes de #412: el
  compose dejaba esas variables vacías y fijas (lo arregla #481, ya en `dev`), y desde #412 el factor fijo `000000` ya
  no sirve. El arreglo es crear las cuentas de servicio con TOTP real (guion de #482, ya en `dev`).
- La sesión en la nube **no llega a TEST** (el proxy bloquea la IP). Por eso necesita tus salidas.

## Reglas para vos

1. **No imprimas, no pegues y no guardes en el chat ningún secreto** (llaves, claves, semillas TOTP, QR). Los guiones
   ya están hechos para no mostrarlos. Nunca hagas `cat` de un archivo de `/opt/aportaya/secretos/` en el chat.
2. **No hagas `UPDATE` ni `DELETE` sobre datos de dinero.**
3. **No mergees ni empujes nada.** Eso lo hace la sesión de la nube.
4. Pasale a Pablo la salida **completa** de cada paso (ya viene sin secretos).

## Paso 0 · Preparar

- Copiá el bloque del final («Anexo») a `aportaya-promocion.sh` en tu máquina.
- Tené un clon actualizado de `PabloArauzCaballero/PasanakuBackend` en la rama **`dev`**
  (`git fetch origin dev && git checkout dev && git pull`): de ahí salen los guiones de `despliegue/ensayos/`.

## Paso 1 · Estado actual (solo lectura)

```bash
ssh root@161.97.85.216 'bash -s -- estado'   < aportaya-promocion.sh
ssh root@161.97.85.216 'bash -s -- chequear' < aportaya-promocion.sh
```

## Paso 2 · Crear las llaves que pide la próxima promoción (no las imprime)

```bash
ssh root@161.97.85.216 'bash -s -- llaves-mfa' < aportaya-promocion.sh
```

Crea, solo si no existen y con permiso 600, en `/opt/aportaya/secretos/`: `mfa-llaves-test.txt`,
`entregas-llave-cifrado-cuentas-test.txt` y `aportes-webhook-secreto-test.txt`.

## Paso 3 · Crear las cuentas de servicio con TOTP real (no imprime nada)

Desde el clon en `dev`:

```bash
ssh root@161.97.85.216 'bash -s -- --estado' < despliegue/ensayos/aprovisionar_cuentas_servicio_test.sh
ssh root@161.97.85.216 'bash -s'             < despliegue/ensayos/aprovisionar_cuentas_servicio_test.sh
ssh root@161.97.85.216 'bash -s -- --estado' < despliegue/ensayos/aprovisionar_cuentas_servicio_test.sh
```

Deja `grupos-`, `entregas-` e `inversiones-cuenta-servicio-test.env` en `/opt/aportaya/secretos/` (600). `garantia`
se salta hasta que llegue su rol (PR #483); cuando llegue, se vuelve a correr y crea solo esa.

## Paso 4 · Pablo carga las variables en Coolify (no la sesión)

Coolify → aplicación `aportaya-api` → *Environment Variables*. Pablo copia cada valor él mismo desde su terminal
(`ssh root@161.97.85.216 'cat /opt/aportaya/secretos/<archivo>'`), nunca por el chat:

| Variable | De dónde |
|---|---|
| `MFA_LLAVES` | `mfa-llaves-test.txt` |
| `MFA_LLAVE_VIGENTE` | `1` |
| `ENTREGAS_LLAVE_CIFRADO_CUENTAS` | `entregas-llave-cifrado-cuentas-test.txt` |
| `APORTES_WEBHOOK_SECRETO` | `aportes-webhook-secreto-test.txt` (distinto de `WEBHOOK_SECRETO`) |
| `JWT_CLAVE_FIRMA` | confirmar que **ya existe y no está vacía** (desde #442 identidad no arranca sin ella) |
| `GRUPOS_SERVICIO_TELEFONO`, `GRUPOS_SERVICIO_CLAVE`, `GRUPOS_SERVICIO_TOTP_SEMILLA` | `grupos-cuenta-servicio-test.env` |
| `ENTREGAS_SERVICIO_TELEFONO`, `ENTREGAS_SERVICIO_CLAVE`, `ENTREGAS_SERVICIO_TOTP_SEMILLA` | `entregas-cuenta-servicio-test.env` |
| `INVERSIONES_SERVICIO_TELEFONO`, `INVERSIONES_SERVICIO_CLAVE`, `INVERSIONES_SERVICIO_TOTP_SEMILLA` | `inversiones-cuenta-servicio-test.env` |
| `*_SERVICIO_FACTOR_FIJO` (de los tres) | **vacía** |

No hace falta redesplegar todavía: la promoción redespliega.

## Paso 5 · Chequeo previo a la promoción (solo lectura)

```bash
ssh root@161.97.85.216 'bash -s -- chequear' < aportaya-promocion.sh
```

Tiene que dar `RESULTADO: PASA`. En particular, en 0: coberturas en `SOLICITADA`, entregas en `EFECTIVO_ORGANIZADOR`
y reservas/coberturas/planes vivos del respaldo. Si algo da distinto de 0, **no se toca la base**: pasale la salida a
Pablo.

**Avisale a Pablo que mande esta salida a la sesión de la nube: con `PASA`, ella promueve `dev` → `test`.**

## Paso 6 · Después de la promoción (unos 2-3 minutos después de que la nube avise)

```bash
ssh root@161.97.85.216 'bash -s -- estado' < aportaya-promocion.sh
```

## Paso 7 · TOTP de los operadores humanos de prueba (lo corre Pablo, en su terminal, fuera de Claude)

```bash
ssh root@161.97.85.216 'bash -s -- --estado' < despliegue/ensayos/arrancar_totp_test.sh
```

Si faltan, Pablo corre sin `--estado`, en su propia terminal: el guion muestra la semilla una sola vez para cargarla en
su app autenticadora, y no tiene que quedar en ningún chat.

## Qué le devolvés a Pablo

Las salidas de los pasos 1, 2, 3 (`--estado` antes y después), 5 y 6, en ese orden.

---

## Anexo: aportaya-promocion.sh

```bash
#!/usr/bin/env bash
# aportaya-promocion.sh — lo que Pablo corre en el VPS de TEST (161.97.85.216) durante la
# promoción de dev a test del plan de correcciones (2026-11-21).
#
# Uso, desde tu máquina (no copia nada al servidor; el guion viaja por la entrada estándar):
#   ssh root@161.97.85.216 'bash -s -- chequear'      < aportaya-promocion.sh   # 1) ANTES de promover
#   ssh root@161.97.85.216 'bash -s -- estado'        < aportaya-promocion.sh   # 2) DESPUÉS de promover
#   ssh root@161.97.85.216 'bash -s -- llaves-mfa'    < aportaya-promocion.sh   # 3) solo antes de promover #412
#
# Qué hace y qué NO hace:
#   - «chequear» y «estado» SOLO LEEN: no escriben en la base, no reinician nada, no despliegan.
#   - «llaves-mfa» crea UN archivo nuevo, /opt/aportaya/secretos/mfa-llaves-test.txt (permiso 600),
#     y no lo pisa si ya existe. No toca Coolify: el valor lo pegás vos en su panel.
#   - No imprime ninguna contraseña ni ninguna llave. La de la base la lee del .env del servidor,
#     igual que despliegue/ensayos/sembrar_test.sh.
set -uo pipefail

PG="${PG_CONTENEDOR:-aportaya-postgres}"
ENV_PG="${PG_ENV:-/opt/aportaya/postgres/.env}"
BASE="${PG_BASE:-pasanaku}"
LOG_AUTODESPLIEGUE="${LOG_AUTODESPLIEGUE:-/opt/aportaya/logs/autodespliegue.log}"
DIR_SECRETOS="${DIR_SECRETOS:-/opt/aportaya/secretos}"

ok()   { printf '  \033[32mOK\033[0m    · %s\n' "$*"; }
mal()  { printf '  \033[31mFALLA\033[0m · %s\n' "$*"; FALLAS=$((FALLAS+1)); }
nota() { printf '  ·       %s\n' "$*"; }
FALLAS=0

valor_env() {
  [ -r "$ENV_PG" ] || return 0
  grep -E "^[[:space:]]*(export[[:space:]]+)?$1=" "$ENV_PG" | head -1 | sed -E "s/^[^=]*=//; s/^[\"']//; s/[\"']\$//"
}

conectar_base() {
  docker inspect -f '{{.State.Running}}' "$PG" 2>/dev/null | grep -q true \
    || { echo "ERROR: el contenedor de Postgres «$PG» no está en marcha" >&2; exit 3; }
  PGU="$(valor_env POSTGRES_USER)"; [ -n "$PGU" ] || PGU="$(valor_env BD_USUARIO_ADMIN)"
  [ -n "$PGU" ] || PGU="$(docker exec "$PG" printenv POSTGRES_USER 2>/dev/null)"
  PGP="$(valor_env POSTGRES_PASSWORD)"; [ -n "$PGP" ] || PGP="$(valor_env BD_CLAVE_ADMIN)"
  [ -n "$PGU" ] || { echo "ERROR: no pude determinar el usuario administrador de la base" >&2; exit 3; }
  psql_ro() {  # solo lectura: la transacción se abre READ ONLY
    PGPASSWORD="$PGP" PGOPTIONS='-c default_transaction_read_only=on' \
      docker exec -i -e PGPASSWORD -e PGOPTIONS "$PG" psql -X -q -tA -F '|' -v ON_ERROR_STOP=1 -U "$PGU" -d "$BASE" "$@"
  }
  psql_ro -c 'SELECT 1' >/dev/null 2>&1 || { echo "ERROR: no pude conectarme a la base «$BASE»" >&2; exit 3; }
}

chequear() {
  echo "== 1 · Chequeo previo a promover dev → test (solo lectura) =="
  conectar_base
  ok "base «$BASE» alcanzable en «$PG», en modo solo lectura"

  # Las restricciones nuevas de #411 (importes nunca negativos) se agregan al desplegar.
  # Si ya hay filas que las violan, el trabajo de esquema falla y NO arranca ningún servicio.
  local sql
  sql=$(cat <<'SQL'
SELECT 'aportes.reembolso (monto <= 0)', count(*) FROM aportes.reembolso WHERE monto <= 0
UNION ALL SELECT 'aportes.obligacion_aporte (montos < 0)', count(*) FROM aportes.obligacion_aporte
  WHERE monto_pagado < 0 OR monto_recargo < 0 OR monto_condonado < 0 OR monto_cubierto_garantia < 0
UNION ALL SELECT 'aportes.pago (montos < 0)', count(*) FROM aportes.pago
  WHERE monto_comision_proveedor < 0 OR monto_neto_acreditado < 0
UNION ALL SELECT 'aportes.orden_cobro (monto_exacto < 0)', count(*) FROM aportes.orden_cobro WHERE monto_exacto < 0
UNION ALL SELECT 'aportes.disputa_pago (monto_disputado < 0)', count(*) FROM aportes.disputa_pago WHERE monto_disputado < 0;
SQL
)
  local salida; salida="$(psql_ro -c "$sql" 2>&1)" || { mal "no pude correr el chequeo de importes: $salida"; resumen; return; }
  while IFS='|' read -r tabla n; do
    [ -z "$tabla" ] && continue
    if [ "$n" = "0" ]; then ok "$tabla: 0 filas"; else mal "$tabla: $n filas que violan la restricción nueva"; fi
  done <<< "$salida"

  # Informativo: asientos del mayor que ya estaban mal antes del arreglo (#411 no los toca,
  # solo impide que entren nuevos). Si hay, se corrigen con contra-asiento, nunca con UPDATE.
  local anomalos
  anomalos="$(psql_ro -c "SELECT count(*) FROM nucleo_financiero.movimiento_contable WHERE debe < 0 OR haber < 0 OR (debe > 0 AND haber > 0) OR (debe = 0 AND haber = 0);" 2>/dev/null || echo '?')"
  nota "líneas del mayor ya existentes con signo o lado inválido (informativo): ${anomalos}"

  # #419 retira la subasta: si en TEST quedan subastas, pujas o cesiones vivas, sus retenciones se
  # liberan con la operación de nucleo-financiero (nunca con UPDATE) antes de promover.
  local t vivas
  for t in subasta_cupo puja_subasta cesion_derecho; do
    if [ "$(psql_ro -c "SELECT to_regclass('entregas.$t') IS NOT NULL;" 2>/dev/null)" = "t" ]; then
      vivas="$(psql_ro -c "SELECT string_agg(estado::text||'='||n, ', ') FROM (SELECT estado, count(*) n FROM entregas.$t GROUP BY 1) x;" 2>/dev/null || echo '?')"
    else vivas="sin tabla"; fi
    nota "entregas.$t por estado (informativo, #419): ${vivas:-0 filas}"
  done

  # #424 retira el desembolso en efectivo al organizador: si hay entregas vivas con ese método, se
  # resuelven antes (informativo).
  local efectivo
  efectivo="$(psql_ro -c "SELECT count(*) FROM entregas.entrega_fondo WHERE metodo_desembolso::text = 'EFECTIVO_ORGANIZADOR';" 2>/dev/null || echo '?')"
  nota "entregas.entrega_fondo con EFECTIVO_ORGANIZADOR (informativo, #424): ${efectivo}"

  # #477 saca SOLICITADA del CHECK de garantia.cobertura_incumplimiento: tiene que dar 0 antes de desplegarlo.
  local solicitadas
  solicitadas="$(psql_ro -c "SELECT count(*) FROM garantia.cobertura_incumplimiento WHERE estado::text = 'SOLICITADA';" 2>/dev/null || echo '?')"
  if [ "$solicitadas" = "0" ]; then ok "garantia.cobertura_incumplimiento en SOLICITADA: 0 (#477 se puede desplegar)"
  else mal "garantia.cobertura_incumplimiento en SOLICITADA: $solicitadas (resolverlas antes de #477; nunca con UPDATE)"; fi
  # #463 estrecha el CHECK de entregas.entrega_fondo.metodo_desembolso: EFECTIVO_ORGANIZADOR tiene que dar 0.
  if [ "${efectivo:-?}" != "0" ]; then mal "entrega_fondo con EFECTIVO_ORGANIZADOR: ${efectivo} (tiene que ser 0 antes de promover #463)"; fi

  # #479 retira el respaldo empresarial en garantia: reservas, coberturas y planes vivos tienen que dar 0 (si no, cada
  # fila necesita su contra-asiento en el mayor ANTES de promover; nunca UPDATE).
  local resp
  resp="$(psql_ro -c "SELECT (SELECT count(*) FROM garantia.reserva_respaldo WHERE estado::text = 'VIGENTE')
    + (SELECT count(*) FROM garantia.cobertura_respaldo WHERE estado::text IN ('APLICADA','RECUPERADA_PARCIAL'))
    + (SELECT count(*) FROM garantia.plan_contingencia WHERE estado::text IN ('PROPUESTO','APROBADO'));" 2>/dev/null || echo '?')"
  if [ "$resp" = "0" ]; then ok "respaldo de garantia sin reservas, coberturas ni planes vivos (#479 se puede promover)"
  else mal "respaldo de garantia con $resp filas vivas: contra-asiento antes de promover #479"; fi

  # Contexto para #412: TEST entra hoy al backoffice con el segundo factor 000000.
  local idc perfil
  idc="$(docker ps --format '{{.Names}}' | grep -E '^identidad-' | head -1 || true)"
  if [ -n "$idc" ]; then
    perfil="$(docker exec "$idc" printenv SPRING_PROFILES_ACTIVE 2>/dev/null || true)"
    nota "identidad corre como «$idc», perfil «${perfil:-?}» (hoy acepta 000000; #412 lo elimina)"
    # #439: identidad falla cerrado sin JWT_CLAVE_FIRMA. Solo se mira si está cargada; no se imprime.
    if [ -n "$(docker exec "$idc" printenv JWT_CLAVE_FIRMA 2>/dev/null)" ]; then ok "identidad tiene JWT_CLAVE_FIRMA cargada (no se imprime)"
    else mal "identidad corre SIN JWT_CLAVE_FIRMA: con #439 no arranca. Cargala en Coolify antes de promover"; fi
    for v in MFA_LLAVES MFA_LLAVE_VIGENTE; do
      if [ -n "$(docker exec "$idc" printenv $v 2>/dev/null)" ]; then ok "identidad tiene $v cargada (no se imprime)"
      else nota "el contenedor actual no tiene $v (normal hasta redesplegar; confirmá que está en Coolify antes de promover #412)"; fi
    done
  fi
  # Cuentas de servicio (grupos, entregas, inversiones; garantia desde M07 de L1): sin TELEFONO + CLAVE + un segundo
  # factor, el servicio falla cerrado al hablar con aportes/nucleo. Solo se mira si están cargadas; no se imprime nada.
  local svc pref c t k f
  for svc in grupos entregas inversiones garantia; do
    pref="$(echo "$svc" | tr '[:lower:]' '[:upper:]')"
    c="$(docker ps --format '{{.Names}}' | grep -E "^${svc}-" | head -1 || true)"
    [ -z "$c" ] && { nota "$svc: no encuentro su contenedor"; continue; }
    t="$(docker exec "$c" printenv ${pref}_SERVICIO_TELEFONO 2>/dev/null)"; k="$(docker exec "$c" printenv ${pref}_SERVICIO_CLAVE 2>/dev/null)"
    f="$(docker exec "$c" printenv ${pref}_SERVICIO_TOTP_SEMILLA 2>/dev/null)$(docker exec "$c" printenv ${pref}_SERVICIO_FACTOR_FIJO 2>/dev/null)"
    if [ -n "$t" ] && [ -n "$k" ] && [ -n "$f" ]; then ok "$svc tiene su cuenta de servicio cargada (no se imprime)"
    elif [ "$svc" = garantia ]; then nota "garantia sin cuenta de servicio todavía (la exige M07 de L1 cuando se promueva)"
    else mal "$svc sin cuenta de servicio completa: sorteo/entrega/inversiones fallan cerrados"; fi
  done
  resumen
}

estado() {
  echo "== 2 · Estado después de promover (solo lectura) =="
  if [ -r "$LOG_AUTODESPLIEGUE" ]; then
    nota "últimas líneas de $LOG_AUTODESPLIEGUE:"
    tail -n 15 "$LOG_AUTODESPLIEGUE" | sed 's/^/          /'
  else
    mal "no encuentro $LOG_AUTODESPLIEGUE"
  fi
  local esquema
  esquema="$(docker ps -a --format '{{.Names}}|{{.Status}}' | grep -E '^esquema-' | head -1 || true)"
  if [ -z "$esquema" ]; then nota "no veo el contenedor del trabajo de esquema (puede no haber corrido todavía)"
  elif echo "$esquema" | grep -qE 'Exited \(0\)'; then ok "trabajo de esquema terminó bien: $esquema"
  else mal "trabajo de esquema: $esquema  → mirá: docker logs ${esquema%%|*}"; fi

  local total=0 sanos=0 linea nombre est
  while IFS='|' read -r nombre est; do
    case "$nombre" in esquema-*) continue;; esac
    total=$((total+1))
    if echo "$est" | grep -q '(healthy)'; then sanos=$((sanos+1))
    elif echo "$est" | grep -qE '^Up' && ! echo "$est" | grep -q 'health'; then sanos=$((sanos+1))
    else mal "$nombre → $est"; fi
  done < <(docker ps -a --format '{{.Names}}|{{.Status}}' | grep -E '^(gateway|aportes|auditoria|cumplimiento|entregas|erp|garantia|grupos|identidad|inversiones|notificaciones|nucleo-financiero|organizador|publicidad|tarifas|transparencia)-')
  if [ "$total" -gt 0 ] && [ "$sanos" -eq "$total" ]; then ok "$sanos de $total contenedores de servicios sanos"
  else nota "$sanos de $total contenedores de servicios sanos"; fi

  local codigo
  codigo="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 https://api.aportaya.161.97.85.216.sslip.io/actuator/health || echo 000)"
  if [ "$codigo" = "200" ]; then ok "gateway responde 200 en /actuator/health"; else mal "gateway respondió $codigo en /actuator/health"; fi
  resumen
}

llaves_mfa() {
  echo "== 3 · Llaves que TEST necesita antes de promover (MFA de #412, cifrado de cuentas de #463, webhook de aportes de #458) =="
  local archivo="$DIR_SECRETOS/mfa-llaves-test.txt"
  if [ -e "$archivo" ]; then
    ok "ya existe $archivo; no se pisa (borrarla invalida los factores ya enrolados)"
  else
    umask 077; mkdir -p "$DIR_SECRETOS"
    printf '1:%s\n' "$(openssl rand -base64 48 | tr -d '\n')" > "$archivo"
    chmod 600 "$archivo"
    ok "creada $archivo (permiso 600). No se imprime."
  fi
  nota "En Coolify → aplicación aportaya-api → Environment Variables, agregá:"
  nota "   MFA_LLAVES        = el contenido de ese archivo   (para verlo:  cat $archivo)"
  nota "   MFA_LLAVE_VIGENTE = 1"
  nota "Hacelo ANTES de que #412 llegue a test: sin esas dos variables, identidad no arranca."
  # #463: entregas cifra los números de cuenta con su propia llave (distinta de la pimienta).
  local llave_cuentas="$DIR_SECRETOS/entregas-llave-cifrado-cuentas-test.txt"
  if [ -e "$llave_cuentas" ]; then
    ok "ya existe $llave_cuentas; no se pisa (borrarla deja ilegibles las cuentas cifradas)"
  else
    umask 077; openssl rand -base64 48 | tr -d '\n' > "$llave_cuentas"; chmod 600 "$llave_cuentas"
    ok "creada $llave_cuentas (permiso 600). No se imprime."
  fi
  nota "En la misma pantalla de Coolify agregá también:"
  nota "   ENTREGAS_LLAVE_CIFRADO_CUENTAS = el contenido de $llave_cuentas"
  nota "Sin ella, entregas no arranca desde #463."
  # #458: aportes verifica la firma de los avisos de la pasarela con su propio secreto (>= 32 caracteres), distinto
  # de WEBHOOK_SECRETO (que es de notificaciones). Hoy no tiene que coincidir con nada: no hay pasarela contratada.
  local secreto_webhook="$DIR_SECRETOS/aportes-webhook-secreto-test.txt"
  if [ -e "$secreto_webhook" ]; then
    ok "ya existe $secreto_webhook; no se pisa"
  else
    umask 077; openssl rand -hex 32 | tr -d '\n' > "$secreto_webhook"; chmod 600 "$secreto_webhook"
    ok "creada $secreto_webhook (permiso 600). No se imprime."
  fi
  nota "   APORTES_WEBHOOK_SECRETO = el contenido de $secreto_webhook   (sin ella, aportes no arranca desde #458)"
  resumen
}

resumen() {
  echo
  if [ "$FALLAS" -eq 0 ]; then printf '\033[32mRESULTADO: PASA\033[0m — mandale esta salida a Claude.\n'
  else printf '\033[31mRESULTADO: NO PASA (%d)\033[0m — no promuevas; mandale esta salida a Claude.\n' "$FALLAS"; fi
}

case "${1:-chequear}" in
  chequear)   chequear ;;
  estado)     estado ;;
  llaves-mfa) llaves_mfa ;;
  *) echo "uso: bash -s -- {chequear|estado|llaves-mfa}" >&2; exit 2 ;;
esac
exit $(( FALLAS > 0 ? 1 : 0 ))
```
