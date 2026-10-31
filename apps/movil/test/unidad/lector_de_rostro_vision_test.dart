import 'package:aportaya_movil/infraestructura/lector_de_rostro_vision.dart';
import 'package:flutter_test/flutter_test.dart';

/// Lo que devuelve `LectorDeRostroVision` (AppDelegate.swift) traducido a la lectura del
/// dominio. El canal nativo no corre en `flutter test`: se prueba la traducción con las
/// respuestas que arma Swift.
void main() {
  test(
    'correcto: manda la cara más ancha, centro y ancho tal cual, yaw invertido',
    () {
      final l = lecturaDesdeVision([
        {'x': 0.2, 'y': 0.3, 'ancho': 0.1, 'yaw': 5.0, 'pitch': 1.0},
        {'x': 0.5, 'y': 0.45, 'ancho': 0.4, 'yaw': 20.0, 'pitch': -8.0},
      ])!;
      expect(l.rostros, 2);
      expect(l.centroX, 0.5);
      expect(l.centroY, 0.45);
      expect(l.ancho, 0.4);
      expect(l.giro, -20.0);
      expect(l.inclinacion, -8.0);
      expect(l.ojosAbiertos, isNull);
    },
  );

  test('límite: sin caras, o respuesta nula del canal, no hay lectura', () {
    expect(lecturaDesdeVision(const []), isNull);
    expect(lecturaDesdeVision(null), isNull);
  });

  test('inválido: entradas sin ancho o que no son mapas se descartan', () {
    final l = lecturaDesdeVision([
      'basura',
      {'x': 0.1},
      {'x': 0.6, 'y': 0.5, 'ancho': 0.3},
    ])!;
    expect(l.rostros, 1);
    expect(l.centroX, 0.6);
    expect(l.giro, 0);
    expect(l.inclinacion, 0);
  });
}
