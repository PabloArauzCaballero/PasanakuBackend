# Carril H3 (7.ª vuelta) — App móvil contra TEST real con las dos cuentas sintéticas

Peldaño: **VERIFIED_FUNCTIONAL_ONLY, PARCIAL**. Real y contra TEST: ingreso de A y de B (con segundo factor), lista de participaciones, rechazo real de
«crear grupo», enlace de invitación abierto por deep link con y sin sesión. **NO ocurrió**: grupo creado, invitación, aceptar, rechazar/revocar,
persistencia de participante/solicitud. Causa: TEST responde `422 AP-CU20-04` a toda creación de grupo (licencia sin otorgar, R-LIC-01).
Fecha: 2026-10-11 · APK: `C:\Users\Usuario\Downloads\APK-Pasanaku\aportaya-test-5-TEST.apk` (debug, TEST real, con las correcciones de abajo).
Cuentas: las de `Temp\aportaya-s\cuentas-test.txt` (fuera del repo). Teléfonos y claves enmascarados en capturas y salidas (verificado con grep).

## Bloqueo real (con evidencia): crear grupo no es posible en TEST hoy
`10-respuestas-literales-TEST-api.txt` (respuestas crudas, enmascaradas):
* `POST /sesiones` A y B sin factor → `422 AP-CU04-03`; con `factor {OTP, 000000}` → `200` (A y B).
* `POST /grupos` (A: organizador habilitado, KYC suficiente; pasa `AP-CU20-02/05`) → `422 AP-CU20-04 «El servicio no esta habilitado en este momento»`.
* `GET /licencia/alcance?servicio=GRUPO_PASANAKU` → `200 {"habilitado":false,"via":"NINGUNA","motivo":"Todavia no hay licencia otorgada: GRUPO_PASANAKU no puede ofrecerse."}`.
  Es la regla funcionando (R-LIC-01; `seeders/minimos/08-gobierno-y-licencia.json`: licencia sembrada EN_TRAMITE a propósito). No es un defecto de backend.
* Sin grupo no hay a quién invitar: `POST /grupos/{id}/invitaciones` con grupo ajeno → `422 AP-CU69-07`; `enlace/consultar` y `enlace/aceptar` con enlace
  inventado → `422 AP-CU69-05` (B). `GET /grupos/participaciones` de A y de B → `[]` antes y después: nada se persistió.
* Qué lo destraba (decisión de la persona dueña, no se tocó la base: regla 97.4): otorgar la licencia en TEST o abrir un sandbox de `GRUPO_PASANAKU`
  (cumplimiento/legal). Con eso, el recorrido A→B se repite con este APK y los comandos de `carril-H/INFORME.md`.
* Observación de backend (no corregida, otro servicio): `AP-CU69-05` se usa para causas distintas (revocada, vencida, ya usada, reglamento cambiado,
  «tenés que aceptar el reglamento»); la app no puede distinguirlas. `AP-CU69-10` (ingreso por enlace cerrado si `humana-obligatoria`): la app solo usa
  `enlace/aceptar`, no el canje `canjearInvitacion`; con ese interruptor encendido (valor de producción) B no podría entrar por la app.
* La app no tiene «Rechazar» para B (solo aceptar); el rechazo es por revocación del organizador (`POST /grupos/{g}/invitaciones/{i}/revocacion`, sin pantalla)
  y no hay pantalla de miembros para A.

## Recorrido con UI (un solo emulador, A y luego B en sesiones separadas: la RAM libre rondó 2,3–4,6 GB y el guardián esperó ~45 min)
| Paso | Captura | Resultado |
|---|---|---|
| A: arranque, bienvenida, login | `01`–`04` | OK |
| A: ingreso → pide segundo factor (6 casillas) → `000000` → inicio | `05`, `06`, `07` | OK real contra TEST |
| Inicio sin billetera | `08` (A), `21` (B, oscuro) | «Algo salió mal de nuestro lado»: ver D4 |
| A: pestaña Grupos (participaciones `[]` real → vacío con «Organizar un grupo nuevo») | `09` | OK |
| A: formulario Organizar un grupo (claro / oscuro) | `10`, `11`, `14` / `18` | OK; oscuro legible |
| A: Crear grupo → rechazo real | `15`–`17` ANTES (no se veía nada), `31` DESPUÉS | ver D1 |
| B: login oscuro, ingreso | `20`, `21` | OK |
| B: enlace `aportaya://unirse/<uuid>.<hex64>` (formato válido, inexistente) con sesión | `22` ANTES | rechazo real 422, texto genérico |
| B: mismo enlace sin sesión → Ingresar → login → segundo factor → vuelve a la invitación | `33`, `34`, `35` DESPUÉS | OK: el retorno funciona y dice el motivo (D2) |

