# Reporte — Rediseño bancario, tanda 2: pantallas de trámite

> **AVANCE: 6 / 6 — 100 %.** Seis pantallas de trámite pasan al lenguaje del rediseño; análisis, gate, pruebas y goldens en verde.

- Fecha: 2026-10-29 · Rama: `claude/rediseno-bancario-tanda-2` · Plan de origen: `docs/trabajo/2026-10-21-rediseno-bancario/PLAN.md` (H6.S2.M8, H6.S4.M2, H6.S4.M3)
- Peldaño: **TESTED** + prueba visual con el arnés de capturas (Flutter 3.44.8, fuentes reales). No VERIFIED en dispositivo.
- Skills: `pasanaku-diseno`, `pasanaku-ui-componentes`, `pasanaku-app-movil-ux`, `ux-writing-microcopy`.

## Cómo se eligieron
Con el arnés `test/capturas/rediseno` se fotografiaron las 44 pantallas del inventario en el estado actual de `dev` (1344 capturas). Las de trámite (contraseña, baja, dispositivos, verificación adicional, permuta, retiro) seguían con la composición vieja: un campo suelto sobre el fondo, sin explicar qué hace la pantalla, y botones apagados sin motivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T2.M1 | Molécula `IntroDePantalla` (ícono en círculo, título encabezado, explicación; variante `peligro`) con su prueba | `flutter test test/widget/intro_de_pantalla_test.dart` | +3 PASS |
| T2.M2 | Contraseña, baja, dispositivos y verificación adicional con entrada, tarjeta y motivo del botón apagado | `flutter test` app | +828 PASS |
| T2.M3 | Permuta y retiro: entrada, tarjeta, botón a lo ancho, motivo; estados del servidor en texto legible («Con deuda», «Pendiente») en vez de `DEUDORA` / `Estado: PENDIENTE` | `flutter test test/pasanaku` | +89 PASS |
| T2.M4 | Gate y análisis | `dart analyze --fatal-infos` · `verificar_frontend.py movil/diseno` | No issues · TODO OK |
| T2.M5 | Goldens | `flutter test test/goldens` | diseno +149 (unidad+widget+goldens) · movil +274 |
| T2.M6 | Capturas antes/después, claro y oscuro | arnés (`FASE=tanda2`) | `evidencia/tramites-antes-despues.png`, `evidencia/tramites-oscuro.png` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Resto de H6 (turno, verificación del sorteo, transparencia, reclamo, KYC, notificaciones, recargar/retirar) | TODO | Próximas tandas |
| H4/H5 del plan (campos con relleno suave, cabecera que se compacta, motion) | TODO | — |

## No cubierto
- Dispositivo real. Los textos nuevos solo describen lo que la pantalla hace: «confiar en el dispositivo» no se afirma que evite el código porque el estado no se envía a ningún servicio (`estado_sesion.dart`).

## Decisiones
- `IntroDePantalla` se agregó a `diseno_flutter` porque el patrón se repite en seis pantallas y no existía equivalente (`EstadoVacio` es para vacíos).
- Las 2034 capturas del arnés no se versionan; se guardan solo las hojas comparativas.
