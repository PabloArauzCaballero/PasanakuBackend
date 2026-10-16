import 'package:aportaya_diseno/moleculas/fila_de_cotejo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in Brightness.values) {
    final tokens = brillo == Brightness.dark ? Tokens.oscuro : Tokens.claro;

    testWidgets('valores vacíos quedan pendientes, no aprobados: $brillo', (
      tester,
    ) async {
      await tester.pumpWidget(
        conTema(
          const FilaDeCotejo(campo: 'Nombres', declarado: '', leido: ''),
          brillo: brillo,
        ),
      );

      expect(find.byIcon(Icons.check_circle_outline), findsNothing);
      expect(find.byIcon(Icons.radio_button_unchecked), findsOneWidget);
      expect(find.text('Pendiente de completar'), findsOneWidget);
      expect(
        tester.widget<Icon>(find.byIcon(Icons.radio_button_unchecked)).color,
        tokens.text2,
      );
    });

    testWidgets('valores no vacíos iguales sí coinciden: $brillo', (
      tester,
    ) async {
      await tester.pumpWidget(
        conTema(
          const FilaDeCotejo(
            campo: 'Nombres',
            declarado: 'Ana',
            leido: 'Ana',
          ),
          brillo: brillo,
        ),
      );

      expect(find.byIcon(Icons.check_circle_outline), findsOneWidget);
      expect(find.text('Ana'), findsOneWidget);
      expect(find.text('Pendiente de completar'), findsNothing);
      expect(
        tester.widget<Icon>(find.byIcon(Icons.check_circle_outline)).color,
        tokens.okTexto,
      );
    });

    testWidgets('valores distintos muestran discrepancia: $brillo', (
      tester,
    ) async {
      await tester.pumpWidget(
        conTema(
          const FilaDeCotejo(
            campo: 'Nombres',
            declarado: 'Ana',
            leido: 'Anea',
          ),
          brillo: brillo,
        ),
      );

      expect(find.byIcon(Icons.difference_outlined), findsOneWidget);
      expect(find.text('Declaraste: Ana'), findsOneWidget);
      expect(find.text('El documento dice: Anea'), findsOneWidget);
      expect(
        tester.widget<Icon>(find.byIcon(Icons.difference_outlined)).color,
        tokens.avisoTexto,
      );
    });
  }
}
