import 'package:aportaya_movil/dominio/tutoriales/autolanzamiento.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:flutter_test/flutter_test.dart';

import 'comun.dart';

ProgresoDeTutorial _progreso(
  String id, {
  String version = '1.0.0',
  EstadoDeProgreso estado = EstadoDeProgreso.enProgreso,
}) => ProgresoDeTutorial(
  tutorialId: id,
  version: version,
  estado: estado,
  indice: 0,
  iniciadoEn: ahoraDePrueba,
  ultimaInteraccion: ahoraDePrueba,
);

void main() {
  final tutorial = tutorialDePrueba('intro-app');

  group('debeAutolanzar', () {
    test('correcto: nunca se hizo, se lanza', () {
      expect(debeAutolanzar(tutorial, null), isTrue);
    });

    test('límite: versión nueva del tutorial, se ofrece de nuevo', () {
      final viejo = _progreso(
        'intro-app',
        version: '0.9.0',
        estado: EstadoDeProgreso.completado,
      );
      expect(debeAutolanzar(tutorial, viejo), isTrue);
    });

    for (final estado in [
      EstadoDeProgreso.enProgreso,
      EstadoDeProgreso.omitido,
      EstadoDeProgreso.completado,
    ]) {
      test('límite: ya $estado en esta versión, no insiste', () {
        expect(
          debeAutolanzar(tutorial, _progreso('intro-app', estado: estado)),
          isFalse,
        );
      });
    }

    test('inválido: sin tutorial en el catálogo no lanza nada', () {
      expect(debeAutolanzar(null, null), isFalse);
    });

    test('inválido: tutorial sin pasos no lanza nada', () {
      expect(
        debeAutolanzar(tutorialDePrueba('vacio', pasos: 0), null),
        isFalse,
      );
    });
  });
}
