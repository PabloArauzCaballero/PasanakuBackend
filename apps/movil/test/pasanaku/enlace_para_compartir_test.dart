import 'package:aportaya_movil/pantallas/pasanaku/dominio/enlace_para_compartir.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const delServidor =
      'aportaya://unirse/e74d3f71-eb49-444a-afbf-e100adac3576.a14027750358fc8455a696af29794f165dd5010b';

  group('la dirección de la app web', () {
    test('correcto: sale de la API de TEST', () {
      expect(
        baseDeLaAppWeb(
          appWeb: '',
          api: 'https://api.aportaya.161.97.85.216.sslip.io/api/v1',
        ),
        'https://app.aportaya.161.97.85.216.sslip.io',
      );
    });

    test('límite: el define explícito manda y pierde la barra final', () {
      expect(
        baseDeLaAppWeb(appWeb: 'https://app.aportaya.bo/', api: null),
        'https://app.aportaya.bo',
      );
    });

    test('inválido: sin https o sin host api. no se inventa una dirección', () {
      expect(
        baseDeLaAppWeb(appWeb: '', api: 'http://localhost:4010/api/v1'),
        isNull,
      );
      expect(
        baseDeLaAppWeb(appWeb: '', api: 'https://gateway.x.bo/api/v1'),
        isNull,
      );
      expect(baseDeLaAppWeb(appWeb: 'ftp://x', api: null), isNull);
    });
  });

  group('el enlace para compartir', () {
    const base = 'https://app.aportaya.161.97.85.216.sslip.io';

    test('correcto: el esquema propio pasa a la app web con el mismo código', () {
      expect(
        enlaceParaCompartir(delServidor, baseWeb: base),
        '$base/#/pasanaku/unirse/e74d3f71-eb49-444a-afbf-e100adac3576.a14027750358fc8455a696af29794f165dd5010b',
      );
    });

    test('límite: un enlace que ya es https también se reconoce', () {
      expect(
        codigoDeInvitacion('$base/pasanaku/unirse/abc-1.ff00'),
        'abc-1.ff00',
      );
    });

    test('inválido: sin código o sin dirección web se deja el original', () {
      expect(
        enlaceParaCompartir('aportaya://unirse/', baseWeb: base),
        'aportaya://unirse/',
      );
      expect(
        enlaceParaCompartir('aportaya://unirse/<script>', baseWeb: base),
        'aportaya://unirse/<script>',
      );
      expect(enlaceParaCompartir(delServidor), delServidor);
    });

    test('el mensaje lleva el enlace', () {
      expect(mensajeDeInvitacion('https://x/#/a'), endsWith('https://x/#/a'));
    });
  });
}
