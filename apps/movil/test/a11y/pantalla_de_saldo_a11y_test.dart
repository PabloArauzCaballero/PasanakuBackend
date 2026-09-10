import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';

/// Accesibilidad bloqueante: contraste AA, área táctil ≥ 48 dp y todo control con
/// etiqueta. `meetsGuideline` es el `axe` del mundo Flutter.
void main() {
  testWidgets(
    'la pantalla de saldo cumple las guías de accesibilidad de Flutter',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/billetera/$cuenta/saldo',
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
    },
  );

  testWidgets(
    'el estado de error también: el botón de reintento se alcanza y se lee',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/billetera/$cuenta/saldo',
        (s) => s.reply(401, {
          'codigo': 'AP-CU04-01',
          'mensaje': '',
          'trazaId': 't',
        }),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
    },
  );
}
