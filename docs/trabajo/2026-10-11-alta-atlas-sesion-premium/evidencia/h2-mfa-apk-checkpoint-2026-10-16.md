# Punto de control: MFA y APK de prueba (2026-10-16)

Este documento registra etapas sucesivas del mismo día; las secciones posteriores conservan datos del momento en que se escribieron. Estado vigente: CU-04 pasó 18/18 con PostgreSQL aislado; Gmail se probó con HTTP simulado, sin entrega real; el [PR #87](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/87) del correo KYC se integró en `test` con dos checks correctos. La verificación remota de abajo confirma que los contenedores aún sirven el commit anterior. Falta recorrer MFA desde el APK con entrega de correo en TEST.

## Verificación remota de solo lectura — 21:50 UTC

- `docker exec` consultó exclusivamente `SOURCE_COMMIT`, `SPRING_PROFILES_ACTIVE` y `NOTIFICATION_EMAIL_PROVIDER` del contenedor AportaYa de Identidad: `d7d707fc76920f2ea3aa3e9b9e7bbe2ab34658bb`, `local`, `gmail_api`. En el contenedor AportaYa de Backoffice, `SOURCE_COMMIT` también fue `d7d707fc76920f2ea3aa3e9b9e7bbe2ab34658bb`. Ambos estaban `Up` y `healthy`. No se leyeron secretos, datos de cuentas ni la base de datos; no se ejecutó despliegue ni escritura remota.
- El merge del PR #87 a `test` es `2b3a2ad3a54a222acacb23ae7eb653b9d80104fc` (21:42:23 UTC). El código de `d7d707fc…` muestra teléfono en la tarjeta KYC y filtra por nombre/teléfono, sin campo de correo. Su `DesafioDeDesarrollo` usa `000000` y no llama a un canal de correo. Configurar `gmail_api` no añade esa llamada al código antiguo.
- El backoffice público respondió HTTP 200 y su ruta de verificaciones cargó `chunk-Dbcan6Bd.js`; ese componente no contiene el campo `correo`. La API pública respondió HTTP 200 en `/actuator/health`. Estas sondas no prueban el flujo MFA ni el contenido de una cuenta; el commit del contenedor es la evidencia principal de la versión servida.
- Resultado: el cambio de correo KYC está en GitHub `test`, pero no en el Backoffice servido al usuario a esa hora. La entrega MFA real continúa sin comprobarse; el APK por sí solo no puede cambiar el backend. H2.S1.M1 sigue A MEDIAS.

## Diagnóstico posterior: reenvío MFA y correo visible en KYC (2026-10-16)

- El correo de la captura del 15 de octubre usa el asunto de la plantilla de confirmación de registro (`Tu codigo de verificacion de AportaYa`); no demuestra que el segundo factor de ingreso haya sido enviado. El MFA usa una plantilla separada (`Tu codigo para ingresar a AportaYa`).
- Se eliminó el fallback silencioso de `CorreoDeVerificacion.enviarCodigoDeIngreso`: ahora cada proveedor debe implementar explícitamente el envío MFA. Gmail envía la plantilla de ingreso; el proveedor local falla con `CanalDeCorreoNoDisponible` en lugar de fingir éxito o reutilizar el mensaje de alta.
- La ficha KYC ya expone el correo en el contrato OpenAPI, la consulta del expediente, la respuesta del controlador y la tarjeta del backoffice; el filtro y su etiqueta local incluyen correo. La captura remota no muestra el campo porque el backoffice desplegado todavía no contiene estos cambios locales.
- Revalidación: `:servicios:identidad:test --tests bo.aportaya.identidad.GmailApiCorreoDeVerificacionTest --tests bo.aportaya.identidad.DesafioDeDesarrolloTest` PASS (9/9); `:servicios:identidad:webTest --tests bo.aportaya.identidad.web.VerificacionesControllerWebTest` PASS (15/15), incluido `$.correo`; backoffice dominio PASS (8/8) y tarjeta visible por componente PASS (2/2), incluido el fallback «No registrado». Gmail se verificó con HTTP simulado: no se usaron credenciales ni se envió un correo real.
- Causa operativa pendiente: el APK apunta al gateway TEST, pero el servidor aún no tiene el backend actualizado/configurado. Para entregar correo real, TEST debe ejecutar esta versión de Identidad con `NOTIFICATION_EMAIL_PROVIDER=gmail_api` y secretos `GMAIL_*` válidos. Para que aparezca el correo en el panel, también debe actualizarse el backoffice de TEST. No se desplegó ni se tocó producción.

## Actualización: correo MFA y correo en expedientes (2026-10-16)

- APK local de prueba actualizado en `C:\Users\leonardo\Downloads\AportaYa-MFA-correo-backoffice-TEST-20261016-debug-arm64.apk`; 148169985 bytes, versionCode `2026101618`, paquete `bo.aportaya.aportaya_movil`, SHA-256 `FE8FB5C769F8753297AB2A7EE093AF139EFCDDFFD5C6A75F9FFB4FC984AAF0B9`. `aapt dump badging` verificó paquete/versión y `apksigner verify --verbose` confirmó firma Android v2 con un firmante.
- Compilación Android debug ARM64 completada correctamente. Para respetar el límite de RAM se redujo temporalmente Gradle a `-Xmx2G`; al terminar se restauró `apps/movil/android/gradle.properties` a `-Xmx8G` y se detuvo el daemon. El primer intento se interrumpió al medir 14.43/15.32 GiB usados; el segundo terminó dentro del límite.
- La suite del backoffice pasó: 73 archivos, 482 pruebas. Incluye filtro por nombre, teléfono y correo, y el correo aparece en las tarjetas de KYC. `typecheck` también pasó.
- `:servicios:identidad:test :servicios:identidad:webTest`: PASS. `flutter test test/identidad/autenticar_test.dart test/identidad/pantalla_mfa_test.dart`: PASS, 17 pruebas. `flutter analyze` de los archivos MFA modificados: PASS.
- AmpliaciÃ³n de `GmailApiCorreoDeVerificacionTest`: prueba que el cÃ³digo MFA se incluye en el MIME enviado al destinatario verificado con vigencia de cinco minutos y que un rechazo OAuth corta el flujo antes de invocar Gmail. Comando `:servicios:identidad:test --tests bo.aportaya.identidad.GmailApiCorreoDeVerificacionTest --tests bo.aportaya.identidad.DesafioDeDesarrolloTest`: PASS, 9 pruebas (4 Gmail + 5 adaptador), 0 fallos/errores. Es prueba del adaptador con HTTP simulado, no una entrega real a Gmail.
- La primera repetición de integración reutilizó una base temporal ya poblada y falló por teléfonos de fixture duplicados; no fue un fallo funcional del cambio. Se creó una base aislada nueva `pasanaku_cu04_fresh` en el clúster temporal y `:servicios:identidad:integrationTest --tests bo.aportaya.identidad.CU04Test` pasó (18 pruebas). El clúster se detuvo; el puerto 55439 ya no escucha. No se accedió a PostgreSQL de TEST ni producción.
- El backend ahora solo comunica “envío por correo confirmado” cuando el adaptador de correo acepta el mensaje; la app ya no presenta como exitoso el código sintético de compatibilidad del backend viejo. La lista de verificación expone el correo disponible para búsqueda y visualización.
- Limitación importante para la prueba física: el APK apunta al gateway de TEST, pero el backend y el backoffice siguen siendo cambios locales. Por eso este APK no puede hacer que TEST envíe correos hasta que se apruebe y despliegue el backend con `gmail_api` correctamente configurado; tampoco mostrará todavía el correo nuevo en el backoffice remoto. No se desplegó, publicó ni hizo push de ningún cambio.
- Próximo paso: instalar el APK y revisar la respuesta honesta del botón en TEST. Para validar que el código llega a Gmail y que el backoffice lista correos hay que autorizar después el despliegue del backend y backoffice en TEST, con configuración Gmail ya disponible. Producción permanece fuera de alcance.

## Actualizacion posterior: APK local entregado (2026-10-16)

- APK actual para instalar en telefono ARM64: `C:\Users\leonardo\Downloads\AportaYa-MFA-TEST-20261016-debug-arm64.apk` (148170433 bytes), versionCode `2026101617`, paquete `bo.aportaya.aportaya_movil`.
- SHA-256: `26FF450748A344CDCF4C227D52290CBA821C3960E864EA46533C202E359EE667`.
- Build exitoso: `flutter build apk --debug --target-platform=android-arm64 --build-number=2026101617 --dart-define=API=https://api.aportaya.161.97.85.216.sslip.io/api/v1 --dart-define=HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io`. `aapt dump badging` confirmo paquete/version y `apksigner verify --print-certs` confirmo firma Android Debug.
- El intento release de esta iteracion fallo en `compileReleaseJavaWithJavac`: el `GeneratedPluginRegistrant.java` local referencia `integration_test` y `patrol`, pero Gradle excluye plugins `dev_dependency` del classpath release. No se cambio el codigo ni dependencias para forzar release; el APK de prueba debug compilo correctamente.
- El APK apunta al gateway TEST pero no despliega cambios ni contiene el backend. Por tanto, el reenvio visible en la app no enviara correo mientras el backend TEST no tenga el cambio local de MFA por correo. No se hizo push, PR ni despliegue.
- Se limito temporalmente Gradle a 1.5 GiB para el build y se restauro `android/gradle.properties`; el daemon se detuvo al terminar. La RAM quedo en 13.54/15.32 GiB usados.

## Actualizacion posterior: integracion CU-04 con PostgreSQL aislado (2026-10-16)

- Se creo una instancia PostgreSQL 18 temporal propia bajo `%LOCALAPPDATA%\Temp\aportaya-mfa-pg-20261016`, puerto `55439`; no se conecto ni altero el servicio PostgreSQL existente, TEST o produccion. Se aplico `sql/aplicar.sql` solo a la base temporal `pasanaku`.
- `:servicios:identidad:integrationTest --tests bo.aportaya.identidad.CU04Test`: PASS, 18 tests, 0 fallos, 0 errores, 0 omitidos. Informe JUnit: `servicios/identidad/build/test-results/integrationTest/TEST-bo.aportaya.identidad.CU04Test.xml`.
- La primera corrida detecto que el telefono fijo del test MFA duplicaba el de otro caso; se cambio a un telefono aleatorio de ensayo y se repitio desde una base temporal limpia. La corrida final paso.
- La instancia temporal se detuvo al terminar; `pg_isready` confirmo que el puerto 55439 ya no acepta conexiones. El directorio temporal se conserva y no contiene datos reales.
- H2.S1.M1 sigue A MEDIAS: la prueba cubre CU-04 y el correo simulado, no la entrega real Gmail de TEST ni el recorrido E2E desde el APK. No se desplego nada.

## Actualizacion posterior: MFA por correo (2026-10-16)

Se implemento localmente el primer tramo del arreglo, sin publicar cambios:

- CU-04 solo consulta el correo de la cuenta si `correo_verificado_en` esta informado; nunca toma un email escrito en la pantalla de ingreso.
- El segundo factor de participantes con correo confirmado se emite por el proveedor de correo existente. La plantilla identifica el mensaje como codigo de ingreso, el OTP vence a los 5 minutos y el valor no se registra.
- Se limita el envio a 60 segundos entre emisiones, 4 por hora y 10 por dia. La tabla de politica `SEGUNDO_FACTOR` admite ahora `CORREO` en su semilla fuente y SQL regenerado.
- La pantalla MFA tiene accion de reenvio; esta vuelve a validar telefono y contrasena y hace que el backend emita un reto nuevo. Si falla el proveedor, el backend responde `AP-CU04-09`; el adaptador `local` no simula un envio exitoso.
- El perfil Spring `local` conserva el OTP de ensayo solo cuando el correo no puede entregarse por falta del adaptador de correo. Si el OTP se envio por correo, ese reto debe validarse y `000000` no lo sustituye.

Verificacion local actual:

- `:servicios:identidad:test --tests bo.aportaya.identidad.DesafioDeDesarrolloTest`: PASS (4 pruebas), incluye formato/secuencia del OTP, uso unico, cooldown y fallback local sin transporte. `compileTestJava` tambien compilo la nueva prueba PostgreSQL de CU-04.
- `flutter test test/identidad/pantalla_mfa_test.dart`: PASS (8 pruebas entre claro/oscuro), incluye accion de reenvio y accesibilidad.
- `flutter analyze` sobre los 5 archivos Flutter cambiados: PASS, sin issues.
- `python scripts/generar_semillas.py`: PASS; 1.513 filas validadas y SQL generado desde el JSON.
- `git diff --check`: PASS.

Al redactar esta seccion aun estaban pendientes la prueba PostgreSQL, confirmar Gmail de TEST y recorrer el APK; la actualizacion posterior arriba registra que la integracion PostgreSQL ya paso y que se genero un APK debug local. Siguen pendientes Gmail/runtime de TEST y E2E con backend que entregue OTP. El servidor remoto TEST no se cambio, por lo que ese APK no puede confirmar el envio real de correo.

Riesgo residual: `DesafioLocal` conserva los retos en memoria; un reinicio o varias replicas pueden perder el reto. Debe migrarse a `identidad.token_verificacion` antes de considerar este flujo durable o apto para produccion. La entrega real depende de que TEST tenga `NOTIFICATION_EMAIL_PROVIDER=gmail_api` y sus variables `GMAIL_*` ya configuradas; sus valores no se guardan en el repositorio.

## Alcance y seguridad

- Solo AportaYa, rama local `codex/alta-atlas-sesion-premium-2026-10-11`.
- No se hizo push, PR ni despliegue. El APK se compiló apuntando al gateway de TEST; la compilación no modifica ese entorno.
- No se usaron credenciales reales ni se copió información de cuentas de TEST a la base local.

## Verificaciones realizadas

- `:servicios:identidad:test --tests bo.aportaya.identidad.DesafioDeDesarrolloTest`: PASS. Verifica que `000000` solo lo acepte el adaptador de ensayo `DesafioDeDesarrollo`; `DesafioLocal` no lo acepta.
- `flutter test` de `cu02_actualizar_fecha_expiracion_test.dart`, `mensaje_de_error_dio_test.dart`, `pantalla_completar_kyc_errores_test.dart` y `resolver_cuenta_kyc_test.dart`: PASS, 8 comprobaciones.
- Build release Android contra TEST: PASS con Flutter 3.47.5. El intento inicial con `--no-pub` falló porque conservó un `GeneratedPluginRegistrant.java` de pruebas; el build normal regeneró el archivo excluyendo `integration_test` y `patrol`, y compiló correctamente.
- Firma: `apksigner verify --verbose` confirma firma v2 con un firmante. Es firma de depuración para instalación de prueba, no de Play Store.
- APK generado: `apps/movil/build/app/outputs/flutter-apk/app-release.apk`, 111377426 bytes. SHA-256: `6D9322B34E73FD02EAC5197CA6B530D9F028754B774CCC17BBFF7EDA5CE65B0D`.
- Se preservó el APK previo como `apps/movil/build/app/outputs/flutter-apk/app-release-prev-20261016-1057.apk`.

## Lo que aún no está resuelto

- `DesafioDeDesarrollo` se activa exclusivamente con perfil Spring `local`; la lectura remota anterior sí confirmó ese perfil en TEST, pero no verificó el factor activo ni una sesión de la cuenta `67774016`. La prueba unitaria no demuestra que esa cuenta pueda entrar con `000000`. No se activó en producción.
- El envío y reenvío de MFA ya están implementados localmente y la integración CU-04 con PostgreSQL aislado pasó 18/18. Falta confirmar el proveedor Gmail y la versión de Identidad en TEST, además de la entrega real y el recorrido APK–backend. `/usuarios/verificaciones/correo` sigue siendo únicamente la verificación del alta.
- No se hizo recorrido de ingreso de extremo a extremo con cuenta de ensayo contra un backend que entregue el OTP; el backend no se desplegó. El APK debug local se generó después y queda documentado arriba.
- Los 18 goldens de MFA ejecutados en Windows no coincidieron pixel a pixel con los PNG de referencia; no se actualizaron baselines. La validación previa de 274 goldens PASS en Linux/Flutter 3.44.8 está documentada en `h10-s1-m6-m7-linux-3448-2026-10-16.md`.

## Siguiente verificación segura

1. Migrar los retos MFA de memoria a `identidad.token_verificacion`, incluidos intentos, vencimiento, consumo y límite de reenvíos transaccional.
2. Completar prueba integrada del CU-04 en PostgreSQL y probar el adaptador Gmail con respuestas simuladas; confirmar configuración de proveedor en TEST sin leer ni imprimir secretos.
3. Mostrar en la pantalla el destino enmascarado que confirma el backend (el cliente ahora dice correo verificado, pero no recibe esa máscara por el contrato actual).
4. Ejecutar recorrido local aislado y AVD; después generar un APK que apunte al backend local de prueba o a TEST tras actualizar ese servicio.
5. Esperar aprobación expresa antes de desplegar cualquier cambio en TEST o producción.
