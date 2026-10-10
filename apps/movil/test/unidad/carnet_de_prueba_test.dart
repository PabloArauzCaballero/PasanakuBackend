import 'dart:math';

import 'package:aportaya_movil/pantallas/identidad/dominio/carnet_de_prueba.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('lee lo que la persona escribió: el cotejo coincide', () {
    final d = leerCarnetDePrueba(
      numeroEscrito: ' 8102294 ',
      lugarEscrito: 'LP',
    );
    expect(d.numeroDocumento, '8102294');
    expect(d.lugarExpedicion, 'LP');
  });

  test('sin nada escrito, un número sintético de 7 dígitos y SC', () {
    final d = leerCarnetDePrueba(azar: Random(1));
    expect(d.numeroDocumento, matches(RegExp(r'^[1-9]\d{6}$')));
    expect(d.lugarExpedicion, 'SC');
  });

  test('nunca el mismo número fijo: dos altas de prueba no chocan', () {
    final a = leerCarnetDePrueba(azar: Random(1)).numeroDocumento;
    final b = leerCarnetDePrueba(azar: Random(2)).numeroDocumento;
    expect(a, isNot(b));
    expect(a, isNot('1234567'));
  });
}
