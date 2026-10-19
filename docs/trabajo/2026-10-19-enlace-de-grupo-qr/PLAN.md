# Plan — Enlace de grupo con QR que abre la app

- Fecha: 2026-10-19 · Repos: PasanakuBackend (servicios/identidad, servicios/grupos, docs/entidades, apps/movil) · Predecesor: `docs/trabajo/2026-10-19-app-arreglos-del-telefono/`
- Rama: `claude/enlace-de-grupo-qr-20261019` (desde `test`)
- Resultado observable: el organizador abre «Invitar» en su grupo, ve un QR y un enlace del grupo (para cualquiera), lo comparte por WhatsApp; quien lo toca en un Android con la app instalada entra a la app (no a una página), ve el grupo y pide unirse; el organizador ve la solicitud con el nombre de la persona y la acepta o rechaza.
- Kill-test: tocar el enlace `https://app.aportaya.161.97.85.216.sslip.io/g/<código>` en WhatsApp y que se abra el navegador en vez de la app.

## Decisiones del usuario (2026-10-19)
- Ingreso por enlace: **el organizador aprueba** cada solicitud.
- Vigencia: **hasta llenar cupos o 7 días**, o hasta que el organizador lo anule (puede generar otro).

## Hallazgos (fase 1)
- Hoy solo hay invitaciones individuales: `invitarAlGrupo` (CU-69) atada a un teléfono, token de un solo uso emitido por `identidad`, una vigente por teléfono (`AP-CU69-09`). No existe enlace de grupo.
- El organizador ya puede listar y decidir solicitudes: `listarSolicitudesDeIngreso` / `decidirSolicitudDeIngreso` (`@Permiso("GRUPO_ADMINISTRAR")`, `web/RutasDeSolicitudes.java`). La app no tiene pantalla para eso.
- `SolicitudDeIngreso` solo trae `usuarioId`: sin nombre. `MiPerfil` (identidad) no trae nombre; `identidad.usuario` sí tiene `nombres`/`apellidos`.
- `grupos` habla con `identidad` propagando el token del usuario (`TokensDeInvitacionPorHttp`, `ClienteDeServicio::propagarElToken`).
- La app solo atiende `aportaya://` (no tocable en WhatsApp). Ya existe el átomo `CodigoQR` (`qr_flutter`).
- La app web la sirve nginx (`apps/movil/docker/nginx.conf`) con rutas `#/`. APK firmado con la clave debug de esta máquina (SHA-256 `F6:0A:…:5C:B9`).
- El esquema sale de `docs/entidades/*.puml` (skill `boveda-modelo`).

## Alcance
- IN: enlace de grupo (emitir, anular, consultar, solicitar), nombre visible en la solicitud, pantallas de la app (invitar con QR, solicitudes, unirse por enlace), App Links Android + `assetlinks.json` + redirección `/g/` en la web.
- OUT: iOS Universal Links; Play Store; cambiar las invitaciones individuales existentes (siguen andando).
- Ambigüedades registradas:
  - Nombre que ve el organizador: se toma «primer nombre + inicial del apellido» (por ej. «Rosa Q.»), del nombre verificado. Confirmar con el usuario.
  - Sin la app instalada, el enlace abre la app web en la misma pantalla de «unirse».

## H1 — identidad: nombre visible en «mi perfil»
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | `MiPerfil.nombreVisible` («Rosa Q.», o null sin nombre) | `GET /usuarios/yo/perfil` lo devuelve | test web de identidad | HECHO |

## H2 — grupos: modelo y caso de uso
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | Caso de uso del enlace de grupo (criterios Gherkin) | `verificar_criterios.py` OK | comando | HECHO |
| H2.M2 | Tabla `enlace_grupo` + columnas de `solicitud_ingreso` (`enlace_grupo_id`, `nombre_visible`) en el `.puml` | generadores y `verificar_boveda.py` OK | comandos | HECHO |

## H3 — grupos: contrato y casos de uso
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.M1 | Contrato: emitir/anular (organizador), consultar/solicitar (participante); `SolicitudDeIngreso.nombreVisible` | clientes generados compilan | `generateOpenApiClients` | HECHO |
| H3.M2 | Emitir: uno activo por grupo (el nuevo anula el anterior), token aleatorio guardado como hash, 7 días | test | `./gradlew :servicios:grupos:test` | HECHO |
| H3.M3 | Consultar y solicitar: vigente, no lleno, no participante, sin solicitud pendiente; crea solicitud PENDIENTE con nombre visible | test con los tres niveles | igual | HECHO |
| H3.M4 | Autorización negativa: no organizador no emite/anula; sin sesión no consulta | test | igual | HECHO |

## H4 — app
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.M1 | «Invitar»: QR + compartir por WhatsApp + copiar + anular | prueba de widget | `flutter test` | HECHO |
| H4.M2 | «Solicitudes»: lista con nombre, aceptar/rechazar | prueba de widget | igual | HECHO |
| H4.M3 | `/g/<código>`: ver grupo y pedir unirse (con identidad verificada) | prueba de widget | igual | HECHO |
| H4.M4 | Enlaces `https://…/g/<código>` llevan a esa pantalla | prueba unitaria | igual | HECHO |

## H5 — abrir la app desde WhatsApp
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.M1 | `intent-filter` https con `autoVerify` + `assetlinks.json` + nginx `/g/` | `adb shell pm get-app-links` → verified; `/g/x` en el navegador → `#/g/x` | emulador + curl | A MEDIAS — escrito y nginx -t OK; falta verificar tras desplegar |

## H6 — entrega
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H6.M1 | CI, merge, despliegue TEST | servicios sanos | log del servidor | TODO |
| H6.M2 | APK v11 | APK en Descargas | aapt | TODO |
| H6.M3 | Prueba en teléfonos reales | el usuario lo recorre | usuario | TODO |

## Riesgos
| Riesgo | Mitigación |
|---|---|
| App Links con dominio sslip.io: la verificación de Android puede fallar | Probar en emulador con `pm verify-app-links`; si falla, la web en `/g/` muestra «Abrir en la app» con `intent://` |
| La clave del APK del CI es otra | `assetlinks.json` lista la huella de la clave con la que se firma el APK de prueba |
