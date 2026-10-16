# H2 — Revisión de capturas «No tenés acceso a esto»

Fecha de revisión: 2026-10-15. Fuente: las seis imágenes que el usuario dejó en `evidencia/telefono-real-2026-10-15/`. Se abrieron y revisaron visualmente en esta sesión. Las imágenes se conservan en su carpeta original; este informe no las copia.

## Observación por captura

| Archivo | Pantalla/acción visible | Observación directa | Código de seguimiento visible |
|---|---|---|---|
| `WhatsApp Image 2026-10-15 at 00.27.07.jpeg` | Movimientos → Extracto | Error «No tenés acceso a esto» y botón «Volver a intentar». | `68ed99f3-738a-e2bb-f408-d11d24b6de5b` |
| `WhatsApp Image 2026-10-15 at 00.27.07 (1).jpeg` | Inicio → Tu billetera | Mismo texto y mismo código que Extracto. | `68ed99f3-738a-e2bb-f408-d11d24b6de5b` |
| `WhatsApp Image 2026-10-15 at 00.27.07 (2).jpeg` | Formulario «Organizar un grupo» | Formulario con datos ingresados; aviso «No tenés acceso a esto» aparece antes de tocar «Crear grupo». | `e7523034-2172-81d9-7d90-d591825ab3c0` |
| `WhatsApp Image 2026-10-15 at 00.27.08.jpeg` | Inicio → Tu billetera, reintento posterior | El mismo error visible con otro código. | `ea568f92-2db4-a50c-a323-f33855a09b5c` |
| `WhatsApp Image 2026-10-15 at 00.27.08 (1).jpeg` | Tutorial, paso 2 de 3 | La guía dice que el elemento de esta parte aún no está disponible y ofrece reintentar o salir. | No aparece un código legible en esta captura. |
| `WhatsApp Image 2026-10-15 at 00.27.08 (2).jpeg` | Tutorial, paso 1 de 3, sobre Inicio | Se ve el error de billetera detrás de la guía. | `b753daee-b52a-e4ae-db83-53cdd8a9dfac` |

## Qué permite concluir la evidencia

- La app presenta al menos dos superficies de billetera inaccesibles (Inicio y Extracto) y el intento de crear grupo también muestra falta de acceso. Hay cuatro códigos de seguimiento distintos visibles en el conjunto.
- La imagen no muestra el status HTTP, el cuerpo JSON ni una respuesta de servidor; por eso no identifica por sí sola el permiso denegado ni demuestra una caída del backend. El mapeador móvil traduce HTTP 403 al texto exacto «No tenés acceso a esto» (`apps/movil/lib/dominio/errores.dart:95`); la asignación concreta de 403 a cada código debe confirmarse con red/logs correlacionados, que no están disponibles desde este equipo.
- La causa de billetera hallada en el código es consistente con el síntoma: los endpoints exigen `BILLETERA_VER`, y el rol `PARTICIPANTE` otorga ese permiso (`sql/60_semillas/10-roles-y-permisos.sql`). La aprobación manual ahora asigna `PARTICIPANTE` globalmente para aprobaciones nuevas; ese cambio pasó el PR #70 y se integró en `test`. No se verificó el rol de la cuenta de las capturas ni se hizo una lectura autenticada tras el despliegue. Cuentas aprobadas antes de ese cambio no reciben la asignación automáticamente.
- Crear grupos es una autorización separada: `GruposController.crearGrupo` exige `GRUPO_CREAR`, que el catálogo concede al rol `ORGANIZADOR`. No se debe quitar ese control ni conceder `ORGANIZADOR` como parte de la aprobación de identidad.
- Contraste adicional del arreglo: el catálogo asigna `BILLETERA_VER` a `PARTICIPANTE` y `GRUPO_CREAR` solo a `ORGANIZADOR`; el esquema permite una asignación `GLOBAL` con `ambito_id` nulo y no exige que ese campo copie `rol.ambito`. Las semillas dev existentes también asignan `PARTICIPANTE` con ámbito `GLOBAL` (`sql/61_dev/15-usuarios-dev.sql`, `17-usuarios-e2e-sinteticos.sql`). La concesión automática reproduce ese patrón base y no abre la creación de grupos. Esto sigue siendo cotejo estático del contrato; sin lectura autenticada no demuestra el permiso efectivo de la cuenta de las capturas.
- El tutorial muestra como no disponible un objetivo y, en otra imagen, queda superpuesto a la superficie con error de billetera. Es compatible con un destino que no llegó a cargarse, pero las capturas no prueban la dependencia causal ni una falla propia del tutorial.

