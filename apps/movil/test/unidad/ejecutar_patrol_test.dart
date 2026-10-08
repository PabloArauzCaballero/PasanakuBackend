import 'package:flutter_test/flutter_test.dart';

import '../../scripts/ejecutar_patrol.dart';

void main() {
  test('Patrol con cero pruebas no cumple el gate', () {
    expect(totalDePruebasPatrol('Test summary:\nTotal: 0\n'), 0);
    expect(codigoDeSalidaPatrol(0, 'Test summary:\nTotal: 0\n'), 1);
  });

  test('Patrol con pruebas ejecutadas cumple el conteo', () {
    expect(totalDePruebasPatrol('Test summary:\nTotal: 2\n'), 2);
    expect(codigoDeSalidaPatrol(0, 'Test summary:\nTotal: 2\n'), 0);
  });

  test('un proceso sin resumen tampoco cumple el gate', () {
    expect(totalDePruebasPatrol('Gradle terminó sin resumen'), isNull);
    expect(codigoDeSalidaPatrol(0, 'Gradle terminó sin resumen'), 1);
  });

  test('el código de error nativo nunca se oculta', () {
    expect(codigoDeSalidaPatrol(7, 'Test summary:\nTotal: 2\n'), 7);
  });
}
