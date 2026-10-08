import 'package:aportaya_movil/pantallas/billetera/pantalla_retirar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_transferir.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';

void main() {
  for (final (nombre, ruta, operacion, pantalla) in [
    (
      'Retirar',
      '/billetera/retiros',
      'solicitarRetiro',
      const PantallaRetirar(cuentaId: cuenta),
    ),
    (
      'Transferir',
      '/billetera/transferencias',
      'transferirSaldo',
      const PantallaTransferir(cuentaOrigenId: cuenta),
    ),
  ]) {
    testWidgets('$nombre: 503 no afirma que el dinero no se envió', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 640);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) => s.reply(
          503,
          ejemplo('nucleo-financiero', operacion, 'intermitente')['cuerpo'],
        ),
      );
      await tester.pumpWidget(conApp(pantalla, dio: dio, escalaDeTexto: 2));
      await tester.pump();
      final campos = find.byType(TextField);
      if (nombre == 'Retirar') {
        await tester.enterText(campos.at(0), '10,00');
        await tester.enterText(
          campos.at(1),
          '22222222-2222-4222-8222-222222222222',
        );
        await tester.enterText(campos.at(2), '123456');
      } else {
        await tester.enterText(campos.at(0), 'destinatario');
        await tester.enterText(campos.at(1), '10,00');
        await tester.enterText(campos.at(2), 'aporte');
      }
      final boton = find.widgetWithText(FilledButton, nombre);
      await tester.ensureVisible(boton);
      await tester.pumpAndSettle();
      await tester.tap(boton);
      await asentar(tester);

      expect(
        find.textContaining('No pudimos confirmar el resultado'),
        findsOneWidget,
      );
      expect(find.textContaining('traza-intermitente'), findsOneWidget);
      expect(find.text('Volver a intentar'), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.ensureVisible(find.text('Volver a intentar'));
      await tester.pumpAndSettle();
      expect(
        tester.getBottomRight(find.text('Volver a intentar')).dy,
        lessThanOrEqualTo(640),
      );
    });
  }
}
