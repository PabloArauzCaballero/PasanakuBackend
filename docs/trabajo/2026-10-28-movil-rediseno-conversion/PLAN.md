# Plan — App móvil: rediseño con foco en conversión (portada, registro y onboarding)

- Fecha: 2026-10-28 · Repos afectados: PasanakuBackend (`apps/movil`, `packages/diseno_flutter` si hace falta) · Predecesor: ninguno
- Resultado observable: quien abre la app nativa por primera vez entiende en la portada qué gana, cuánto tarda el alta y qué necesita, llega al alta sin pasos obligatorios de más y termina con un siguiente paso claro.
- Kill-test: abrir la portada y tocar «Crear mi cuenta» obliga a recorrer el tour de 4 láminas antes del alta (hoy) → no está hecho.
- Skills leídas: `pasanaku-diseno`, `pasanaku-ui-componentes`, `pasanaku-app-movil-ux`; de marketing (`mantra-core-health/.claude/skills`): `onboarding`, `signup`.

## Alcance
- IN: portada, tour, entrada al alta (expectativas: tiempo y qué se necesita), textos de conversión, primer paso tras el alta.
- OUT: cambiar campos o pasos regulatorios del alta (KYC, contrato, perfil transaccional); backend; la versión web pública.
- Ambigüedades registradas:
  - El tiempo que tarda el alta se declara como estimación a confirmar con Pablo; no se inventan métricas ni testimonios (regla 00.2).
  - «Rediseño completo» de la app se hace por tandas después de esta; esta tanda es portada/registro/onboarding.

## H1 — Portada y entrada al alta orientadas a conversión
**CA:** Dado alguien sin cuenta, cuando abre la app, entonces ve la propuesta de valor, el tiempo y lo que necesita para el alta, y «Crear mi cuenta» lo lleva al alta (el tour queda opcional).
**DoD:** `dart analyze --fatal-infos` · `dart format` · `verificar_frontend.py movil` · pruebas dirigidas · capturas 360/412 claro y oscuro.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.M1 | Línea base: análisis y pruebas de la app | se conoce el estado previo | `dart analyze --fatal-infos` + `flutter test` | HECHO |
| H1.M2 | Capturas «antes» del flujo portada → tour → alta | capturas en `evidencia/` | build web + Playwright | HECHO |
| H1.M3a | «Crear mi cuenta» abre el alta directo; el tour queda en «¿Cómo funciona?» | `/registro` tras el toque | `flutter test test/widget/portada_test.dart` | HECHO |
| H1.M3b | La portada dice qué hay que tener a mano (`LoQueNecesitas`) | los cuatro textos visibles | `flutter test test/widget/portada_test.dart` | HECHO |
| H1.M3c | Goldens de la portada regenerados con Flutter 3.44.8 (la del CI) y mirados | golden nuevo coincide | `flutter test test/goldens/h10_portada_visual_test.dart test/goldens/h8_portada_mfa_oscuro_test.dart` | HECHO |
| H1.M3d | Suites completas de la app sin regresiones | front, a11y y goldens en verde | `flutter test …` (3.44.8) | EN CURSO |
| H1.M4 | Capturas «después» y comparación | capturas en `evidencia/` | build web + Playwright | TODO |

## Decisiones
- El tour deja de ser obligatorio antes del alta (skill `signup`/`onboarding`: no atrapar en tutoriales). Cambia el requisito que fijaba `portada_test.dart`; la prueba se actualizó al requisito nuevo (pedido de Pablo, 2026-10-28), no se debilitó.
- **Propuesta no aplicada**: sacar «Confirmar contraseña» del paso 1 (la skill `signup` lo desaconseja y el campo ya tiene el ojo). Toca validación del alta y sus pruebas: espera confirmación de Pablo.
- Los goldens de la app se comparan en Linux con Flutter 3.44.8 (`ci.yml`, job `app-goldens`); con 3.47.8 fallan aun sin cambios. Se regeneraron con 3.44.8.
