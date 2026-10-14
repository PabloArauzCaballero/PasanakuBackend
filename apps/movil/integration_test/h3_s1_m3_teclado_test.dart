import 'package:aportaya_diseno/atomos/fila_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/hoja_de_seleccion.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('H3.S1.M3 la hoja sube con el teclado Android real', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Builder(
          builder: (context) => Scaffold(
            body: Center(
              child: FilledButton(
                onPressed: () => mostrarHojaDeSeleccion(
                  context,
                  titulo: 'Departamento',
                  ayuda: 'Elegí dónde vivís',
                  opciones: [
                    for (var i = 0; i < 30; i++)
                      (texto: 'Opción $i', detalle: 'Detalle $i'),
                  ],
                ),
                child: const Text('Abrir selector'),
              ),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Abrir selector'));
    await tester.pumpAndSettle();
    final buscador = find.byType(TextField);
    expect(buscador, findsOneWidget);
    await tester.tap(buscador);

    var insetInferior = 0.0;
    for (var intento = 0; intento < 80; intento++) {
      await tester.pump(const Duration(milliseconds: 100));
      insetInferior = MediaQuery.of(tester.element(buscador)).viewInsets.bottom;
      if (insetInferior > 0) break;
    }

    expect(
      insetInferior,
      greaterThan(0),
      reason: 'El teclado del sistema Android no se abrió.',
    );
    // viewInsets llega antes que termine AnimatedPadding; espera la animación
    // antes de evaluar la geometría final de la hoja.
    await tester.pump(const Duration(milliseconds: 800));
    final medio = MediaQuery.of(tester.element(buscador));
    insetInferior = medio.viewInsets.bottom;
    final bordeTeclado = medio.size.height - insetInferior;
    final rectBuscador = tester.getRect(buscador);

    final filas = find.byType(FilaDeSeleccion);
    final rects = [
      for (var indice = 0; indice < filas.evaluate().length; indice++)
        tester.getRect(filas.at(indice)),
    ];
    final cantidadDeFilasVisibles = rects
        .where((rect) => rect.top >= 0 && rect.bottom <= bordeTeclado)
        .length;

    // La pausa deja la pantalla estable para una captura de sistema completa con
    // adb screencap (incluye la ventana del IME, no solo la superficie Flutter).
    // ignore: avoid_print
    print(
      'H3_CAPTURE_READY screen=${medio.size.width}x${medio.size.height}; '
      'inset=$insetInferior; tecladoTop=$bordeTeclado; '
      'buscador=$rectBuscador; filas=$rects; visibles=$cantidadDeFilasVisibles',
    );
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(seconds: 20)),
    );
    expect(
      rectBuscador.bottom,
      lessThanOrEqualTo(bordeTeclado),
      reason: 'El campo de búsqueda no debe quedar bajo el teclado.',
    );
    expect(
      cantidadDeFilasVisibles,
      greaterThanOrEqualTo(2),
      reason: 'La lista debe ceder altura, sin encoger el buscador.',
    );
  });
}
