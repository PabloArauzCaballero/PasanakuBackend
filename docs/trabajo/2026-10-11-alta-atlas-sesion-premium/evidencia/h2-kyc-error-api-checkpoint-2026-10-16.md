# H2 — diagnóstico y corrección local del error al completar KYC

Fecha: 2026-10-16 · Rama: `codex/alta-atlas-sesion-premium-2026-10-11`

## Resultado observable

Al guardar la fecha del documento o subir una foto desde «Completar verificación KYC», la app debe enviar el formato OpenAPI correcto y, si la API rechaza la operación, mostrar el mensaje de dominio/red y la referencia de soporte cuando exista; un error inesperado debe hablar de la operación KYC y no del ingreso. Si la billetera devuelve un 403 por permiso, se consulta el KYC propio para explicar la revisión sin conceder acceso.

## Criterios

- REQ-H2-KYC-1: DADO un `DioException` que contiene `ErrorDeApi`, CUANDO falla una operación KYC, ENTONCES se muestra el mensaje mapeado y su traza, sin cuerpo crudo de API.
- REQ-H2-KYC-2: DADO un `DioException` de red, CUANDO falla una operación KYC, ENTONCES se muestra el mensaje de conexión y no se inventa una referencia.
- REQ-H2-KYC-3: DADO un error desconocido, CUANDO falla guardar fecha o subir foto, ENTONCES el mensaje identifica la operación KYC y no dice «ingreso».
- REQ-H2-KYC-4: DADA una fecha civil, CUANDO se guarda, ENTONCES el cuerpo usa `yyyy-MM-dd` (OpenAPI `format: date`) y no incluye hora/zona.
- REQ-H2-KYC-5: DADO un 403 `AP-SEG-01` al resolver la billetera, CUANDO el KYC propio está pendiente/en revisión, ENTONCES se informa el proceso y se conserva el bloqueo de billetera.

## IN / OUT

IN: `apps/movil/lib/pantallas/identidad/dominio/estado_sesion.dart`, pantalla KYC, serialización de fecha civil, texto de acceso denegado de billetera basado en consulta KYC propia y pruebas enfocadas.

OUT: otorgar roles/permisos, aprobar o rechazar el expediente, cambiar estado/datos de la cuenta, cambiar backend/contratos/esquema, despliegue, pruebas físicas por el agente, GymSheet y cualquier otro flujo/módulo. El usuario pidió además un APK local completo después de corregir; eso no autoriza publicación ni modificación de la cuenta. Las dos capturas recibidas se conservan intactas y no se duplican en este informe porque contienen datos personales.

## Causa comprobada antes del cambio

Hay dos defectos locales comprobados:

1. `_TraduccionDeErrores` envuelve `ErrorDeApi`/`ErrorDeRed` dentro de `DioException.error`; el mapeador del KYC no los desenvolvía, por lo que perdía mensaje y referencia.
2. El modelo Dart generado `FechaExpiracionDocumento` serializa `DateTime` mediante `toIso8601String()` (por ejemplo, `2031-12-17T00:00:00.000`), mientras el contrato `servicios/identidad/src/main/resources/openapi/identidad.yaml` declara `format: date` y el controlador recibe `LocalDate`. La nueva operación móvil envía `yyyy-MM-dd` sin hora/zona.

La billetera también podía recibir `403 AP-SEG-01` pese a que un permiso del token dejara pasar la navegación. Ahora, solo en la resolución de la billetera propia, ese código consulta el estado KYC del titular y presenta la guía de revisión/acceso sin abrir la cuenta. Otros 403 no se convierten en falsos estados KYC.

El status HTTP exacto de la captura original no quedó correlacionado. El payload de fecha anterior sí era objetivamente contrario al contrato; el guardado en servidor aún requiere recorrido end-to-end para acreditar persistencia.

## Baseline y límite del entorno

Pruebas locales posteriores al cambio (desde `apps/movil`):

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/identidad/cu02_actualizar_fecha_expiracion_test.dart test/identidad/mensaje_de_error_dio_test.dart test/identidad/pantalla_completar_kyc_errores_test.dart test/identidad/resolver_cuenta_kyc_test.dart
00:03 +8: All tests passed!

