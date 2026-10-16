# Plan — Pautas de foto: ejemplos de carnet y de rostro, consejos y aviso antes de escanear

- Fecha: 2026-10-15 · Repos: Pasanaku · Rama: `pablo/feat/pautas-de-foto-2026-10-15` (desde `origin/test` dbfbf35b) · Predecesor: `2026-10-15-visual-bancario` (guías de cámara)
- Pedido literal: «¿arreglaste la foto de guía en la foto de carnets y de cómo sacarse foto? … está pésimo, horripilante».
- Resultado observable: en «Pautas para tus fotos», en los consejos de la cámara y en «Antes de escanear», la persona ve ejemplos sobrios y legibles: un carnet con proporción ID-1 real y una silueta de busto neutra dentro del óvalo, y los defectos dibujados de verdad (desenfoque real, perspectiva real, reflejo, contraluz, lentes y gorra).
- Kill-test: golden de las 9 escenas; si aparece la cabeza verde con ojos de punto, óvalos blancos como «borroso» o un carnet sólo girado como «inclinado», NO está hecho.

## Alcance
- IN: `ilustraciones/ilustracion_de_pauta.dart`, `pantalla_pautas_de_fotos.dart` (marco de color del ejemplo), `pasos_alta/consejos_para_la_foto.dart` (ícono por consejo), `pasos_alta/hoja_antes_de_escanear.dart` (ilustración), sus tests y goldens.
- OUT: textos de las pautas (no se cambian), dominio `pautas_de_fotos.dart`, lógica de captura.
- Ambigüedades: A1 «horripilante» no dice qué parte; supuesto: las tres piezas de guía de foto. Confirmar con Pablo en el dispositivo.

## H1 — Guías de foto sobrias
**CA:** Dado el alta, cuando la persona abre las pautas, los consejos de la cámara o el aviso antes de escanear, entonces los ejemplos muestran el defecto de forma inequívoca y con el estilo de la cámara (óvalo único, carnet ID-1).
**DoD:** goldens nuevos mirados (claro/oscuro) · tests dirigidos en verde · `dart analyze --fatal-infos` limpio · regresión sin rojos nuevos.
**Estado:** A MEDIAS — microtareas HECHO; falta verlo en el teléfono y que Pablo confirme A1

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Carnet ID-1 + defectos reales (oscuro, reflejo, desenfoque con blur, perspectiva) | golden muestra los 5 carnets distinguibles | `flutter test test/widget/ilustracion_de_pauta_golden_test.dart --update-goldens` + inspección | HECHO |
| H1.S1.M2 | Rostro: busto neutro en óvalo (frente, ladeado fuera del óvalo, lentes+gorra, contraluz) | golden sin cara verde ni ojos de punto | ídem | HECHO |
| H1.S1.M3 | Ejemplo con marco verde/rojo según correcto/incorrecto | además del texto «Correcto/Incorrecto» | `flutter test test/widget/pautas_publicas_test.dart test/widget/pautas_publicas_golden_test.dart` | HECHO |
| H1.S2.M1 | Consejos de cámara con un ícono por consejo | no son 4 tildes iguales | `flutter test test/widget/roles_color_acento_test.dart test/widget/pantalla_camara_de_respaldo_test.dart` | HECHO |
| H1.S2.M2 | «Antes de escanear» con la ilustración del carnet | la hoja muestra el ejemplo | `flutter test test/widget/hoja_antes_de_escanear_test.dart` | HECHO |
| H1.S3.M1 | Análisis + regresión de la app | sin rojos nuevos vs. `fallos-final.lst` del trabajo anterior | `dart analyze --fatal-infos` + `flutter test` | HECHO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Goldens que contienen estas piezas cambian | diff grande | se regeneran solo los que las contienen y se miran |
