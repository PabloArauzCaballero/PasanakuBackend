# Plan — Alta: fecha en tres selects, actividad con buscador, diagnóstico del alta y captura automática

- Fecha: 2026-10-03 · Repos afectados: PasanakuBackend (worktree `PasanakuBackend-alta-v2`, rama `justin/fix/alta-fecha-actividad-autocaptura` sobre `origin/justin/feature/escaner-identidad-atlas` @ `7214ae3d`) · Predecesor: `docs/trabajo/2026-10-02-escaner-identidad-5-caras/`
- Resultado observable: en la app (Flutter), quien se da de alta (1) elige sus fechas con tres selects Día / Mes / Año (o el calendario, sin el modo de texto `mm/dd/yyyy`); (2) elige su actividad económica de una lista corta con buscador y puede escribir «Otra, ¿cuál?»; (3) sabe por qué el alta no avanza; (4) toma anverso y reverso con el escáner del sistema y la prueba de vida de frente, izquierda y derecha se captura sola desde el video.
- Kill-test: abrir el paso 1 y ver un campo de texto `mm/dd/yyyy` o un calendario como única forma de elegir; abrir «Actividad económica» y no poder escribir para filtrar; tener que tocar «Tomar foto» en las selfies.

## Alcance
- IN: `packages/diseno_flutter` (`CampoDeSeleccion`, `CampoDeFecha`, hoja con buscador, `CampoBusqueda`), `apps/movil` (alta: paso 1, paso 7, paso de capturas, cámara, escáner), tests de ambos, gate `scripts/verificar_frontend.py` (regla del buscador), este plan y su reporte.
- OUT: backend de identidad (el alta no manda el perfil transaccional al servidor hoy — `catalogo_del_perfil.dart` lo dice; no se agrega contrato), backoffice, infraestructura de TEST (VPS/Coolify), subida a TestFlight (se firma y sube desde la Mac, `docs/trabajo/2026-09-28-testflight-ios/PLAN.md:8`).
- Ambigüedades registradas:
  1. «Si quiere ponerlo manual, tres espacios» → supuesto: los tres selects son la forma principal y el calendario queda como atajo, **sin** el modo de texto. Confirmar con el usuario.
  2. «Disciplina» del buscador → supuesto: regla del componente (más de 12 opciones ⇒ buscador, automática en `CampoDeSeleccion`) + gate que impide usar `DropdownButton*` fuera de ese componente en `apps/movil`. Con 12 o menos (meses, departamentos) queda el desplegable.
  3. «El mismo comportamiento de Atlas» para la prueba de vida → Atlas (`AtlasFrontend/apps/consumer-app/app/(onboarding)/identidad.tsx`) **no** tiene captura automática en las selfies (son tres `takePictureAsync` manuales); el usuario pide explícitamente captura automática del video. Se sigue al usuario: escáner del sistema como Atlas para el carnet y detección de rostro (ML Kit) para las tres poses. Umbrales de ángulo propios, declarados como punto de partida.
  4. «Expo Go» → la app de Pasanaku es Flutter; Expo Go es de Atlas. Supuesto: se refiere al build de TestFlight / Android de Pasanaku.

## H1 — Los selects con muchas opciones tienen buscador (disciplina)
**CA:** Dado un `CampoDeSeleccion` con más de 12 opciones, cuando se toca, entonces se abre una hoja con buscador que filtra sin tildes ni mayúsculas, y elegir cierra la hoja con el valor puesto; con 12 o menos se ve el desplegable de siempre.
**DoD:** `flutter test` del paquete de diseño en verde · `flutter analyze` sin issues · `python scripts/verificar_frontend.py movil` sin fallas.
**Estado:** HECHO (TESTED: tests de widget + gate; sin runtime en dispositivo)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Hoja `mostrarListaConBuscador` + `CampoBusqueda` acepta teclado | La hoja filtra «educacion» → «Educación» | test widget de la hoja | HECHO — `evidencia/h1-s1-m1-m2-campo-de-seleccion.txt` (5/5) |
| H1.S1.M2 | `CampoDeSeleccion` abre la hoja con >12 opciones | 13 opciones ⇒ hoja; 12 ⇒ desplegable | test widget | HECHO — `evidencia/h1-s1-m1-m2-campo-de-seleccion.txt` (5/5) |
| H1.S1.M3 | Gate: `DropdownButton*` prohibido en `apps/movil/lib` fuera del sistema de diseño | El gate falla con un `DropdownButton` suelto y pasa sin él | `python scripts/verificar_frontend.py movil` (con y sin el caso) | HECHO — `evidencia/h1-s1-m3-gate.txt` (OK limpio, FALLA con el caso) |

