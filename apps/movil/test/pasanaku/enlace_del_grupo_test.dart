import 'package:aportaya_movil/navegacion/enlaces_profundos.dart';
import 'package:aportaya_movil/navegacion/lectura_de_qr.dart';
import 'package:aportaya_movil/navegacion/retorno_de_invitacion.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/enlace_del_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/enlace_para_compartir.dart';
import 'package:flutter_test/flutter_test.dart';

/// El enlace del grupo: `https://<app web>/g#/g/<tokenId>.<64 hex>` (SEG-005: el código en el
/// fragmento no llega al servidor) — antes `/g/<código>`, que se sigue leyendo. Abre
/// la app (App Links) o se escanea como QR.
void main() {
  const base = 'https://app.aportaya.161.97.85.216.sslip.io';
  const codigo =
      'e74d3f71-eb49-444a-afbf-e100adac3576.'
      'a14027750358fc8455a696af29794f165dd5010ba14027750358fc8455a696af';

  group('el código', () {
    test('correcto: el enlace se arma con la app web y el código', () {
      expect(enlaceDelGrupo(codigo, baseWeb: base), '$base/g#/g/$codigo');
      expect(
        codigoDeEnlaceDelGrupo(
          Uri.parse(enlaceDelGrupo(codigo, baseWeb: base)!),
        ),
        codigo,
      );
    });

    test(
      'SEG-005: el código no viaja al servidor (ni en el path ni en la consulta)',
      () {
        final uri = Uri.parse(enlaceDelGrupo(codigo, baseWeb: base)!);
        expect(uri.path, '/g');
        expect(uri.query, isEmpty);
        expect(uri.path.contains(codigo.split('.').last), isFalse);
      },
    );

    test(
      'compatibilidad: los enlaces ya compartidos (/g/<código>) se siguen leyendo',
      () {
        expect(codigoDeEnlaceDelGrupo(Uri.parse('$base/g/$codigo')), codigo);
      },
    );

    test('límite: también se lee con la ruta de la app web (#/g/…)', () {
      expect(codigoDeEnlaceDelGrupo(Uri.parse('$base/#/g/$codigo')), codigo);
    });

    test('inválido: un código con otra forma no es un enlace del grupo', () {
      expect(enlaceDelGrupo('abc', baseWeb: base), isNull);
      expect(codigoDeEnlaceDelGrupo(Uri.parse('$base/g/../../otro')), isNull);
      expect(
        codigoDeEnlaceDelGrupo(Uri.parse('$base/g/${codigo.toUpperCase()}')),
        isNull,
      );
    });
  });

  group('al llegar desde afuera', () {
    test(
      'correcto: el enlace https (App Links) va a la pantalla de unirse',
      () {
        expect(rutaInternaDesde(Uri.parse('$base/g#/g/$codigo')), '/g/$codigo');
        expect(rutaInternaDesde(Uri.parse('$base/g/$codigo')), '/g/$codigo');
      },
    );

    test(
      'límite: el escáner acepta el QR del grupo solo del dominio propio',
      () {
        expect(
          rutaDeInvitacionDesdeQr('$base/g#/g/$codigo', appWeb: base),
          '/g/$codigo',
        );
        expect(
          rutaDeInvitacionDesdeQr('$base/g/$codigo', appWeb: base),
          '/g/$codigo',
        );
        expect(
          rutaDeInvitacionDesdeQr(
            'https://otro-sitio.com/g/$codigo',
            appWeb: base,
          ),
          isNull,
        );
      },
    );

    test('inválido: después del ingreso solo se vuelve a una ruta propia', () {
      expect(retornoDeInvitacion('/g/$codigo'), '/g/$codigo');
      expect(retornoDeInvitacion('$base/g/$codigo'), isNull);
      expect(retornoDeInvitacion('/g/no-es-un-codigo'), isNull);
    });
  });

  test('el mensaje lleva el nombre del grupo y el enlace', () {
    final m = mensajeDelEnlaceDelGrupo(
      '$base/g/$codigo',
      nombreDelGrupo: 'Pasanaku del barrio',
    );
    expect(m, contains('«Pasanaku del barrio»'));
    expect(m, endsWith('$base/g/$codigo'));
  });
}
