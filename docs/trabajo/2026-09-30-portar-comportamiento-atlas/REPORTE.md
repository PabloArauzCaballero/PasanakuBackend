# Reporte — Portar a la app móvil de Pasanaku los comportamientos de la app cliente de Atlas

> **AVANCE: 15 / 20 — 75,0 %.** (Sin cerrar: 1 `A MEDIAS` y 4 `BLOQUEADO` por decisión de negocio/infraestructura pendiente. Nada de esto se ejecutó en un dispositivo real.)

- Fecha: 2026-09-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/fix/docker-test-coolify-2026-09-25` · mergeado a `main` en el PR #34 (2026-09-30 18:53 UTC) con el CI todavía corriendo: ningún check estaba en verde al mergear
- Peldaño de evidencia alcanzado: **`TESTED`** para la lógica y las pantallas (tests dirigidos en verde, suite completa en verde, goldens mirados) y **`RUNS`** para el adaptador nativo (`flutter build ios --simulator` compila). **No se llegó a `VERIFIED`**: ninguna pieza se ejercitó con cámara real, push real ni la app contra el backend TEST.

## Qué se pidió y cómo se interpretó
"Cargar el mismo comportamiento de la app de Atlas" se acotó, con Pablo, a cuatro bloques **adaptados a Pasanaku**: permisos y arranque, push y navegación por aviso, cámara/QR/escáner de carnet, tour/voz/tutoriales. El descubrimiento corrigió supuestos del pedido: Atlas **ya no pide permisos al arrancar** y **nunca abre Ajustes**; Pasanaku **ya tenía** tour, motor de tutoriales y captura de cámara del alta; `mobile_scanner` estaba declarado pero sin ningún import.

## Completado
| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H0.S1.M1 | Línea base | `flutter analyze && flutter test` | PASS — sin issues, `+176` · `evidencia/h0-*.txt` |
| H1.S1.M1 | Política: la guía `intro-app` se autolanza solo si nunca se hizo (o cambió de versión) | `flutter test test/unidad/tutoriales/autolanzamiento_test.dart` | PASS `+7` · `evidencia/h1s1m1.txt` |
| H1.S1.M2 | El inicio lanza la guía la primera vez, una sola vez por arranque, sin pisar otra en curso | `flutter test test/widget/tutoriales/autolanzamiento_inicio_test.dart` | PASS `+5` contra el motor real · `evidencia/h1s1m2.txt` |
| H1.S1.M3 | Perfil → «Ver la guía otra vez» reinicia y lanza de cero | `flutter test test/widget/tutoriales/ver_guia_otra_vez_test.dart` | PASS `+2` (prueba la acción; el botón no tiene test de widget propio) |
| H2.S1.M1 | QR → ruta solo si es invitación `aportaya://unirse/<uuid>.<hash>`; cerrojo anti multi-lectura | `flutter test test/unidad/lectura_de_qr_test.dart` | PASS `+16` · `evidencia/h2s1m1.txt` |
| H2.S1.M2 | Adaptador real sobre `mobile_scanner 7.4.1` (API verificada en el código instalado) | `flutter analyze` | PASS (solo `RUNS`) |
| H2.S1.M3 | Pantalla de escaneo: visor, rechazo de QR ajeno, cámara denegada/no disponible, enlace pegado a mano | `flutter test test/widget/pantalla_escanear_test.dart` | PASS `+10` · `evidencia/h3.txt` |
| H2.S1.M4 | El ícono del inicio abre el escáner | `flutter test test/widget/acceso_escanear_test.dart` | PASS `+1` · `evidencia/h2s1m4.txt` |
| H3.S1.M1 | `app_settings ^9.0.0` agregado y justificado; no rompe el build de iOS | `flutter build ios --simulator --debug --no-codesign` | PASS `Built build/ios/iphonesimulator/Runner.app` · `evidencia/h3-build-ios.txt` |
| H3.S1.M2 | Problema de cámara `denegado / noDisponible / otro` con doble | `flutter test test/widget/pantalla_escanear_test.dart` | PASS `+10` |
| H3.S1.M3 | Cámara denegada → «Abrir ajustes» (solo en ese caso) | idem | PASS `+10` |
| H4.S1.M1 | Un aviso solo abre destinos de la casa (lista blanca derivada de `enlaces_profundos.dart`) | `flutter test test/unidad/ruta_del_aviso_test.dart` | PASS `+29` · `evidencia/h4s1m1.txt` |
| H4.S1.M2 | Toque de aviso → navega una vez (dedupe 2 s); error del flujo no mata la escucha; conectado en `app.dart` | `flutter test test/unidad/abridor_de_avisos_test.dart` | PASS `+8` · `evidencia/h4s1m2.txt` |
| H6.S1.M1 | Regresión completa | `flutter analyze && flutter test` | PASS — `No issues found!`, `+260: All tests passed!` · `evidencia/h6-test-full.log` |
| H6.S1.M2 | Este reporte | `python3 .claude/hooks/plan_status.py` | PASS |

