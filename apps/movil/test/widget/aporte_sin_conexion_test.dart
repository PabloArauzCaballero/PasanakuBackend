import 'dart:async';

import 'package:aportaya_movil/dominio/puertos/conectividad.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _ConexionControlada implements Conectividad {
  final _eventos = StreamController<bool>.broadcast();

  @override
  Stream<bool> get cambios => _eventos.stream;

  @override
  Future<bool> hayConexion() async => true;

  void establecer(bool valor) => _eventos.add(valor);

  Future<void> cerrar() => _eventos.close();
}

void main() {
  const obligacionId = '68a98585-2854-4b71-80fd-5ef490fe60f6';
  const ruta = '/aportes/obligaciones/$obligacionId/pagos';

  for (final ancho in [320.0, 360.0]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets(
        'aporte offline conserva datos y bloquea POST ($ancho, $brillo)',
        (tester) async {
          tester.view.physicalSize = Size(ancho, 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          final conexion = _ConexionControlada();
          addTearDown(conexion.cerrar);
          final (:dio, :adaptador) = dioSimulado();
          var envios = 0;
          dio.interceptors.add(
            InterceptorsWrapper(
              onRequest: (opciones, manejador) {
                if (opciones.method == 'POST' && opciones.path == ruta) {
                  envios++;
                }
                manejador.next(opciones);
              },
            ),
          );
          adaptador.onPost(
            ruta,
            (servidor) => servidor.reply(
              200,
              ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo'],
            ),
          );
          await tester.pumpWidget(
            conApp(
              const PantallaAportar(
                obligacionId: obligacionId,
                montoDelPeriodo: '125.00',
              ),
              dio: dio,
              conectividad: conexion,
              brillo: brillo,
              escalaDeTexto: 2,
            ),
          );
          await asentar(tester);
          await tester.enterText(find.byType(TextField), 'REF-OFFLINE');
          await tester.ensureVisible(
            find.widgetWithText(FilledButton, 'Revisar aporte'),
          );
          await tester.pumpAndSettle();
          await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
          await tester.pumpAndSettle();
          final confirmar = find.widgetWithText(
            FilledButton,
            'Confirmar aporte',
          );
          await tester.ensureVisible(confirmar);
          await tester.pumpAndSettle();
          final confirmacionAnterior = tester
              .widget<FilledButton>(confirmar)
              .onPressed;
          expect(confirmacionAnterior, isNotNull);

          conexion.establecer(false);
          await tester.pumpAndSettle();
          expect(find.text('Sin conexión'), findsOneWidget);
          expect(find.text(TextosPasanaku.aporteSinConexion), findsOneWidget);
          expect(find.text('Bs 125,00'), findsOneWidget);
          expect(find.text('REF-OFFLINE'), findsOneWidget);
          expect(tester.widget<FilledButton>(confirmar).onPressed, isNull);
          confirmacionAnterior!(); // Callback antiguo tampoco puede enviar.
          await tester.pump();
          expect(envios, 0);
          await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
          await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
          await expectLater(tester, meetsGuideline(textContrastGuideline));
          expect(tester.takeException(), isNull);

          conexion.establecer(true);
          await tester.pumpAndSettle();
          expect(find.text('Sin conexión'), findsNothing);
          expect(tester.widget<FilledButton>(confirmar).onPressed, isNotNull);
          await tester.ensureVisible(confirmar);
          await tester.tap(confirmar);
          await asentar(tester);
          expect(envios, 1);
          expect(find.text('Aporte registrado'), findsOneWidget);
        },
      );
    }
  }
}
