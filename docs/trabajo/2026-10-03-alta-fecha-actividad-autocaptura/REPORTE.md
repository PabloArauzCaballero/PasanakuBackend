# Reporte — Alta: fecha en tres selects, actividad con buscador, por qué no deja hacer el alta y captura automática

> **AVANCE: 21 / 27 — 77,8 %.** Bloqueadas: API de TEST caída, prueba en teléfono real, push de la rama y firma de iOS (las dos últimas por permisos/credenciales). A medias: compilar el APK y el workflow que lo compila en GitHub.

- Fecha: 2026-10-03/04 · Plan: [PLAN.md](./PLAN.md) · Rama: `justin/fix/alta-fecha-actividad-autocaptura` (worktree `PasanakuBackend-alta-v2`, sobre `origin/justin/feature/escaner-identidad-atlas` @ `7214ae3d`)
- Peldaño de evidencia alcanzado, por área:
  - Selects con buscador, fecha en tres selects, actividad con «Otra, ¿cuál?»: **TESTED** (tests de widget + gate). Sin corrida en dispositivo.
  - Mensaje ante 503 y los dos defectos de calidad de captura: **TESTED**.
  - Escáner del sistema y prueba de vida automática: **TESTED** contra dobles de la cámara y del escáner. Ni compilado en Android (pendiente) ni probado en teléfono real (bloqueado).
  - API de TEST: **DISCOVERED** (diagnóstico desde afuera); no se pudo arreglar.
  - **Peldaño del trabajo: TESTED**, el más bajo de sus áreas.

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H1.S1.M1–M2 | `CampoDeSeleccion` con más de 12 opciones abre una hoja con buscador que filtra sin tildes ni mayúsculas; con 12 o menos sigue el desplegable | `flutter test test/widget/campo_de_seleccion_test.dart` (diseño) | PASS 5/5 — `evidencia/h1-s1-m1-m2-campo-de-seleccion.txt` |
| H1.S1.M3 | Gate: un `DropdownButton`, `DropdownMenu` o `showDatePicker` suelto en `apps/movil/lib` hace fallar `verificar_frontend.py` | gate limpio y con un caso plantado | PASS — `evidencia/h1-s1-m3-gate.txt` (OK / FALLA) |
| H2.S1.M1–M2 | `CampoDeFecha` = tres selects Día · Mes · Año (año y día con buscador numérico); 31 de febrero → «Esa fecha no existe…»; 29/02/1996 sí; el calendario abre en `calendarOnly` (sin el lápiz `mm/dd/yyyy`) | `flutter test test/widget/campo_de_fecha_test.dart` | PASS 6/6 — `evidencia/h2-s1-m1-m2-campo-de-fecha.txt` |
| H2.S1.M3 | El paso 1 del alta sigue andando con los selects (incluida «Vence el») | `flutter test test/widget/paso_datos_test.dart` | PASS 6/6 — `evidencia/h2-s1-m3-paso-datos.txt` |
| H3.S1.M1–M2 | Actividad económica: 19 opciones de ≤ 40 caracteres, con buscador, y «Otra (¿cuál?)» que pide «¿Cuál?» y no deja seguir vacía; el texto queda en `detalleDeLaActividad` | `flutter test test/widget/paso_perfil_transaccional_test.dart` | PASS 4/4 — `evidencia/h3-paso-perfil.txt` |
| H4.S1.M1 | Diagnóstico: TestFlight apunta a `https://api.aportaya.161.97.85.216.sslip.io/api/v1`, que responde **503 «no available server»** en todas las rutas (incluidas `POST /usuarios` y `GET /cumplimiento/contratos/vigentes`); los tres fronts responden 200 | `curl` | `evidencia/h4-s1-m1-api-test-503.txt` |
| H4.S1.M3 | Ante 502/503/504 la app dice «El servicio no está disponible en este momento. No es tu conexión ni tus datos…» en vez de «Algo salió mal de nuestro lado» | `flutter test test/unidad/errores_del_alta_test.dart` | PASS 9/9 — `evidencia/h4-s1-m3-mensaje-503.txt` |
| H4.S1.M4 | **Defecto encontrado y corregido:** la cámara sacaba fotos a ~720p (`ResolutionPreset.high` = 1280×720) y el chequeo pide lado largo ≥ 1400 px: toda foto real se rechazaba «muy pequeña». Ahora `veryHigh` (~1080p) | `flutter test test/unidad/captura_desde_archivo_test.dart` | PASS 6/6 — en `evidencia/h5-s2-m4-m5-prueba-de-vida.txt` |
| H4.S1.M5 | **Defecto encontrado y corregido:** la cámara de la app exigía la proporción del carnet (ID-1 ±12 %) a la foto entera: 16:9 se desvía 12,2 % y 4:3 15,9 %, así que todo anverso/reverso por cámara se rechazaba «No parece el carnet entero». Como en Atlas, la proporción se exige solo a lo que devuelve el escáner (ya recortado) | ídem | PASS — ídem |
| H5.S1.M1, H5.S2.M1 | Dependencias verificadas contra su código fuente: `cunning_document_scanner` 3.0.3 (VisionKit / ML Kit, como el escáner de Atlas) y `google_mlkit_face_detection` 0.15.1; iOS 15.0 → 15.5 y Podfile según el README de ML Kit | `flutter pub get` | PASS — `evidencia/h5-dependencias-pub-get.txt` |
| H5.S1.M2–M3 | Anverso y reverso salen del escáner del sistema (bordes y disparo automáticos); lo escaneado pasa el chequeo de Atlas y queda en el riel; un escaneo sin forma de carnet se rechaza con aviso; sin escáner, cae a la cámara de la app | `flutter test test/widget/escaner_del_carnet_test.dart` | PASS 3/3 — `evidencia/h5-s1-m2-m3-escaner.txt` |
| H5.S2.M2 | Juez de pose: de frente / izquierda / derecha, centrado, tamaño, inclinación, ojos, una sola cara, pose sostenida 4 cuadros | `flutter test test/unidad/juez_de_pose_test.dart` | PASS 9/9 — `evidencia/h5-s2-m2-juez-de-pose.txt` |
| H5.S2.M4–M5 | Prueba de vida: las tres poses se capturan solas, sin tocar nada; la pose equivocada no dispara y la pista dice qué hacer; sin detector o a los 20 s aparece «Tomar foto» de respaldo | `flutter test test/widget/prueba_de_vida_test.dart` | PASS 5/5; con el signo del giro invertido fallan 4 — `evidencia/h5-s2-m4-m5-prueba-de-vida.txt` |
| H6.S1.M1 | Regresión y gates (ver «Evidencia») | `flutter test --concurrency=1` (móvil y diseño), `verificar_frontend.py movil` / `diseno`, `flutter analyze` | Dirigidos 53/53; gates TODO OK; analyze sin issues; los únicos rojos son goldens de Windows (ENVIRONMENT) |
| H6.S1.M2 | Este reporte | — | — |

