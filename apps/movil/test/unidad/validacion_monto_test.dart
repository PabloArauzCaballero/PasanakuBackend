import 'package:aportaya_movil/dominio/validacion.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('valida montos decimales positivos sin punto flotante', () {
    for (final monto in ['', '0', '0.00', '000.00', '-1', 'NaN', '1.234']) {
      expect(montoValido(monto), false, reason: monto);
    }
    for (final monto in ['0.01', '1', '1.2', '001.00']) {
      expect(montoValido(monto), true, reason: monto);
    }
  });
}
