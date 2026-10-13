# H1.S6 backend — límite de verificación

Fecha: 2026-10-12. No se publicó ni desplegó ningún cambio.

- `./gradlew :servicios:identidad:openApiValidate` no valida el contrato: la tarea `ValidateTask` falla porque no tiene `inputSpec` configurado en este proyecto.
- `./gradlew :servicios:identidad:compileTestJava --no-configuration-cache` se detuvo antes de compilar, en `:servicios:identidad:generateJooq`, porque la conexión a `127.0.0.1:5433` fue rechazada.
- El segundo intento excluyendo `:servicios:identidad:generateJooq` tampoco puede compilar: las clases de `bo.aportaya.identidad.generado` dependen de la generación jOOQ que no llegó a ejecutarse.
- Docker Desktop no está operativo en esta sesión; no se pudo levantar la pila local. La instancia PostgreSQL observada en 5432 no acepta las credenciales predeterminadas del proyecto. No se inspeccionaron secretos ni se modificó la instancia.

Resultado honesto: M2–M5 están implementadas pero `A MEDIAS` porque sus DoD de Gradle no corrieron; M8 queda `BLOQUEADO` hasta que exista la BD de pruebas en 5433 y pueda arrancar la pila. El código y pruebas web/integración siguen pendientes de ejecutar en ese entorno.

Hallazgos de revisión atendidos en código, aún sin prueba de integración: `DELETE /sesion` lleva `@Permiso("PARTICIPANTE")` más `@PermisoAlternativo({"BACKOFFICE"})`; `POST /sesion/refrescar` admite `*/*` para conservar cookie-only sin `Content-Type` JSON. Un access JWT robado sigue vigente hasta su expiración; la revocación actual invalida sesión y familia refresh, no proporciona revocación inmediata en todos los servicios.

## Reintento acotado — 2026-10-13

- La inspección actual confirma que `SesionesController.autenticar` ya bifurca: WEB emite cookie; ANDROID/IOS ponen `tokenRefresco` en el cuerpo. `SesionesControllerWebTest` contiene expectativas de no `Set-Cookie` en móvil y de cookie sin `tokenRefresco` en WEB; no se afirma que esas pruebas pasaron.
- Corrí `./gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.parallel=false' '-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=192m -Dfile.encoding=UTF-8' :servicios:identidad:test --tests '*SesionesControllerWebTest'`, con `JAVA_TOOL_OPTIONS=-Xmx384m -XX:MaxMetaspaceSize=192m` para limitar memoria. El build alcanzó `:servicios:identidad:generateJooq` y se detuvo antes de compilar/ejecutar la clase: `Connection to 127.0.0.1:5433 refused`.
- Estado observado después: `docker ps` no listó contenedores activos; no hay `compose.yaml`/`compose.yml` local enumerado, solo `docker-compose.coolify.yml` de configuración desplegada, que no se arrancó. El servicio de generación jOOQ depende de una BD viva; no se saltó ni se fabricó código generado.
- Conclusión sin cambio: H1.S6.M2 está implementada en la ruta web, pero sigue `A MEDIAS` hasta ejecutar sus pruebas con PostgreSQL/jOOQ disponibles. M3–M5 siguen sin verificación de su DoD backend; M8 continúa `BLOQUEADO`. El test no falló por una aserción y no llegó a la fase de tests.

## Revalidación del entorno — 2026-10-13

- El servicio Windows `postgresql-x64-18` está `Running`; `pg_isready` confirma que acepta conexiones TCP en `127.0.0.1:5432`. El puerto requerido por `generateJooq`, `5433`, continúa cerrado. Un intento de `psql` sin credenciales terminó en `fe_sendauth: no password supplied`; no se buscaron/imprimieron secretos ni se alteró la instancia.
- Docker Desktop sigue sin daemon accesible (`docker ps` no conecta con `dockerDesktopLinuxEngine`).
- `flutter emulators` enumera `Pixel_2` y `Samsung_A06-L`, ambos configurados con 2048 MB; `adb devices -l` no enumera dispositivos conectados. Memoria física libre observada: 1,73 GiB. No se inició un AVD ni Gradle para evitar exceder el margen de memoria.
- Esta comprobación aporta disponibilidad parcial del servicio PostgreSQL, pero no la base/puerto/credenciales que requiere el proyecto. No cambia el estado ni el bloqueo de las microtareas backend.
