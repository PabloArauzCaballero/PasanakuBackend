# Carril H (4.ª vuelta) — App móvil Flutter en emuladores Android

Peldaño de evidencia: **VERIFIED_FUNCTIONAL_ONLY contra un DOBLE** (no contra el backend real).
Fecha: 2026-10-10 · App: `apps/movil` · APK: `C:\Users\Usuario\Downloads\APK-Pasanaku\aportaya-test-3-debug.apk`
(debug, compilado con `--dart-define=API=http://10.0.2.2:4010/api/v1` = el doble con estado descripto abajo;
no sirve contra el stack real ni contra TEST).

## Qué backend se usó (y por qué)

* El stack real de G (`aportaya-d3`, gateway `127.0.0.1:18086`) estuvo arriba a ratos. Lo probé con curl (login real de un
  organizador sintético devuelve `422 AP-CU04-03`, que la app ya traduce a «segundo factor») y lo bajaron otros carriles
  mientras corrían mis emuladores: la captura `04-A-login-error-sin-conexion-stack-real-caido.png` es de ese momento
  (estado «sin conexión» real de la app). No pude sostener un recorrido completo contra el stack real.
* Recorrido completo: **doble con estado en Node** (`scratchpad/h/doble_con_estado.mjs`, fuera del repo) sobre el simulado
  Prism de `packages/simulado`. Implementa en memoria solo: `POST /sesiones`, `POST /grupos`, `POST /grupos/{id}/invitaciones`,
  `GET /grupos/participaciones`, `POST /grupos/invitaciones/enlace/{consultar,aceptar}`; el resto lo responde Prism (ejemplos
  estáticos: por eso el saldo «Bs 1.240,00» y «Bs 4,44» de las pantallas son datos del ejemplo, no de un mayor).
  Usuarios sintéticos `+59170000001` (A, organizadora) y `+59170000002` (B, invitado). La revocación (rechazo) se hace por
  `POST /__revocar` del doble porque la app no tiene pantalla para revocar.
* Los efectos «reales» (acreditar, KYC, reglamento oficial, invitación SMS/WhatsApp) NO se ejercitaron.

## Emuladores

Dos AVD creados a mano (no había `cmdline-tools`): `aportaya_a` (5554) y `aportaya_b` (5556), Android 16 (API 36,
google_apis_playstore x86_64), 720x1600, 2048 MB, `-no-window -no-audio -no-snapshot`, vía `heavy.sh`.
**No entraron dos a la vez** (con un emulador arriba la RAM libre bajaba de 3,5 GB y el guardián no dejaba lanzar ni el
build ni el segundo): corrí A (login, crear grupo, invitar) y después B (login desde el enlace, aceptar, rechazo) en
sesiones separadas, compartiendo el estado del doble. Un emulador murió solo (OOM del anfitrión) y lo relancé.

## Recorrido y capturas (todas inspeccionadas con Read)

| Paso | Captura |
|---|---|
| A arranca / login (sin conexión, stack real caído) | `01-A-arranque`, `02`, `03`, `04-...sin-conexion...` |
| A login contra el doble: formulario, enviando, inicio | `05`, `06`, `07-A-tras-login-doble` (ANTES) · `31`, `32`, `33`, `60` (DESPUÉS) |
| A crea grupo desde la UI (botón habilitado, creando, éxito) | `35`, `36`, `62-A-grupo-creado-tarjeta` |
| A invita a B: formulario, enviando, éxito + enlace, «Copiado» | `12`, `15` (ANTES) · `39`, `63`, `64`, `40`, `41`, `42` (DESPUÉS) |
| B abre el enlace sin sesión → ingresa → vuelve a la invitación | `20`, `23` (ANTES) · `50`, `51`, `52` (DESPUÉS) |
| **B ACEPTA** (casilla, enviando, éxito) | `24`, `25`, `26` (ANTES) · `53`, `54`, `55` (DESPUÉS); bitácora del doble: `ACEPTADA por +591…0002 -> participante …` |
| **RECHAZO** (invitación revocada por la organizadora) | `28` (ANTES) · `57`/`56` claro/oscuro (DESPUÉS); bitácora: `ENLACE REVOCADA rechazado a +591…0002` |
| Segundo enlace con la app abierta | `65` → `66` (muestra el 2.º grupo; ANTES se quedaba con el 1.º, ver D8) |
| Efecto en A de la aceptación | Solo en el estado del doble (`GET /__estado`: B figura como `PARTICIPANTE`). La app no tiene pantalla de miembros del grupo para el organizador: **no hay efecto visible en la pantalla de A** (hueco de producto, no se inventó). |

