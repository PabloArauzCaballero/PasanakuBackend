# Plan — Ilustraciones HD de las pautas de foto (carnet y rostro)

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (`apps/movil`) · Predecesor: `c0207532` (pautas sobrias)
- Resultado observable: en «Pautas para tus fotos» y en «Antes de escanear», el carnet de ejemplo y el
  muñeco del selfie se ven con detalle y profundidad (sombras, degradés, textura de seguridad, pelo,
  orejas, cuello, ropa, rasgos simplificados) en vez de bloques planos.
- Kill-test: el golden `ilustracion_de_pauta_escenas.png` regenerado sigue mostrando bloques planos sin
  sombra ni detalle → no está hecho.

## Alcance
- IN: `lib/pantallas/identidad/ilustraciones/ilustracion_de_pauta.dart`, `persona_de_ejemplo.dart` (nuevo, compartido),
  `lib/pantallas/identidad/ilustraciones/escena_de_rostro.dart`, golden
  `test/widget/goldens/ilustracion_de_pauta_escenas.png` (y `pautas_publicas_360.png` si cambia).
- OUT: textos de las pautas, el enum `EscenaDePauta`, la guía de cámara (`trazos_de_cara.dart`), otras pantallas.
- Ambigüedades registradas:
  - «Ultra HD» se interpreta como dibujo vectorial (CustomPainter) con mucho más detalle, nítido a
    cualquier densidad; no como fotos/bitmaps (las pautas son ilustración original sin fotos personales).
    Confirmar con Pablo.
  - El commit previo pedía un busto «sin rasgos». El pedido nuevo («muñecos dibujados» mejores) se
    interpreta como permitir rasgos simplificados genéricos. Confirmar con Pablo.

## H1 — Ilustraciones con detalle
**CA:** Dado el usuario en la pantalla de pautas, cuando ve cada pareja, entonces el carnet y el muñeco
se ven con volumen y detalle, y cada defecto (oscuro, reflejo, borroso, inclinado, ladeado, accesorios,
contraluz) se sigue distinguiendo a simple vista.
**DoD:** goldens regenerados e inspeccionados · `flutter test` de los tests de pautas en verde · `flutter analyze` sin errores nuevos.
**Estado:** HECHO (peldaño TESTED + prueba visual en 360 dp claro)

### H1.S1 — Redibujo
**CA:** como H1. **DoD:** como H1. **Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Carnet con sombra, degradé, guilloche, chip, foto con busto detallado, renglones y MRZ | el carnet claro muestra sombra y textura | `flutter test --update-goldens test/widget/ilustracion_de_pauta_golden_test.dart` + inspección del PNG | HECHO |
| H1.S1.M2 | Muñeco del selfie con pelo, orejas, cuello sombreado, ropa con cuello y rasgos simples; accesorios y contraluz coherentes | las 4 escenas de rostro muestran el muñeco detallado | mismo comando + inspección del PNG | HECHO |
| H1.S1.M3 | Regresión de tests de pautas y análisis | tests en verde, analyze sin errores | `flutter test test/widget/ilustracion_de_pauta_golden_test.dart test/widget/pautas_publicas_test.dart` · `flutter analyze lib/pantallas/identidad/ilustraciones` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Golden de `pautas_publicas_360` cambia | test en rojo | regenerarlo e inspeccionarlo |
