import 'package:aportaya_movil/pantallas/identidad/pantalla_portada.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('portada al 200 % · ${brillo.name} cumple a11y', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final (:dio, adaptador: _) = dioSimulado();
      await tester.pumpWidget(
        conApp(
          const PantallaDePortada(),
          dio: dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await tester.pumpAndSettle();

      // El isotipo y la palabra forman un solo logotipo, no dos paradas del lector.
      expect(find.bySemanticsLabel('AportaYa'), findsOneWidget);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      // El verificador de píxeles confunde los dos colores del logotipo (una
      // imagen semántica) con primer plano/fondo. Comprobamos el texto real
      // contra el fondo; el contraste de logotipos está exento en WCAG 1.4.3.
      final t = brillo == Brightness.light ? Tokens.claro : Tokens.oscuro;
      expect(_contraste(t.text, t.bg), greaterThanOrEqualTo(4.5));
      expect(_contraste(t.text2, t.bg), greaterThanOrEqualTo(4.5));
      expect(_contraste(t.brandTexto, t.brandBg), greaterThanOrEqualTo(4.5));
      expect(tester.takeException(), isNull);
    });
  }
}

double _contraste(Color primerPlano, Color fondo) {
  final a = primerPlano.computeLuminance();
  final b = fondo.computeLuminance();
  final claro = a > b ? a : b;
  final oscuro = a > b ? b : a;
  return (claro + 0.05) / (oscuro + 0.05);
}