## A medias
### H1.S1.M4 — Prueba visual de la guía y del botón de Perfil
- Qué anda: la lógica de autolanzamiento y la acción de Perfil pasan sus tests con el motor real.
- Qué no anda: no hay capturas de la guía superpuesta ni del botón en Perfil; la app no se corrió con sesión.
- Qué falta exactamente: correr la app contra el backend TEST (el mock Prism no completa el login) y capturar en 3 viewports, claro y oscuro.
- Dónde quedó: compila, tests en verde, sin commitear.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H4.S2.M1 Contrato `registrarDispositivoPush` | BLOQUEADO — DECISION_REQUIRED (A2/A3) | Hoy no hay endpoint, tabla ni consumidor en `servicios/notificaciones` (solo emite). Pablo decide proveedor y dueño; requiere modelo de datos (regla 97), servicio y prueba de contrato. |
| H4.S2.M2 Registro del token al iniciar sesión | BLOQUEADO — DECISION_REQUIRED (A2/A3) | Lo mismo: sin endpoint real sería una fachada que no llama a nada. |
| H4.S3.M1 SDK push nativo (FCM/APNs) | BLOQUEADO — DECISION_REQUIRED (A2) | Pablo: proveedor, proyecto Firebase, capability Push en iOS. |
| H5.S1.M1 Voz de bienvenida | BLOQUEADO — DECISION_REQUIRED (A4) | Pablo: si habrá voz y con qué servicio TTS propio. La de Atlas depende de un motor ajeno y nombra a la persona (PII). |

## Evidencia
```text
flutter analyze            → No issues found!
flutter test (completo)    → 00:07 +260: All tests passed!
flutter build ios --simulator --debug --no-codesign → ✓ Built build/ios/iphonesimulator/Runner.app
```
Archivos en `evidencia/`: `h0-*`, `h1s1m1`, `h1s1m2`, `h2s1m1`, `h2s1m3`/`h3`, `h2s1m4`, `h3-build-ios`, `h4s1m1`, `h4s1m2`, `h6-*`. Capturas (goldens): `apps/movil/test/goldens/imagenes/escanear_denegado_{movil,tableta,escritorio}_{claro,oscuro}.png`. Sin datos personales en ninguna salida.

## No cubierto
- **Cámara real y escaneo real de QR**: el visor real (`mobile_scanner`) nunca se ejercitó; las pruebas usan un visor doble.
- **Push de punta a punta**: los tres caminos (app cerrada, en segundo plano, primer plano) no se pueden ejercitar sin SDK ni dispositivo. Solo está probado que, si llega un toque, se abre el destino correcto.
- **«Abrir ajustes» en dispositivo**: probado con doble; el plugin solo se compiló para simulador.
- **Android**: no se compiló ni se corrió.
- **Legibilidad real del texto en los goldens**: usan la fuente de prueba (bloques); layout y desbordes sí se miraron, el texto no.
- **Tema claro/oscuro y 3 viewports de la guía y de Perfil** (ver A medias); del inicio solo cambió su golden.
- **Captura de documento del alta** (`paso_captura.dart`): no distingue permiso denegado (ver deuda).
- **Backend real**: ningún cambio de backend ni de OpenAPI; la ruta de invitación escaneada no se probó contra el servicio.

