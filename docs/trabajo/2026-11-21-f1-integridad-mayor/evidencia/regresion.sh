#!/bin/bash
# Uso: regresion.sh <base> ; aplica esquema+semillas+dev+humo+verificaciones sobre una base desechable
set -u
B=$1; R=/home/user/PasanakuBackend; export PGPASSWORD=pasanaku
P="psql -h 127.0.0.1 -p 5433 -U pasanaku -X"
$P -d postgres -q -c "DROP DATABASE IF EXISTS $B" -c "CREATE DATABASE $B OWNER pasanaku" 
$P -d $B -q -c "ALTER DATABASE $B SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, inversiones, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public"
cd $R/sql
$P -d $B -q -c "ALTER DATABASE $B SET app.entorno = 'dev'"
for f in aplicar.sql 60_semillas/sembrar.sql 61_dev/sembrar_dev.sql; do
  $P -d $B -v ON_ERROR_STOP=1 -q -f $f > /tmp/reg_$B.log 2>&1; echo "$f exit=$?"; grep -E "ERROR" /tmp/reg_$B.log | head -3
done
$P -d $B -q -f 50_verificacion/prueba_humo.sql > /tmp/humo_$B.txt 2>&1
echo "humo: OK=$(grep -c '^ *OK ' /tmp/humo_$B.txt) FALLA=$(grep -c 'FALLA' /tmp/humo_$B.txt) ERROR=$(grep -c 'ERROR' /tmp/humo_$B.txt)"
$P -d $B -A -t -f 50_verificacion/verificaciones.sql > /tmp/verif_$B.txt 2>&1
echo "verificaciones: lineas_no_vacias=$(grep -cv '^\s*$\|^SET$' /tmp/verif_$B.txt)"
