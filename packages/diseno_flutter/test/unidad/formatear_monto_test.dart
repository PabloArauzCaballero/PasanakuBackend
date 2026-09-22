import 'dart:convert';
import 'dart:io';

import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:flutter_test/flutter_test.dart';

/// La referencia del formato de dinero vive en `packages/tokens`; el Dart reproduce
/// los mismos 5.006 vectores, centavo por centavo. Si difiere, el mismo saldo se ve
/// distinto en la app y en la web.
void main() {
  test('pasa los vectores compartidos con Angular', () {
    var dir = Directory.current;
    while (!File('${dir.path}/settings.gradle.kts').existsSync()) {
      dir = dir.parent;
    }
    final vectores =
        jsonDecode(
              File(
                '${dir.path}/packages/tokens/vectores/monto.json',
              ).readAsStringSync(),
            )
            as List<dynamic>;
    expect(vectores.length, greaterThanOrEqualTo(5000));
    for (final v in vectores.cast<Map<String, dynamic>>()) {
      expect(
        formatearMonto(
          monto: v['monto'] as String,
          moneda: v['moneda'] as String,
        ),
        v['esperado'],
      );
    }
  });
}
