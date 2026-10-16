import 'package:aportaya_diseno/atomos/fila_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/hoja_de_seleccion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('una lista corta conserva tres filas sobre el teclado', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1;
    tester.view.viewInsets = FakeViewPadding.zero;
    await tester.pumpWidget(
      conTema(
        Builder(
          builder: (context) => Scaffold(
            body: Center(
              child: ElevatedButton(
                onPressed: () => mostrarHojaDeSeleccion(
                  context,
                  titulo: 'Origen de los fondos',
                  opciones: [
                    for (var i = 0; i < 7; i++)
                      (texto: 'Origen $i', detalle: null),
                  ],
                ),
                child: const Text('Abrir'),
              ),
            ),
          ),
        ),
        pantallaEntera: true,
      ),
    );
    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();

    tester.view.viewInsets = const FakeViewPadding(bottom: 300);
    await tester.pumpAndSettle();

    final bordeTeclado = tester.view.physicalSize.height - 300;
    final filasVisibles = tester
        .widgetList<FilaDeSeleccion>(find.byType(FilaDeSeleccion))
        .where((fila) {
          final rect = tester.getRect(find.byWidget(fila));
          return rect.top >= 0 && rect.bottom <= bordeTeclado;
        });
    expect(filasVisibles.length, greaterThanOrEqualTo(3));
    expect(tester.takeException(), isNull);
    tester.view.reset();
  });
}