## Sonda pública de conectividad (2026-10-15)

Comandos ejecutados después de revisar las imágenes:

```text
curl.exe -sS -m 12 -i https://api.aportaya.161.97.85.216.sslip.io/actuator/health
HTTP/1.1 200 OK
{"status":"UP","groups":["liveness","readiness"]}

curl.exe -sS -m 12 -o NUL -w 'GET /api/v1/sesiones sin token: %{http_code}\n' https://api.aportaya.161.97.85.216.sslip.io/api/v1/sesiones
GET /api/v1/sesiones sin token: 401

curl.exe -sS -m 12 -i https://api.aportaya.161.97.85.216.sslip.io/actuator/info
HTTP/1.1 200 OK
{}
```

La comprobación más reciente también encontró `/actuator/info` habilitado pero vacío; el backend no expone allí el SHA/build desplegado. Esto demuestra conectividad y el 401 esperado sin token, pero no identifica el SHA de la imagen ni reproduce las llamadas autenticadas/correlaciona los IDs de las capturas.

## Correlación temporal con el arreglo de rol (2026-10-15)

Se consultó el estado remoto de solo lectura después de recibir las capturas:

```text
gh pr view 70 --repo PabloArauzCaballero/PasanakuBackend --json number,state,mergedAt,baseRefName,headRefName,headRefOid,url
{"baseRefName":"test","headRefName":"codex/fix-kyc-participante-test-2026-10-15","headRefOid":"141710ec5d43373de67819549ecb2109e4c4e6d9","mergedAt":"2026-10-15T05:14:37Z","number":70,"state":"MERGED","url":"https://github.com/PabloArauzCaballero/PasanakuBackend/pull/70"}

gh pr checks 70 --repo PabloArauzCaballero/PasanakuBackend
Bóveda, seguridad, carriles y despliegue coherentes  pass
Esquema, semillas y prueba de humo sobre PostgreSQL real  pass
```

Los nombres de archivo de WhatsApp y el reloj visible del teléfono sitúan las capturas aproximadamente a las 00:23–00:27, hora de Bolivia (UTC−4). La fusión reportada fue a las 01:14 de Bolivia. **Inferencia, no prueba de causalidad:** las capturas parecen preceder en unos 47–51 minutos a la fusión del arreglo que asigna `PARTICIPANTE` en aprobaciones nuevas. No hay despliegue backend de GitHub listado para ese cambio, y `/actuator/info` sigue sin SHA; por tanto no se puede afirmar que el servicio atendiendo al teléfono ya ejecutaba ese commit.

El código de `EmitirAcceso.ejecutar` consulta las asignaciones vigentes al emitir cada JWT. `SesionController.renovarSesion` vuelve a emitir el acceso a partir de las asignaciones vigentes en ese momento. Así, tras desplegar el arreglo, una renovación/reinicio de sesión puede llevar `BILLETERA_VER` **solo si esa cuenta ya tiene una asignación `PARTICIPANTE`**; las cuentas aprobadas antes de la fusión no reciben el rol retroactivamente por este cambio. La ruta de crear grupo sigue exigiendo `GRUPO_CREAR`, concedido a `ORGANIZADOR`; no se amplió ese permiso.

