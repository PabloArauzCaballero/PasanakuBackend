# Reporte — Unirse a un grupo con la imagen del QR o con el enlace

> **AVANCE: 9 / 12 — 75,0 %.**

- Fecha: 2026-10-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/unirse-por-qr-y-enlace-20261019` (PR #157)
- Peldaño: `TESTED` (backend con pruebas web; app con pruebas de pantalla y goldens regenerados en Linux). Falta el despliegue y la prueba en teléfonos.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | `MiPerfil` devuelve `nombreCompleto` y `telefono` | `gradlew :servicios:identidad:webTest` | 144/144 |
| H1.M2 | `solicitud_ingreso.telefono_contacto` y `correo_contacto` en el modelo y el DDL | `generar_ddl.py`, `verificar_boveda.py`, `verificar_criterios.py` | «Sin pendientes» · TODO OK · sin divergencias |
| H1.M3 | El pedido por enlace guarda nombre, teléfono y correo; la lista los devuelve | `gradlew :servicios:grupos:webTest` | 95/95 |
| H2.M1 | Botón «Unirse a un grupo» debajo de «Organizar un grupo nuevo» | `flutter test test/pasanaku/acciones_de_grupo_test.dart` | PASS |
| H2.M2 | Subir la imagen del QR o pegar el enlace; valida y abre `/g/<código>` | `flutter test test/pasanaku/pantallas_enlace_del_grupo_test.dart` | 6 casos: correcto, límite e inválido |
| H2.M3 | Invitar crea el enlace al entrar; QR como PNG por WhatsApp; enlace para compartir o copiar | tests de pantalla + `flutter build apk` | PASS · APK exit 0 |
| H2.M4 | La tarjeta de solicitud muestra nombre, teléfono y correo | test de pantalla | PASS |
| H2.M5 | APK v12 (`C:\Users\leonardo\Downloads\Aporteo-test-2026-10-20-v12.apk`, versionCode 2026102001) | `flutter build apk --release …` | exit 0, 66,5 MB, `FileProvider` en el manifiesto |
| H2.M6 | Goldens de la pestaña Grupos regenerados en Linux | flujo temporal `goldens-linux-temporal.yml` (borrado) | success; 25 PNG en 68c6acd3 |

## A medias
### H1.M4 — AP-CU69-12 (no crear el enlace de un grupo en BORRADOR)
- Qué anda: el código lo rechaza; el criterio está en CU-69 y `verificar_criterios.py` lo reconoce.
- Qué no anda / falta: la prueba `rechazaGenerarSiElGrupoTodaviaNoEstaAbierto` usa PostgreSQL real; corre en el CI del PR #157.
- Dónde quedó: `CU69EnlaceDeGrupo.java`, `CU69EnlaceDeGrupoTest.java`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Despliegue | TODO | fusionar el PR #157 en `test` |
| Prueba en dos teléfonos | TODO | APK v12 y dos cuentas |
| Integraciones con base real (CU69, CU07) | TODO | CI del PR #157 |

## Evidencia
Ver `evidencia/pruebas.txt`.

## No cubierto
- Leer el QR desde una imagen recomprimida por WhatsApp en un teléfono real (`analyzeImage` solo corre en Android/iOS; en las pruebas el lector es un doble).
- Compartir la imagen por WhatsApp de punta a punta (intent nativo).
- iOS: el canal de compartir no existe; la pantalla avisa y se puede copiar el enlace.

## Desvíos del plan
- Causa de fondo encontrada en TEST: faltaba el tópico `aportaya.tarifas.tarifa_congelada` y los grupos no salían de BORRADOR. Se corrió `despliegue/host/crear-topicos.sh` (paso documentado en `despliegue/TEST.md`). Se agregó AP-CU69-12 para que no vuelva a fallar en silencio.
- El dibujo del PNG del QR vive en `packages/diseno_flutter` (`codigoQrComoPng`), al lado de `CodigoQR`, para no sumar `qr_flutter` como dependencia directa de la app.
- `dart format` reformateó 6 pruebas de goldens ajenas: se revirtieron.
- La generación de clientes en paralelo en Windows pisó el cliente Dart de núcleo-financiero (CI usa `--no-parallel`): se regeneró ese cliente solo. Los clientes no se commitean.

## Riesgos residuales
- La suite completa en Windows da 311 rojas: todas son píxeles de goldens hechos en Linux, más 5 `UnmountedRefException` en `SeguimientoDelAlta.liberarCapturas` (subida del expediente), ajenas a este trabajo.
- El organizador ve teléfono y correo de quien pide: dato personal. Se avisa a quien pide antes de tocar «Pedir unirme».
- El código del enlace sigue guardado solo en el teléfono del organizador que lo creó.

## Decisiones y ambigüedades
- Del dueño (2026-10-20): invitar solo por QR y por enlace; unirse subiendo la imagen o pegando el enlace; el organizador ve nombre y teléfono o correo. Se muestran los tres. Confirmar si el correo debe verse.
- Se dejó «Escanear con la cámara» como enlace chico en «Unirse a un grupo», además de las dos formas pedidas.
