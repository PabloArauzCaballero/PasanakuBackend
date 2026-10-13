import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_baja.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('la baja pide confirmación en una hoja y permite cancelar', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const PantallaDeBaja(),
      ),
    );
    await tester.tap(find.text('Confirmar baja').first);
    await tester.pumpAndSettle();

    expect(find.text('Dar de baja tu cuenta'), findsWidgets);
    expect(find.text('Cancelar'), findsOneWidget);
    expect(find.byType(BottomSheet), findsOneWidget);

    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();
    expect(find.byType(BottomSheet), findsNothing);
  });
}