Nota de captura: tras un `uiautomator dump` Android dibuja un anillo lima de foco de accesibilidad alrededor de la pantalla;
es del arnés de prueba, no de la app (sin dump previo la captura sale limpia).

## Defectos hallados y corrección (con prueba)

| # | Defecto (evidencia) | Corrección | Prueba |
|---|---|---|---|
| D1 | `10.0.2.2` rechazado: «AportaYa no puede arrancar» (`validarGateway` solo admitía loopback; contra `entregables/defines-por-plataforma.md`) | `dominio/configuracion.dart`: `10.0.2.2` cuenta como host de desarrollo (OK en debug, rechazado en release incluso listado) | `test/unidad/configuracion_test.dart` (2 casos nuevos) |
| D2 | Android 9+ bloquea http en claro: sin esto toda llamada al simulado falla | `android/app/src/debug/AndroidManifest.xml`: `usesCleartextTraffic` SOLO en debug | APK instalado y funcionando |
| D3 | «Crear grupo» no se habilitaba nunca (`completo` exigía `fechaDeInicio`, que solo se asigna al apretar el botón) — `10-A-crear-grupo-lleno` (apagado) vs `35` (activo) | `pantalla_crear_grupo.dart` | `test/pasanaku/pantalla_crear_grupo_test.dart` |
| D4 | Ninguna pantalla llevaba a crear grupo ni a invitar (rutas huérfanas) | `acciones_de_grupo.dart` en la pestaña Grupos («Organizar un grupo nuevo», «Invitar a alguien a mi grupo») | `test/pasanaku/acciones_de_grupo_test.dart` |
| D5 | Tutorial de bienvenida con texto subrayado en amarillo (Text sin Material) — `07` vs `60` | `capa_de_tutorial.dart`: `Material(type: transparency)` | `test/widget/tutoriales/motor_test.dart` (ancestro Material) |
| D6 | Éxito de invitación: texto plano, «Invitacion» sin tilde (venía del doble), enlace crudo, botón sin confirmación — `15` vs `41`/`42` | `invitacion_creada.dart`: tarjeta con ícono, título «Invitación creada», vigencia, enlace en caja monoespaciada, «Copiar»→«Copiado» + aviso, «Invitar a otra persona»; cabecera común (`CabeceraDeSeccion`) | `test/pasanaku/invitacion_creada_test.dart` |
| D7 | «Se levanta pagando 4.44» (punto, sin moneda) junto a «Bs 4,44» — `11`/`27` | `pantalla_mi_estado.dart`: todo con `formatearMonto` + espacio sin corte (antes «Bs» quedaba solo al final de línea) | `test/pasanaku/pantalla_mi_estado_montos_test.dart` |
| D8 | Con la app abierta en una invitación, un segundo enlace no consultaba el nuevo (mismo estado reutilizado) — `28` | `rutas.dart`: `PantallaUnirse` con `ValueKey(codigo)` | `test/pasanaku/unirse_segundo_enlace_test.dart` + `65`→`66` |
| D9 | Pantalla de la invitación (lado B) sin jerarquía: «100.00 BOB … MENSUAL» crudo, casilla desalineada, botón casi invisible — `23`/`26`/`28` | `pantalla_unirse.dart` rediseñada con tarjetas, importe «Bs 100,00 por mes», `Casilla`, `BarraDeAccion`, `Alerta` de error — `52`,`55`,`57` | `test/pasanaku/pantalla_unirse_test.dart` (3 casos: aceptar con casilla, rechazo 422, sin sesión) |
| D10 | Ingreso: el celular exigía tipear `+591` a mano (el alta lo trae puesto) y «Ingresar» quedaba apagado sin decir por qué — `02` | `pantalla_sesion.dart`: prefijo `+591` fijo, 8 dígitos | `test/identidad/pantalla_sesion_prefijo_test.dart`; `21`/`51` |
| D11 | Formulario de invitar: etiquetas inconsistentes, teléfono con `+591` manual, botón fuera de la barra de acción — `12` | `pantalla_invitar.dart`: `Pregunta` + prefijo + `BarraDeAccion` — `39`/`63`/`64` | cubierto por D6/D4 + captura (sin test propio del formulario) |
| D12 | «Grupo creado» (éxito): texto crudo «Fondo por período: 0.00» — `37` | `organismos_crear_grupo.dart`: tarjeta de éxito, importe formateado, botón «Invitar» — `62` | captura (sin test propio) |
| D13 | Tests de `apps/movil` rotos antes de tocar nada: `paso_contrasena_test` no compilaba (faltaba `import http_mock_adapter`), `paso_datos_test` esperaba SMS por omisión (es CORREO), `enchufe_de_rutas`/`gate_del_shell` fallaban por separador `\` de Windows | arreglos en los tests (sin debilitar aserciones; el de canal pasó a esperar `CORREO`, que es el valor real) | `03` vs `18` |

## Registrado, NO corregido

* `flutter test`: 9 goldens (`pantalla_de_saldo`, `pantalla_escanear` ×6, `transiciones`) difieren en píxeles (3–4 %): los
  PNG de referencia son de otra plataforma/fuente (Mac/CI). ENVIRONMENT; no se regeneraron para no esconder nada.
* `flutter analyze`: 1 info preexistente (`cu02_subir_foto.dart:65 use_null_aware_elements`).
* **Compartir** del enlace: no hay plugin de share en `pubspec.yaml`; agregarlo es una dependencia nueva (regla 90.4).
  Hoy solo «Copiar».
* La pestaña «Grupos» se titula «Estado de tu aporte» también cuando la persona organiza un grupo: es diseño actual
  (muestra el estado de aportes del participante, no el grupo); no hay endpoint de detalle de grupo para listarlo.
  «Invitar a alguien» aparece a cualquier participante; el servidor decide (el doble responde 403 al no organizador).
* El rechazo se muestra con un mensaje único («No pudimos verificar la invitación…»): la app no distingue revocada /
  vencida / de otro teléfono (todas llegan 422 sin código propio en el catálogo).
* Registro con cámara/documento: no ejercitado (el emulador no tiene cámara). `integration_test` (Patrol, 7 archivos):
  NO ejecutados en Android — el proyecto no tiene `android/app/src/androidTest` ni `RunnerTest` de Patrol (ver
  `integration_test/LEEME.md`), necesitan `patrol_cli` + esa infraestructura nativa.
* Tema oscuro: verificado solo en la pantalla de rechazo de B (`56`).
* `build_runner` AOT lo bloquea el Control de aplicaciones de Windows (`gen_snapshot.exe`): se generaron los clientes Dart
  con `--force-jit` (`scratchpad/h/clientes_jit.sh`). `packages/diseno_flutter/lib/tokens/tokens.dart` (gitignored) se copió
  de `packages/tokens/generado/`.

## Comandos y resultados

* `flutter analyze` → `19-analyze-final3.log`: 1 issue (info preexistente).
* `flutter test` → `18-flutter-test-final3.log` (277 pasan, 9 fallan = goldens): ver líneas finales; **todos pasan salvo los 9 goldens**.
* `flutter build apk --debug` → `20-build-apk-final3.log`: OK (es el APK entregado; un 4.º intento de rebuild por un retoque de texto venció por RAM y se revirtió el retoque).
* Apagado: emuladores (`adb emu kill`), daemon Gradle (`gradlew --stop`), Prism y doble (procesos node) al terminar.