Esto mejora la hipótesis para las capturas de billetera, pero no las cierra: siguen sin verificarse los 4 IDs contra HTTP/logs, el rol real del usuario, la revisión de una cuenta preexistente ni una respuesta autenticada del backend desplegado. La captura de crear grupo requiere comprobar por separado la elegibilidad/rol de organizador; el tutorial requiere una repetición con la pantalla real cargada.

## Traducción cliente/UI del HTTP 403 (pruebas focalizadas, Flutter 3.47.5)

Corridas locales con respuestas **simuladas**; verifican únicamente el contrato cliente y el estado UI, no el servidor remoto:

Comando desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/unidad/cliente_test.dart
```

Salida:

```text
00:00 +0: loading C:/Users/leonardo/Repositories/pasanaku/PasanakuBackend/apps/movil/test/unidad/cliente_test.dart
00:00 +0: un 403 llega traducido, con traza y sin el mensaje del backend
00:00 +1: sin red llega como ErrorDeRed
00:00 +2: All tests passed!
```

Comando desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/widget/pantalla_de_saldo_test.dart
```

Salida:

```text
00:00 +0: loading C:/Users/leonardo/Repositories/pasanaku/PasanakuBackend/apps/movil/test/widget/pantalla_de_saldo_test.dart
00:00 +0: cargando: lo dice mientras espera, sin pantalla en blanco
00:00 +1: éxito: muestra el saldo que respondió el contrato, sin recalcular nada
00:00 +2: vacío: una cuenta en cero dice por qué no hay nada y qué hacer
00:00 +3: error: mensaje humano, reintento a la vista y traza para soporte
00:00 +4: sin red: no se queda cargando para siempre, y dice que no hay conexión
00:00 +5: un 403 no dice más que «no tenés acceso»
00:00 +6: reintentar vuelve a pedir el saldo al servidor: nunca se ajusta en memoria
00:00 +7: refrescar emite haptica de seleccion
00:00 +8: movimientos del servidor conservan orden y muestran los posteriores juntos
00:00 +9: All tests passed!
```

```text
VEREDICTO:   PASS (mapeo y widget con doble); no acredita reproducción contra TEST
Peldaño:     TESTED
Superficie:  Mobile — unidad del cliente y widget de billetera
Demuestra:   una respuesta 403 entregada al cliente se traduce al mensaje genérico y la pantalla lo muestra sin filtrar el backend
No cubierto: status real de los cuatro códigos de seguimiento, HTTP con token real, rol de la cuenta de las capturas, persistencia/permisos en TEST ni ejecución en AVD
```

## Veredicto y pendiente

```text
VEREDICTO:   BLOCKED (reproducción parcial; síntoma observado, causa HTTP por código de seguimiento sin confirmar)
Peldaño:     DISCOVERED
Superficie:  Mobile — capturas del teléfono aportadas por el usuario
Comando:     inspección visual de las seis imágenes listadas arriba
Salida:      cuatro códigos de seguimiento; errores de billetera, creación de grupo y objetivo de tutorial no disponible
Demuestra:   qué pantallas muestran el mensaje y qué códigos se pueden buscar en logs
No cubierto: status/cuerpo HTTP por código, logs del backend, rol real de la cuenta, prueba de billetera tras renovar sesión, ni reproducción Flutter/backend en AVD
```

La cuenta existente ya se autenticó en AVD y el texto se reprodujo en `Tu billetera`; ver [evidencia autenticada](./h2-reproduccion-cuenta-existente-avd-2026-10-15.md). Para cerrar H2.S1.M1 todavía hacen falta logs de TEST para correlacionar los códigos/status HTTP, verificar el permiso efectivo de esa cuenta y probar por separado Extracto y las demás condiciones de ingreso/alta. La creación de grupo requiere cuenta autorizada con `ORGANIZADOR`; no debe probarse ni concederse ese rol a esta cuenta sin esa autorización.

## Reanudación: logging seguro y límites del alta nueva (2026-10-15)

