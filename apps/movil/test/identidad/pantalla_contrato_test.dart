import 'package:aportaya_movil/pantallas/identidad/pantalla_contrato.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// El gate propio del carril: "el botón de aceptar deshabilitado hasta hacer
/// scroll completo, o equivalente verificable" (CU-05).
void main() {
  testWidgets(
    'aceptar sigue deshabilitado con scroll parcial, y se habilita al llegar '
    'al final y marcar los tres consentimientos',
    (tester) async {
      final s = dioSimulado();
      await tester.pumpWidget(conApp(const PantallaDeContrato(), dio: s.dio));
      await asentar(tester);

      Finder boton() =>
          find.widgetWithText(FilledButton, 'Aceptar y continuar');
      expect(tester.widget<FilledButton>(boton()).onPressed, isNull);

      // Scroll parcial: sigue deshabilitado.
      await tester.drag(find.byType(Scrollable).first, const Offset(0, -200));
      await tester.pump();
      expect(tester.widget<FilledButton>(boton()).onPressed, isNull);

      // Scroll hasta el final.
      await tester.fling(
        find.byType(Scrollable).first,
        const Offset(0, -5000),
        3000,
      );
      await asentar(tester);

      for (final texto in [
        'Acepto el contrato de adhesión',
        'Acepto el tarifario vigente',
        'Acepto el tratamiento de mis datos personales',
      ]) {
        await tester.tap(find.text(texto));
        await tester.pump();
      }

      expect(tester.widget<FilledButton>(boton()).onPressed, isNotNull);
    },
  );
}
