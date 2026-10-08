import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

double _contraste(Color primerPlano, Color fondo) {
  final a = primerPlano.computeLuminance();
  final b = fondo.computeLuminance();
  return (a > b ? a + 0.05 : b + 0.05) / (a > b ? b + 0.05 : a + 0.05);
}

void main() {
  for (final (tokens, brillo) in [
    (Tokens.claro, Brightness.light),
    (Tokens.oscuro, Brightness.dark),
  ]) {
    testWidgets('error de invitación AA y salida táctil · ${brillo.name}', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      addTearDown(adaptador.close);
      await tester.pumpWidget(
        conApp(
          const PantallaUnirse(codigo: 'invalida'),
          dio: dio,
          brillo: brillo,
        ),
      );
      await tester.pumpAndSettle();
      final mensaje = tester.widget<Text>(
        find.textContaining('El enlace de invitación no es válido.'),
      );
      expect(mensaje.style?.color, tokens.errTexto);
      expect(_contraste(tokens.errTexto, tokens.bg), greaterThanOrEqualTo(4.5));
      final salida = find.widgetWithText(TextButton, 'Ir a portada');
      expect(tester.getSize(salida).height, greaterThanOrEqualTo(48));
      expect(tester.takeException(), isNull);
    });
  }
}
