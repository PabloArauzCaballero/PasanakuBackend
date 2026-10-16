# Plan — Portar a la app móvil de Pasanaku los comportamientos de la app cliente de Atlas

- Fecha: 2026-09-30 · Repos afectados: Pasanaku (`apps/movil`; backend/OpenAPI solo donde se indica) · Predecesor: `docs/trabajo/2026-09-28-testflight-ios` (cerrado con REPORTE.md, 18/21; sus cambios sin commitear en `apps/movil/ios` y `lib/infraestructura` **no se tocan ni se mezclan**).
- Resultado observable: en la app Flutter de Pasanaku (a) la primera vez que la persona entra al inicio arranca sola la guía `intro-app` y puede relanzarla desde Perfil; (b) puede escanear un QR con la cámara; (c) si el permiso de cámara está denegado/bloqueado la app lo dice y ofrece abrir Ajustes; (d) un aviso push tocado abre la pantalla correcta (app cerrada, en segundo plano y en primer plano); (e) la voz de bienvenida queda resuelta según la decisión A4.
- Kill-test: `grep -rn "iniciar(" apps/movil/lib` fuera de `pantalla_centro_de_ayuda.dart` → vacío (la guía no se autolanza); `grep -rn "mobile_scanner" apps/movil/lib` → vacío (no hay escáner).
- Fuente de comparación: `atlas/AtlasFrontend/apps/consumer-app` (Expo/RN, no es repo git, **solo lectura**).

## Hechos del descubrimiento (peldaño DISCOVERED; informe de subagente de solo lectura, rutas a re-verificar al tocar cada una)
- Pasanaku YA tiene: tour de 4 láminas (`pantallas/identidad/pantalla_tour.dart`), motor de tutoriales completo (`dominio/tutoriales/*`, `proveedores/tutoriales.dart`, `pantallas/soporte/capa_de_tutorial.dart`), cámara del sistema (`infraestructura/camara_del_sistema.dart`), puerto `AvisosPush` (`dominio/puertos/avisos_push.dart`), deep links `aportaya://` con whitelist (`navegacion/enlaces_profundos.dart`), bandeja en memoria (`pantallas/notificaciones/proveedor_bandeja.dart`).
- Brechas: `intro-app` solo se inicia desde el centro de ayuda; `mobile_scanner` declarado (`pubspec.yaml:17`) sin un solo import; cámara denegada = `null` sin distinguir bloqueo (`camara_del_sistema.dart:43-47`); push: `avisos_push_android.dart` / `_ios.dart` son huecos declarados, nadie consume `toques`; no hay `firebase_*` ni `permission_handler` ni reproductor de audio en `pubspec.yaml`.
- Backend: `servicios/notificaciones/.../notificaciones.yaml` NO tiene registro de token de dispositivo, ni bandeja, ni preferencias. No existe endpoint de audio de bienvenida.
- Corrección al pedido: Atlas **ya no pide permisos al arrancar** (desde 2026-09-18 van dentro del alta) y **nunca llama a `openSettings`**; "permisos y arranque" en Pasanaku = pedir cámara/notificaciones donde se usan + salida a Ajustes (mejora, no paridad). Ubicación, contactos y rastreo de Atlas **no aplican** a Pasanaku y no se portan.

