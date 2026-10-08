import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_flotante.dart';
import 'package:aportaya_diseno/atomos/boton_icono.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/hundido_al_tocar.dart';
import 'package:aportaya_diseno/atomos/luz_de_boton.dart';
import 'package:aportaya_diseno/atomos/superficie_viva.dart';
import 'package:aportaya_diseno/moviles/transicion_de_eje.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// El movimiento de la casa: botones vivos y una transición sin zoom.
void main() {
  Boton boton(
    BotonVariante v, {
    VoidCallback? alTocar,
    bool cargando = false,
  }) => Boton(
    texto: 'Seguir',
    variante: v,
    cargando: cargando,
    onPressed: alTocar ?? () {},
  );

  testWidgets('primario, secundario y peligro llevan piel viva; '
      'solo el primario brilla', (tester) async {
    for (final v in [
      BotonVariante.primario,
      BotonVariante.secundario,
      BotonVariante.peligro,
    ]) {
      await tester.pumpWidget(conTema(boton(v)));
      final piel = tester.widget<SuperficieViva>(find.byType(SuperficieViva));
      expect(piel.brilla, v == BotonVariante.primario, reason: '$v');
    }
  });

  testWidgets('fantasma y enlace no se pintan con relleno, pero se hunden', (
    tester,
  ) async {
    for (final v in [BotonVariante.fantasma, BotonVariante.enlace]) {
      await tester.pumpWidget(conTema(boton(v)));
      expect(find.byType(SuperficieViva), findsNothing, reason: '$v');
      expect(find.byType(HundidoAlTocar), findsOneWidget, reason: '$v');
    }
  });

  testWidgets('apagado no tiene degradado ni brillo', (tester) async {
    await tester.pumpWidget(
      conTema(
        const Boton(
          texto: 'Seguir',
          variante: BotonVariante.primario,
          onPressed: null,
        ),
      ),
    );
    expect(find.byType(SuperficieViva), findsNothing);
  });

  testWidgets('el brillo cruza y se detiene: la pantalla se asienta', (
    tester,
  ) async {
    await tester.pumpWidget(conTema(boton(BotonVariante.primario)));
    await tester.pump(const Duration(milliseconds: 500));
    expect(_luz(tester).avance, isNotNull, reason: 'cruzando al aparecer');
    // Si el brillo fuera eterno, esto no terminaría nunca.
    await tester.pumpAndSettle();
    expect(_luz(tester).avance, isNull);
  });

  testWidgets('se hunde al apretar y vuelve al soltar', (tester) async {
    await tester.pumpWidget(conTema(boton(BotonVariante.primario)));
    await tester.pumpAndSettle();
    final gesto = await tester.startGesture(
      tester.getCenter(find.text('Seguir')),
    );
    // Un cuadro para que arranque el ticker y otro con tiempo transcurrido.
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 200));
    expect(_escala(tester), lessThan(1));
    await gesto.up();
    await tester.pumpAndSettle();
    expect(_escala(tester), closeTo(1, 0.001));
  });

  testWidgets('con «reducir movimiento» no se hunde ni brilla', (tester) async {
    await tester.pumpWidget(
      MediaQuery(
        data: const MediaQueryData(disableAnimations: true),
        child: conTema(boton(BotonVariante.primario)),
      ),
    );
    await tester.pump(const Duration(milliseconds: 500));
    expect(_luz(tester).avance, isNull);
  });

  testWidgets('el toque sigue llegando a través de la piel', (tester) async {
    var toques = 0;
    await tester.pumpWidget(
      conTema(boton(BotonVariante.primario, alTocar: () => toques++)),
    );
    await tester.tap(find.text('Seguir'));
    expect(toques, 1);
  });

  testWidgets('botón de ícono y flotante también responden al dedo', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        Column(
          children: [
            BotonIcono(
              icono: Icons.close,
              etiqueta: 'Cerrar',
              onPressed: () {},
            ),
            BotonFlotante(
              icono: Icons.add,
              etiqueta: 'Nuevo',
              onPressed: () {},
            ),
          ],
        ),
      ),
    );
    expect(find.byType(HundidoAlTocar), findsOneWidget);
    expect(find.byType(SuperficieViva), findsOneWidget);
    expect(find.byTooltip('Cerrar'), findsOneWidget);
    expect(find.byTooltip('Nuevo'), findsOneWidget);
  });

  testWidgets('la transición de página desplaza y funde, nunca escala', (
    tester,
  ) async {
    late BuildContext ctx;
    await tester.pumpWidget(
      conTema(
        Builder(
          builder: (context) {
            ctx = context;
            return const Text('origen');
          },
        ),
        pantallaEntera: true,
      ),
    );
    Navigator.of(
      ctx,
    ).push(MaterialPageRoute<void>(builder: (_) => const Text('destino')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 150));
    final escalas = tester
        .widgetList<Transform>(find.byType(Transform))
        .expand((t) => [t.transform.storage[0], t.transform.storage[5]])
        .where((s) => (s - 1).abs() > 0.0001);
    expect(escalas, isEmpty, reason: 'ninguna pantalla se infla ni se achica');
    await tester.pumpAndSettle();
    expect(find.text('destino'), findsOneWidget);
    expect(
      Theme.of(ctx).pageTransitionsTheme.builders[TargetPlatform.android],
      isA<TransicionDeEje>(),
    );
  });
}

LuzDeBoton _luz(WidgetTester tester) =>
    tester
            .widget<CustomPaint>(
              find.descendant(
                of: find.byType(SuperficieViva),
                matching: find.byWidgetPredicate(
                  (w) => w is CustomPaint && w.foregroundPainter is LuzDeBoton,
                ),
              ),
            )
            .foregroundPainter!
        as LuzDeBoton;

double _escala(WidgetTester tester) => tester
    .widget<Transform>(
      find
          .descendant(
            of: find.byType(SuperficieViva),
            matching: find.byType(Transform),
          )
          .first,
    )
    .transform
    .storage[0];