dart analyze --fatal-infos [8 archivos Dart modificados/de prueba]
No issues found!
```

El test de cliente confirma que se envía la fecha `2031-02-03` sin hora, y los 3 tests de billetera distinguen PENDIENTE/EN_REVISION/APROBADA sin habilitar acceso.

Verificación backend intentada: `.\gradlew.bat --no-daemon :servicios:identidad:test --tests bo.aportaya.identidad.web.UsuariosSubirDocumentoWebTest`; falló antes de compilar tests porque `generateJooq` requiere PostgreSQL local en `127.0.0.1:5433`, que no está disponible. No hay Docker ni servicio PostgreSQL instalado/activo en esta sesión. El endpoint de salud del gateway de TEST respondió HTTP 200; no se hizo PUT contra la cuenta real.

El baseline antiguo de pruebas de pantalla sigue documentado: faltan `.g.dart` ignorados de clientes Dart no requeridos por `cliente_test.dart`; no se regeneran clientes ajenos al slice.

## APK local 2026-10-16

Build release ARM64 completo: `flutter build apk --release --target-platform android-arm64 --no-pub --build-number=2026101601` con API de TEST. Resultado: `apps/movil/build/app/outputs/flutter-apk/app-release.apk`, 69,008,128 bytes, paquete `bo.aportaya.aportaya_movil`, `versionCode=2026101601`. `apksigner verify` confirma firma v2 válida y el APK contiene `lib/arm64-v8a/libapp.so` y `libflutter.so`. SHA-256: `E3653A1F2FA4B9F6DDB3B4C5BD77CC81F92B751133B90A07709D951D91C42A59`.

Se copió a `C:\Users\leonardo\Downloads\AportaYa-kyc-fecha-wallet-fix-20261016-arm64.apk` sin sobrescribir otro archivo y se comprobó que coincide byte a byte con el artefacto. Importante para instalación: su certificado Android Debug no coincide con el APK anterior `AportaYa-perfil-kyc-test-20261016.apk`; Android no lo aceptará como actualización de esa instalación. No desinstalar la versión anterior sin decisión del usuario, porque puede perderse estado local. Para actualización sin desinstalar se necesita el keystore privado con el que se firmó el APK anterior; el certificado público del APK no permite reconstruirlo. No se publicó ni desplegó nada.

La compilación falló primero cuando se usó `--no-pub` sin regenerar la configuración de plugins; se corrigió ejecutando `flutter pub get` y `flutter build apk --config-only`, luego compilando ARM64 para limitar memoria. El build final terminó correctamente. La persistencia real contra el servidor y el recorrido físico siguen pendientes; no se afirma que la cuenta esté aprobada.

## Estado remoto AportaYa (consulta de solo lectura, 2026-10-16)

SSH al VPS funcionó con la clave local ya autorizada. `hostname; uptime` devolvió `vmi3544825`; no se ejecutaron comandos de escritura ni despliegue. `docker ps` filtrado por AportaYa mostró saludables Redis, Kafka, PgBouncer, MinIO, PostgreSQL, identidad (`aportaya/identidad:test`) y backoffice (`aportaya/backoffice:test`). La sonda pública `https://api.aportaya.161.97.85.216.sslip.io/actuator/health` respondió HTTP 200.

Esto solo acredita disponibilidad del stack de TEST en el momento de la consulta; no acredita que el cambio local esté desplegado, que el guardado de KYC persista ni que el expediente haya sido aprobado. No se consultaron tablas ni secretos, y no se tocó ningún servicio ajeno a AportaYa.

## Skills leídas para este slice

`skills-router`, `outcome-first`, `context-thrift`, `factual-discovery`, `anti-hallucination-guard`, `scope-discipline`, `root-cause-debugging`, `rationalization-guard`, `native-code-patterns`, `progress-reporting`, `evidence-and-verification`, `visual-proof`, `finish-your-turn`, `requirements-and-acceptance`, `work-report-md`, `flutter-development`, `flutter-state-architecture`, `flutter-testing`, `mobile-ux-design`, `frontend-ux-states`, `ux-writing-microcopy`, `frontend-accessibility`, `kyc-identity-verification`, `data-privacy-financial`, `security-guardrails`, `error-handling-contract`, `unit-testing`, `qa-evidence-reporting`.

## Estado inicial

`TESTED` para serialización del body, mapeo de errores, mensaje 403/KYC en pruebas sintéticas, build y firma estructural del APK ARM64. `WRITTEN/REVIEWED` para el uso en el servicio real: falta prueba backend (PostgreSQL local ausente), instalación/recorrido en teléfono y confirmación end-to-end de persistencia en una cuenta de ensayo. El APK no actualiza la instalación anterior por diferencia de certificado. La cuenta real del usuario no fue modificada ni aprobada.

## Revalidación del test web backend (2026-10-16)

La barrera de PostgreSQL local se resolvió sin usar la instancia existente de `5432` ni la base remota. Se inició `postgres:16` en el contenedor temporal `aportaya-plan-pg-20261016`, limitado a 1 GiB, con `tmpfs` (sin volumen persistente), puerto `127.0.0.1:5433` y montaje de solo lectura de `sql/`. Se aplicó `sql/aplicar.sql` con `ON_ERROR_STOP=1`; la salida terminó en `COMMIT` y código 0. `generateJooq` generó identidad correctamente. jOOQ advirtió que PostgreSQL 16.15 es más antiguo que su versión preferida, pero no hubo error de generación.