## Alcance
- IN: `apps/movil/lib`, `apps/movil/test`, `apps/movil/pubspec.yaml` (dependencias justificadas en H3/H4), y —solo para H4— el contrato OpenAPI de `servicios/notificaciones` + cliente dart regenerado.
- OUT: voz/TTS real (A4), cambios de `apps/movil/ios` del trabajo TestFlight, ubicación/contactos/rastreo, backend de tutoriales, refactors ajenos, el job `release-ios` de CI.
- Ambigüedades registradas (supuesto tomado → a quién confirmar):
  - A1 "El mismo comportamiento" se interpretó como los 4 bloques elegidos por Pablo, **adaptados** (no copia literal). → Pablo.
  - A2 Proveedor push: ADR-035 lo deja abierto (`avisos_push_android.dart:7-13`). Supuesto: FCM para Android y APNs vía FCM en iOS; **decisión de negocio/infra**, no se crea proyecto Firebase sin confirmación. → Pablo.
  - A3 El registro de token de dispositivo no tiene contrato en backend. Supuesto: se define el contrato `registrarDispositivoPush` en OpenAPI (H4.S1) y la app se cierra contra un doble (regla 65). → Pablo / dueño de `notificaciones`.
  - A4 Voz de bienvenida: depende de un TTS de Atlas ajeno a Pasanaku, y nombra a la persona (PII). Supuesto: **no se porta audio** hasta que exista servicio propio; se deja el puerto `VozDeBienvenida` con doble silencioso. → Pablo (decisión de negocio).
  - A6 El QR de Pasanaku no tenía consumidor definido; se adaptó a la invitación a un grupo (único enlace con formato estricto ya validado en el repo). Otro uso del QR (p. ej. cobro) NO se implementó. → Pablo.
  - A5 Paquetes Flutter nuevos (`permission_handler`, `firebase_core`, `firebase_messaging`, audio): nombre y versión se verifican en pub.dev/context7 antes de escribirlos (regla 00 §1.3). → agente al ejecutar.

## H0 — Línea base
**CA:** Dado el árbol actual, cuando se corren analyze y tests de `apps/movil`, entonces se conoce el baseline en rojo/verde.
**DoD:** salidas pegadas en `evidencia/`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.S1.M1 | Baseline `flutter analyze` + `flutter test` en `apps/movil` | Hay salida literal guardada | `cd apps/movil && flutter analyze && flutter test` → evidencia/h0-analyze.txt, h0-test.txt | HECHO — analyze sin issues; `+176: All tests passed!` |

## H1 — La guía `intro-app` se autolanza y se relanza desde Perfil
**CA:** Dado una persona que entra por primera vez al inicio, cuando carga la pantalla, entonces arranca `intro-app` una sola vez sin bloquear tareas; y desde Perfil puede volver a verla.
**DoD:** tests de unidad/widget en verde + captura en simulador.
**Estado:** A MEDIAS

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Política `debeAutolanzar(visto, sesionRestaurada)` pura | Devuelve true solo si no visto | `flutter test test/unidad/tutoriales/autolanzamiento_test.dart` (correcto, límite: ya visto, inválido: sin catálogo) | HECHO — `+7: All tests passed!` (evidencia/h1s1m1.txt) |
| H1.S1.M2 | Conectar en el inicio (shell/pantalla de saldo) | La primera entrada llama `motor.iniciar('intro-app')` una vez | `flutter test test/widget/tutoriales/autolanzamiento_inicio_test.dart` | HECHO — `+5: All tests passed!` contra motor real (evidencia/h1s1m2.txt). Cubre el lanzador; el montaje post-frame en `PantallaDeSaldo` solo se verificó porque sus tests existentes siguen verdes (`+16`). |
| H1.S1.M3 | Entrada "Ver guía otra vez" en Perfil | Reinicia y lanza `intro-app` | `flutter test test/widget/tutoriales/ver_guia_otra_vez_test.dart` | HECHO — `+2: All tests passed!`. Cubre la acción (provider); el botón en `PantallaDePerfil` compila pero no tiene test de widget propio. |
| H1.S1.M4 | Prueba visual (3 viewports, claro/oscuro) | Capturas inspeccionadas | `evidencia/h1-*.png` mirados | A MEDIAS — Qué anda: lanzador y botón de Perfil probados por lógica (H1.S1.M2/M3). Qué no anda/falta: no hay capturas de la guía superpuesta ni del botón en Perfil, ni corrida en simulador con sesión (el mock Prism no completa el login, ver memoria del proyecto). Falta: correr la app contra el backend TEST y capturar en 3 viewports. Dónde quedó: código compila, tests en verde. |

