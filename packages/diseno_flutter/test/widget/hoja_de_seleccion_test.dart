import 'dart:ui' show Tristate;

import 'package:aportaya_diseno/atomos/hoja_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/fila_de_seleccion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  setUp(() {
    TestWidgetsFlutterBinding.ensureInitialized();
  });

  Future<void> abrir(
    WidgetTester tester, {
    int? elegida,
    List<OpcionDeHoja>? opciones,
  }) async {
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
                  titulo: 'Departamento',
                  ayuda: 'Elegí dónde vivís',
                  opciones:
                      opciones ??
                      [
                        for (var i = 0; i < 30; i++)
                          (texto: 'Opción $i', detalle: 'Detalle $i'),
                      ],
                  elegida: elegida,
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
  }

  tearDown(() {});

  testWidgets('reserva tres filas visibles con teclado en 360 por 640', (
    tester,
  ) async {
    await abrir(tester);
    tester.view.viewInsets = const FakeViewPadding(bottom: 300);
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    expect(find.text('Buscar en departamento'), findsOneWidget);
    expect(
      tester.getTopLeft(find.text('Buscar en departamento')).dy,
      lessThan(340),
    );
    final filasVisibles = tester
        .widgetList<FilaDeSeleccion>(find.byType(FilaDeSeleccion))
        .where((fila) {
          final rect = tester.getRect(find.byWidget(fila));
          return rect.top >= 0 && rect.bottom <= 340;
        });
    expect(filasVisibles.length, greaterThanOrEqualTo(3));
    tester.view.reset();
  });

  testWidgets('busca sin tildes también en el detalle y resalta coincidencia', (
    tester,
  ) async {
    await abrir(
      tester,
      opciones: const [
        (texto: 'Cochabamba', detalle: 'Valle de Sacaba'),
        (texto: 'Santa Cruz', detalle: 'Andrés Ibáñez'),
      ],
    );
    await tester.enterText(find.byType(TextField), 'andres');
    await tester.pumpAndSettle();
    expect(find.text('Santa Cruz', findRichText: true), findsOneWidget);
    expect(find.byType(RichText), findsWidgets);
    expect(find.text('Cochabamba', findRichText: true), findsNothing);
  });

  testWidgets('la opción elegida fuera de vista se desplaza al abrir', (
    tester,
  ) async {
    await abrir(tester, elegida: 29);
    expect(find.text('Opción 29', findRichText: true), findsOneWidget);
    final fila = tester.getRect(find.text('Opción 29', findRichText: true));
    expect(fila.top, greaterThanOrEqualTo(0));
    expect(fila.bottom, lessThan(640));
  });

  testWidgets('título enfocado sin invocar el teclado', (tester) async {
    await abrir(tester);
    expect(
      tester
          .widgetList<Focus>(find.byType(Focus))
          .any(
            (f) =>
                f.focusNode?.debugLabel == 'Título del selector' &&
                f.focusNode!.hasFocus,
          ),
      isTrue,
    );
    expect(tester.testTextInput.isVisible, isFalse);
  });

  testWidgets('cada fila expone etiqueta, acción de botón y selección', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    await abrir(
      tester,
      elegida: 1,
      opciones: const [
        (texto: 'La Paz', detalle: 'Sede de gobierno'),
        (texto: 'Santa Cruz', detalle: 'Andrés Ibáñez'),
      ],
    );

    final noElegida = find.byWidgetPredicate(
      (widget) =>
          widget is Semantics &&
          widget.properties.button == true &&
          widget.properties.label == 'La Paz, Sede de gobierno',
    );
    final elegida = find.byWidgetPredicate(
      (widget) =>
          widget is Semantics &&
          widget.properties.button == true &&
          widget.properties.label == 'Santa Cruz, Andrés Ibáñez',
    );
    expect(noElegida, findsOneWidget);
    expect(elegida, findsOneWidget);
    final filaNoElegida = tester.getSemantics(noElegida);
    final filaElegida = tester.getSemantics(elegida);

    expect(filaNoElegida.label, 'La Paz, Sede de gobierno');
    expect(filaNoElegida.flagsCollection.isButton, isTrue);
    expect(filaNoElegida.flagsCollection.isSelected, Tristate.isFalse);
    expect(filaElegida.label, 'Santa Cruz, Andrés Ibáñez');
    expect(filaElegida.flagsCollection.isButton, isTrue);
    expect(filaElegida.flagsCollection.isSelected, Tristate.isTrue);
    semantics.dispose();
    tester.view.reset();
  });

  testWidgets('reducir movimiento vuelve instantáneos inset y estado vacío', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1;
    await tester.pumpWidget(
      MediaQuery(
        data: const MediaQueryData(disableAnimations: true),
        child: conTema(
          Builder(
            builder: (context) => Scaffold(
              body: Center(
                child: ElevatedButton(
                  onPressed: () => mostrarHojaDeSeleccion(
                    context,
                    titulo: 'Ciudad',
                    opciones: const [(texto: 'La Paz', detalle: null)],
                  ),
                  child: const Text('Abrir'),
                ),
              ),
            ),
          ),
          pantallaEntera: true,
        ),
      ),
    );
    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();
    expect(
      tester
          .widgetList<AnimatedPadding>(find.byType(AnimatedPadding))
          .any((padding) => padding.duration == Duration.zero),
      isTrue,
    );
    await tester.enterText(find.byType(TextField), 'sin coincidencia');
    await tester.pump();
    expect(
      tester.widget<AnimatedSwitcher>(find.byType(AnimatedSwitcher)).duration,
      Duration.zero,
    );
  });

  testWidgets('vacío, elección de un toque y búsqueda vacía al reabrir', (
    tester,
  ) async {
    await abrir(tester, opciones: const [(texto: 'Uno', detalle: null)]);
    await tester.enterText(find.byType(TextField), 'xyz');
    await tester.pumpAndSettle();
    expect(find.text('Nada coincide con «xyz».'), findsOneWidget);
    await tester.enterText(find.byType(TextField), '');
    await tester.pumpAndSettle();
    await tester.tap(find.text('Uno', findRichText: true));
    await tester.pumpAndSettle();
    expect(find.text('Buscar en departamento'), findsNothing);
  });
}
