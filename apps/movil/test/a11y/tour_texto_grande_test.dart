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
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('tour completo a 320 dp y texto 200 % ($brillo)', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final router = crearEnrutador(inicial: '/tour');
      addTearDown(router.dispose);
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: brillo == Brightness.dark
                ? ThemeMode.dark
                : ThemeMode.light,
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(
                context,
              ).copyWith(textScaler: const TextScaler.linear(2)),
              child: child!,
            ),
            routerConfig: router,
          ),
        ),
      );
      await asentar(tester);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));

      for (var i = 0; i < laminasDelTour.length; i++) {
        expect(find.text(laminasDelTour[i].texto), findsOneWidget);
        expect(tester.takeException(), isNull);
        final accion = i == laminasDelTour.length - 1
            ? TextosIdentidad.portadaCrearCuenta
            : TextosIdentidad.tourSiguiente;
        expect(find.text(accion), findsOneWidget);
        await tester.ensureVisible(find.text(laminasDelTour[i].texto));
        await tester.pumpAndSettle();
        final cuerpo = tester.getRect(find.text(laminasDelTour[i].texto));
        expect(cuerpo.top, lessThan(760));
        expect(cuerpo.bottom, greaterThan(0));
        final boton = tester.getRect(find.text(accion));
        expect(boton.top, greaterThanOrEqualTo(0));
        expect(boton.bottom, lessThanOrEqualTo(760));
        expect(tester.takeException(), isNull);
        if (i < laminasDelTour.length - 1) {
          await tester.tap(find.text(accion));
          await asentar(tester);
        }
      }
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
    });
  }
}
