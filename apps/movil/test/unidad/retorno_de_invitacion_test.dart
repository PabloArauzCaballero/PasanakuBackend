import 'package:aportaya_movil/navegacion/retorno_de_invitacion.dart';
import 'package:aportaya_movil/navegacion/guardias.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const destino =
      '/pasanaku/unirse/12345678-1234-4234-8234-123456789abc.'
      '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

  test('conserva el enlace de invitación después del ingreso', () {
    expect(retornoDeInvitacion(destino), destino);
  });

  test('rechaza redirecciones externas y destinos ajenos', () {
    expect(retornoDeInvitacion('https://otro.example/$destino'), isNull);
    expect(retornoDeInvitacion('//otro.example/$destino'), isNull);
    expect(retornoDeInvitacion('/billetera/inicio'), isNull);
    expect(retornoDeInvitacion('$destino?x=1'), isNull);
  });

  test('la guardia no copia una query externa al login', () {
    final destinoIngreso = rutaDeIngresoPara(
      Uri.parse('/billetera/inicio?documento=CI_FICTICIA&monto=125.00'),
    );
    expect(destinoIngreso, '/ingreso');
    expect(destinoIngreso, isNot(contains('CI_FICTICIA')));
    expect(destinoIngreso, isNot(contains('125.00')));
  });

  test('la guardia conserva solo una invitación interna válida', () {
    final destinoIngreso = rutaDeIngresoPara(Uri.parse(destino));
    expect(destinoIngreso, '/ingreso?volver=${Uri.encodeComponent(destino)}');
  });
}
