# Carril H2 (6.ª vuelta) — App móvil Flutter contra el servidor TEST real

Peldaño de evidencia: **VERIFIED_FUNCTIONAL_ONLY, PARCIAL** contra TEST real (pantallas de ingreso, alta y enlace de invitación con
respuestas reales del servidor). **El recorrido A organiza/invita, B acepta/rechaza NO ocurrió**: no hay ninguna cuenta utilizable en TEST.
Fecha: 2026-10-11 · APK: `C:\Users\Usuario\Downloads\APK-Pasanaku\aportaya-test-4-TEST.apk` (debug, `API=https://api.aportaya.161.97.85.216.sslip.io/api/v1`
+ `HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io`; con https no hace falta el cleartext de debug; el host ajeno exige la lista de
hosts, por eso el segundo define).

## Por qué no hubo dos usuarios reales (BLOQUEADO, con evidencia)
* Alta (`POST /usuarios`) exige `verificacionCorreoId` ya CONFIRMADO con el OTP de 6 dígitos del correo. `POST /usuarios/verificaciones/correo`
  responde 202 pero el código no viaja en la respuesta (`SolicitarVerificacionCorreo.java`) y no hay buzón alcanzable: correos sintéticos
  `@example.com` (dominio reservado). TEST corre `NOTIFICATION_EMAIL_PROVIDER` fijado en Coolify (no visible desde acá); si fuera `local`
  el código no se entrega a nadie; si fuera `gmail_api`, llega a un buzón que no existe. Forzar el código (1 000 000 combinaciones, 5 intentos)
  sería un ataque, no una prueba: no se intentó. Ver `10-respuestas-literales-TEST.txt`.
* El segundo factor de dev `000000` (`DesafioDeDesarrollo`, perfil `local`) sirve solo para quien YA tiene cuenta y la contraseña.
* Cuentas sembradas: `seeders/dev/15-usuarios-dev.json` es la única con hash real y lleva correos personales (regla 90.2): no se usó. Las demás
  semillas llevan hashes de relleno (no autentican).
* Qué destraba: (a) una persona con el buzón o el secreto de Gmail de TEST, o (b) un alta sembrada con contraseña sintética en TEST
  (decisión de la persona dueña del entorno, regla 97.4: no se inserta a mano). Con cualquiera de las dos, el recorrido de `carril-H/INFORME.md`
  (que ya pasó contra el doble) se repite tal cual con este APK.

## Qué sí se ejercitó contra TEST real (capturas inspeccionadas con Read)
| Paso | Captura | Resultado |
|---|---|---|
| Arranque con la API de TEST (config válida, sin pantalla de bloqueo) | `01` | OK |
| Ingreso con celular sintético inexistente → rechazo real `422 AP-CU04-01` | `03`, `04` (ANTES), `21` (DESPUÉS) | ver D1 |
| Alta paso 1 (datos), selector de departamento y de fecha | `05`–`08`, `22`, `23` | ver D2, D3 |
| Alta paso 2 (contraseña) → paso 3: la app pidió el OTP real (`202`) y mostró `h2***@example.com` | `10`, `11` | OK |
| Código de correo incorrecto → rechazo real `422 AP-CU01-09` | `12` (ANTES), `24` (DESPUÉS) | ver D1 |
| Enlace `aportaya://unirse/…` mal formado / bien formado sin sesión | `13`, `14` | OK (pide ingresar / crear cuenta; no consulta al servidor sin sesión) |

## Defectos hallados contra TEST real y corrección
| # | Defecto | Corrección | Prueba |
|---|---|---|---|
| D1 | El catálogo de errores de la app no coincidía con los códigos reales de identidad: credencial inválida (`AP-CU04-01`) decía «El código no coincide» (`04` y `05` cruzados: «Demasiados intentos» estaba en 04, que es el código del factor); el rechazo del OTP de correo (`AP-CU01-07..11`) caía en el genérico «Algo salió mal de nuestro lado» (`12`). | `apps/movil/lib/dominio/errores.dart`: textos propios para CU04-01/02/04/05/06/07 y CU01-07..11 | `test/unidad/errores_identidad_test.dart` (5 casos: correcto/límite/inválido) y `test/identidad/autenticar_test.dart` (aserción alineada al contrato real: 01 = credencial, no «código»); capturas `21`, `24` |
| D2 | Fecha de nacimiento: se elegía «11 oct 1990» y el campo mostraba «10 oct 1990» (`08`) en todo teléfono fuera de UTC-4 (`Fecha.formatear` resta 4 h a una fecha de calendario). En un dato que se cruza con el documento (KYC) es grave. | `packages/diseno_flutter/lib/atomos/fecha.dart` (`formatearDia`) y `moleculas/campo_de_fecha.dart`. Fuera de `apps/movil`, mínimo, declarado. | `packages/diseno_flutter/test/unidad/logica_pura_test.dart` (4 aserciones); captura `23` («11 oct 1990») |

## Registrado, NO corregido
* **D3 · selector de fecha en inglés** («Thu, Oct 11», «October 1990», `07`/`22`) con el teléfono en inglés: la app no declara `flutter_localizations`
  ni `supportedLocales`. Corregirlo toca `pubspec.yaml`/`pubspec.lock` de `apps/movil` (paquete del SDK, pero cambia el lock), que estaba reservado
  al agente N: A MEDIAS, a coordinar con N.
* El enlace de invitación con sesión vencida o sin red muestra el mismo texto único (ya registrado en carril H).
* Tiempos: el campo del OTP del emulador perdió el último dígito al teclear por adb a ritmo de máquina (artefacto del arnés; tecleando de a uno entra).
* No cubierto: aceptar/rechazar invitación, crear grupo, invitar, pestaña Grupos, tema oscuro, deep link con sesión, registro con cámara y subida de
  fotos, segundo emulador B (no tenía sentido sin cuenta), `integration_test` Patrol, push.

## Comandos y resultados
* `flutter test` dirigido (paquete: `logica_pura_test.dart`; app: errores_identidad, errores_qr, autenticar, pantalla_de_saldo ×2): todo verde, ver `20-tests-y-build.log`.
* `flutter analyze` de lo tocado: `No issues found`.
* `flutter build apk --debug` con los dos defines: OK, dos veces (antes y después de las correcciones); el APK entregado es el segundo.
* Emulador: `aportaya_a` (5554, API 36, 2 GB) por `heavy.sh`, uno solo; apagado con `adb emu kill`; daemon Gradle detenido (`gradlew --stop`).
