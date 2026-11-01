import 'dart:ui' as ui;

import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  mainCentral();
  testWidgets(
    'hundido cubre icono, insignia y etiqueta sin perder semántica ni área táctil',
    (tester) async {
      var destinoElegido = -1;
      await tester.pumpWidget(
        conTema(
          BarraPestanas(
            destinos: [
              (
                texto: 'Inicio',
                icono: Icons.home_outlined,
                iconoActivo: Icons.home,
                novedades: 0,
              ),
              (
                texto: 'Grupos',
                icono: Icons.group_outlined,
                iconoActivo: Icons.group,
                novedades: 4,
              ),
              (
                texto: 'Perfil',
                icono: Icons.person_outline,
                iconoActivo: Icons.person,
                novedades: 0,
              ),
            ],
            actual: 0,
            onChanged: (i) => destinoElegido = i,
          ),
          pantallaEntera: true,
        ),
      );
      await tester.pumpAndSettle();

      final destino = find.byKey(const ValueKey('destino-Grupos'));
      expect(
        tester.getSize(destino).height,
        greaterThanOrEqualTo(Tactil.minimo),
      );
      expect(find.byTooltip('Grupos, 4 sin leer'), findsOneWidget);
      expect(find.text('4'), findsOneWidget, reason: 'se conserva la insignia');
      expect(
        find.descendant(of: destino, matching: find.byType(Focus)),
        findsWidgets,
        reason: 'el destino sigue en el árbol de foco de InkWell',
      );

      final semantica = tester.getSemantics(destino).getSemanticsData();
      expect(semantica.label, 'Grupos, 4 sin leer');
      expect(semantica.hasAction(SemanticsAction.tap), isTrue);
      expect(semantica.flagsCollection.isButton, isTrue);
      expect(semantica.flagsCollection.isSelected, ui.Tristate.isFalse);
      final seleccionada = tester
          .getSemantics(find.byKey(const ValueKey('destino-Inicio')))
          .getSemanticsData();
      expect(seleccionada.flagsCollection.isSelected, ui.Tristate.isTrue);

      // El punto está sobre la etiqueta, no sobre el icono. El wrapper de presión
      // debe abarcar todo el destino mientras InkWell conserva el toque.
      final gesto = await tester.startGesture(
        tester.getCenter(find.text('Grupos')),
      );
      await tester.pump();
      await tester.pump(Movimiento.micro);
      final escala = tester
          .widget<Transform>(
            find
                .descendant(of: destino, matching: find.byType(Transform))
                .first,
          )
          .transform
          .storage[0];
      expect(escala, closeTo(0.97, 0.001));
      await gesto.up();
      await tester.pumpAndSettle();
      expect(destinoElegido, 1);
    },
  );
}

void _sinAccion(int _) {}

/// ADR-053 (A6): el botón del medio no es una pestaña. Se anuncia con su acción, no
/// cuenta como destino y no cambia la pestaña activa.
void mainCentral() {
  testWidgets('el botón central se anuncia por lo que hace y no es un destino', (
    tester,
  ) async {
    var tocado = 0;
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(
      conTema(
        BarraPestanas(
          destinos: const [
            (
              texto: 'Inicio',
              icono: Icons.home,
              iconoActivo: Icons.home,
              novedades: 0,
            ),
            (
              texto: 'Grupos',
              icono: Icons.group,
              iconoActivo: Icons.group,
              novedades: 0,
            ),
            (
              texto: 'Movimientos',
              icono: Icons.swap_horiz,
              iconoActivo: Icons.swap_horiz,
              novedades: 0,
            ),
            (
              texto: 'Perfil',
              icono: Icons.person,
              iconoActivo: Icons.person,
              novedades: 0,
            ),
          ],
          actual: 0,
          onChanged: _sinAccion,
          central: (
            icono: Icons.qr_code,
            etiqueta: 'Escanear invitación',
            alTocar: () => tocado++,
          ),
        ),
        pantallaEntera: true,
      ),
    );
    await tester.pumpAndSettle();

    final nodo = tester.getSemantics(
      find.bySemanticsLabel('Escanear invitación'),
    );
    expect(nodo.flagsCollection.isButton, isTrue);
    expect(nodo.flagsCollection.isSelected, isNot(ui.Tristate.isTrue));
    await tester.tap(find.byKey(const ValueKey('accion-central')));
    expect(tocado, 1);
    // Los cuatro destinos y el botón quedan en orden: Inicio, Grupos, [QR], Movimientos, Perfil.
    final xs = [
      for (final k in [
        'destino-Inicio',
        'destino-Grupos',
        'accion-central',
        'destino-Movimientos',
        'destino-Perfil',
      ])
        tester.getCenter(find.byKey(ValueKey(k))).dx,
    ];
    expect(xs, orderedEquals([...xs]..sort()));
    // «Movimientos» entra entero, sin «Movimien…».
    expect(find.text('Movimientos'), findsOneWidget);
    expect(tester.takeException(), isNull);
    semantica.dispose();
  });
}
