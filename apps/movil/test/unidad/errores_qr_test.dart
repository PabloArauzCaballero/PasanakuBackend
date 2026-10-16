import 'package:aportaya_movil/dominio/errores.dart';
import 'package:flutter_test/flutter_test.dart';

/// H4.S1.M6 · lado cliente: el QR interno y el interoperable no se confunden en los mensajes.
void main() {
  group('mensajeDe · QR interno vs interoperable (CU-12)', () {
    test(
      'correcto: cada rechazo del QR tiene su propio texto, ninguno es el genérico',
      () {
        final generico = mensajeDe('AP-DESCONOCIDO', estado: 422);
        final textos = [
          for (final n in ['07', '08', '09', '10'])
            mensajeDe('AP-CU12-$n', estado: 422),
        ];
        expect(
          textos.toSet().length,
          4,
          reason: 'cuatro rechazos, cuatro textos',
        );
        for (final t in textos) {
          expect(t, isNot(generico));
        }
      },
    );

    test(
      'límite: el QR interoperable no habilitado no se llama «inválido» ni promete que se puede pagar',
      () {
        final m = mensajeDe('AP-CU12-10', estado: 422);
        expect(m, contains('interoperable'));
        expect(m.toLowerCase(), contains('todavía no'));
        expect(m.toLowerCase(), isNot(contains('no es válido')));
        expect(m, isNot(mensajeDe('AP-CU12-07', estado: 422)));
      },
    );

    test(
      'inválido: un código de QR que no existe cae en el mensaje por estado, sin inventar interoperabilidad',
      () {
        final m = mensajeDe('AP-CU12-99', estado: 422);
        expect(m.toLowerCase(), isNot(contains('interoperable')));
      },
    );
  });
}