## H2 — Escáner QR
**CA:** Dado la pantalla de escaneo, cuando la cámara lee un QR, entonces se entrega el payload a un único manejador (cerrojo anti multi-lectura), la cámara se apaga al perder foco y hay entrada manual alternativa. Nada se acredita por escanear (regla 91.3); el payload no se loguea (90.2.1).
**DoD:** tests + simulador.
**Estado:** A MEDIAS

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Puerto `EscanerQr` + doble (correcto / doble lectura / payload inválido) | Cerrojo entrega 1 sola vez | `flutter test test/unidad/lectura_de_qr_test.dart` | HECHO — `+16: All tests passed!` (evidencia/h2s1m1.txt). Desvío: el QR se define como la invitación `aportaya://unirse/<uuid>.<hash>` (reusa `enlaces_profundos.dart` + `retornoDeInvitacion`); el puerto es el visor, no el parser (ver M3). |
| H2.S1.M2 | Adaptador `mobile_scanner` (API verificada en la versión instalada) | Compila y arranca | `flutter analyze` exit 0 | HECHO — `No issues found!` API verificada en `mobile_scanner-7.4.1/lib/src` (errorBuilder, MobileScannerErrorCode.permissionDenied, BarcodeFormat.qrCode). Solo RUNS: no se probó con cámara real. |
| H2.S1.M3 | Pantalla de escaneo con 4 estados + entrada manual | Cargando/datos/vacío/error | `flutter test test/widget/pantalla_escanear_test.dart` | HECHO — `+6: All tests passed!` con visor doble (evidencia/h2s1m3.txt). |
| H2.S1.M4 | Ruta `/pasanaku/escanear` + ícono en la cabecera del inicio | Navegable | `flutter test test/widget/acceso_escanear_test.dart` | HECHO — `+1: All tests passed!` (evidencia/h2s1m4.txt): tocar el ícono del inicio abre `PantallaEscanear`. Goldens de la pantalla en 3 viewports × 2 temas, mirados (evidencia: `apps/movil/test/goldens/imagenes/escanear_denegado_*.png`); ver desvío de rediseño en el REPORTE. |

## H3 — Estado del permiso de cámara y salida a Ajustes
**CA:** Dado permiso de cámara denegado o bloqueado, cuando la persona intenta capturar, entonces ve qué pasó y un botón "Abrir ajustes" (o la alternativa manual), sin perder lo cargado.
**DoD:** tests + verificación en simulador.
**Estado:** A MEDIAS

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Justificar y agregar dependencia (90.4.2) tras verificar nombre/versión | Justificación escrita + lockfile | `flutter pub get` exit 0 | HECHO — `app_settings ^9.0.0` agregado (abrir Ajustes; `AppSettings.openAppSettings()` verificado en `~/.pub-cache/.../app_settings-9.0.0/lib/src/app_settings.dart:9`). Desvío: NO se agregó `permission_handler` (más invasivo en iOS) porque el plugin del escáner ya informa `permissionDenied`. Build iOS simulador con el plugin: `Built build/ios/iphonesimulator/Runner.app` (evidencia/h3-build-ios.txt). |
| H3.S1.M2 | Modelo de problema de cámara `ProblemaDeCamara {denegado, noDisponible, otro}` + doble | 3 niveles simulados | `flutter test test/widget/pantalla_escanear_test.dart` | HECHO — `+10: All tests passed!` (evidencia/h3.txt). Desvío: no hay estado `bloqueado`: iOS/Android no lo exponen vía el plugin. |
| H3.S1.M3 | Mensaje accionable + "Abrir ajustes" en el escáner | Denegado → botón que abre Ajustes; otros problemas → sin botón | `flutter test test/widget/pantalla_escanear_test.dart` | HECHO en el escáner (`+10`). `paso_captura.dart` (alta) queda FUERA: el puerto `Camara` devuelve `null` tanto para cancelar como para denegar; distinguirlo cambia el contrato del puerto y los fakes de otros carriles. Anotado como deuda. |

