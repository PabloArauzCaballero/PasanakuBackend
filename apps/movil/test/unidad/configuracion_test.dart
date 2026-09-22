import 'package:aportaya_movil/dominio/configuracion.dart';
import 'package:flutter_test/flutter_test.dart';

const _hosts = ['api.aportaya.bo'];

void main() {
  group('validarGateway · casos válidos', () {
    test('host propio con TLS en release', () {
      final r = validarGateway(
        'https://api.aportaya.bo/api/v1',
        release: true,
        hostsPermitidos: _hosts,
      );
      expect(r.valida, isTrue);
      expect(r.url, 'https://api.aportaya.bo/api/v1');
    });

    test('el simulado (Prism) en debug, vía 10.0.2.2 (alias del emulador)', () {
      final r = validarGateway(
        'http://10.0.2.2:4010/api/v1',
        release: false,
        hostsPermitidos: const ['10.0.2.2'],
      );
      expect(r.valida, isTrue);
    });

    test('localhost en debug, sin necesidad de lista', () {
      final r = validarGateway(
        'http://localhost:4010/api/v1',
        release: false,
      );
      expect(r.valida, isTrue);
    });
  });

  group('validarGateway · casos límite', () {
    test('con puerto explícito', () {
      final r = validarGateway(
        'https://api.aportaya.bo:8443/api/v1',
        release: true,
        hostsPermitidos: const ['api.aportaya.bo'],
      );
      expect(r.valida, isTrue);
    });

    test('con barra final', () {
      final r = validarGateway(
        'https://api.aportaya.bo/api/v1/',
        release: true,
        hostsPermitidos: _hosts,
      );
      expect(r.valida, isTrue);
      expect(r.url, 'https://api.aportaya.bo/api/v1');
    });

    test('normaliza mayúsculas en el host', () {
      final r = validarGateway(
        'https://API.APORTAYA.BO/api/v1',
        release: true,
        hostsPermitidos: _hosts,
      );
      expect(r.valida, isTrue);
    });

    test('normaliza espacios en la lista de hosts permitidos', () {
      final r = validarGateway(
        'https://api.aportaya.bo/api/v1',
        release: true,
        hostsPermitidos: const ['  api.aportaya.bo  '],
      );
      expect(r.valida, isTrue);
    });
  });

  group('validarGateway · casos inválidos, cada uno con un motivo distinto', () {
    test('vacía', () {
      expect(validarGateway(null, release: true).motivo, 'vacia');
      expect(validarGateway('', release: true).motivo, 'vacia');
      expect(validarGateway('   ', release: true).motivo, 'vacia');
    });

    test('la lista de hosts vacía en release: inválido (nada puede ser "propio")', () {
      final r = validarGateway(
        'https://api.aportaya.bo/api/v1',
        release: true,
        hostsPermitidos: const [],
      );
      expect(r.valida, isFalse);
      expect(r.motivo, 'host-ajeno');
    });

    test('esquema no soportado', () {
      final r = validarGateway(
        'ftp://api.aportaya.bo/api/v1',
        release: true,
        hostsPermitidos: _hosts,
      );
      expect(r.motivo, 'esquema-no-soportado');
    });

    test('esquema de script', () {
      final r = validarGateway('javascript:alert(1)', release: true);
      expect(r.motivo, 'esquema-no-soportado');
    });

    test('sin TLS en release', () {
      final r = validarGateway(
        'http://api.aportaya.bo/api/v1',
        release: true,
        hostsPermitidos: _hosts,
      );
      expect(r.motivo, 'sin-tls-en-produccion');
    });

    test('host ajeno, fuera de la lista compilada', () {
      final r = validarGateway(
        'https://evil.example.com/api/v1',
        release: true,
        hostsPermitidos: _hosts,
      );
      expect(r.motivo, 'host-ajeno');
    });

    test('sin la ruta versionada', () {
      final r = validarGateway(
        'https://api.aportaya.bo/api',
        release: true,
        hostsPermitidos: _hosts,
      );
      expect(r.motivo, 'sin-ruta-versionada');
    });

    test('la forma "//otro-host/..." nunca se acepta', () {
      final r = validarGateway(
        '//evil.example.com/api/v1',
        release: true,
        hostsPermitidos: const ['evil.example.com'],
      );
      expect(r.motivo, 'esquema-relativo-a-protocolo');
    });

    test('localhost nunca se acepta en release', () {
      final r = validarGateway('http://localhost:4010/api/v1', release: true);
      expect(r.motivo, 'localhost-no-permitido');
    });

    test('127.0.0.1 nunca se acepta en release', () {
      final r = validarGateway('https://127.0.0.1/api/v1', release: true);
      expect(r.motivo, 'localhost-no-permitido');
    });

    test('nunca lanza, incluso con basura', () {
      expect(
        () => validarGateway(':::: no es una url', release: true),
        returnsNormally,
      );
    });
  });
}
