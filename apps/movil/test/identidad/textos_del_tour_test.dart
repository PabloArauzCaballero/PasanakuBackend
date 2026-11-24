import 'package:aportaya_movil/pantallas/identidad/laminas_del_tour.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter_test/flutter_test.dart';

/// RESPALDO-003: la lámina del fondo no promete más que CU-23 / CU-22 (regla 97.5.4).
void main() {
  const lamina =
      TextosIdentidad.tour4Titulo +
      TextosIdentidad.tour4Destacado +
      TextosIdentidad.tour4Texto;

  test(
    'correcto · dice lo que dice el CU: deuda viva, fondo del grupo, espera',
    () {
      expect(TextosIdentidad.tour4Texto, contains('La deuda no se perdona'));
      expect(TextosIdentidad.tour4Texto, contains('fondo del grupo'));
      expect(TextosIdentidad.tour4Texto, contains('la entrega espera'));
      expect(TextosIdentidad.tour4Texto, contains('La empresa no pone plata'));
    },
  );

  test(
    'límite · sin cifras ni porcentajes de cobertura (no tienen fuente)',
    () {
      expect(RegExp(r'[0-9%]').hasMatch(lamina), isFalse);
    },
  );

  test(
    'inválido · no promete que el turno se entrega igual ni que la empresa cubre',
    () {
      final minuscula = lamina.toLowerCase();
      for (final promesa in [
        'no se frena',
        'igual',
        'garantiz',
        'asegura',
        'la empresa cubre',
        'pozo completo',
      ]) {
        expect(minuscula, isNot(contains(promesa)), reason: promesa);
      }
    },
  );

  test('la lámina 4 del tour usa exactamente estos textos', () {
    final cuarta = laminasDelTour[3];
    expect(cuarta.titulo, TextosIdentidad.tour4Titulo);
    expect(cuarta.destacado, TextosIdentidad.tour4Destacado);
    expect(cuarta.texto, TextosIdentidad.tour4Texto);
  });
}
