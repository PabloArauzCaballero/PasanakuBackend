#!/usr/bin/env bash
FP="/c/fl3448"; export PATH="$FP/bin:$FP/bin/cache/dart-sdk/bin:$PATH"
export FLUTTER_SUPPRESS_ANALYTICS=true CI=true JAVA_TOOL_OPTIONS=
cd /c/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-cierre/apps/movil
echo "== dart format (lo tocado)"
dart format lib/localizacion.dart lib/app.dart lib/dominio/cu13_cuenta_propia.dart lib/dominio/errores.dart lib/dominio/tutoriales/motor.dart lib/pantallas/billetera lib/pantallas/identidad/pantalla_mfa.dart lib/pantallas/identidad/rutas.dart lib/pantallas/identidad/textos.dart lib/pantallas/pasanaku/pantalla_unirse.dart lib/pantallas/pasanaku/dominio/cu69_unirse_por_enlace.dart ../../packages/diseno_flutter/lib/atomos/campo_o_t_p.dart test/widget/inicio_de_billetera_test.dart test/widget/idioma_test.dart test/identidad/pantalla_mfa_test.dart test/widget/pantalla_de_saldo_test.dart test/widget/tutoriales/motor_test.dart test/unidad/errores_grupos_test.dart test/widget/pestanas_test.dart
echo "== analyze"; flutter analyze --no-pub 2>&1 | tail -30; echo "RC_ANALYZE=${PIPESTATUS[0]}"
echo "== flutter test dirigido"
flutter test --no-pub -r expanded test/widget/inicio_de_billetera_test.dart test/widget/idioma_test.dart test/identidad/pantalla_mfa_test.dart test/widget/pantalla_de_saldo_test.dart test/a11y/pantalla_de_saldo_a11y_test.dart test/widget/tutoriales/motor_test.dart test/unidad/errores_grupos_test.dart test/widget/pestanas_test.dart test/pasanaku test/widget/pantallas_de_h_tema_y_desborde_test.dart test/unidad/errores_identidad_test.dart 2>&1 | tail -80; echo "RC_TEST=${PIPESTATUS[0]}"
