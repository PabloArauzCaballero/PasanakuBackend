import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/fila_de_movimiento.dart';
import 'package:aportaya_diseno/moleculas/tipo_de_movimiento.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart' show RenderParagraph;
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

Widget _enLista(List<Widget> filas, {double escala = 1}) => conTema(
  Scaffold(body: ListView(children: filas)),
  escala: escala,
  pantallaEntera: true,
);

/// La fila del extracto (ADR-053, fintech-ux): signo siempre, un anuncio por fila y el importe
/// que baja de renglón antes que dejar el concepto en «Movi…».
void main() {
  testWidgets('lo que sale lleva «−» tipográfico; lo que entra, «+»', (
    tester,
  ) async {
    await tester.pumpWidget(
      _enLista(const [
        FilaDeMovimiento(concepto: 'Recarga', monto: '25.00', moneda: 'BOB'),
        FilaDeMovimiento(concepto: 'Retiro', monto: '-4.44', moneda: 'BOB'),
      ]),
    );
    expect(find.text('+'), findsOneWidget);
    expect(find.text('−'), findsOneWidget);
    expect(find.text('-'), findsNothing);
    // Sin tipo en el contrato, el ícono sale del sentido y no se adivina por la glosa.
    expect(find.byIcon(IconosDeMarca.entra), findsOneWidget);
    expect(find.byIcon(IconosDeMarca.sale), findsOneWidget);
  });

  testWidgets('con tipo, el ícono dice qué pasó', (tester) async {
    await tester.pumpWidget(
      _enLista(const [
        FilaDeMovimiento(
          tipo: TipoDeMovimiento.aporte,
          concepto: 'Aporte · La Ramada',
          monto: '-250.00',
          moneda: 'BOB',
        ),
      ]),
    );
    expect(find.byIcon(TipoDeMovimiento.aporte.icono), findsOneWidget);
  });

  testWidgets('la fila es un solo anuncio para el lector', (tester) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(
      _enLista(const [
        FilaDeMovimiento(
          concepto: 'Recarga',
          monto: '-4.44',
          moneda: 'BOB',
          detalle: '15/01 · 10:30',
        ),
      ]),
    );
    final nodo = tester.getSemantics(find.byType(FilaDeMovimiento));
    expect(nodo.label, startsWith('Débito: Recarga'));
    expect(nodo.label, contains('15/01 · 10:30'));
    expect(nodo.label, contains('4,44'));
    expect(RegExp('Recarga').allMatches(nodo.label), hasLength(1));
    semantica.dispose();
  });

  testWidgets('a 360 px y 200 % el concepto no queda en «Movi…»', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      _enLista(const [
        FilaDeMovimiento(
          concepto: 'Movimiento',
          monto: '1234.56',
          moneda: 'BOB',
          detalle: '12/10 · 09:24',
        ),
      ], escala: 2),
    );
    expect(tester.takeException(), isNull);
    // Apilado: el importe va debajo del concepto, no a su derecha.
    final concepto = tester.getRect(find.text('Movimiento'));
    final importe = tester.getRect(find.text('+'));
    expect(importe.top, greaterThan(concepto.bottom));
    final pintor = tester.renderObject<RenderParagraph>(
      find.text('Movimiento'),
    );
    expect(pintor.didExceedMaxLines, isFalse);
  });
}
