import 'package:aportaya_movil/dominio/errores.dart';
import 'package:flutter_test/flutter_test.dart';

/// Cada rechazo del alta que declara el contrato (identidad.yaml, `registrarUsuario`)
/// tiene su texto: un código sin traducir cae en «algo salió mal de nuestro lado», que
/// culpa al sistema de algo que la persona puede corregir.
void main() {
  const generico =
      'Algo salió mal de nuestro lado. Probá de nuevo en un momento.';

  for (final codigo in [
    'AP-CU01-01',
    'AP-CU01-03',
    'AP-CU01-06',
    'AP-CU01-07',
  ]) {
    test('$codigo tiene texto propio', () {
      expect(mensajeDe(codigo, estado: 422), isNot(generico));
    });
  }

  test('la clave rechazada dice qué cambiar', () {
    expect(mensajeDe('AP-CU01-06'), contains('Elegí otra'));
  });
}
