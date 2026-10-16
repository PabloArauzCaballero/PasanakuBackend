import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/paso_contrato_de_alta.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('no inventa una referencia cuando no hay traza', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const Scaffold(body: ReferenciaDeError(trazaId: null)),
      ),
    );
    expect(find.textContaining('Referencia:'), findsNothing);
  });

  testWidgets('presenta el paso de contrato sin duplicar el error global', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: PasoContratoDeAlta(enviando: false, onIrAlContrato: () {}),
        ),
      ),
    );

    expect(find.textContaining('Último paso:'), findsOneWidget);
    expect(find.textContaining('Referencia:'), findsNothing);
  });
}
