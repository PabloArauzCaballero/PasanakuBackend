// F12.1 — CU de alta con cámara → contrato → recarga → movimientos.
// Estado de ejecución y límites: integration_test/LEEME.md.
import 'package:flutter/material.dart';
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  patrolTest('registro con cámara, contrato, recarga y movimientos', ($) async {
    await arrancarApp($);

    // El punto de entrada real es la billetera (initialLocation de crearEnrutador).
    await $(
      'Perfil',
    ).tap(); // navega a /identidad, donde arranca el alta si no hay sesión.
    await $('Empezar mi registro').tap();

    // Patrol acepta el diálogo NATIVO de permiso de cámara — esto es exactamente lo
    // que `integration_test` solo no puede hacer (planes/15 §F12.1).
    await $.native.grantPermissionWhenInUse();

    await $('Tomar foto del documento').tap();
    await $.pumpAndSettle();
    await $('Firmar el contrato de adhesión').tap();
    await $('Acepto').tap();

    await $('Recargar saldo').tap();
    await $(TextField).enterText('100');
    await $('Confirmar recarga').tap();

    await $('Ver movimientos').tap();
    await $('Recarga').waitUntilVisible();
  });
}
