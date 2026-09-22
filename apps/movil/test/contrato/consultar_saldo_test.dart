import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// **La prueba de contrato del lado Dart**: el ejemplo que Prism sirve y que Vitest
/// usa se lee con el tipo GENERADO del contrato. Si el ejemplo se edita a mano y deja
/// de encajar, esta prueba lo dice antes que la pantalla.
void main() {
  final forma = RegExp(r'^-?\d+\.\d{2}$');

  for (final escenario in ['ok', 'vacio', 'adverso']) {
    test(
      'consultarSaldo · $escenario encaja en SaldoBilletera y los importes son cadena',
      () {
        final e = ejemplo('nucleo-financiero', 'consultarSaldo', escenario);
        expect(e['estado'], 200);
        final saldo = SaldoBilletera.fromJson(
          e['cuerpo'] as Map<String, dynamic>,
        );
        expect(saldo.disponible.monto, matches(forma));
        expect(saldo.retenido.monto, matches(forma));
        expect(saldo.disponible.moneda.value, anyOf('BOB', 'USD'));
      },
    );
  }

  test('el rechazo trae código y traza, que es lo que la app traduce', () {
    final e = ejemplo('nucleo-financiero', 'consultarSaldo', 'rechazo');
    final cuerpo = e['cuerpo'] as Map<String, dynamic>;
    expect(e['estado'], greaterThanOrEqualTo(400));
    expect(cuerpo['codigo'], isA<String>());
    expect(cuerpo['trazaId'], isA<String>());
  });
}
