# Plan — Pantalla de escaneo de QR y pantalla de perfil, mejor terminadas

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (`apps/movil`) · Rama: `claude/escanear-y-perfil-20261030` desde `origin/test` (`edc5f310`), en el worktree `../Pasanaku-qr-perfil`
- Predecesor: ninguno (la foto de perfil, `509dad69`, ya está en test)
- Pedido de Pablo: «mejoremos ya mismo el escaneado del QR porque puede verse mucho mejor la pantalla … la pantalla de mi perfil puede verse muchísimo mejor».
- Resultado observable: en la app, «Escanear invitación» muestra un visor cuadrado con marco de esquinas, velo fuera del marco y una pista de dónde apuntar; el enlace manual queda en una tarjeta aparte. «Tu perfil» abre con una cabecera verde con foto, nombre y botón de foto, y agrupa cuenta, seguridad y salida.
- Kill-test: capturas de «después» iguales a las de «antes» (`scratchpad/qr-perfil/antes/*.png`) o algún test existente de estas pantallas en rojo.

## Hechos (descubrimiento)
- Escaneo: `lib/pantallas/pasanaku/pantalla_escanear.dart` (visor de 320 px de alto, sin marco); puerto `dominio/puertos/visor_qr.dart`; adaptador `infraestructura/visor_qr_mobile_scanner.dart`. Tests: `test/widget/pantalla_escanear_test.dart`, `test/widget/acceso_escanear_test.dart`, goldens `test/goldens/pantalla_escanear_golden_test.dart` (estado denegado).
- Perfil: `lib/pantallas/identidad/pantalla_perfil.dart`, `foto_de_perfil_editable.dart` (Avatar + «Agregar foto»), `seccion_verificacion_kyc.dart`. Tests: `pantalla_perfil_{correo,foto,cerrar_sesion}_test.dart`, goldens `h10_perfil_visual_test.dart`, `h8_perfil_oscuro_test.dart`, `h8_break_ui_test.dart`.
- Tokens ya existentes para esto: `camaraVelo/camaraGuia/camaraTinta` (visor de identidad) y `heroInicio/heroFin/heroBrillo/heroTexto/heroTexto2` + `Elevacion.heroe` (patrón de `organismos/encabezado_de_saldo.dart:64-83`).
- El test de correo exige un `TextField` deshabilitado bajo «Correo» → el campo se mantiene.

## Alcance
- IN: `pantalla_escanear.dart`, nuevo `marco_de_escaneo.dart`, `textos_invitacion.dart` (1 texto), `pantalla_perfil.dart`, `foto_de_perfil_editable.dart`, `textos_del_perfil.dart` (1 texto), goldens de esas pantallas.
- OUT: lógica de lectura del QR y del cerrojo, adaptador de `mobile_scanner`, linterna, leer QR desde galería, contratos/backend, otras pantallas.
- Ambigüedades: «mucho mejor» no define estilo → se sigue el sistema existente (cabecera verde del saldo, velo de la cámara de identidad). Confirmar con Pablo con las capturas.
- Fase 6 (visual): capturas a 390 dp claro/oscuro + goldens existentes (360/412/600 y tres viewports del escaneo).

## H1 — Escaneo de QR con visor guiado
**CA:** Dado quien abre «Escanear invitación», cuando la cámara anda, entonces ve un visor cuadrado con marco de esquinas, velo afuera y la pista «Centrá el QR dentro del marco»; si el QR no es una invitación, el marco se pone en rojo además del aviso; el enlace manual sigue disponible en su tarjeta.
**DoD:** `flutter test test/widget/pantalla_escanear_test.dart test/widget/acceso_escanear_test.dart test/goldens/pantalla_escanear_golden_test.dart` en verde · capturas inspeccionadas · `flutter analyze` sin issues.
**Estado:** HECHO

### H1.S1 — Visor y tarjeta del enlace
**CA/DoD:** como H1. **Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `MarcoDeEscaneo`: velo con hueco redondeado, esquinas que se cierran al entrar (respeta movimiento reducido), color de error | se ve el marco sobre el visor | captura `despues/escanear_*.png` inspeccionada | HECHO |
| H1.S1.M2 | Visor cuadrado + pista + enlace manual en tarjeta con título | tests de escaneo en verde | `flutter test test/widget/pantalla_escanear_test.dart test/widget/acceso_escanear_test.dart` | HECHO |
| H1.S1.M3 | Goldens del escaneo regenerados | golden verde tras regenerar e inspeccionar | `flutter test test/goldens/pantalla_escanear_golden_test.dart` | HECHO |

## H2 — Perfil con cabecera y secciones
**CA:** Dado quien abre «Tu perfil», entonces ve arriba una tarjeta verde con su foto (o iniciales), su nombre y el botón de foto; debajo «Tu cuenta» (verificación y correo), «Seguridad y ayuda», cerrar sesión y dar de baja, en claro y oscuro.
**DoD:** `flutter test test/widget/pantalla_perfil_correo_test.dart test/widget/pantalla_perfil_foto_test.dart test/widget/pantalla_perfil_cerrar_sesion_test.dart` verde · goldens de perfil regenerados e inspeccionados · `flutter analyze` sin issues.
**Estado:** HECHO

### H2.S1 — Cabecera y agrupación
**CA/DoD:** como H2. **Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `FotoDePerfilEditable` como cabecera verde: avatar con aro y toque para cambiar, nombre, botón | tests de foto en verde | `flutter test test/widget/pantalla_perfil_foto_test.dart` | HECHO |
| H2.S1.M2 | Sección «Tu cuenta» con verificación y correo | tests de correo y cierre en verde | `flutter test test/widget/pantalla_perfil_correo_test.dart test/widget/pantalla_perfil_cerrar_sesion_test.dart` | HECHO |
| H2.S1.M3 | Goldens de perfil regenerados e inspeccionados | goldens verdes tras regenerar | `flutter test test/goldens/h10_perfil_visual_test.dart test/goldens/h8_perfil_oscuro_test.dart test/goldens/h8_break_ui_test.dart` | HECHO |

## H3 — Regresión
| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Suite completa de la app + lint | sin fallos nuevos frente a `origin/test` | `flutter test` y `flutter analyze` (comparar con línea base) | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Animación en bucle cuelga `pumpAndSettle` en tests | tests en rojo | solo animación de entrada (se asienta), nada en bucle |
| Botón claro sobre verde con poco contraste | accesibilidad | se usa `secundario` (superficie clara) y se mira en ambos temas |
