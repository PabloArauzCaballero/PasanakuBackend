import 'dart:convert';
import 'dart:io';

import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/dinero/interpretar.dart';
import 'package:flutter_test/flutter_test.dart';

/// F1.S4.M1: la app interpreta lo escrito igual que la referencia de `packages/tokens`.
/// Antes «12.50» viajaba como `1250.00`: cien veces más.
void main() {
  var dir = Directory.current;
  while (!File('${dir.path}/settings.gradle.kts').existsSync()) {
    dir = dir.parent;
  }
  final vectores =
      jsonDecode(
            File(
              '${dir.path}/packages/tokens/vectores/monto-escrito.json',
            ).readAsStringSync(),
          )
          as List<dynamic>;

  test('pasa los vectores compartidos con Angular, en los tres niveles', () {
    final niveles = <String>{};
    for (final v in vectores.cast<Map<String, dynamic>>()) {
      niveles.add(v['nivel'] as String);
      expect(
        interpretarMontoEscrito(v['escrito'] as String),
        v['contrato'],
        reason: 'escrito: «${v['escrito']}»',
      );
      expect(
        CampoMonto.aCadenaDelContrato(v['escrito'] as String),
        v['contrato'],
      );
    }
    expect(niveles, {'correcto', 'limite', 'invalido'});
  });

  test('el campo dice qué se va a enviar, o el error asociado', () {
    expect(CampoMonto.lecturaDe('', 'BOB'), (ayuda: null, error: null));
    expect(CampoMonto.lecturaDe('12.50', 'BOB'), (
      ayuda: 'Vas a enviar Bs 12,50',
      error: null,
    ));
    expect(CampoMonto.lecturaDe('1.240', 'BOB'), (
      ayuda: 'Vas a enviar Bs 1.240,00',
      error: null,
    ));
    expect(CampoMonto.lecturaDe('1.240.50', 'BOB'), (
      ayuda: null,
      error: 'Escribí un monto como 1.240,50',
    ));
  });
}
