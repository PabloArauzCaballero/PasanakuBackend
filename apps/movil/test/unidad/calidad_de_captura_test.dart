import 'package:aportaya_movil/pantallas/identidad/dominio/calidad_de_captura.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('evaluarCalidad — carnet', () {
    test('acepta una foto grande con la proporción de un carnet ID-1', () {
      final motivo = evaluarCalidad(ancho: 1600, alto: 1009, esDocumento: true);
      expect(motivo, isNull);
    });

    test('rechaza una foto más chica que el lado mínimo', () {
      final motivo = evaluarCalidad(ancho: 1399, alto: 884, esDocumento: true);
      expect(motivo, 'La imagen salió muy chica. Acercá un poco el teléfono.');
    });

    test('acepta el lado mínimo de 1400 píxeles en orientación vertical', () {
      final motivo = evaluarCalidad(ancho: 884, alto: 1400, esDocumento: true);
      expect(motivo, isNull);
    });

    test('rechaza una proporción que no es la de un carnet', () {
      final motivo = evaluarCalidad(ancho: 1600, alto: 1600, esDocumento: true);
      expect(motivo, contains('el carnet entero'));
    });

    test('acepta ambos bordes exactos de la tolerancia (±12 %)', () {
      // Estas proporciones equivalen exactamente a ID-1 ±12 %.
      expect(
        evaluarCalidad(ancho: 5992, alto: 3375, esDocumento: true),
        isNull,
      );
      expect(
        evaluarCalidad(ancho: 4708, alto: 3375, esDocumento: true),
        isNull,
      );
    });

    test('acepta el carnet ID-1 vertical', () {
      expect(
        evaluarCalidad(ancho: 1009, alto: 1600, esDocumento: true),
        isNull,
      );
    });

    test('rechaza apenas fuera de la tolerancia', () {
      final motivo = evaluarCalidad(ancho: 6000, alto: 3375, esDocumento: true);
      expect(
        motivo,
        'No parece el carnet entero. Repetila con los cuatro bordes a la vista.',
      );
    });
  });

  group('evaluarCalidad — selfie', () {
    test('solo mira el tamaño: una proporción cuadrada pasa', () {
      final motivo = evaluarCalidad(
        ancho: 1600,
        alto: 1600,
        esDocumento: false,
      );
      expect(motivo, isNull);
    });

    test('igual rechaza una selfie demasiado chica', () {
      final motivo = evaluarCalidad(ancho: 800, alto: 800, esDocumento: false);
      expect(motivo, contains('muy chica'));
    });
  });
}
