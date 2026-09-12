import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_verificar_sorteo.dart';

const sorteoId = '11111111-1111-4111-8111-111111111111';
const ruta = '/publico/sorteos/$sorteoId/verificacion';

/// CU-61 — gate propio de la ficha F5: el sorteo se ve verificable desde la app.
/// El veredicto que se pinta es el que calculó el servidor (`verifica`), nunca uno
/// recompuesto por el cliente.
void main() {
  testWidgets('el sorteo verificado muestra que el orden coincide', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      ruta,
      (s) => s.reply(
        200,
        ejemplo('transparencia', 'verificarSorteo', 'ok')['cuerpo'],
      ),
    );

    await tester.pumpWidget(
      conApp(const PantallaVerificarSorteo(sorteoId: sorteoId), dio: dio),
    );
    await asentar(tester);

    expect(
      find.text('El orden reproducido coincide con el guardado.'),
      findsOneWidget,
    );
  });

  testWidgets(
    'un sorteo que NO verifica lo dice explícitamente, sin suavizarlo',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.reply(
          200,
          ejemplo('transparencia', 'verificarSorteo', 'vacio')['cuerpo'],
        ),
      );

      await tester.pumpWidget(
        conApp(const PantallaVerificarSorteo(sorteoId: sorteoId), dio: dio),
      );
      await asentar(tester);

      expect(
        find.text(
          'El orden reproducido NO coincide. Reportalo: esto es un hallazgo, no un detalle.',
        ),
        findsOneWidget,
      );
    },
  );
}