## H4 — Push y navegación por aviso
**CA:** Dado un aviso con `ruta` válida, cuando se toca en los tres estados de la app, entonces se abre esa ruta interna; una ruta externa o con `..`/`//` se rechaza. El push no lleva monto ni PII.
**DoD:** tests contra doble + contrato OpenAPI + declaración explícita de lo no verificado en dispositivo real.
**Estado:** A MEDIAS

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Validador `rutaDelAviso` reutilizando la whitelist de `enlaces_profundos.dart` | Acepta internas, rechaza externas/`..`/`//` | `flutter test test/unidad/ruta_del_aviso_test.dart` | HECHO — `+29: All tests passed!` (evidencia/h4s1m1.txt). Lista blanca derivada de `enlaces_profundos.dart` (se agregó el getter público `rutasBaseDeEnlaces`). Nota: `Uri` normaliza `..` dentro de un enlace `aportaya://` y el resultado sigue bajo la base permitida. |
| H4.S1.M2 | Proveedor que consume `AvisosPush.toques` y navega (dedupe por id, sin sesión = se descarta) | Toque → ruta, 1 vez | `flutter test test/unidad/abridor_de_avisos_test.dart` | HECHO — `+8: All tests passed!` (evidencia/h4s1m2.txt) contra un `Stream` doble; conectado en `app.dart` (`_ConApertura`). Desvío: el dedupe es por destino+ventana de 2 s porque el puerto `AvisosPush.toques` no trae id. Sin SDK push el flujo no emite: los tres caminos (cerrada/fondo/primer plano) NO se pueden ejercitar sin dispositivo. |
| H4.S2.M1 | Contrato `registrarDispositivoPush` en `notificaciones.yaml` (idempotente) | OpenAPI válido, cliente dart regenerado | lint de OpenAPI + `flutter analyze` | BLOQUEADO — DECISION_REQUIRED (A2/A3): hoy no existe endpoint, tabla ni consumidor en `servicios/notificaciones`; agregarlo exige modelo de datos (regla 97), servicio y test de contrato. Destraba: Pablo decide proveedor y dueño del servicio. |
| H4.S2.M2 | Registro del token al iniciar sesión, contra doble (200 / 500 / timeout) | Fallo de red no bloquea la sesión | `flutter test test/unidad/registro_token_test.dart` | BLOQUEADO — DECISION_REQUIRED (A2/A3): sin endpoint real, un adaptador sería una fachada que no llama a nada. No se escribe hasta decidir. |
| H4.S3.M1 | SDK push real nativo | Requiere A2 | — | BLOQUEADO — DECISION_REQUIRED (A2): proveedor (FCM/APNs) y proyecto Firebase/capability Push. Destraba: Pablo. |

## H5 — Voz de bienvenida
**CA:** Dado A4, cuando la persona entra tras autenticarse, entonces la app no falla ni queda silenciosa de forma rota: el puerto devuelve `null` y se entra sin audio.
**Estado:** BLOQUEADO (DECISION_REQUIRED A4)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Puerto `VozDeBienvenida` + doble (audio / null / error / timeout) | Todo fallo = entrar en silencio | `flutter test test/unidad/voz_de_bienvenida_test.dart` | BLOQUEADO — DECISION_REQUIRED (A4): no hay servicio TTS en Pasanaku y la voz nombra a la persona (PII). Un puerto sin consumidor real sería fachada. Destraba: Pablo decide si habrá voz y con qué servicio. |

## H6 — Cierre transversal
**CA:** `flutter analyze` y toda la suite de `apps/movil` en verde; acople con sesión, rutas, tema claro/oscuro y tutoriales comprobado.
**DoD:** salida literal; `REPORTE.md` con avance primero.
**Estado:** A MEDIAS

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Regresión completa | Sin rojos nuevos vs H0 | `cd apps/movil && flutter analyze && flutter test` | HECHO — analyze `No issues found!`; `+260: All tests passed!` (evidencia/h6-test-full.log). Una corrida intermedia falló `gate_del_shell_test` y no se reprodujo (ver REPORTE). |
| H6.S1.M2 | `REPORTE.md` | Tres secciones + no cubierto | `python3 .claude/hooks/plan_status.py` | HECHO — ver REPORTE.md |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Push real no verificable sin Firebase/iPhone | H4.S3 queda sin cerrar | Doble de tres niveles + declarar no cubierto |
| Dependencia nueva rompe el build iOS de TestFlight | Regresión en el otro trabajo | Verificar `pod install` y build de simulador tras cada `pubspec` |
| Cambios sin commitear del trabajo anterior en el mismo árbol | Mezcla de diffs | Tocar solo archivos propios; `git diff --stat` por microtarea |
