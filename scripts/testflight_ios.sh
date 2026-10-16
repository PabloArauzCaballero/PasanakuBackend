#!/usr/bin/env bash
# Sube la app movil a TestFlight desde un Mac. UN comando:
#
#   bash scripts/testflight_ios.sh            # compila, firma y sube
#   bash scripts/testflight_ios.sh --solo-compilar   # deja el .xcarchive y no sube
#
# Receta ya probada el 2026-09-28 (docs/trabajo/2026-09-28-testflight-ios/REPORTE.md):
# `flutter build ipa --no-codesign` + `xcodebuild -exportArchive` con
# `-allowProvisioningUpdates` (Xcode firma con el certificado administrado del equipo).
# Este script no guarda ni imprime ningun secreto: la firma sale de tu sesion de Xcode.
#
# Requisitos en el Mac (el script los verifica y para si falta alguno):
#   - Xcode con la cuenta del equipo 7A42TVN3Y6 iniciada (Xcode > Settings > Accounts)
#   - Flutter 3.44.8 (el de ci.yml y Dockerfile.movil-web; con otro, pubspec.lock no coincide)
#   - Node 22 + corepack (yarn) y CocoaPods
#   - Java 21 (brew install openjdk@21): clientes/dart/*/lib/src NO esta versionado; si falta, el paso 1 lo genera
#
# Variables opcionales:
#   API              por defecto TEST: https://api.aportaya.161.97.85.216.sslip.io/api/v1
#   HOSTS_PERMITIDOS por defecto el host de TEST (sin esto la app rechaza todo host que no sea loopback)
#   BUILD_NUMBER     por defecto fecha-hora (AAAAMMDDHHMM): TestFlight exige uno mayor en cada subida
set -euo pipefail

SOLO_COMPILAR=0
[ "${1:-}" = "--solo-compilar" ] && SOLO_COMPILAR=1

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
TEAM_ID="7A42TVN3Y6"
BUNDLE="bo.aportaya.aportayaMovil"
API="${API:-https://api.aportaya.161.97.85.216.sslip.io/api/v1}"
HOSTS_PERMITIDOS="${HOSTS_PERMITIDOS:-api.aportaya.161.97.85.216.sslip.io}"
BUILD_NUMBER="${BUILD_NUMBER:-$(date +%Y%m%d%H%M)}"
FLUTTER_ESPERADO="3.44.8"

paso() { printf '\n== %s\n' "$*"; }
falta() { echo "ERROR: $*" >&2; exit 2; }

paso "0) Verificando el entorno"
[ "$(uname)" = "Darwin" ] || falta "esto corre solo en macOS (estas en $(uname))"
command -v xcodebuild >/dev/null || falta "no hay Xcode (xcodebuild)"
command -v flutter >/dev/null    || falta "no hay flutter en el PATH (version $FLUTTER_ESPERADO)"
command -v pod >/dev/null        || falta "no hay CocoaPods (brew install cocoapods)"
command -v corepack >/dev/null   || falta "no hay Node 22 con corepack (brew install node@22)"
VER="$(flutter --version | head -1 | awk '{print $2}')"
[ "$VER" = "$FLUTTER_ESPERADO" ] || falta "Flutter $VER: hace falta $FLUTTER_ESPERADO (con otro, 'flutter pub get --enforce-lockfile' falla). Con fvm: fvm use $FLUTTER_ESPERADO"
echo "xcode: $(xcodebuild -version | head -1) | flutter: $VER | build: $BUILD_NUMBER"
echo "API: $API"
echo "commit: $(git rev-parse --short HEAD) rama: $(git branch --show-current)"
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  echo "AVISO: hay cambios sin commitear; el IPA los llevara (el commit de arriba no los refleja)." >&2
fi

paso "1) Dependencias, tokens de diseno y clientes Dart (mismo orden que ci.yml)"
corepack enable
yarn install --immutable
if ls "$RAIZ"/clientes/dart/*/lib/src >/dev/null 2>&1; then
  echo "clientes Dart: ya generados"
else
  JDK21="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  [ -n "$JDK21" ] || JDK21="$(brew --prefix openjdk@21 2>/dev/null)/libexec/openjdk.jdk/Contents/Home"
  [ -x "$JDK21/bin/java" ] || falta "falta Java 21 para generar los clientes Dart (brew install openjdk@21)"
  (cd "$RAIZ" && JAVA_HOME="$JDK21" ./gradlew generateOpenApiClients --no-parallel --no-build-cache)
fi
yarn workspace @aportaya/tokens build
yarn workspace @aportaya/diseno-flutter build
yarn workspace @aportaya/movil build

paso "2) Compilando el archivo de iOS (sin firmar: la firma va en el paso 3)"
cd "$RAIZ/apps/movil"
flutter build ipa --release --no-codesign \
  --build-number="$BUILD_NUMBER" \
  --dart-define="API=$API" \
  --dart-define="HOSTS_PERMITIDOS=$HOSTS_PERMITIDOS"
ARCHIVO="$RAIZ/apps/movil/build/ios/archive/Runner.xcarchive"
[ -d "$ARCHIVO" ] || falta "no se genero $ARCHIVO"
echo "archivo: $ARCHIVO"

if [ "$SOLO_COMPILAR" = 1 ]; then
  echo "Listo (--solo-compilar): no se subio nada."
  exit 0
fi

paso "3) Firmando y subiendo a App Store Connect (TestFlight)"
OPCIONES="$(mktemp -t aportaya-export).plist"
trap 'rm -f "$OPCIONES"' EXIT
cat > "$OPCIONES" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key><string>app-store-connect</string>
  <key>destination</key><string>upload</string>
  <key>teamID</key><string>$TEAM_ID</string>
  <key>signingStyle</key><string>automatic</string>
  <key>uploadSymbols</key><true/>
</dict>
</plist>
PLIST
SALIDA="$RAIZ/apps/movil/build/ios/ipa-upload"
rm -rf "$SALIDA"
xcodebuild -exportArchive \
  -archivePath "$ARCHIVO" \
  -exportOptionsPlist "$OPCIONES" \
  -exportPath "$SALIDA" \
  -allowProvisioningUpdates

paso "Listo"
echo "Subido: $BUNDLE build $BUILD_NUMBER (equipo $TEAM_ID)."
echo "En App Store Connect > TestFlight tarda unos minutos en 'Procesando'; despues aparece para los testers."
echo "ATENCION: esta compilacion apunta a TEST ($API). La app solo permite el host: $HOSTS_PERMITIDOS"
