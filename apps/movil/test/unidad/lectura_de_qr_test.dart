import 'package:aportaya_movil/navegacion/lectura_de_qr.dart';
import 'package:flutter_test/flutter_test.dart';

const _uuid = '3f2a9c1e-8b47-4d21-9a66-0c5e7d1b2a34';
final _hash = 'ab' * 32;

void main() {
  group('rutaDeInvitacionDesdeQr', () {
    test('correcto: invitación válida lleva a la pantalla de unirse', () {
      expect(
        rutaDeInvitacionDesdeQr('aportaya://unirse/$_uuid.$_hash'),
        '/pasanaku/unirse/$_uuid.$_hash',
      );
    });

    test('límite: espacios alrededor se toleran', () {
      expect(
        rutaDeInvitacionDesdeQr('  aportaya://unirse/$_uuid.$_hash\n'),
        '/pasanaku/unirse/$_uuid.$_hash',
      );
    });

    test(
      'límite: justo en el tope de largo sigue siendo rechazado si no es invitación',
      () {
        expect(rutaDeInvitacionDesdeQr('a' * 512), isNull);
      },
    );

    for (final (nombre, texto) in <(String, String?)>[
      ('nulo', null),
      ('vacío', ''),
      ('solo espacios', '   '),
      ('URL web', 'https://ejemplo.com/unirse/$_uuid.$_hash'),
      ('esquema propio sin código', 'aportaya://unirse'),
      ('código mal formado', 'aportaya://unirse/ABC123'),
      ('hash corto', 'aportaya://unirse/$_uuid.abcd'),
      ('otro destino interno', 'aportaya://billetera'),
      ('más largo que el tope', 'aportaya://unirse/${'a' * 600}'),
      ('texto de un QR de cobro', '000201010212...'),
    ]) {
      test('inválido: $nombre → null', () {
        expect(rutaDeInvitacionDesdeQr(texto), isNull);
      });
    }
  });

  group('CerrojoDeLectura', () {
    test('correcto: la primera lectura pasa', () {
      expect(CerrojoDeLectura().tomar(), isTrue);
    });

    test('límite: la segunda y la tercera no pasan', () {
      final c = CerrojoDeLectura()..tomar();
      expect(c.tomar(), isFalse);
      expect(c.tomar(), isFalse);
    });

    test('límite: tras liberar vuelve a pasar una sola vez', () {
      final c = CerrojoDeLectura()..tomar();
      c.liberar();
      expect(c.tomar(), isTrue);
      expect(c.tomar(), isFalse);
    });
  });
}
