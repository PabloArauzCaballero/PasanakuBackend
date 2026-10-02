import 'package:aportaya_movil/pantallas/identidad/dominio/calidad_de_captura.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('evaluarCalidad — carnet', () {
    test('acepta una foto grande con la proporción de un carnet ID-1', () {
      final motivo = evaluarCalidad(ancho: 1600, alto: 1009, esDocumento: true);
      expect(motivo, isNull);
    });

    test('rechaza una foto más chica que el lado mínimo', () {
      final motivo = evaluarCalidad(ancho: 1200, alto: 756, esDocumento: true);
      expect(motivo, contains('muy pequeña'));
    });

    test('rechaza una proporción que no es la de un carnet', () {
      final motivo = evaluarCalidad(ancho: 1600, alto: 1600, esDocumento: true);
      expect(motivo, contains('el carnet entero'));
    });

    test('acepta el borde de la tolerancia (+12 %)', () {
      const proporcionLimite = (85.6 / 54) * 1.119;
      final alto = (1600 / proporcionLimite).round();
      final motivo = evaluarCalidad(ancho: 1600, alto: alto, esDocumento: true);
      expect(motivo, isNull);
    });

    test('rechaza apenas fuera de la tolerancia', () {
      const proporcionFuera = (85.6 / 54) * 1.2;
      final alto = (1600 / proporcionFuera).round();
      final motivo = evaluarCalidad(ancho: 1600, alto: alto, esDocumento: true);
      expect(motivo, contains('el carnet entero'));
    });
  });

  group('evaluarCalidad — selfie', () {
    test('solo mira el tamaño: una proporción cuadrada pasa', () {
      final motivo = evaluarCalidad(ancho: 1600, alto: 1600, esDocumento: false);
      expect(motivo, isNull);
    });

    test('igual rechaza una selfie demasiado chica', () {
      final motivo = evaluarCalidad(ancho: 800, alto: 800, esDocumento: false);
      expect(motivo, contains('muy pequeña'));
    });
  });
}
