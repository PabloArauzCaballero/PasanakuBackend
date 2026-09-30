import 'package:aportaya_movil/navegacion/ruta_del_aviso.dart';
import 'package:flutter_test/flutter_test.dart';

const _uuid = '3f2a9c1e-8b47-4d21-9a66-0c5e7d1b2a34';
final _hash = 'ab' * 32;

void main() {
  group('correcto', () {
    for (final (entrada, esperada) in <(String, String)>[
      ('aportaya://billetera', '/billetera/inicio'),
      ('aportaya://notificaciones', '/notificaciones/bandeja'),
      ('aportaya://aporte', '/billetera/aportes'),
      ('/billetera/inicio', '/billetera/inicio'),
      ('/notificaciones/bandeja', '/notificaciones/bandeja'),
      ('aportaya://unirse/$_uuid.$_hash', '/pasanaku/unirse/$_uuid.$_hash'),
      ('/pasanaku/unirse/$_uuid.$_hash', '/pasanaku/unirse/$_uuid.$_hash'),
    ]) {
      test('$entrada → $esperada', () {
        expect(rutaDelAviso(entrada), esperada);
      });
    }
  });

  group('límite', () {
    test('espacios alrededor se toleran', () {
      expect(rutaDelAviso('  /billetera/inicio \n'), '/billetera/inicio');
    });

    test(
      'justo en el tope de 300 caracteres sigue rechazando lo que no es ruta',
      () {
        expect(rutaDelAviso('/${'a' * 299}'), isNull);
      },
    );

    test('un tramo debajo de una ruta permitida pasa', () {
      expect(
        rutaDelAviso('/billetera/inicio/detalle'),
        '/billetera/inicio/detalle',
      );
    });

    test(
      'límite: un .. dentro de un enlace lo normaliza Uri y no sale de la lista blanca',
      () {
        expect(
          rutaDelAviso('aportaya://billetera/../x'),
          '/billetera/inicio/x',
        );
        expect(
          rutaDelAviso('aportaya://billetera/../../identidad/baja'),
          startsWith('/billetera/inicio'),
        );
      },
    );

    test('el prefijo parcial no cuenta (/billeteras no es /billetera)', () {
      expect(rutaDelAviso('/billeteras/inicio'), isNull);
    });
  });

  group('inválido', () {
    for (final (nombre, entrada) in <(String, String?)>[
      ('nulo', null),
      ('vacío', ''),
      ('sitio externo', 'https://evil.com/billetera/inicio'),
      ('http', 'http://evil.com'),
      ('protocolo relativo', '//evil.com/billetera/inicio'),
      ('javascript', 'javascript:alert(1)'),
      ('intent', 'intent://scan/#Intent;end'),
      ('escape con ..', '/billetera/../identidad/baja'),
      ('ruta interna no publicada', '/admin/usuarios'),
      ('ruta de baja de cuenta', '/identidad/baja'),
      ('con consulta', '/billetera/inicio?monto=500'),
      ('con fragmento', '/billetera/inicio#x'),
      ('esquema propio desconocido', 'aportaya://retiro-total'),
      ('invitación mal formada', 'aportaya://unirse/ABC123'),
      ('invitación con hash corto', '/pasanaku/unirse/$_uuid.abcd'),
      ('más largo que el tope', '/billetera/${'a' * 400}'),
      ('texto libre', 'Tu aporte fue acreditado'),
    ]) {
      test('$nombre → null', () {
        expect(rutaDelAviso(entrada), isNull);
      });
    }
  });
}
