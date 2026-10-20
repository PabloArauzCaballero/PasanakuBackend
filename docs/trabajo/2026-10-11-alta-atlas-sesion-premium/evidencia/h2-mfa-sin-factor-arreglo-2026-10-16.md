# H2 · MFA sin factor enrolado — corrección local

Fecha: 2026-10-16 (America/La_Paz)

## Acciones realizadas

- En `CU04Autenticar`, una cuenta que requiere MFA y no tiene factor recibe el desafío de ensayo solo cuando el adaptador local de desarrollo declara que admite OTP fijo sin enrolamiento.
- En ese perfil el primer paso devuelve `AP-CU04-03`, y `000000` puede completar el ingreso. Con el adaptador normal, se devuelve `AP-CU04-06` (`FACTOR_NO_ENROLADO`) antes de solicitar un OTP imposible; no se abre sesión.
- Se agregaron casos de integración para las dos ramas: cuenta sin factor con el adaptador normal (denegada) y la secuencia de ensayo con el adaptador local de desarrollo.
- No se consultó ni modificó la cuenta real durante esta corrección; no se desplegó ni se publicó ningún cambio.

## Verificación

- Comando: `.\gradlew.bat :servicios:identidad:test --tests bo.aportaya.identidad.CU04Test`
  - Resultado: compiló el código y las pruebas; el corredor `test` excluye por diseño las pruebas `CU*` de integración, por lo que no encontró `CU04Test`. Reintento en `integrationTest` abajo.
- Comando: `.\gradlew.bat :servicios:identidad:integrationTest --tests bo.aportaya.identidad.CU04Test`
  - Primer intento: la clase compiló, pero no pudo inicializar Testcontainers con Docker apagado: `Could not find a valid Docker environment`.
- `git diff --check`: sin errores.

## Seguimiento de la corrección del adaptador local

- El puerto `DesafioDeFactor` ahora permite declarar explícitamente si el adaptador acepta OTP de ensayo sin factor enrolado; por omisión la respuesta es `false`.
- `DesafioDeDesarrollo` es el único adaptador que habilita esa capacidad. En ese perfil, CU-04 puede emitir el paso `AP-CU04-03` y aceptar `000000` para el ensayo de una cuenta sin factor.
- El adaptador normal `DesafioLocal` no habilita la excepción. Sin factor enrolado responde `AP-CU04-06`; no se abre sesión.
- Se agregó un caso de integración para la secuencia de dos pasos con el adaptador de desarrollo y un test unitario de la diferencia entre ambos adaptadores.
- Comando: `.\gradlew.bat :servicios:identidad:test --tests bo.aportaya.identidad.DesafioDeDesarrolloTest` — `BUILD SUCCESSFUL`.
- Comando: `.\gradlew.bat :servicios:identidad:compileTestJava` — `BUILD SUCCESSFUL`.
- Reintento de integración, comando: `$env:JAVA_TOOL_OPTIONS='-Xmx640m -XX:MaxMetaspaceSize=256m'; .\gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m' :servicios:identidad:integrationTest --tests bo.aportaya.identidad.CU04Test` — `BUILD SUCCESSFUL`; XML `CU04Test`: 17 tests, 0 fallos, 0 errores, 0 omitidos. PostgreSQL efímero real vía Testcontainers.
- Alcance de los 17 casos: incluye cuenta sin factor denegada por el adaptador normal (`AP-CU04-06`) y los dos pasos con OTP `000000` con el adaptador de desarrollo; la sesión se crea solo en este último ensayo local. No modifica la cuenta real ni el backend remoto.
- Recursos/limpieza: Docker Desktop se inició oculto para esta prueba y se detuvo al finalizar; los contenedores Testcontainers ya no estaban activos tras la corrida. RAM observada: 13,6 GiB usados durante la prueba (máximo observado) y 11,5 GiB después de detener Docker; debajo del límite de 15,1 GiB. No queda AVD encendido.
- Comando: `.\gradlew.bat :servicios:identidad:webTest --tests bo.aportaya.identidad.web.SesionesControllerWebTest` — `BUILD SUCCESSFUL`; reporte XML: 17 tests, 0 fallos, 0 errores, 0 omitidos. El caso añadido observó HTTP 422, `AP-CU04-06`, mensaje de factor no activado y ausencia de emisión de token.
- Alcance de esa prueba: MockMvc ejecuta el controlador y su mapeo de errores, pero CU-04 está sustituido por un mock. No acredita base de datos, adaptador de desarrollo, ni login end-to-end.
- Comando móvil (directorio `apps/movil`): `flutter test --no-pub --concurrency=1 --reporter expanded test/unidad/catalogo_de_errores_test.dart test/identidad/pantalla_mfa_test.dart` — `All tests passed!`, 26 pruebas. Incluye catálogo `AP-CU04-06` y rechazo `AP-CU04-04` en temas claro/oscuro; son estados cliente simulados, no una llamada al backend.
- Regresión completa adicional, comando: `$env:JAVA_TOOL_OPTIONS='-Xmx640m -XX:MaxMetaspaceSize=256m'; .\gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m' :servicios:identidad:integrationTest` — `BUILD SUCCESSFUL` en 1m27s. XML: 24 suites, 145 tests, 0 fallos, 0 errores, 0 omitidos. Incluye todos los CU/repositories de Identidad con PostgreSQL efímero.
- La integración extensa muestreó hasta 14,1 GiB usados de 15,3 GiB (<15,1 GiB); al finalizar Docker Desktop quedó detenido, sin contenedores en ejecución y RAM en 11,2 GiB usada / 4,1 GiB libre. No se dejó ningún AVD encendido.
- **Corrección de nota anterior:** la integración CU-04 sí se ejecutó correctamente después de iniciar Docker Desktop: 17/17 casos; además, la regresión completa de Identidad terminó con 145/145. La nota previa que decía que CU-04 no se había ejecutado quedó obsoleta y se reemplaza por estos resultados. Esto acredita el caso de uso local con PostgreSQL efímero, no el VPS TEST ni el login de la app contra ese backend.

## Estado y límite conocido

La excepción de ensayo permite probar el ingreso con `000000` solo cuando corre el adaptador `DesafioDeDesarrollo` del perfil `local`; no es un canal de correo ni un factor permanente. En los perfiles normales se conserva el cierre seguro. La integración local y la regresión completa de Identidad pasan contra PostgreSQL efímero, pero no equivalen a probar la imagen actual de TEST ni la aplicación móvil contra ese servidor. Para dar a la cuenta real un método de MFA utilizable y duradero falta un flujo seguro de enrolamiento/recuperación por correo: enviar únicamente al correo ya asociado, verificar su titularidad (el correo de esa cuenta aparece como no verificado), aplicar límites/expiración/uso único y conectar ese factor con el login móvil. El APK de usuario no cambia con este parche; no desplegar ni publicar sin aprobación del usuario.

## Archivos del cambio

- `servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/CU04Autenticar.java`
- `servicios/identidad/src/main/java/bo/aportaya/identidad/dominio/puertos/DesafioDeFactor.java`
- `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/DesafioDeDesarrollo.java`
- `servicios/identidad/src/test/java/bo/aportaya/identidad/CU04Test.java`
- `servicios/identidad/src/test/java/bo/aportaya/identidad/DesafioDeDesarrolloTest.java`
