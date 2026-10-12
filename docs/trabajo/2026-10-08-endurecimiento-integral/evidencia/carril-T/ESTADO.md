# Carril T — ESTADO (corte por traspaso, 2026-10-11). Peldaño: WRITTEN (nada compilado ni testeado salvo pub get)

## Verificado (único)
- `flutter pub get` y `flutter pub get --enforce-lockfile` con Flutter 3.44.8 (C:\fl3448): RC 0 ambos (`01-pub-get.log`).
  pubspec.lock cambió SOLO aditivo: `flutter_localizations` (sdk) + `intl 0.20.2` (transitiva).
- `verificar_frontend.py movil` (previo a mis cambios): única falla = `pantalla_unirse.dart` 214 líneas
  (NO `estado_alta.dart`, que tiene 120). Tras el corte quedó en 187 (sin re-correr el gate).
- NO corrió: dart format, flutter analyze, flutter test, build APK. El guardián de RAM nunca dio paso (libres 0,5–1,2 GB; piso 3500; 2 esperas de 3000 s).

## Escrito SIN verificar (todo en apps/movil + packages/diseno_flutter)
1. D4: `lib/dominio/cu13_cuenta_propia.dart` (GET /billetera/cuentas/propia?moneda=BOB; AP-CU13-01 -> null), `lib/pantallas/billetera/resolver_cuenta.dart` (ResolverCuenta + PantallaConCuenta),
   `pantalla_de_saldo.dart` (cuentaId opcional), `rutas.dart` (sin id fijo), `textos.dart`, `errores.dart` (AP-CU13-01 = «Tu billetera todavía no está activa.»).
   El cliente Dart generado NO trae consultarCuentaPropia (clientes/dart/nucleo-financiero está viejo): se parsea a mano `CuentaPropia`; reemplazar al regenerar.
2. Textos: «Banco Unión» sacado (billetera/textos.dart, identidad/textos.dart x2): solo aparece como dato de ejemplo en docs/Views/AportaYa-Maqueta.html, ninguna fuente documenta el banco. Frase neutral «cuenta de custodia».
   Pantalla MFA (`pantalla_mfa.dart`): explicación + botón «Confirmar»; `CampoOTP.onCambio` nuevo (diseno_flutter); ruta mfa movida a rutasDeEntrada (sin barra de pestañas).
   Tutorial: `dominio/tutoriales/motor.dart` ya no muestra el id «billetera.saldo» ni rutas (textoAnclaAusente / textoNoSePudoAbrir).
3. Split: `pantalla_unirse.dart` -> `pantallas/pasanaku/dominio/cu69_unirse_por_enlace.dart` (EnlaceDeInvitacion, textoDelRechazoDeInvitacion). Sin cambio de comportamiento buscado.
4. Idioma: `lib/localizacion.dart` + `app.dart` (supportedLocales es-BO/es, GlobalMaterialLocalizations) + pubspec.yaml (flutter_localizations).
5. Compartir: NO se agrega share_plus (dependencia nueva, regla 90.4). Decisión en comentario de `invitacion_creada.dart`. ABIERTO: aprobar share_plus o dejar solo «Copiar».
- Tests nuevos/ajustados (nunca corridos): test/widget/inicio_de_billetera_test.dart, test/widget/idioma_test.dart, test/identidad/pantalla_mfa_test.dart; editados pantalla_de_saldo_test, motor_test, errores_grupos_test, pestanas_test.

## Falta
- Correr `verificar-dirigido.sh` (en esta carpeta; ajusta FP/ruta si hace falta), arreglar lo que falle (posibles: firmas de CampoDeFecha en idioma_test, find.byType(Dialog), formato de dart).
- flutter analyze limpio; flutter test completo; listar los 8 goldens que fallan por rasterizado (test/goldens: pantalla_de_saldo x2, escanear x2, transiciones...; ver carril-L/06-flutter-test-2-final.log) como ENVIRONMENT, NO regenerar.
- Goldens de saldo pueden cambiar por el texto de custodia (sin "Banco Unión"): también ENVIRONMENT/macOS.
- Re-correr `python scripts/verificar_frontend.py movil`.
- APK `aportaya-test-6-TEST.apk` + LEEME (defines API=https://api.aportaya.161.97.85.216.sslip.io/api/v1, HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io), JDK Adoptium 21; solo al final, por heavy.sh.
- Prueba visual real (emulador) no hecha; AP-CU13-01 real contra TEST no re-ejercitado.

## Comandos
`cd PasanakuBackend-cierre && MAX_WAIT_S=3000 bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash <ruta>/verificar-dirigido.sh`
(PATH: /c/fl3448/bin; CI=true FLUTTER_SUPPRESS_ANALYTICS=true; flutter ... --no-pub).
## Supuestos
- moneda de la billetera = BOB; el estado de CuentaDeLaSesion no se interpreta (el servidor devuelve "ACTIVA").
- Otros cambios sin commitear en el worktree (backoffice, servicios, docs/CasosDeUso) NO son míos y no se incluyen.