Comando desde la raíz del repo (con `BD_URL_ADMIN=jdbc:postgresql://127.0.0.1:5433/pasanaku`, usuario/clave de ensayo `pasanaku`, y `GRADLE_OPTS=-Xmx384m -XX:MaxMetaspaceSize=256m`):

```text
.\gradlew.bat --no-daemon --no-parallel --max-workers=1 :servicios:identidad:webTest --tests 'bo.aportaya.identidad.web.UsuariosSubirDocumentoWebTest'
BUILD SUCCESSFUL in 57s
```

El XML `servicios/identidad/build/test-results/webTest/TEST-bo.aportaya.identidad.web.UsuariosSubirDocumentoWebTest.xml` confirma **19 tests, 0 failures, 0 errors, 0 skipped**. Cubre la subida autenticada por titular, denegaciones de token/sesión ajenos y el guardado de fecha de documento con sesión propia, incluso en revisión y sin permiso de billetera. El primer intento no ejecutó pruebas porque PowerShell separó mal `-Dorg.gradle.parallel=false`; se repitió con `--no-parallel` y pasó.

Después de la corrida se detuvo únicamente el contenedor temporal (su `--rm` eliminó el contenedor y `tmpfs`) y se detuvo Docker Desktop. No quedó listener en `5433`, contenedor del intento, Gradle/Java ni emulador activos. Uso máximo observado durante la sesión: 12,87/15,32 GiB, por debajo del tope indicado de 15,1 GiB. Esta prueba valida el contrato web local; no acredita persistencia en TEST ni completa H2.S1.M1, que sigue A MEDIAS por los recorridos de alta e ingreso pendientes.

## MinIO local y regresión completa de identidad (2026-10-16)

Se destrabó el almacenamiento local fijado en `despliegue/compose/base.yml` sin cambiar ni publicar el compose. La descarga del contenedor de Quay no estaba disponible en el entorno; se compilaron los binarios desde los repositorios oficiales y se empaquetaron en una imagen **solo local** para satisfacer esos tags de Compose:

- `minio/minio` tag `RELEASE.2025-09-07T16-13-09Z`, commit `07c3a429bfed433e49018cb0f78a52145d4bedeb`, Go `1.24.13 linux/amd64`, SHA-256 `97749E73E7411DE531FF20D90AF09AA11E266FDE1195C38458C9BD2DCDA129A4`.
- `minio/mc` tag `RELEASE.2025-08-13T08-35-41Z`, commit `7394ce0dd2a80935aded936b09fa12cbb3cb8096`, Go `1.24.13 linux/amd64`, SHA-256 `2A51A16A1DB199AF4C25A03E6C725B16110918A2013CB547996AFC328A6FF06F`.
- Reempaque local sobre `alpine:3.17`, tags locales `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` y `quay.io/minio/mc:latest`. **No es la imagen OCI oficial** y no se usó `push`, despliegue ni modificación de la imagen remota.

La prueba del stack AportaYa ejecutó `docker compose --profile base -f despliegue/compose/base.yml up -d postgres minio minio_bucket`: PostgreSQL y MinIO llegaron a `healthy`; `minio_bucket` terminó `0` tras crear `aportaya-archivos`, activar versionado y dejar acceso anónimo deshabilitado. En un contenedor de humo descartable, `mc cp` subió 13 bytes y `mc stat` devolvió un `VersionID`; se eliminó el contenedor descartable al terminar. La base local reportó 436 tablas.

Regresión completa del módulo backend de identidad, en serie y con Gradle acotado:

```text
.\gradlew.bat --no-daemon --no-parallel --max-workers=1 :servicios:identidad:test :servicios:identidad:webTest :servicios:identidad:integrationTest
BUILD SUCCESSFUL in 2m 26s
```

Conteos sumados de los XML JUnit locales: `test` 21/21, `webTest` 121/121, `integrationTest` 143/143; en los tres suites, `failures=0`, `errors=0`, `skipped=0`. Los XML permanecen en `servicios/identidad/build/test-results/{test,webTest,integrationTest}`. Pico de memoria del equipo observado durante la verificación: 14,40/15,32 GiB, bajo el límite solicitado de 15,1 GiB.

Esto elimina el bloqueo de **disponibilidad de MinIO para pruebas locales**, pero no demuestra el recorrido móvil completo: aún faltan el arranque y recorrido de identidad local con OTP ensayable y AVD, las filas de alta/login/contraseña incorrecta/expiración de H2.S1.M1, y la evidencia visual integral de H10.S2.M2. No se usó teléfono físico, no se simularon rostros reales, no se accedió a TEST para cambiar datos y no se desplegó nada.