## A medias

### H5.S2.M3 — Compilar el APK con los plugins nativos nuevos
- Qué anda: `flutter analyze` sin issues y los tests de todo el flujo en verde; la conversión del video a `InputImage` sigue el ejemplo oficial de `google_mlkit_commons` y se verificó contra el código de `camera_android_camerax` 0.7.5+1 (NV21 de un plano, formato 17) y de `camera_avfoundation` 0.10.3+1.
- Qué no anda / no se probó: no se compiló todavía (`flutter build apk --debug`), así que la integración nativa (Gradle, ML Kit, escáner) no está demostrada.
- Intentos (2026-10-04, con el OK de `pasanaku-ef`): el primero salió con `No Android SDK found` (el shell no tenía `ANDROID_HOME`; el SDK está en `C:/Users/DELL/tools/android-sdk`). El segundo, con `ANDROID_HOME` puesto, **lo detuvo Claude Code por memoria crítica** (Docker/WSL ~9,8 GB + emulador + Gradle). No es un fallo de la compilación; no se relanzó sin pedido del usuario.
- Qué falta exactamente: con memoria libre (Docker y el emulador apagados), `ANDROID_HOME=C:/Users/DELL/tools/android-sdk flutter build apk --debug` en `apps/movil` y pegar el exit code. Se esperó porque la máquina tenía 5,9 GB libres con Docker y el emulador de otra sesión corriendo (antes el sistema mató procesos por memoria), y se le pidió permiso a esa sesión (`pasanaku-ef`).
- Dónde quedó: rama `justin/fix/alta-fecha-actividad-autocaptura`, compila a nivel de Dart.

