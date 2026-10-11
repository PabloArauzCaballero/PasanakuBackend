import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Hallazgo del carril H (emulador): el ingreso pedía escribir «+591» a mano mientras el
/// alta lo trae puesto; sin él, «Ingresar» quedaba apagado sin explicación.
void main() {
  Boton ingresar(WidgetTester tester) =>
      tester.widget<Boton>(find.widgetWithText(Boton, 'Ingresar'));

  testWidgets(
    'el prefijo +591 viene puesto: con ocho dígitos y clave se habilita',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
      await tester.pump();
      expect(find.text('+591'), findsWidgets);
      expect(ingresar(tester).onPressed, isNull);

      final campos = find.byType(EditableText);
      await tester.enterText(campos.at(0), '70000001');
      await tester.enterText(campos.at(1), 'Clave-Segura-123');
      await tester.pump();
      expect(ingresar(tester).onPressed, isNotNull);
    },
  );

  testWidgets('con menos de ocho dígitos sigue apagado', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
    await tester.pump();
    final campos = find.byType(EditableText);
    await tester.enterText(campos.at(0), '7000');
    await tester.enterText(campos.at(1), 'Clave-Segura-123');
    await tester.pump();
    expect(ingresar(tester).onPressed, isNull);
  });
}
