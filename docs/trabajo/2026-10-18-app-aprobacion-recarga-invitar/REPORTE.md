# Reporte — App: subir lo pendiente, aprobación sin reingresar, recargar pantallas, invitar de verdad

> **AVANCE: 8 / 10 — 80,0 %.**

- Fecha: 2026-10-18 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/app-kyc-diferido-y-pin-20261018` · PR #139 (a `test`)
- Peldaño: `TESTED` (pruebas dirigidas y suite no visual en verde; APK compilado). Sin prueba en el teléfono del usuario todavía.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| (pedido 1) | El backend #126 (alta sin carnet) ya estaba en TEST | `POST /usuarios` sin `documento` contra TEST | 422 `AP-CU01-11` (pide el código de correo; ya no exige documento). No crea cuenta |
| H1.M1 | La app de la sesión anterior, combinada de a tres versiones contra `test` | `flutter analyze` | No issues found |
| H1.M2 | Suite no visual | `flutter test` (sin goldens/capturas) | +727 −7 (las 7 son píxeles) — `evidencia/suite-final-no-visual.txt` |
| H2.M1 | Aprobación sin reingresar: consulta cada 30 s y al volver a la app; renueva el acceso solo | `flutter test test/navegacion/vigilante_de_aprobacion_test.dart` | +4 (aprobada / en revisión / sin red / ya habilitada) |
| H3.M1 | Deslizar hacia abajo recarga Grupos y el estado del grupo; crear un grupo actualiza la lista | `flutter test test/pasanaku` | +27, incluye la prueba del refresco |
| H4.M1 | Enlace https de la app web para compartir; la app lo acepta solo del dominio propio | `flutter test test/pasanaku/enlace_para_compartir_test.dart test/unidad/lectura_de_qr_test.dart` | PASS (correcto / límite / inválido) |
| H4.M3 | Sin «Le va a llegar»; botones Enviar por WhatsApp / SMS / otra app / Copiar | `flutter test test/pasanaku test/widget/pantallas_de_h_tema_y_desborde_test.dart test/unidad/lectura_de_qr_test.dart` | +74 |
| H5.M2 | APK v8 | `flutter build apk --release …` | `Downloads/AportaYa-test-2026-10-18-v8.apk` (69 336 612 bytes, build 2026101801) |

## A medias
### H4.M2 — Compartir nativo (WhatsApp / SMS / otra app)
- Qué anda: `CompartirMensaje.kt` y el canal `bo.aportaya/compartir` compilan en el APK; la pantalla los llama y, si no abre, ofrece copiar (probado con un doble del puerto `Compartir`).
- Qué no anda / falta: no se ejercitó en un teléfono real (que WhatsApp abra el chat del número con el mensaje, y el SMS).
- Dónde quedó: `apps/movil/android/app/src/main/kotlin/bo/aportaya/aportaya_movil/CompartirMensaje.kt`, `lib/pantallas/pasanaku/botones_de_compartir.dart`.

### H5.M1 — CI del PR #139 y goldens
- Qué anda: el PR está abierto; `verificar_frontend.py` y `dart format` en verde localmente.
- Qué falta: que el CI termine. Si el job de goldens (Linux) falla por las pantallas cambiadas a propósito (candado, paso del carnet, invitación, billetera en cero), tomar del artefacto `goldens-diferencias` la imagen nueva de esas pantallas, revisarla y dejarla como referencia.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Prueba en teléfono | TODO | El usuario instala el APK v8: PIN, aprobación automática, compartir por WhatsApp/SMS |

## Evidencia
```text
flutter test (sin goldens/capturas): 01:41 +727 ~1 -7: Some tests failed.
  componentes_de_fase, h4_ci_icono_visual, motivo_de_salida (2), paso_capturas (2), pautas_publicas: todas "Pixel test failed"
python scripts/verificar_frontend.py → TODO OK
```

## No cubierto
- Ningún flujo nuevo se probó en un teléfono real.
- La app web de TEST recibiendo el enlace https y completando la unión al grupo: se comprobó que responde 200, no el flujo completo.
- iOS: el canal de compartir no existe ahí; la pantalla ofrece copiar el enlace.

## Desvíos del plan
- Para pasar las reglas del frontend se partieron 7 archivos de más de 200 líneas (sin cambiar comportamiento; las pruebas del candado y la suite lo confirman) y el proveedor de renovación pasó a `lib/dominio/`.
- Las imágenes golden generadas en Windows no se subieron: se regeneran en Linux.

## Riesgos residuales
- Android 12+ abre el enlace https en el navegador (app web) y no en la app instalada: no hay App Links verificados (`assetlinks.json`). Queda como mejora.
- `verificar_rutas_frontend.py` falla por rutas `/erp` y `/publicidad` del backoffice; no corre en CI y esta rama no las toca.

## Decisiones y ambigüedades
- «Recargar la pantalla» = deslizar hacia abajo. Confirmar con el usuario.
- El vigilante consulta cada 30 s solo mientras la cuenta espera aprobación.
