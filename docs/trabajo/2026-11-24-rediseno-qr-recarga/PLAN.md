# Plan — Rediseño de la pantalla de recarga por QR (app móvil)

- Fecha: 2026-11-24 · Repos afectados: `apps/movil` (solo `lib/pantallas/billetera/` y `test/`) · Predecesor: `2026-11-23-continuar-traspaso` (FLUJO-QR-001)
- Resultado observable: quien recarga por QR ve primero cuánto se acredita y el QR, un estado vivo
  «esperando el pago», los pasos para pagar y su saldo como dato secundario; acreditada, error y la
  build sin pagos de prueba se ven igual de claros, en 360/412/768 y en claro/oscuro.
- Kill-test: la captura 412 claro «pendiente» sigue mostrando el saldo más grande que el monto, o
  algún test de `cobro_de_recarga_test.dart` / `pantalla_recargar_test.dart` queda en rojo.

## Alcance
- IN: `cobro_de_recarga.dart`, `saldo_de_la_recarga.dart`, `textos.dart` y piezas nuevas en
  `lib/pantallas/billetera/`; tests de esa pantalla y sus capturas.
- OUT: backend, contratos, proveedores/dominio (`cu10_*`), `packages/diseno_flutter`, otras pantallas.
- Ambigüedades registradas: el contrato sólo trae `acreditara` (no el importe a pagar). Supuesto:
  el monto protagonista se rotula «Monto a acreditar», no «a pagar». Confirmar con Pablo.

## H1 — Pantalla de cobro por QR rediseñada
**CA:** Dado una orden con QR, cuando se muestra la pantalla, entonces el monto y el QR son lo
principal, el estado de espera es visible y vivo, y los 4 estados se ven sin desbordes.
**DoD:** format + analyze + tests dirigidos + `test:front` + capturas inspeccionadas.
**Estado:** HECHO

### H1.S1 — Implementación visual
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Capturas «antes» | 24 PNG en `evidencia/movil-antes/` | `ls … \| wc -l` → 24 | HECHO |
| H1.S1.M2 | Rediseño de `CobroDeRecarga` y piezas | tests dirigidos verdes | `flutter test test/widget/cobro_de_recarga_test.dart test/widget/pantalla_recargar_test.dart` | HECHO |
| H1.S1.M3 | Capturas «después» en 3 anchos × 2 temas × 4 estados | 24 PNG sin excepción, revisados | `flutter test --update-goldens test/capturas/qr_recarga_capturas_test.dart test/capturas/sin_pagos_de_prueba_capturas_test.dart` | HECHO |
| H1.S1.M4 | Gates | sin issues, suite verde | `dart format --set-exit-if-changed lib test` · `dart analyze --fatal-infos` · `flutter test <carpetas de test:front>` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Un latido en bucle rompe `pumpAndSettle` | tests colgados | latido finito re-armado con `Timer`, como `SuperficieViva` |
| Cambiar el padre del saldo pierde el estado de `MontoQueSube` | el saldo sube desde cero | misma posición y clave del saldo en todos los estados |
