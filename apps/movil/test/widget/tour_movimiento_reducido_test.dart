import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/identidad/laminas_del_tour.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets(
    'Siguiente cambia de lámina sin recorrido al reducir movimiento',
    (tester) async {
      final router = crearEnrutador(inicial: '/tour');
      addTearDown(router.dispose);
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(context).copyWith(disableAnimations: true),
              child: child!,
            ),
            routerConfig: router,
          ),
        ),
      );
      await asentar(tester);

      for (var i = 1; i < laminasDelTour.length; i++) {
        await tester.tap(find.text(TextosIdentidad.tourSiguiente));
        await tester.pump();

        expect(
          tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual,
          i,
        );
        final controlador = tester
            .widget<PageView>(find.byType(PageView))
            .controller;
        expect(controlador, isNotNull);
        expect(controlador?.page, i);
        expect(find.text(laminasDelTour[i].texto), findsOneWidget);
        final puntos = find.descendant(
          of: find.byType(BarraDePuntos),
          matching: find.byType(AnimatedContainer),
        );
        for (final punto in tester.widgetList<AnimatedContainer>(puntos)) {
          expect(punto.duration, Duration.zero);
        }
        expect(tester.takeException(), isNull);
      }
      expect(find.text(TextosIdentidad.portadaCrearCuenta), findsOneWidget);
    },
  );
}