- Se agregó `apps/movil/lib/dominio/diagnostico_api.dart`: en builds debug registra método, ruta canonizada, status HTTP, código API y traza para cada respuesta no-2xx. No registra cuerpo, query, credenciales, bearer ni IDs dinámicos; en release no emite estos eventos. El mapeo conserva la misma UI.
- Verificación local: `dart analyze --fatal-infos lib/dominio/cliente.dart lib/dominio/diagnostico_api.dart` → `No issues found!`; `flutter test --no-pub --concurrency=1 test/unidad/cliente_test.dart` → `2/2 All tests passed!`; APK debug compilada contra el host TEST.
- Intento de confirmar la regla desde backend: `gradlew --no-daemon :servicios:nucleo-financiero:webTest --tests '*CuentaPropiaWebTest'` no llegó al test; `generateJooq` falló porque `127.0.0.1:5433` rechaza conexiones. No se inició Docker ni se tocó la PostgreSQL ajena de `5432`.
- Se lanzó la APK en `Samsung_A06-L`, pero la sesión llegó al límite preventivo de RAM (14,90/15,32 GiB); AVD y Flutter se cerraron y los datos locales de la app se limpiaron. No se capturó una respuesta API autenticada en esta corrida ni una línea `API rechazo`; por tanto, no se afirma aquí un nuevo HTTP/código de error.
- No se creó otra cuenta: [despliegue/TEST.md](../../../../despliegue/TEST.md) documenta que el alta normal exige OTP por correo y TEST no tiene un buzón alcanzable. No se enviaron datos sintéticos al backend remoto. Flutter Secure Storage también registró un fallo de descifrado y fallback en el AVD; es un aviso separado, sin evidencia de que causara el 403. El AVD quedó apagado; no hay `qemu`, Gradle ni Flutter activos.
- Repetición posterior con la APK debug recompilada contra TEST: la cuenta existente volvió a entrar y la billetera volvió a mostrar “No tenés acceso a esto” y el botón “Volver a intentar”. `flutter attach` se conectó antes de tocar el reintento, pero no emitió `API rechazo`; tampoco se encontró esa línea en logcat. El HTTP/código real, por tanto, siguen sin probarse; el mensaje visual por sí solo no demuestra 403. Esta corrida no creó cuenta nueva y no cambia el bloqueo de OTP.
- RAM con el tope autorizado actualizado por el usuario a 15,1 GiB: AVD/attach apagados preventivamente al medir 14,64/15,32 GiB; tras apagar AVD y detener el daemon Gradle de esta compilación quedaron 9,20/15,32 GiB usados. Sin procesos QEMU, Flutter, Dart ni Java/Gradle activos.

## Respuesta real capturada en AVD (2026-10-15)

El reingreso a la app ocurrió antes de la llamada observada; por tanto, no es solo un token antiguo en memoria local. La semilla enlaza `PARTICIPANTE` con `BILLETERA_VER`; `RevisionRepositorio` asigna ese rol durante la aprobación KYC cuando el usuario pasa de `PENDIENTE_VERIFICACION` a `ACTIVO`. No hay backfill para una cuenta que ya estuviera activa antes de ese cambio. Esto vuelve más probable que a la cuenta le falte la asignación persistida, aunque requiere consulta de solo lectura en identidad para confirmarlo.

Se cambió el logger debug de `developer.log` a `debugPrint` (sigue gated por `kDebugMode`) porque el primero no aparecía en la consola Android/`flutter attach`. Con `flutter run`, ya conectado antes de autenticar y abrir la billetera, se capturó:

```text
API rechazo GET /billetera/cuentas/propia HTTP 403 codigo=AP-SEG-01 traza=3b5d9532-77ab-b3da-5653-c6f11bb86fec
```

