import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';

void main() {
  test('arma la cifra como la maqueta la muestra', () {
    expect(formatearMonto(monto: '1240.00', moneda: 'BOB'), 'Bs 1.240,00');
    expect(
      formatearMonto(monto: '1234567.89', moneda: 'BOB'),
      'Bs 1.234.567,89',
    );
    expect(formatearMonto(monto: '-1240.50', moneda: 'BOB'), '-Bs 1.240,50');
    expect(formatearMonto(monto: '10.00', moneda: 'USD'), 'USD 10,00');
  });

  test('rechaza lo que no tiene la forma del contrato', () {
    for (final malo in ['1.5', '1240', '1.234,00', '']) {
      expect(
        () => formatearMonto(monto: malo, moneda: 'BOB'),
        throwsFormatException,
      );
    }
  });

  test(
    'pasa los mismos vectores que el Monto de Angular, centavo por centavo',
    () {
      final archivo = File(
        '${raizDelRepositorio()}/packages/tokens/vectores/monto.json',
      );
      final vectores = jsonDecode(archivo.readAsStringSync()) as List<dynamic>;
      expect(vectores.length, greaterThanOrEqualTo(5000));
      for (final v in vectores.cast<Map<String, dynamic>>()) {
        final formateado = formatearMonto(
          monto: v['monto'] as String,
          moneda: v['moneda'] as String,
        );
        expect(formateado, v['esperado'], reason: 'vector ${v['monto']}');
        expect(desformatearMonto(formateado), v['monto']);
      }
    },
  );
}