Nota: tras un `uiautomator dump` Android pinta un anillo lima; es del arnés. Capturas inspeccionadas con Read una por una. Tema oscuro: `18`, `20`, `21`, `22`.

## Defectos de `apps/movil` hallados contra TEST real y corrección
| # | Defecto | Corrección | Prueba |
|---|---|---|---|
| D1 | «Crear grupo»: el rechazo del servidor (`422 AP-CU20-04`) no se mostraba en ningún lado: el botón dejaba de girar y no pasaba nada (`15`–`17`). | `organismos_crear_grupo.dart`: `Alerta` de error sobre la barra de acción con el mensaje del catálogo y el código de seguimiento | `test/pasanaku/pantalla_crear_grupo_test.dart` «rechaza el servidor (422 AP-CU20-04)…»; captura `31` |
| D2 | Enlace de invitación rechazado (`AP-CU69-05`): un único texto «No pudimos verificar la invitación» (`22`). | `pantalla_unirse.dart` usa el texto del código si la app lo conoce; `errores.dart` agrega CU-20 (01–05), CU-69 (01,03,04,05,07,08,09,10), CU-13-01 y `tieneMensajePropio` | `test/unidad/errores_grupos_test.dart` (4 casos), `pantalla_unirse_test.dart` (422 dice «ya no es válida»; un 500 conserva el texto general); captura `35` |
| D3 | Aporte en cero («00,00») habilitaba «Crear grupo» (`12`). | `pantalla_crear_grupo.dart`: `completo` exige un dígito 1–9 | `pantalla_crear_grupo_test.dart` «límite: un monto en cero deja el botón apagado»; captura `30` |
| D4 | Inicio de billetera: la app consulta `/billetera/<id fijo 1111…>/saldo` (`_cuentaPorOmision` en `pantallas/billetera/rutas.dart`) en vez de la cuenta de la persona; TEST responde `422 AP-CU13-01 «Esa cuenta no existe»` y la app lo mostraba como falla del servidor. | Solo el texto (`AP-CU13-01`). **El id fijo NO se corrigió**: existe `GET /billetera/cuentas/propia?moneda=BOB` en el contrato y el archivo lo posee el carril de billetera/dinero. | `errores_grupos_test`; `08`/`21` son el estado ANTES |

## Registrado, NO corregido
* Segundo factor: pantalla con título y 6 casillas, sin explicación ni botón, con la barra inferior visible y «Perfil» marcado (`05`, `34`).
* Ingreso desde el enlace: sin flecha de volver (`33`). Invitación rechazada: solo «Reintentar», sin salida (`35`).
* El tutorial dice «No encontramos «billetera.saldo» en esta pantalla» (id interno a la vista; `07`).
* «Tu plata queda en custodia en Banco Unión» (pantalla de ingreso): afirmación comercial que no pude verificar; confirmar con cumplimiento.
* Tipeo por adb a ritmo de máquina pierde dígitos (artefacto del arnés): se tecleó de a uno.
* Compartir enlace (sin plugin), pestaña «Grupos» sin detalle de grupo, `integration_test` Patrol, cámara/KYC: sin cambios respecto de H y H2.

## Comandos y resultados
* `flutter analyze --no-pub` (apps/movil) → `No issues found`. `flutter test` dirigido (errores_grupos, errores_identidad, pantalla_unirse, pantalla_crear_grupo,
  acciones_de_grupo, invitacion_creada, unirse_segundo_enlace) → `All tests passed` (23). Salida en `20-tests-flutter.log`. No corrí la versión sin arreglo de los tests nuevos.
* `flutter build apk --debug` con los dos defines → OK; copiado como `aportaya-test-5-TEST.apk` + LEEME. Daemon Gradle detenido.
* Emulador `aportaya_a` (5554, API 36, 2 GB) por `heavy.sh` (MAX_WAIT_S=3000, MIN_FREE_MB 3500; no hizo falta bajar el piso). `adb emu kill` y `adb kill-server` al terminar.
* No cubierto: dos emuladores a la vez, aceptar/rechazar/revocar, persistencia en TEST, efecto en A, dinero/billetera, push, cámara, release, regresión completa de `flutter test`.