La query `moneda=BOB` se omite intencionalmente del registro; la pantalla envía ese valor. `BilleteraController.consultarCuentaPropia` exige `@Permiso("BILLETERA_VER")` y `CuentaPropiaWebTest.sinPermiso` comprueba que la misma ruta devuelve 403 sin ese permiso. El mensaje visible, por tanto, corresponde a un rechazo de autorización de la ruta; no es la respuesta `422 AP-CU13-01` de “billetera todavía no activa”.

Conclusión: el token/sesión de esta cuenta no tiene autorización efectiva para `BILLETERA_VER` (o el servidor está validando una asignación/rol anterior). La captura por sí sola no determina si falta la asignación `PARTICIPANTE` persistida, si el JWT quedó obsoleto o si TEST corre una versión anterior; hace falta consultar el rol efectivo y el SHA/despliegue de TEST. No otorgar permisos ni recrear/eliminar la cuenta sin verificar su estado de aprobación. La traza identificadora para buscar en logs de TEST es `3b5d9532-77ab-b3da-5653-c6f11bb86fec`.

Verificación local: `dart format --output=none --set-exit-if-changed` y `dart analyze --fatal-infos lib/dominio/cliente.dart lib/dominio/diagnostico_api.dart` pasaron; `flutter test --no-pub --concurrency=1 test/unidad/cliente_test.dart` pasó 2/2 (incluye el 403 simulado, cuyo código de fixture es `AP-SEG-03`, no el real); `flutter run ... --no-pub` recompiló e instaló la APK debug y capturó la respuesta real anterior. `CuentaPropiaWebTest.sinPermiso` en fuentes afirma 403 sin `BILLETERA_VER`; intenté correrla con `-x generateJooq`, pero no compiló porque las clases jOOQ no estaban generadas. La ejecución normal ya había fallado antes al no existir PostgreSQL en `127.0.0.1:5433`. TEST `/actuator/health` respondió `UP`; `/actuator/info` respondió `{}`, sin SHA desplegado. El AVD se cerró en 14,45/15,32 GiB; tras parar el run, AVD y daemons Gradle/Kotlin, la medición final fue 8,56/15,32 GiB. No quedó emulador activo.

Prueba adicional de la guardia: `gradlew --no-daemon :plataforma:comun-web:webTest --tests '*GuardiaDePermisoWebTest'` pasó **12/12**; el reporte prueba específicamente 403 `AP-SEG-01` en acceso denegado.

## Prueba de regresión para el registro seguro (2026-10-15)

Se agregaron pruebas de `diagnostico_api.dart` para comprobar que los IDs de cuenta y grupo se canonizan, las rutas desconocidas se omiten, no aparecen query params y el contexto útil (`HTTP`, código y traza) se conserva. La primera compilación del test reveló un tipo de callback no exportado por el SDK; se corrigió el tipo de la variable de sustitución y se repitió la prueba.

