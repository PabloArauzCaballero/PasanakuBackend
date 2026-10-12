#!/usr/bin/env bash
# Carril Q: compila SOLO lo que toque (javac suelto, JVM chica, sin Gradle ni Docker) contra las clases ya construidas, y corre las
# pruebas SIN base de datos. Es peldaño RUNS/TESTED parcial: NO reemplaza test/integrationTest.
REPO="C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-cierre"
I="C:/Users/Usuario/AppData/Local/Temp/claude/C--Users-Usuario-Downloads-Entrypoint-GitHUb-Pasanaku/c468f89e-ca7f-4172-b7d5-74080b60e5a8/scratchpad/i"
Q="C:/Users/Usuario/AppData/Local/Temp/claude/C--Users-Usuario-Downloads-Entrypoint-GitHUb-Pasanaku/c468f89e-ca7f-4172-b7d5-74080b60e5a8/scratchpad/q"
JAVAC="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin/javac.exe"
JAVA="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin/java.exe"
cd "$REPO" || exit 2
JARS="$(cat $I/jars.txt)"
PLAT="$(cat $I/cp_plat.txt)"

compilar() { # compilar <nombre> <cp-extra> <archivos...>
  local nombre="$1" extra="$2"; shift 2
  local OUT="$Q/out/$nombre"; rm -rf "$OUT"; mkdir -p "$OUT"
  { printf -- "-proc:none\n-encoding\nUTF-8\n-d\n%s\n-cp\n\"%s;%s;%s;%s\"\n" "$OUT" "$OUT" "$extra" "$PLAT" "$JARS"
    for f in "$@"; do echo "\"$f\""; done; } > "$Q/javac_$nombre.args"
  "$JAVAC" -J-Xmx500m @"$Q/javac_$nombre.args" 2>&1 | grep -v "^Note:" | head -60
  echo "== $nombre: RC ${PIPESTATUS[0]}"
}

N=servicios/nucleo-financiero
NM=$N/src/main/java/bo/aportaya/nucleofinanciero
NT=$N/src/test/java/bo/aportaya/nucleofinanciero
compilar nucleo-main "$N/build/classes/java/main;$N/build/generated/jooq;$N/build/generated/openapi" \
  $NM/aplicacion/AperturaDeBilleteras.java $NM/aplicacion/ConsumoDeAportes.java $NM/aplicacion/PagoDeCompraEnMercado.java \
  $NM/aplicacion/CU13EjecutarConDestino.java $NM/consumo/SobreDeEvento.java $NM/consumo/ConsumidorDeUsuariosKafka.java \
  $NM/consumo/ConsumidorDeGruposKafka.java $NM/consumo/ConsumidorDeAportesKafka.java \
  $NM/infraestructura/CuentaBilleteraRepositorio.java $NM/infraestructura/clientes/HechosPorHttp.java \
  $NM/infraestructura/clientes/OfertasPorHttp.java $NM/dominio/puertos/OfertasDelMercado.java

compilar nucleo-test "$Q/out/nucleo-main;$N/build/classes/java/main;$N/build/classes/java/test" \
  $NT/BaseDeApertura.java $NT/CUAperturaDeBilleterasTest.java $NT/CUAperturaYAporteTest.java $NT/CUAporteNoAplicadoTest.java \
  $NT/CU16CierreConDeudaTest.java $NT/BaseDeInversiones.java $NT/CU13MercadoTest.java \
  $NT/CUConsumoDeAportesRechazosTest.java $NT/CUConsumoDeAportesConcurrenciaTest.java \
  $NT/infraestructura/ContratosConOtrosServiciosTest.java $NT/infraestructura/HechosPorHttpPorUsuarioTest.java \
  $NT/infraestructura/PactoConEntregasContratoTest.java $NT/consumo/ConsumidorDeAperturasKafkaTest.java

A=servicios/aportes
AM=$A/src/main/java/bo/aportaya/aportes
AT=$A/src/test/java/bo/aportaya/aportes
compilar aportes-main "$A/build/classes/java/main;$A/build/generated/jooq;$A/build/generated/openapi" \
  $AM/aplicacion/CU21CompensarAporte.java $AM/consumo/ConsumidorDeAportesNoAplicadosKafka.java \
  $AM/infraestructura/PagoRepositorio.java
compilar aportes-test "$Q/out/aportes-main;$A/build/classes/java/main;$A/build/classes/java/test" \
  $AT/CU21CompensaTest.java $AT/consumo/ConsumidorDeAportesNoAplicadosKafkaTest.java

correr() { # correr <extra-cp> <clases...>
  local extra="$1"; shift
  { printf -- "-Xmx400m\n-XX:+UseSerialGC\n-cp\n\"%s;%s;%s;%s;%s\"\nCorrer\n" "$I/runner/out" "$extra" "$PLAT" "$(cat $I/jars_run.txt)" ""; for c in "$@"; do echo "$c"; done; } > "$Q/java.args"
  "$JAVA" @"$Q/java.args" 2>&1
}
echo "######## nucleo sin BD"
correr "$Q/out/nucleo-test;$Q/out/nucleo-main;$N/build/classes/java/test;$N/build/classes/java/main;$N/build/generated/jooq;$N/build/generated/openapi" \
  bo.aportaya.nucleofinanciero.consumo.ConsumidorDeAperturasKafkaTest \
  bo.aportaya.nucleofinanciero.infraestructura.HechosPorHttpPorUsuarioTest \
  bo.aportaya.nucleofinanciero.infraestructura.ContratosConOtrosServiciosTest \
  bo.aportaya.nucleofinanciero.infraestructura.PactoConEntregasContratoTest
echo "######## aportes sin BD"
correr "$Q/out/aportes-test;$Q/out/aportes-main;$A/build/classes/java/test;$A/build/classes/java/main;$A/build/generated/jooq" \
  bo.aportaya.aportes.consumo.ConsumidorDeAportesNoAplicadosKafkaTest
