# Plan — segunda pasada de marketing: entrada + alta

- Fecha: 2026-10-29 · Repos afectados: PasanakuBackend (`apps/movil`) · Predecesor: rediseño bancario (2026-10-21) y primera pasada de signup/onboarding (15dc6699)
- Resultado observable: quien abre la app entiende en la portada qué es y qué le van a pedir (sin promesas falsas), y cada pantalla de ingreso/alta tiene una acción principal con verbo concreto y errores/vacíos que orientan.
- Kill-test: una captura `despues-marketing` de la portada a 360 que todavía corte la lista «qué te pedimos» o diga que el carnet es obligatorio para abrir la cuenta.
- Brief: `scratchpad/marketing/BRIEF.md` · Stock: `../stock-entrada-alta.md`

## Alcance
- IN: textos y cuerpo (no el `Scaffold`/`appBar`) de portada, tour, ingreso, mfa, registro, pautas, cámara (aviso de permiso), subida, estado, bienvenida, verificar-identidad, identidad-requerida; sus tests de texto y goldens.
- OUT: `Scaffold`/`appBar` (otro agente migra a `PantallaConCabecera`), perfil/trámites/KYC de sesión, `packages/diseno_flutter`, navegación, validación, API.
- Ambigüedad: quitar «Confirmar contraseña» del paso 1 — decisión de negocio pendiente (DECISION_REQUIRED, Pablo). No se toca.

## H1 — Entrada y alta con copy y jerarquía de conversión
**CA:** Dado alguien sin cuenta, cuando recorre portada → alta, entonces cada pantalla dice qué hace, pide solo lo que el código pide y tiene un primario claro.
**DoD:** `dart analyze --fatal-infos lib test` · `python3 scripts/verificar_frontend.py movil` · suite de flutter test del brief · capturas `despues-marketing` miradas.
**Estado:** HECHO (verificación del brief en verde; la revisión humana va en el PR)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Portada: titular verificable, garantías en fila de 3, «qué te pedimos» veraz (carnet postergable) | La lista entra entera a 360 y no dice que el carnet es obligatorio | captura portada 360 claro/oscuro | HECHO |
| H1.S1.M2 | Tour: CTA final y salida dicen a dónde llevan | Botón final = «Crear mi cuenta» | `flutter test test/widget/tour_test.dart` | HECHO |
| H1.S1.M3 | Ingreso: el botón apagado dice por qué | Texto bajo «Ingresar» cuando falta dato | `flutter test test/identidad` | HECHO |
| H1.S1.M4 | MFA: textos con tildes y voseo, sin jerga, en textos.dart | Sin «verificacion», «Revisa» | `flutter test test/identidad/pantalla_mfa_test.dart` | HECHO |
| H1.S2.M1 | Registro: intro del paso 1 dice qué viene (y que el carnet se puede postergar) | Sin «coincidir con tu documento» en el paso de contacto | captura registro | HECHO |
| H1.S2.M2 | Pautas: `SelectorSegmentado` del sistema (sin naranja) | Sin `SegmentedButton` | `flutter test test/widget/pautas_publicas_test.dart` | HECHO |
| H1.S2.M3 | Cámara (documento): el aviso de permiso dice para qué | Texto de uso en modo documento | `flutter test test/widget` | HECHO |
| H1.S2.M4 | Subida: pendiente orienta («En espera»); la bajada se probó y se sacó (ver stock) | Sin «—» mudo | goldens h8/h10 subida | HECHO |
| H1.S2.M5 | Bienvenida: sin «nueva cuenta», pasos con verbo | Texto en textos | goldens bienvenida | HECHO |
| H1.S2.M6 | Identidad requerida: qué se pide y tokens de tipografía | Sin `titleLarge`/56 literal | `flutter test test/identidad/identidad_requerida_test.dart` | HECHO |
| H1.S3.M1 | Verificación completa + capturas + stock | Todo verde | comandos del brief | HECHO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Choque con la migración a `PantallaConCabecera` | merge | no tocar `Scaffold`/`appBar` |
| Archivos al borde de 200 líneas | gate | mover textos a archivos de textos |

## Cierre (2026-10-29)
- `dart analyze --fatal-infos lib test` → `No issues found!`
- `python3 scripts/verificar_frontend.py movil` → `TODO OK`
- Suite del brief → `06:16 +1125: All tests passed!` (después, solo se tocaron las tarjetas de la portada; sus pruebas → `+32: All tests passed!`)
- Capturas `despues-marketing` (entrada 120, alta 324), miradas en claro y oscuro a 360 y 412.
- Merge de `origin/dev` (#296, `PantallaConCabecera` en pautas) sin conflictos.