Comando desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter=expanded test/unidad/diagnostico_api_test.dart
```

Salida final literal:

```text
00:00 +0: loading C:/Users/leonardo/Repositories/pasanaku/PasanakuBackend/apps/movil/test/unidad/diagnostico_api_test.dart
00:00 +0: canoniza el ID de cuenta y conserva el contexto de soporte
00:00 +1: canoniza rutas de grupo y omite por completo las desconocidas
00:00 +2: All tests passed!
```

`dart format --output=none --set-exit-if-changed lib/dominio/diagnostico_api.dart test/unidad/diagnostico_api_test.dart` informó 0 archivos reformateados. `dart analyze --fatal-infos lib/dominio/diagnostico_api.dart test/unidad/diagnostico_api_test.dart` → `No issues found!`, exit code 0. Estas pruebas protegen el saneamiento del log; no prueban el rol efectivo de la cuenta en TEST ni cierran H2.S1.M1.

## Lectura remota del rol de la cuenta (2026-10-15)

Con la clave SSH ya autorizada por el usuario, consulté en solo lectura la base `pasanaku` del contenedor `aportaya-postgres` de TEST. La consulta se ejecutó dentro de `BEGIN READ ONLY` y terminó con `ROLLBACK`; se filtró únicamente por el teléfono terminado en `67774016`, sin seleccionar nombre, correo, documentos ni secretos.

Salida relevante:

```text
USUARIO|PENDIENTE_VERIFICACION|NINGUNO
KYC|EN_REVISION
ROLLBACK
```

La consulta de asignaciones no devolvió filas: la cuenta no tiene un rol asignado vigente, incluido `PARTICIPANTE`. Esto explica directamente el 403 `AP-SEG-01` de `GET /billetera/cuentas/propia`: su KYC sigue en revisión, por lo que todavía no corresponde otorgarle el rol de participante. No se modificó ninguna fila ni se intentó aprobar o rechazar el expediente.

No aparecieron las trazas `3b5d9532-77ab-b3da-5653-c6f11bb86fec` ni `dc9563f5-e681-3d11-b0d6-3b2f50cb0644` en los logs disponibles del contenedor de identidad durante la ventana consultada; esos logs no aportan una segunda correlación.

El contenedor de identidad declara `SOURCE_COMMIT=eeae182879a276c6b1fcec9b77866156c7ebe394` (imagen `sha256:a4d0f5d80bda53e1bb53ba36ae4c5e0fb30615ac2d5a3a21ab7de277cc1ba4b0`). `gh pr view 77` confirma que ese SHA es exactamente el merge de PR #77 a `test` (21:56:46Z), que agregó la búsqueda/teléfono de la cola KYC visible en la captura del usuario. `gh pr checks 77` muestra ambos checks requeridos en PASS: «Bóveda, seguridad, carriles y despliegue coherentes» (23 s) y «Esquema, semillas y prueba de humo sobre PostgreSQL real» (38 s). La comparación de GitHub `a5706a02e98309dfbad2e572a75fd91974f75fd1...eeae182879a276c6b1fcec9b77866156c7ebe394` devuelve `status=ahead`, `ahead_by=16`, `behind_by=0`; PR #70 (merge `a5706a02…`, asignación `PARTICIPANTE` al aprobar) es ancestro del código que declara la imagen de identidad desplegada. Esto confirma que TEST ya incluye ese arreglo; no respalda la hipótesis de despliegue anterior como causa del 403 actual.

**Qué queda para H2.S1.M1:** la causa de la denegación de billetera de esta cuenta sí queda establecida como ausencia justificada de rol mientras su KYC está `EN_REVISION`, y se confirmó que TEST contiene el arreglo de rol al aprobar. La cuenta aún necesita una decisión humana cuando su expediente esté completo; esta consulta no lo aprueba ni lo rechaza. La microtarea permanece `A MEDIAS` porque su DoD también pide los recorridos de alta/ingreso correcto, contraseña incorrecta y expiración de sesión, con su tabla de pantallas, códigos y HTTP. No se creó una cuenta remota ni se aprobaron, rechazaron o alteraron datos.

## Comprobación actual del stack local (2026-10-15)

Abrí Docker Desktop brevemente para comprobar el estado real, sin iniciar contenedores. La caché contiene `postgres:16` y `confluentinc/cp-kafka:7.7.1`; no contiene imagen MinIO. Los contenedores previos `aportaya-postgres` y `aportaya-kafka` siguen detenidos. La descarga de `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` ya había fallado con 401 en el intento anterior; el archivo oficial de esa versión consultado en [el archivo oficial de MinIO](https://dl.min.io/server/minio/release/linux-amd64/archive/minio.RELEASE.2025-09-07T16-13-09Z) devolvió 410 Gone al abrirlo. No se cambió la imagen fijada ni se sustituyó por una construcción comunitaria; no se arrancó un stack sin almacenamiento. Docker Desktop se cerró tras la inspección. RAM: 12,21 GiB usados con Desktop abierto y 10,84 GiB después de cerrarlo (15,32 GiB total; máximo solicitado 15,1 GiB). No quedaron contenedores AportaYa ejecutándose.
