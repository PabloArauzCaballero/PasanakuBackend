import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Hallazgo del carril H3: la pantalla del segundo factor eran seis casillas sueltas, sin decir
/// para qué ni cómo seguir, y sin botón. Ahora explica, tiene «Confirmar» (que solo se enciende con
/// los seis dígitos) y, tras un rechazo, permite reintentar el mismo código.
void main() {
  for (final (brillo, nombreTema) in [
    (Brightness.light, 'claro'),
    (Brightness.dark, 'oscuro'),
  ]) {
    group('360 px · texto 130 % · tema $nombreTema', () {
      var pedidos = 0;

      Future<void> montar(
        WidgetTester tester, {
        String codigoRespuesta = 'AP-CU04-04',
      }) async {
        pedidos = 0;
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onPost('/sesiones', (s) {
          return s.reply(422, {
            'codigo': codigoRespuesta,
            'mensaje': 'x',
            'detalle': <String, dynamic>{},
            'trazaId': 'traza-mfa',
          });
        }, data: Matchers.any);
        // Se cuentan los pedidos que de verdad salen: el callback de `onPost` corre al registrar la
        // ruta, no al recibir un pedido, y contar ahí daba 1 antes de escribir nada.
        dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (o, h) {
              if (o.method == 'POST' && o.path.endsWith('/sesiones')) {
                pedidos += 1;
              }
              h.next(o);
            },
          ),
        );
        tester.view.physicalSize = const Size(360, 740);
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.textScaleFactorTestValue = 1.3;
        addTearDown(tester.view.reset);
        addTearDown(tester.platformDispatcher.clearAllTestValues);
        await tester.pumpWidget(
          ProviderScope(
            retry: (r, e) => null,
            overrides: [
              almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
              dioProvider.overrideWithValue(dio),
            ],
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              home: const PantallaDeMfa(),
            ),
          ),
        );
        await tester.pump();
      }

      Boton confirmar(WidgetTester tester) =>
          tester.widget<Boton>(find.widgetWithText(Boton, 'Confirmar'));

      testWidgets('explica qué se pide y el botón espera los seis dígitos', (
        tester,
      ) async {
        await montar(tester);
        expect(find.text('Verificación en dos pasos'), findsOneWidget);
        expect(find.textContaining('te pedimos un código de 6 dígitos'), findsOneWidget);
        expect(confirmar(tester).onPressed, isNull);

        await tester.enterText(find.byType(TextField).first, '12345');
        await tester.pump();
        expect(
          confirmar(tester).onPressed,
          isNull,
          reason: 'con cinco dígitos todavía no se puede confirmar',
        );
        expect(pedidos, 0, reason: 'no se manda nada hasta tener los seis');
        expect(tester.takeException(), isNull);
      });

      testWidgets(
        'rechazo (AP-CU04-04): lo dice, el botón queda para reintentar el mismo código',
        (tester) async {
          await montar(tester);
          await tester.enterText(find.byType(TextField).first, '123456');
          await asentar(tester);
          expect(pedidos, 1, reason: 'al completar las seis casillas se envía');
          expect(find.textContaining('Ese código no sirve'), findsOneWidget);
          expect(confirmar(tester).onPressed, isNotNull);

          await tester.tap(find.text('Confirmar'));
          await asentar(tester);
          expect(pedidos, 2, reason: 'Confirmar reintenta el mismo código');
          expect(tester.takeException(), isNull);
          final contexto = tester.element(find.byType(Scaffold).first);
          expect(Theme.of(contexto).brightness, brillo);
          expect(
            MediaQuery.textScalerOf(contexto).scale(10),
            closeTo(13, 0.001),
          );
        },
      );

      testWidgets('cumple las guías de accesibilidad de Flutter', (
        tester,
      ) async {
        await montar(tester);
        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      });

      testWidgets('permite solicitar otro codigo al correo verificado', (
        tester,
      ) async {
        await montar(tester, codigoRespuesta: 'AP-CU04-10');
        await tester.scrollUntilVisible(
          find.text('Reenviar código por correo'),
          180,
          scrollable: find.byType(Scrollable).first,
        );
        expect(find.text('Reenviar código por correo'), findsOneWidget);
        await tester.tap(find.text('Reenviar código por correo'));
        await asentar(tester);
        expect(pedidos, 1);
        await tester.scrollUntilVisible(
          find.textContaining('Te mandamos un código nuevo'),
          180,
          scrollable: find.byType(Scrollable).first,
        );
        expect(
          find.textContaining('Te mandamos un código nuevo'),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
      });

      testWidgets(
        'no afirma que envio el correo si el servidor solo pide MFA',
        (tester) async {
          await montar(tester, codigoRespuesta: 'AP-CU04-03');
          await tester.scrollUntilVisible(
            find.text('Reenviar código por correo'),
            180,
            scrollable: find.byType(Scrollable).first,
          );
          await tester.tap(find.text('Reenviar código por correo'));
          await asentar(tester);
          await tester.scrollUntilVisible(
            find.textContaining('No pudimos confirmar que el correo salió'),
            180,
            scrollable: find.byType(Scrollable).first,
          );
          expect(
            find.textContaining('No pudimos confirmar que el correo salió'),
            findsOneWidget,
          );
          expect(find.textContaining('Te mandamos un código nuevo'), findsNothing);
          expect(tester.takeException(), isNull);
        },
      );
    });
  }
}
