import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('formatearPorcentaje', () {
    test('un decimal por omisión, como lo mostraba la tarjeta de riesgo', () {
      expect(formatearPorcentaje(99.8), '99.8%');
      expect(formatearPorcentaje(100), '100.0%');
      expect(formatearPorcentaje(0), '0.0%');
    });

    test('redondea al decimal pedido', () {
      expect(formatearPorcentaje(99.84), '99.8%');
      expect(formatearPorcentaje(99.86), '99.9%');
      expect(formatearPorcentaje(12.345, decimales: 2), '12.35%');
      expect(formatearPorcentaje(7.5, decimales: 0), '8%');
    });
  });
}