## Desvíos del plan
0. **Merge a `main` (PR #34):** hubo un conflicto en `apps/backoffice/e2e/tablero-y-permisos.e2e.ts` (PR #18 de otra persona); se tomó la versión de `main`, con lo que la rama pierde ahí aserciones sobre título, h1 y cantidad de accesos. `cliente_refresco_test.dart` (de `main`) asumía `baseDelGateway` como `String`; se usó una URL de prueba fija. Tras el merge: `flutter analyze` limpio y `+262` en verde, con los clientes Dart regenerados localmente (`main` trackea paquetes stub en `clientes/dart/` que pisan los generados).
1. **El QR se definió como la invitación a un grupo** (A6). Pasanaku no tenía consumidor para un QR; se reusó el único enlace con formato estricto ya validado.
2. **No se agregó `permission_handler`** (H3): más invasivo en iOS, y el escáner ya informa `permissionDenied`. Se agregó `app_settings` para abrir Ajustes. No existe el estado «bloqueado permanente».
3. **«Abrir ajustes» no llegó a la captura del alta**: exigiría cambiar el contrato del puerto `Camara` y los fakes de otros carriles.
4. **Rediseño tras la prueba visual**: el aviso de cámara denegada quedaba cortado dentro de la caja del visor y escondía el botón de ajustes. Ahora el visor sale de la pantalla y el aviso ocupa su lugar. Lo detectó el golden, no un test funcional.
5. **Goldens de `pantalla_de_saldo` (claro y oscuro) actualizados** a propósito: la única diferencia era el ícono nuevo de escanear (revisado en `isolatedDiff`). Son los únicos goldens existentes que se modificaron.
6. El dedupe de avisos es por destino + ventana de 2 s porque el puerto `AvisosPush.toques` no trae id (Atlas dedupea por id).
7. **Un agente de solo lectura** (Explore) hizo el descubrimiento comparativo. No editó nada.

## Riesgos residuales y deuda
- **`gate_del_shell_test` falló una sola vez** en una corrida completa y **no se reprodujo** (aislado pasa; otra corrida completa pasa). Causa no demostrada. No se cambió ningún test ni timeout.
- **Si falla la lectura del avance guardado, la guía de inicio se vuelve a ofrecer** (el almacén arranca vacío). Es molesto, no dañino; queda limitada a una vez por arranque.
- **`AvisosPushAndroid` figura como «soportado»** en `capacidades.dart` sin receptor nativo (hueco previo, no tocado): la app cree que hay push y no lo hay.
- **`mobile_scanner` no probado en hardware**: permisos de cámara de iOS (`NSCameraUsageDescription`) y Android (declarado por el plugin) están, pero no se vio el diálogo.
- **El puente de cámara de desarrollo (`CAMARA_DEV`) usa fotos reales del Mac**; no se verificó que el CI de release lo prohíba (preexistente, anotado).
- **Cambios del trabajo TestFlight siguen sin commitear** en el árbol local (no entraron al PR #34) (`apps/movil/ios`, `capacidades.dart`, `plataforma.dart`, etc.); `pubspec.lock` y `Podfile.lock` ahora incluyen además `app_settings`. Al commitear hay que separar los dos trabajos.

## Decisiones y ambigüedades
- **A1** «Mismo comportamiento» = los 4 bloques, adaptados, no copia literal. → Pablo.
- **A2** Proveedor push (FCM/APNs): no se decidió ni se creó proyecto Firebase. → Pablo.
- **A3** Contrato de registro de dispositivo push: no existe; se propone, no se implementa. → Pablo / dueño de `notificaciones`.
- **A4** Voz de bienvenida: no se porta hasta que exista servicio propio. → Pablo.
- **A5** Paquetes nuevos verificados contra el código instalado antes de usarse (`mobile_scanner 7.4.1`, `app_settings 9.0.0`). → resuelta.
- **A6** El QR significa «invitación a un grupo»; cualquier otro uso (p. ej. cobro) queda fuera. → Pablo.
- Regla 91: ningún cambio mueve ni calcula dinero; escanear o tocar un aviso solo navega, nada se da por pagado. El texto de un QR o de un aviso se trata como no confiable y no se registra en logs.
