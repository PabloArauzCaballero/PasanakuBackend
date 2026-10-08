import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/pantallas/identidad/pantalla_portada.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';

import '../comun.dart';

void main() {
  testWidgets('con texto al 200 %, las acciones acompañan el scroll', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(const PantallaDePortada(), dio: dio, escalaDeTexto: 2),
    );
    await tester.pumpAndSettle();

    final accionCrearCuenta = find.text(TextosIdentidad.portadaCrearCuenta);
    final yInicial = tester.getTopLeft(accionCrearCuenta).dy;

    await tester.drag(
      find.byType(SingleChildScrollView).first,
      const Offset(0, -500),
    );
    await tester.pumpAndSettle();

    expect(
      tester.getTopLeft(accionCrearCuenta).dy,
      lessThan(yInicial),
      reason: 'a texto grande, las acciones deben desplazarse con el contenido',
    );
    expect(tester.takeException(), isNull);
  });
}
