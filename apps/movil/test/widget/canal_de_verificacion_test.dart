import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/canal_de_verificacion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('un solo canal se informa sin dibujar un selector', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: CanalDeVerificacion(
            canales: const ['CORREO'],
            valor: 'CORREO',
            onElegido: (_) {},
          ),
        ),
      ),
    );

    expect(find.byType(SelectorSegmentado<String>), findsNothing);
    expect(find.text('Te mandamos el código a tu correo.'), findsOneWidget);
  });
}
