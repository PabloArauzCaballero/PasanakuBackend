# Plan — App: subir lo pendiente, aprobación sin reingresar, recargar pantallas, invitar de verdad

- Fecha: 2026-10-18 · Repo: PasanakuBackend, rama `claude/app-kyc-diferido-y-pin-20261018` (desde `origin/test`) · Predecesores: `alta-atlas-sesion-work/docs/trabajo/2026-10-17-kyc-diferido`, `2026-10-17-correcciones-app-v5`
- Pedido del usuario (2026-10-18), en su orden:
  1. ¿Se desplegó? → el backend #126 ya estaba en TEST (verificado con un alta sin carnet que llega a la validación del correo).
  2. Goldens del CI.
  3. Subir lo pendiente (la app vivía sin commit en `alta-atlas-sesion-work`).
  4. Al aprobar la cuenta, la app no se entera hasta salir y volver a entrar.
  5. Recargar las pantallas (p. ej. Grupos tras crear uno) sin salir.
  6. Invitar: el enlace `aportaya://…` no sirve y «Le va a llegar a +591…» es falso (no hay SMS/WhatsApp conectados).
  7. Pruebas por partes.
- Kill-test: aprobar una cuenta desde el backoffice y que la app siga con el aviso «Estamos verificando» después de un minuto; o compartir una invitación y que WhatsApp no la muestre como enlace tocable.

## Alcance
- IN: traslado de la app (apps/movil, packages/diseno_flutter, packages/tokens) desde `alta-atlas-sesion-work` con merge de tres vías contra `test`; vigilante de aprobación; RefreshIndicator en las pantallas de datos; enlace https de invitación + compartir nativo (WhatsApp/SMS/otra app) sin dependencias nuevas; goldens regenerados en Linux (CI) solo para pantallas cambiadas a propósito.
- OUT: proveedor real de SMS/WhatsApp desde el servidor; Android App Links verificados (assetlinks); iOS nativo de compartir (cae a copiar).
- Ambigüedades: «recargar esa pantalla» → deslizar hacia abajo (pull-to-refresh) en grupos, mi estado, billetera y perfil. Confirmar con el usuario.

## H1 — Subir la app pendiente
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Traslado + merge de tres vías; resolver conflictos (traducción de errores movida a `traduccion_de_errores.dart`, ingreso por `identificador`) | `flutter analyze` limpio | `flutter analyze` → No issues found | HECHO |
| H1.M2 | Suite no visual de la app en verde | 0 fallas nuevas | `flutter test` (sin goldens/capturas) → +709 −7 (las 7 son píxeles, `evidencia/h1-suite-no-visual.txt`) | HECHO |

## H2 — Aprobación sin reingresar
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | Vigilante: con la cuenta sin habilitar, consulta el estado cada 30 s y al volver a la app; si está verificada renueva el acceso solo | Aprobada → el aviso desaparece sin tocar nada | test de widget con reloj falso (correcto / sigue en revisión / falla de red) | TODO |

## H3 — Recargar pantallas
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.M1 | Deslizar hacia abajo recarga Grupos (y el resto de pantallas con datos) | Al soltar, vuelve a pedir los datos | test de widget: `fling` → segunda llamada | TODO |

## H4 — Invitar de verdad
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.M1 | Enlace https a la app web (`#/pasanaku/unirse/<código>`) armado desde el del servidor | WhatsApp lo muestra como enlace | test unitario (correcto / sin esquema / inválido) | TODO |
| H4.M2 | Canal nativo `bo.aportaya/compartir`: WhatsApp al número, SMS al número, hoja de compartir | Abre la app elegida con el mensaje | compila en el APK; prueba en teléfono | TODO |
| H4.M3 | Textos honestos: sin «Le va a llegar»; botones Enviar por WhatsApp / SMS / otra app / Copiar | Texto nuevo visible | test de widget de `InvitacionCreada` | TODO |

## H5 — CI y entrega
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.M1 | PR a `test`, goldens regenerados en Linux para las pantallas cambiadas | CI verde | `gh pr checks` | TODO |
| H5.M2 | APK v8 en Descargas | Archivo existe | `ls` | TODO |
