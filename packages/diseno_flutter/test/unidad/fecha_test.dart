import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test(
    'un día calendario se muestra tal cual, sin correrse por zona horaria',
    () {
      // El valor que devuelve showDatePicker: medianoche local, sin zona.
      expect(Fecha.formatearDia(DateTime(1995, 10, 15)), '15 oct 1995');
      expect(Fecha.formatearDia(DateTime(2001, 1, 1)), '1 ene 2001');
      expect(Fecha.formatearDia(DateTime(1990, 12, 31)), '31 dic 1990');
    },
  );

  test('un instante en UTC sí se pasa a la hora de La Paz', () {
    expect(
      Fecha.formatear('2026-10-03T02:30:00Z'),
      '2 oct 2026, 22:30 (La Paz)',
    );
  });
}
