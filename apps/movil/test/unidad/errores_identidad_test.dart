import 'package:aportaya_movil/dominio/errores.dart';
import 'package:flutter_test/flutter_test.dart';

/// Carril H2 · los rechazos de identidad que el servidor TEST real devuelve con código propio
/// (AP-CU04-01/02/04/05, AP-CU01-07..11) se explican por separado: antes caían en «El código no
/// coincide» (credencial mal) o en el genérico «Algo salió mal de nuestro lado» (código de correo).
void main() {
  final generico = mensajeDe('AP-DESCONOCIDO', estado: 422);

  group('mensajeDe · identidad', () {
    test(
      'correcto: cada rechazo de CU-04 tiene su texto y ninguno es el genérico',
      () {
        final textos = [
          for (final n in ['01', '02', '04', '05', '06', '07'])
            mensajeDe('AP-CU04-$n', estado: 422),
        ];
        expect(textos.toSet().length, textos.length);
        for (final t in textos) {
          expect(t, isNot(generico));
        }
      },
    );

    test(
      'límite: credencial inválida habla de celular y contraseña, no de «código»',
      () {
        final m = mensajeDe('AP-CU04-01', estado: 422).toLowerCase();
        expect(m, contains('contraseña'));
        expect(m, isNot(contains('código')));
      },
    );

    test(
      'límite: demasiados intentos es AP-CU04-05, no el 04 (que es el código del factor)',
      () {
        expect(
          mensajeDe('AP-CU04-05', estado: 422),
          contains('Demasiados intentos'),
        );
        expect(
          mensajeDe('AP-CU04-04', estado: 422),
          isNot(contains('Demasiados')),
        );
      },
    );

    test(
      'correcto: los cinco rechazos del código de correo del alta son distintos y accionables',
      () {
        final textos = [
          for (final n in ['07', '08', '09', '10', '11'])
            mensajeDe('AP-CU01-$n', estado: 422),
        ];
        expect(textos.toSet().length, 5);
        for (final t in textos) {
          expect(t, isNot(generico));
        }
      },
    );

    test(
      'inválido: un código AP-CU01 desconocido sigue cayendo en el mensaje por estado',
      () {
        expect(mensajeDe('AP-CU01-99', estado: 422), generico);
      },
    );
  });
}
