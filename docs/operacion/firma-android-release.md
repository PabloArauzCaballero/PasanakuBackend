# Firma del release de Android (CONFIG-001)

El build de release de `apps/movil` **ya no se firma con la clave de depuración**. Lee la
keystore de producción de cuatro variables y, si falta alguna, **falla** antes de compilar
(`verificarFirmaDeRelease` en `apps/movil/android/app/build.gradle.kts`). El build `debug` no cambió.

> Esto lo hace una persona: la keystore y los secretos **no los crea ni los escribe Claude**.
> Quien los crea (Pablo) guarda la keystore y sus contraseñas en un gestor de secretos, fuera del repositorio.

## Variables que lee Gradle

Se toman del entorno o de una propiedad de Gradle con el mismo nombre (`-PNOMBRE=valor`).

| Variable | Qué es |
|---|---|
| `APORTAYA_KEYSTORE_FILE` | Ruta al archivo `.jks`/`.keystore` |
| `APORTAYA_KEYSTORE_PASSWORD` | Contraseña de la keystore |
| `APORTAYA_KEY_ALIAS` | Alias de la clave |
| `APORTAYA_KEY_PASSWORD` | Contraseña de la clave |

## 1. Crear la keystore (una sola vez, en la máquina de Pablo)

```bash
keytool -genkeypair -v \
  -keystore aportaya-release.jks -storetype PKCS12 \
  -alias aportaya-release -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` pregunta las contraseñas y los datos del certificado. Después:

1. Hacer **dos copias** de `aportaya-release.jks` y de las contraseñas en lugares distintos del gestor
   de secretos. Perder esta clave impide publicar actualizaciones de la app.
2. Anotar la huella para comparar luego:
   `keytool -list -v -keystore aportaya-release.jks -alias aportaya-release | grep SHA256`.
3. **No** commitearla (el `.gitignore` de `android/` ya ignora `*.jks` y `*.keystore`; verificarlo con `git status`).

## 2. Secretos de GitHub Actions

En el repositorio: *Settings → Secrets and variables → Actions → New repository secret*.

| Secreto | Valor |
|---|---|
| `ANDROID_RELEASE_KEYSTORE_BASE64` | `base64 -w0 aportaya-release.jks` |
| `ANDROID_RELEASE_KEYSTORE_PASSWORD` | contraseña de la keystore |
| `ANDROID_RELEASE_KEY_ALIAS` | `aportaya-release` (el alias elegido arriba) |
| `ANDROID_RELEASE_KEY_PASSWORD` | contraseña de la clave |

## 3. Cómo lo usa un workflow de release de producción

Hoy el único workflow que compila release es `apk-de-prueba.yml` (APK de **prueba** contra TEST); ese
usa la keystore estable de depuración (`APK_DEBUG_KEYSTORE`) pasada por las mismas variables, no la de
producción. Un workflow de producción debe hacer:

```yaml
- name: keystore de produccion
  env:
    KS_B64: ${{ secrets.ANDROID_RELEASE_KEYSTORE_BASE64 }}
  run: |
    test -n "$KS_B64" || { echo "::error::Falta ANDROID_RELEASE_KEYSTORE_BASE64"; exit 1; }
    echo "$KS_B64" | base64 -d > "$RUNNER_TEMP/aportaya-release.jks"

- name: flutter build apk --release
  working-directory: apps/movil
  env:
    APORTAYA_KEYSTORE_FILE: ${{ runner.temp }}/aportaya-release.jks
    APORTAYA_KEYSTORE_PASSWORD: ${{ secrets.ANDROID_RELEASE_KEYSTORE_PASSWORD }}
    APORTAYA_KEY_ALIAS: ${{ secrets.ANDROID_RELEASE_KEY_ALIAS }}
    APORTAYA_KEY_PASSWORD: ${{ secrets.ANDROID_RELEASE_KEY_PASSWORD }}
  run: flutter build apk --release --dart-define=API=... --dart-define=HOSTS_PERMITIDOS=...

- name: verificar firma
  run: |
    apksigner=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)/apksigner
    "$apksigner" verify --print-certs apps/movil/build/app/outputs/flutter-apk/app-release.apk
    # la huella impresa debe ser la anotada en el paso 1
- name: borrar keystore
  if: always()
  run: rm -f "$RUNNER_TEMP/aportaya-release.jks"
```

## 4. Comprobación local

```bash
cd apps/movil
# Sin variables: debe FALLAR con «Build de release SIN firma: faltan APORTAYA_...»
flutter build apk --release
# Con variables: firma con la keystore de produccion
APORTAYA_KEYSTORE_FILE=/ruta/aportaya-release.jks APORTAYA_KEYSTORE_PASSWORD=... \
APORTAYA_KEY_ALIAS=aportaya-release APORTAYA_KEY_PASSWORD=... flutter build apk --release
```

## Fuera de este cambio

`hostDeLaAppWeb` sigue con su valor por omisión (TEST); exigirlo explícito en release es la otra mitad de
`F4.L6.M22` y no se hizo acá.
