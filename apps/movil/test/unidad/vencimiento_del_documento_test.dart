import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/validaciones.dart';
import 'package:flutter_test/flutter_test.dart';

/// El vencimiento del carnet en el alta (H12 · H6). Es ayuda: la regla la aplica el
/// servidor (AP-CU01-07), y estas pruebas fijan que la ayuda diga lo mismo.
void main() {
  final hoy = DateTime(2026, 9, 24, 15, 30);

  test('sin fecha pide elegirla', () {
    expect(
      errorVencimientoDocumento(null, hoy: hoy),
      'Elegí hasta cuándo vale tu carnet.',
    );
  });

  test('vencido ayer no sirve', () {
    expect(
      errorVencimientoDocumento(DateTime(2026, 9, 23), hoy: hoy),
      'Tu carnet está vencido. Para abrir la cuenta hace falta uno vigente.',
    );
  });

  test('vence hoy todavía vale, igual que en el servidor', () {
    expect(errorVencimientoDocumento(DateTime(2026, 9, 24), hoy: hoy), isNull);
  });

  test('vence más adelante vale', () {
    expect(errorVencimientoDocumento(DateTime(2031, 1, 1), hoy: hoy), isNull);
  });

  test('el rechazo del servidor se muestra con el mismo texto', () {
    expect(
      mensajeDe('AP-CU01-07'),
      'Tu carnet está vencido. Para abrir la cuenta hace falta uno vigente.',
    );
  });
}
