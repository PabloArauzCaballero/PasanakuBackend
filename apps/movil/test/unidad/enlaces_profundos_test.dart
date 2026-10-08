import 'package:aportaya_movil/navegacion/enlaces_profundos.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const codigo =
      '12345678-1234-4234-8234-123456789abc.'
      '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

  test('aportaya://unirse con código firmado abre la invitación', () {
    expect(
      rutaInternaDesde(Uri.parse('aportaya://unirse/$codigo')),
      '/pasanaku/unirse/$codigo',
    );
  });

  test('la query externa no se copia a la ruta interna', () {
    expect(
      rutaInternaDesde(
        Uri.parse('aportaya://unirse/$codigo?documento=CI_FICTICIA'),
      ),
      '/pasanaku/unirse/$codigo',
    );
  });

  for (final enlace in [
    'aportaya://unirse/ABC%3Fdocumento%3DCI_FICTICIA',
    'aportaya://unirse/ABC%2Fdocumento%2FCI_FICTICIA',
    'aportaya://unirse/$codigo/CI_FICTICIA',
  ]) {
    test('segmento no permitido no pasa datos al router: $enlace', () {
      final ruta = rutaInternaDesde(Uri.parse(enlace));
      expect(ruta, '/pasanaku/unirse/invalida');
      expect(ruta, isNot(contains('CI_FICTICIA')));
    });
  }

  test('un destino conocido no acepta segmentos externos adicionales', () {
    expect(
      rutaInternaDesde(
        Uri.parse('aportaya://billetera/%3Fdocumento%3DCI_FICTICIA'),
      ),
      '/enlace-no-valido',
    );
  });

  test('aportaya://billetera sin resto va al inicio de billetera', () {
    expect(
      rutaInternaDesde(Uri.parse('aportaya://billetera')),
      '/billetera/inicio',
    );
  });

  test('un esquema distinto de aportaya no se traduce', () {
    expect(rutaInternaDesde(Uri.parse('https://aportaya.bo/unirse/X')), isNull);
  });

  test('un host propio desconocido tampoco traslada sus datos al router', () {
    final ruta = rutaInternaDesde(
      Uri.parse('aportaya://desconocido/%3Fdocumento%3DCI_FICTICIA'),
    );
    expect(ruta, '/enlace-no-valido');
    expect(ruta, isNot(contains('CI_FICTICIA')));
  });
}