## H2 — Fechas con tres selects Día / Mes / Año
**CA:** Dado el paso 1, cuando la persona elige día, mes y año en tres selects, entonces la fecha queda puesta; si elige 31 de febrero ve «Esa fecha no existe»; el calendario sigue disponible pero sin el modo de escribir `mm/dd/yyyy`.
**DoD:** tests de `CampoDeFecha` + `test/widget/paso_datos_test.dart` en verde.
**Estado:** HECHO (TESTED)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `CampoDeFecha` con tres selects + calendario `calendarOnly` | Elegir 12 / abril / 1995 emite `DateTime(1995,4,12)` | test widget | HECHO — `evidencia/h2-s1-m1-m2-campo-de-fecha.txt` (6/6) |
| H2.S1.M2 | Fecha imposible o fuera de rango no se emite y se explica | 31/feb ⇒ texto de error, `onElegida` no se llama | test widget | HECHO — `evidencia/h2-s1-m1-m2-campo-de-fecha.txt` (31/feb, 29/feb/1996) |
| H2.S1.M3 | Paso 1 sigue andando con los selects | `paso_datos_test.dart` verde | `flutter test test/widget/paso_datos_test.dart` | HECHO — `evidencia/h2-s1-m3-paso-datos.txt` (6/6) |

## H3 — Actividad económica: lista corta, buscador y «Otra, ¿cuál?»
**CA:** Dado el paso 7, cuando se toca «Actividad económica», entonces se abre la lista con buscador; los textos entran en un renglón; al elegir «Otra» aparece «¿Cuál?» y no se puede seguir sin escribirla.
**DoD:** test widget del paso 7 en verde.
**Estado:** HECHO (TESTED)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Catálogo con textos cortos + opción `OTRA` | Ningún texto supera 40 caracteres; existe `OTRA` | test unitario del catálogo | HECHO — `evidencia/h3-paso-perfil.txt` (test del catálogo) |
| H3.S1.M2 | Campo «¿Cuál?» obligatorio con `OTRA`, guardado en el estado del alta | Sin texto ⇒ error; con texto ⇒ avanza y queda en el estado | test widget | HECHO — `evidencia/h3-paso-perfil.txt` (4/4) |

## H4 — Por qué no deja hacer el alta
**CA:** La causa queda demostrada con evidencia y, lo que está en la app, corregido.
**DoD:** salidas literales en `evidencia/`.
**Estado:** A MEDIAS — causa de la app corregida (H4.S1.M3–M5); la API de TEST sigue caída (H4.S1.M2 BLOQUEADO)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Diagnóstico del entorno al que apunta TestFlight | Se sabe qué responde `api.aportaya…sslip.io` | `curl` a la API y a los fronts | HECHO — `evidencia/h4-s1-m1-api-test-503.txt`: fronts 200, API 503 «no available server» |
| H4.S1.M2 | Restaurar la API de TEST | `POST /usuarios` deja de dar 503 | `curl` | BLOQUEADO — el clasificador de permisos de Claude Code denegó `ssh root@161.97.85.216` («Production Reads»); reiniciar la app de Coolify es una acción sobre infraestructura compartida con Atlas TEST. Lo destraba: el usuario corre los comandos de diagnóstico (`! ssh …`) o agrega una regla de permiso. No hay contrato que simular: es el entorno mismo |
| H4.S1.M4 | Hallazgo: la cámara de la app saca fotos a ~720p (`ResolutionPreset.high`, 1280×720) y el chequeo exige lado largo ≥ 1400 → toda foto real se rechaza «muy pequeña» (la E2E previa solo usó el carnet de prueba sintético 1600×1009). Subir a `veryHigh` (1080p) | Preset ≥ 1080p en el adaptador; test que fija que 1280×720 se rechaza y 1920×1080 pasa | `flutter test test/unidad/calidad_de_captura_test.dart` | HECHO — `evidencia/h5-s2-m4-m5-prueba-de-vida.txt` incluye `captura_desde_archivo_test` (720p rechazada, 1080p aceptada); preset `veryHigh` en `camara_en_vivo_plugin.dart` |
| H4.S1.M5 | Hallazgo: la cámara de la app exigía la proporción ID-1 a la foto ENTERA (16:9 desvía 12,2 %, 4:3 15,9 %, tolerancia 12 %) → todo anverso/reverso por cámara se rechazaba «No parece el carnet entero». Atlas solo la exige a la salida del escáner | `capturaDesdeArchivo(recortadaAlCarnet:)`: true solo para el escáner | `flutter test test/unidad/captura_desde_archivo_test.dart` | HECHO — 6/6, en `evidencia/h5-s2-m4-m5-prueba-de-vida.txt` |
| H4.S1.M3 | La app dice algo entendible cuando el servidor no está (503/sin conexión) | Con 503 la persona ve un mensaje de «servicio no disponible», no uno genérico | test unitario de `mensajeDeError` | HECHO — `evidencia/h4-s1-m3-mensaje-503.txt` (9/9) |

## H5 — Captura automática: escáner del carnet y prueba de vida desde el video
**CA:** Dado el paso de capturas en un teléfono real, cuando se toma el anverso o el reverso, entonces se abre el escáner del sistema (VisionKit / ML Kit) que encuentra los bordes y dispara solo; cuando se hace la prueba de vida, la cámara frontal detecta el rostro y guarda sola las fotos de frente, izquierda y derecha; si no hay escáner o detector (emulador), cae a la cámara de la app.
**DoD:** tests unitarios del detector de pose y de la validación del escaneo · `flutter analyze` · `flutter build apk --debug` exit 0 · prueba en teléfono real (ver riesgos).
**Estado:** A MEDIAS — código y tests listos; falta compilar el APK (H5.S2.M3) y probar en teléfono real (H5.S2.M6)