Al terminar se ejecutó `docker compose --profile base -f despliegue/compose/base.yml stop postgres minio`; los volúmenes de AportaYa se conservaron, no se eliminaron. Docker Desktop quedó apagado (`docker desktop status`: no activo), sin Gradle/Java ni emulador; RAM final: 11,00/15,32 GiB.

## Revalidación móvil dirigida de fecha KYC y estados de acceso (2026-10-16)

Desde `apps/movil`, sin iniciar Docker ni AVD, se ejecutó:

```text
flutter test test/identidad/cu02_actualizar_fecha_expiracion_test.dart test/identidad/mensaje_de_error_dio_test.dart test/identidad/pantalla_completar_kyc_errores_test.dart test/identidad/resolver_cuenta_kyc_test.dart --timeout 60s
00:01 +8: All tests passed!

flutter analyze lib/pantallas/billetera/resolver_cuenta.dart lib/pantallas/identidad/dominio/estado_sesion.dart lib/pantallas/identidad/dominio/cu02_actualizar_fecha_expiracion.dart lib/pantallas/identidad/pantalla_completar_kyc.dart test/identidad/cu02_actualizar_fecha_expiracion_test.dart test/identidad/mensaje_de_error_dio_test.dart test/identidad/pantalla_completar_kyc_errores_test.dart test/identidad/resolver_cuenta_kyc_test.dart
No issues found! (ran in 42.9s)
```

Los cuatro archivos de prueba pasan (8 tests reportados por el runner) y el analizador no detecta incidencias en los ocho archivos Dart seleccionados. `flutter test` resolvió dependencias y avisó que 37 paquetes tienen versiones más nuevas fuera de las restricciones actuales; no se actualizó ninguna dependencia. Memoria observada después de las verificaciones: 10,40/15,32 GiB en uso, debajo del límite de 15,1 GiB. ADB permanece sin dispositivos conectados. Esto refuerza la verificación local del cambio de fecha/mensajes/estado de billetera, pero no prueba una escritura end-to-end en backend ni cierra H2.S1.M1 o los recorridos H10.S2.

Como regresión más amplia se ejecutó `flutter test test/identidad test/pasanaku --timeout 60s --concurrency=1 --reporter expanded` con Flutter 3.47.5/Dart 3.13.4: **91 tests pasaron y 2 fallaron**. Los dos fallos son golden de `motivo_de_salida_en_ingreso_test.dart` (`h1-ingreso-caducada.png` y `h1-ingreso-cerrada.png`), ambos con 1,38 % de píxeles distintos (4.835 px). Coinciden con los dos diffs de ingreso ya registrados en H10.S1.M6; no se actualizaron baselines. Flutter dejó los PNG de diagnóstico en `apps/movil/test/identidad/failures/`; se conservaron. El conjunto identidad/Pasanaku excluye GymSheet. Resultado general: validación dirigida KYC 8/8 y suites ampliadas con una excepción de renderer conocida; no es una regresión atribuida a los cambios KYC sin una comparación del renderer canónico.

## APK release completo de TEST recompilado (2026-10-16)

Se generó el APK release multiarquitectura con el código local actual y los defines del gateway de TEST:

```text
flutter build apk --release --no-pub --target=lib/main.dart \
  --dart-define=API=https://api.aportaya.161.97.85.216.sslip.io/api/v1 \
  --dart-define=HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io
Built build/app/outputs/flutter-apk/app-release.apk (106.2MB)
```

Verificación del artefacto: paquete `bo.aportaya.aportaya_movil`, `versionCode=1`, firma Android Debug v2 verificada, SHA-256 `AFCF908DBBEB3C20DE8FA02F8A0BC555E4E023F31141BAFB0E06A89ACD06B9ED`. Se copió byte a byte a `C:\Users\leonardo\Downloads\AportaYa-KYC-fecha-vencimiento-TEST-20261016-local.apk` (111.377.426 bytes). El certificado coincide con `AportaYa-kyc-fecha-wallet-fix-20261016-arm64.apk`, pero no con `AportaYa-perfil-kyc-test-20261016.apk`; si esta última es la instalada, Android no permitirá actualizarla encima con este archivo. No desinstalarla sin valorar el estado local.

El compilador tuvo que excluir de la metadata local generada los plugins `integration_test` y `patrol`, que son dependencias de desarrollo y no están en el classpath release; también se limitó Gradle a un worker/3 GiB durante el empaquetado. Después se regeneró `.flutter-plugins-dependencies` con `flutter pub get --enforce-lockfile --offline`, se restauró `android/gradle.properties` y `pubspec.lock` permaneció intacto. No se cambió ningún archivo de código para ese workaround. Aun así, el APK no se instaló ni se hizo PUT sobre el expediente de `67774016`: la fecha y su persistencia en TEST quedan pendientes de la prueba del usuario. Sin publicación ni despliegue.
