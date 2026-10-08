import 'package:aportaya_diseno/atomos/hundido_al_tocar.dart';
import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('reducir movimiento muestra el contenido en el primer cuadro', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        const MediaQuery(
          data: MediaQueryData(disableAnimations: true),
          child: AparicionEscalonada(
            children: [Text('Uno'), Text('Dos'), Text('Tres')],
          ),
        ),
      ),
    );
    await tester.pump();

    for (final transicion in tester.widgetList<FadeTransition>(
      find.byType(FadeTransition),
    )) {
      expect(transicion.opacity.value, 1);
    }
    expect(find.text('Uno'), findsOneWidget);
    expect(find.text('Tres'), findsOneWidget);
  });

  testWidgets('reducir movimiento elimina la deformación al tocar', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        const MediaQuery(
          data: MediaQueryData(disableAnimations: true),
          child: HundidoAlTocar(child: Text('Confirmar')),
        ),
      ),
    );

    expect(
      find.descendant(
        of: find.byType(HundidoAlTocar),
        matching: find.byType(AnimatedBuilder),
      ),
      findsNothing,
    );
    expect(find.text('Confirmar'), findsOneWidget);
  });

  testWidgets('reducir movimiento cambia los puntos sin transición', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        const MediaQuery(
          data: MediaQueryData(disableAnimations: true),
          child: BarraDePuntos(total: 4, actual: 0),
        ),
      ),
    );
    await tester.pumpWidget(
      conTema(
        const MediaQuery(
          data: MediaQueryData(disableAnimations: true),
          child: BarraDePuntos(total: 4, actual: 3),
        ),
      ),
    );

    for (final punto in tester.widgetList<AnimatedContainer>(
      find.byType(AnimatedContainer),
    )) {
      expect(punto.duration, Duration.zero);
    }
    expect(find.bySemanticsLabel('Pantalla 4 de 4'), findsOneWidget);
  });

  testWidgets(
    'la entrada escalonada completa ocho elementos en menos de 800 ms',
    (tester) async {
      await tester.pumpWidget(
        conTema(
          AparicionEscalonada(
            children: [for (var i = 0; i < 8; i++) Text('Elemento $i')],
          ),
        ),
      );
      await tester.pump(const Duration(milliseconds: 799));

      for (final transicion in tester.widgetList<FadeTransition>(
        find.byType(FadeTransition),
      )) {
        expect(transicion.opacity.value, 1);
      }
    },
  );
}