### H5.S1 — Escáner del carnet
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Dependencia del escáner del sistema verificada en pub.dev (iOS VisionKit + Android ML Kit) | `flutter pub get` exit 0 | salida | HECHO — `evidencia/h5-dependencias-pub-get.txt` (cunning_document_scanner 3.0.3) |
| H5.S1.M2 | `escanearDocumento()` real + validación de proporción ID-1 y lado largo (umbrales de Atlas) | Imagen 1400×880 pasa; 800×500 «muy pequeña»; 1400×1400 «no parece el carnet entero» | test unitario | HECHO — `evidencia/h5-s1-m2-m3-escaner.txt` (3/3) + `captura_desde_archivo_test` (escaneo 1600×1600 rechazado) |
| H5.S1.M3 | El paso de capturas registra la `Captura` que devuelve el escáner | Escaneo ⇒ captura en el riel | test widget con escáner doble | HECHO — `evidencia/h5-s1-m2-m3-escaner.txt` |

### H5.S2 — Prueba de vida automática
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S2.M1 | Dependencia de detección de rostro verificada (ML Kit, iOS + Android) | `flutter pub get` exit 0 | salida | HECHO — `evidencia/h5-dependencias-pub-get.txt` (google_mlkit_face_detection 0.15.1; iOS 15.0→15.5) |
| H5.S2.M2 | Dominio puro: juez de pose (de frente / izquierda / derecha, rostro centrado y grande, sostenido N cuadros) | Secuencias de ángulos ⇒ dispara o no según la tabla | test unitario | HECHO — `evidencia/h5-s2-m2-juez-de-pose.txt` (9/9) |
| H5.S2.M3 | Adaptador cámara → detector (stream NV21/BGRA → `InputImage`) | Compila en Android | `flutter build apk --debug` | A MEDIAS — `flutter analyze` limpio y la conversión verificada contra el código de los plugins; falta `flutter build apk --debug` (se esperó: 5,9 GB libres con Docker y el emulador de otra sesión) |
| H5.S2.M4 | Pantalla de prueba de vida guiada: tres poses seguidas, guarda las tres capturas | Con detector doble, la pantalla devuelve 3 capturas | test widget | HECHO — `evidencia/h5-s2-m4-m5-prueba-de-vida.txt` (5/5; mutación del signo → 4 fallan) |
| H5.S2.M5 | Respaldo manual si no hay detector o pasan 20 s sin lograr la pose | Con detector que falla ⇒ aparece «Tomar foto» | test widget | HECHO — `evidencia/h5-s2-m4-m5-prueba-de-vida.txt` (sin detector y a los 20 s) |
| H5.S2.M6 | Prueba en teléfono real | Las 5 capturas sin tocar «Tomar foto» en las selfies | captura/video del teléfono | BLOQUEADO — hace falta un teléfono real: el emulador no tiene escáner VisionKit/ML Kit de verdad ni una cara que gire, y el build de TestFlight se firma y sube desde la Mac (`docs/trabajo/2026-09-28-testflight-ios/PLAN.md:8`). Simulado en los tres niveles con dobles: correcto (tres poses → 3 capturas), límite (giro 28/27/71, 4 cuadros vs 3) e inválido (pose equivocada, dos caras, sin detector, 20 s). Lo destraba: probar el build en un teléfono Android y uno iPhone y confirmar el signo del giro |

## H6 — Regresión y reporte
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Regresión del móvil y del sistema de diseño | Suites verdes salvo goldens (se regeneran en macOS) | `flutter test` en ambos | HECHO — móvil 329 ✓ / 9 ✗ y diseño 63 ✓ / 15 ✗, todos los ✗ son goldens de pantallas no tocadas que difieren en Windows (las referencias se generan en macOS): `evidencia/h6-regresion-movil.txt`, `h6-regresion-diseno.txt`; dirigidos 53/53 tras los últimos cambios (`h6-dirigidos-final.txt`); gates `h6-gates.txt` sin fallas |
| H6.S1.M2 | Reporte | `REPORTE.md` con las tres secciones | archivo | HECHO — `REPORTE.md` |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| La API de TEST está caída (503) y arreglarla exige SSH/Coolify en un VPS compartido con Atlas | TestFlight no puede dar de alta a nadie | Diagnóstico desde afuera; la restauración la autoriza el usuario (acción sobre infraestructura compartida) |
| Docker y emulador en uso por otras sesiones (`pasanaku-ef`, campaña) | No hay E2E local inmediato | Coordinar por `SendMessage`; tests de widget con dobles mientras tanto |
| El emulador no tiene escáner ni rostro real | H5 no se puede verificar en runtime acá | Prueba en teléfono real (H5.S2.M6); se declara el peldaño alcanzado |
| Goldens del alta cambian | CI rojo en goldens | Se regeneran en el runner macOS (`goldens-macos.yml`) |
| Límite de 200 líneas por archivo del gate | Gate rojo | Partir en archivos |
