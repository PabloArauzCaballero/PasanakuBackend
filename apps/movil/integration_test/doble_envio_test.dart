// F12.1 — Dos toques rápidos en cada operación de dinero ⇒ UN efecto, misma
// Idempotency-Key. Estado de ejecución: integration_test/LEEME.md.
//
// Ver el invariante 6 del frontend (planes/15, gate de salida): "doble envío
// bloqueado en TODA operación de dinero". Este archivo lo ejercita para recarga y
// aporte; agregar el resto de operaciones de dinero (retiro, pago de mora) es
// extender esta misma tabla, no un archivo nuevo.
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  for (final operacion in ['Recargar saldo', 'Pagar con saldo']) {
    patrolTest('doble toque en "$operacion" produce un único movimiento', (
      $,
    ) async {
      await arrancarApp($);

      await $(operacion).tap();
      await $('Confirmar').tap();
      await $(
        'Confirmar',
      ).tap(); // segundo toque antes de que responda el servidor

      await $('Ver movimientos').tap();
      // Un solo movimiento con esa causa — no dos.
      expect($(operacion).$(Text).evaluate().length, 1);
    });
  }
}
