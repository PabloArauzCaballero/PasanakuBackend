import 'package:aportaya_movil/navegacion/acceso_a_rutas.dart';
import 'package:flutter_test/flutter_test.dart';

/// UX-A11Y-001: retiro del grupo, traspaso de cupo y permuta de turno exigen identidad
/// en el servidor; sin la guardia, la persona llenaba el formulario y recién al enviar
/// recibía un 403.
void main() {
  group('correcto: lo que compromete a la persona pide identidad antes', () {
    for (final ruta in [
      '/pasanaku/grupos/g1/retiro',
      '/pasanaku/grupos/g1/cupos/c1/traspaso',
      '/pasanaku/turnos/permuta',
      '/billetera/retirar',
      '/pasanaku/obligaciones/o1/aportar',
    ]) {
      test(ruta, () => expect(exigeIdentidadVerificada(ruta), isTrue));
    }
  });

  group('límite', () {
    test('con query sigue exigiendo', () {
      expect(
        exigeIdentidadVerificada('/pasanaku/turnos/permuta?turno=3'),
        isTrue,
      );
    });
    test('ver el grupo no exige', () {
      expect(exigeIdentidadVerificada('/pasanaku/grupos/g1'), isFalse);
    });
  });

  group('inválido: rutas parecidas no se confunden', () {
    for (final ruta in [
      '/pasanaku/grupos/g1/retiro/extra',
      '/pasanaku/grupos//retiro',
      '/pasanaku/turnos/permutas',
    ]) {
      test(ruta, () => expect(exigeIdentidadVerificada(ruta), isFalse));
    }
  });
}
