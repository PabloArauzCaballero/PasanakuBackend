import 'package:aportaya_diseno/atomos/monto_que_sube.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// **Las pruebas que impiden que una animación asuste a alguien con su propio saldo.**
///
/// Un importe que cuenta hacia arriba es lindo y es un riesgo: si queda trabado en
/// cero, si tarda, o si el lector de pantalla anuncia el conteo en vez del saldo, la
/// persona concluye que perdió la plata o que la app está rota. Cada `it` de acá
/// corresponde a una de esas formas de fallar.
void main() {
  const saldo = '1240.00';
  const leido = 'Saldo disponible: Bs 1.240,00';

  Finder textoDelMonto() => find.byType(Text);
  String visible(WidgetTester tester) =>
      tester.widget<Text>(textoDelMonto()).data!;

  testWidgets('termina exactamente en el importe, no cerca', (tester) async {
    await tester.pumpWidget(
      conTema(
        const MontoQueSube(
          monto: saldo,
          moneda: 'BOB',
          etiqueta: 'Saldo disponible',
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(visible(tester), 'Bs 1.240,00');
  });

  testWidgets(
    'nunca se queda en cero: aunque el widget muera a mitad de la cuenta, lo último '
    'que se vio va camino al saldo y el valor de reposo es el saldo',
    (tester) async {
      await tester.pumpWidget(
        conTema(const MontoQueSube(monto: saldo, moneda: 'BOB')),
      );
      // A mitad de la animación ya subió, y al asentar llega al total.
      await tester.pump(const Duration(milliseconds: 325));
      final aMitad = visible(tester);
      expect(
        aMitad,
        isNot('Bs 0,00'),
        reason: 'se quedó trabado en el arranque',
      );
      await tester.pumpAndSettle();
      expect(visible(tester), 'Bs 1.240,00');
    },
  );

  testWidgets('el lector de pantalla dice el saldo final desde el primer frame', (
    tester,
  ) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(
      conTema(
        const MontoQueSube(
          monto: saldo,
          moneda: 'BOB',
          etiqueta: 'Saldo disponible',
        ),
      ),
    );
    await tester.pump();
    // Sin `pumpAndSettle`: en el primer frame, cuando la cifra visible todavía va por
    // el camino, lo que se anuncia ya es el importe completo.
    expect(find.bySemanticsLabel(leido), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.bySemanticsLabel(leido), findsOneWidget);
    semantica.dispose();
  });

  testWidgets('con «reducir movimiento» aparece entero en el primer frame', (
    tester,
  ) async {
    await tester.pumpWidget(
      MediaQuery(
        data: const MediaQueryData(disableAnimations: true),
        child: conTema(const MontoQueSube(monto: saldo, moneda: 'BOB')),
      ),
    );
    await tester.pump();
    expect(visible(tester), 'Bs 1.240,00');
  });

  testWidgets('refrescar con el mismo importe no lo manda de vuelta a cero', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(const MontoQueSube(monto: saldo, moneda: 'BOB')),
    );
    await tester.pumpAndSettle();
    // Mismo saldo otra vez, como tras un «tirar para refrescar» que no cambió nada.
    await tester.pumpWidget(
      conTema(const MontoQueSube(monto: saldo, moneda: 'BOB')),
    );
    await tester.pump();
    expect(visible(tester), 'Bs 1.240,00');
  });

  testWidgets(
    'cuando el saldo cambia, cuenta desde el anterior y no desde cero',
    (tester) async {
      await tester.pumpWidget(
        conTema(const MontoQueSube(monto: saldo, moneda: 'BOB')),
      );
      await tester.pumpAndSettle();
      await tester.pumpWidget(
        conTema(const MontoQueSube(monto: '1500.00', moneda: 'BOB')),
      );
      await tester.pump(const Duration(milliseconds: 80));
      final enCamino = visible(tester);
      expect(
        enCamino,
        isNot('Bs 0,00'),
        reason: 'un cambio de saldo no puede pasar por cero',
      );
      await tester.pumpAndSettle();
      expect(visible(tester), 'Bs 1.500,00');
    },
  );

  testWidgets('un saldo en cero se muestra en cero, sin dar vueltas', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(const MontoQueSube(monto: '0.00', moneda: 'BOB')),
    );
    await tester.pump();
    expect(visible(tester), 'Bs 0,00');
  });

  testWidgets(
    'la cuenta no dura más de lo que alguien tolera esperando su saldo',
    (tester) async {
      await tester.pumpWidget(
        conTema(const MontoQueSube(monto: saldo, moneda: 'BOB')),
      );
      await tester.pump(const Duration(milliseconds: 700));
      expect(visible(tester), 'Bs 1.240,00');
    },
  );
}