## Pendiente

### H7 — Disponible para probar (agregado 2026-10-04)
- **Android (APK):** `.github/workflows/apk-de-prueba.yml` compila el APK en GitHub (release firmado con claves de depuración, contra TEST) y lo publica como prerelease `apk-prueba-<n>`. Está commiteado en local; **no corrió** porque el push fue denegado por el clasificador de permisos («Create Public Surface»). Con el push hecho, se dispara solo.
- **iPhone (TestFlight):** el repo no tiene secretos de firma, así que el job `release-ios` no puede subir. Desde la Mac de Pablo (como en `docs/trabajo/2026-09-28-testflight-ios/`), en `apps/movil`:
  ```
  flutter build ipa --release --no-codesign --build-number=<AAAAMMDDhhmm>     --dart-define=API=https://api.aportaya.161.97.85.216.sslip.io/api/v1     --dart-define=HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io
  cd ios && pod install && cd ..   # el Podfile cambió (iOS 15.5, ML Kit)
  xcodebuild -exportArchive -archivePath build/ios/archive/Runner.xcarchive     -exportOptionsPlist <method=app-store-connect, destination=upload, signingStyle=automatic, teamID=7A42TVN3Y6>     -exportPath build/ios/ipa-upload -allowProvisioningUpdates
  ```
- Mientras la API de TEST siga en 503, cualquiera de los dos builds abre pero no puede hacer el alta.


| ID | Estado | Qué lo destraba |
|---|---|---|
| H4.S1.M2 — Levantar la API de TEST | BLOQUEADO | El clasificador de permisos de Claude Code denegó `ssh root@161.97.85.216` («Production Reads»), y el VPS es compartido con Atlas TEST. Lo destraba el usuario: correr `! ssh root@161.97.85.216 'docker ps --format "{{.Names}} {{.Status}}" \| grep -i gateway; tail -50 /opt/aportaya/logs/autodespliegue.log'` o agregar una regla de permiso. Pistas de `despliegue/TEST.md`: con los fronts en 200 y solo la API en 503, o el contenedor `gateway` no está sano, o Coolify perdió su entrada en `docker_compose_domains` (pasa al sacar un servicio del compose o al llenar `fqdn`). |
| H5.S2.M6 — Prueba en teléfono real | BLOQUEADO | Un teléfono Android y un iPhone con el build nuevo. El de iOS se firma y sube a TestFlight desde la Mac. Simulado en los tres niveles (correcto, límite, inválido) con dobles; falta confirmar en el teléfono el **signo del giro** (supuesto: Android sin espejar, iOS espejado). |

## Evidencia

```text
$ curl https://api.aportaya.161.97.85.216.sslip.io/api/v1/usuarios -X POST -d '{}'
no available server
HTTP 503
(fronts aportaya / app / backoffice: 200)

$ flutter test (diseño)   test/widget/campo_de_fecha_test.dart test/widget/campo_de_seleccion_test.dart
00:08 +11: All tests passed!
$ flutter test (móvil)    paso_datos, paso_perfil_transaccional, prueba_de_vida, escaner_del_carnet,
                          juez_de_pose, captura_desde_archivo, errores_del_alta
00:15 +42: All tests passed!

$ python scripts/verificar_frontend.py movil   → TODO OK (exit 0)
$ python scripts/verificar_frontend.py diseno  → TODO OK (exit 0)

$ flutter test --concurrency=1 (móvil)   04:03 +329 -9   — los 9: goldens de pantalla_escanear (QR),
                                          pantalla_de_saldo y transiciones (0,7–1,5 % de píxeles)
$ flutter test --concurrency=1 (diseño)  01:16 +63 -15   — los 15: goldens del catálogo (0,1–3,6 %);
                                          ninguno contiene CampoDeFecha ni CampoDeSeleccion
```

Índice de `evidencia/`: `h1-s1-m1-m2-campo-de-seleccion.txt`, `h1-s1-m3-gate.txt`, `h2-s1-m1-m2-campo-de-fecha.txt`, `h2-s1-m3-paso-datos.txt`, `h3-paso-perfil.txt`, `h4-s1-m1-api-test-503.txt`, `h4-s1-m3-mensaje-503.txt`, `h5-dependencias-pub-get.txt`, `h5-s1-m2-m3-escaner.txt`, `h5-s2-m2-juez-de-pose.txt`, `h5-s2-m4-m5-prueba-de-vida.txt`, `h6-regresion-movil.txt`, `h6-regresion-diseno.txt`, `h6-dirigidos-final.txt`, `h6-gates.txt`.

