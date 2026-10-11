import 'package:aportaya_movil/dominio/errores.dart';
import 'package:flutter_test/flutter_test.dart';

/// Carril H3 · contra el servidor TEST real, crear grupo responde 422 AP-CU20-04 (licencia sin
/// otorgar) y el enlace de una invitación revocada, 422 AP-CU69-05. Ambos caían en el genérico
/// «Algo salió mal de nuestro lado», que dice que falló el servidor cuando fue una regla de negocio.
void main() {
  final generico = mensajeDe('AP-DESCONOCIDO', estado: 422);

  group('mensajeDe · grupos', () {
    test(
      'correcto: cada rechazo de crear grupo (CU-20) tiene texto propio y distinto',
      () {
        final textos = [
          for (final n in ['01', '02', '03', '04', '05'])
            mensajeDe('AP-CU20-$n', estado: 422),
        ];
        expect(textos.toSet().length, textos.length);
        for (final t in textos) {
          expect(t, isNot(generico));
        }
      },
    );

    test(
      'correcto: los rechazos de invitar y unirse (CU-69) tienen texto propio',
      () {
        for (final n in ['01', '03', '04', '05', '07', '08', '09', '10']) {
          expect(
            tieneMensajePropio('AP-CU69-$n'),
            isTrue,
            reason: 'AP-CU69-$n',
          );
          expect(mensajeDe('AP-CU69-$n', estado: 422), isNot(generico));
        }
      },
    );

    test(
      'límite: el enlace inválido explica las tres causas posibles sin acusar al servidor',
      () {
        final m = mensajeDe('AP-CU69-05', estado: 422);
        expect(m, contains('vencer'));
        expect(m, contains('revocada'));
        expect(m.toLowerCase(), isNot(contains('nuestro lado')));
      },
    );

    test(
      'inválido: un código de CU-69 que la app no conoce sigue cayendo en el genérico',
      () {
        expect(tieneMensajePropio('AP-CU69-99'), isFalse);
        expect(mensajeDe('AP-CU69-99', estado: 422), generico);
      },
    );
  });
}