## No cubierto
- **Nada corrió en un dispositivo ni en el emulador**: el emulador y Docker estaban tomados por otras sesiones. Toda la verificación es con tests de widget y dobles.
- Prueba visual (capturas por viewport, claro/oscuro) de la hoja con buscador, los tres selects y la pantalla de prueba de vida: **no hecha**. Los colores salen de tokens y el gate de literales de diseño pasa, pero nadie miró las pantallas.
- El escáner del sistema real (VisionKit / ML Kit) y ML Kit de rostro real: solo dobles.
- Accesibilidad con lector de pantalla: la hoja con buscador tiene `Semantics(button, label, value)` y la pista de la prueba de vida es `liveRegion`, sin probar con TalkBack/VoiceOver.
- iOS no se compiló (no hay Mac en esta máquina).

## Desvíos del plan
- H4.S1.M4 y H4.S1.M5 se agregaron durante la ejecución: aparecieron al cablear la captura (ver «Completado»). Los dos son causas de que el alta no se pudiera terminar con fotos reales, independientes del 503.
- Se agregó `capturaDesdeArchivo` (dominio) y se reemplazó el bloque equivalente de `PantallaDeCamara`: eran tres copias del mismo chequeo (cámara, escáner, prueba de vida).
- Para respetar el límite de 200 líneas se partieron archivos: `parte_de_fecha.dart`, `campo_de_actividad.dart`, `campo_de_detalle.dart`, `pie_de_prueba_de_vida.dart`, `lista_con_buscador.dart`.

## Riesgos residuales
- **Signo del giro de la cabeza**: se dedujo del código de los plugins (iOS espeja la cámara delantera y Android no) y de la documentación de ML Kit. Si en un teléfono sale al revés, la pose «izquierda» no dispara; a los 20 s aparece «Tomar foto» y nadie queda trabado, pero hay que corregirlo en `lector_de_rostro_mlkit.dart` (`_haciaLaIzquierda`).
- **Umbrales** del juez de pose y del chequeo de calidad: punto de partida, igual que en Atlas, no valores medidos.
- **Rendimiento**: ML Kit en modo `accurate` sobre video 1080p puede ir lento en un Android de gama baja; se procesa un cuadro a la vez y se descartan los que llegan mientras tanto.
- **Goldens**: los 24 rojos de Windows no se regeneraron (corresponde al runner macOS, `goldens-macos.yml`). Ninguno cubre lo que cambió.
- `montoMensualEstimado` es `double` en `estado_alta.dart` (la regla 91 prohíbe `double` para dinero). Es una estimación declarada que no se manda al servidor; se anota y no se toca (fuera de alcance).
- El perfil transaccional (incluida «Otra, ¿cuál?») sigue sin mandarse al servidor; así estaba antes.

## Decisiones y ambigüedades
1. Fecha: tres selects como forma principal y el calendario como atajo, sin modo de texto. Confirmar con el usuario.
2. «Disciplina» del buscador: automática en `CampoDeSeleccion` con más de 12 opciones, más el gate que prohíbe armar selects o calendarios por fuera del sistema de diseño en `apps/movil`. Doce deja los meses y los nueve departamentos como desplegable.
3. Prueba de vida automática: **Atlas no la tiene** (sus tres selfies son `takePictureAsync` manuales, `AtlasFrontend/apps/consumer-app/app/(onboarding)/identidad.tsx`). Se siguió lo pedido por el usuario: escáner del sistema como Atlas para el carnet, y detección de rostro en el teléfono (sin mandar la imagen a nadie) para las tres poses.
4. El escáner queda **encendido por omisión** (como en Atlas para desarrollo, preview y producción). Se apaga con `--dart-define=ESCANER_DOCUMENTO=false`.
5. «Expo Go» se tomó como el build de prueba de Pasanaku (Flutter). Expo Go es de Atlas.
6. «Otra» en actividad usa el código `OTRA`, que vive solo en la app. Si algún día el perfil viaja al servidor, hay que alinearlo con `perfil_transaccional.codigo_ciiu`.
