import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';

import '../comun.dart';

const obligacionId = '68a98585-2854-4b71-80fd-5ef490fe60f6';
const ruta = '/aportes/obligaciones/$obligacionId/pagos';

void main() {
  testWidgets('el monto del reglamento se confirma y no se puede editar', (
    tester,
  ) async {
    final (:dio, adaptador: _) = dioSimulado();

    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: obligacionId,
          montoDelPeriodo: '125.00',
        ),
        dio: dio,
      ),
    );
    await tester.pump();

    expect(find.text('Bs 125,00'), findsOneWidget);
    expect(
      find.textContaining('no se puede cambiar'),
      findsOneWidget,
      reason: 'el monto fijo no debe parecer un campo editable',
    );
    expect(find.byType(TextField), findsOneWidget);
    expect(find.text('Referencia del pago'), findsOneWidget);
  });

  testWidgets(
    'valida la referencia y un doble toque genera un solo pago idempotente',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      var llamadas = 0;
      final claves = <String?>[];
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST' && options.path == ruta) {
              llamadas++;
              claves.add(options.headers['Idempotency-Key'] as String?);
            }
            handler.next(options);
          },
        ),
      );
      adaptador.onPost(
        ruta,
        (servidor) => servidor.reply(
          200,
          ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo'],
          delay: const Duration(milliseconds: 200),
        ),
      );

      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: obligacionId,
            montoDelPeriodo: '125.00',
          ),
          dio: dio,
        ),
      );
      await tester.pump();

      await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
      await tester.pump();
      expect(
        find.text('Ingresá la referencia para poder identificar el pago.'),
        findsOneWidget,
      );
      expect(llamadas, 0);

      await tester.enterText(find.byType(TextField), 'REF-001');
      await tester.pump();
      await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
      await tester.pumpAndSettle();
      expect(find.text('Revisá tu aporte'), findsOneWidget);
      expect(find.text('REF-001'), findsOneWidget);
      expect(llamadas, 0, reason: 'revisar no envía el aporte');
      final boton = find.widgetWithText(FilledButton, 'Confirmar aporte');
      final punto = tester.getCenter(boton);
      await tester.tapAt(punto);
      await tester.pump();
      await tester.tapAt(punto);
      await tester.pump();
      await asentar(tester);

      expect(llamadas, 1, reason: 'el doble toque no puede crear dos pagos');
      expect(claves, hasLength(1));
      expect(claves.single, isNotNull);
      expect(find.text('Aporte registrado'), findsOneWidget);
      expect(find.textContaining('medio de pago lo confirme'), findsOneWidget);
      expect(find.textContaining('PENDIENTE'), findsNothing);
    },
  );

  testWidgets('reintentar un fallo conserva la clave y no duplica el pago', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    var llamadas = 0;
    final claves = <String?>[];
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          if (options.method == 'POST' && options.path == ruta) {
            llamadas++;
            claves.add(options.headers['Idempotency-Key'] as String?);
          }
          handler.next(options);
        },
      ),
    );
    adaptador.onPost(
      ruta,
      (servidor) => servidor.reply(
        503,
        ejemplo('aportes', 'cobrarAporte', 'intermitente')['cuerpo'],
      ),
    );

    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: obligacionId,
          montoDelPeriodo: '125.00',
        ),
        dio: dio,
      ),
    );
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'REF-REINTENTO');
    await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar aporte'));
    await asentar(tester);

    expect(find.text('Volver a intentar'), findsOneWidget);
    expect(
      find.textContaining('No pudimos confirmar el resultado'),
      findsOneWidget,
    );
    expect(
      find.textContaining('intento anterior podría haber llegado'),
      findsOneWidget,
    );
    adaptador.onPost(
      ruta,
      (servidor) => servidor.reply(
        200,
        ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo'],
      ),
    );
    await tester.drag(find.byType(CustomScrollView), const Offset(0, -300));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Volver a intentar'));
    await asentar(tester);

    expect(llamadas, 2);
    expect(claves, hasLength(2));
    expect(claves.first, isNotNull);
    expect(claves.last, claves.first);
    expect(find.text('Aporte registrado'), findsOneWidget);
  });

  testWidgets('un 409 de aporte pide consultar estado y no ofrece reenvío', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      ruta,
      (servidor) => servidor.reply(409, {
        'codigo': 'AP-CU21-09',
        'trazaId': 'traza-409-aporte',
      }),
    );
    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: obligacionId,
          montoDelPeriodo: '125.00',
        ),
        dio: dio,
        escalaDeTexto: 2,
      ),
    );
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'REF-409');
    await tester.ensureVisible(
      find.widgetWithText(FilledButton, 'Revisar aporte'),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(
      find.widgetWithText(FilledButton, 'Confirmar aporte'),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar aporte'));
    await asentar(tester);

    expect(
      find.textContaining('Esta operación ya existe o cambió'),
      findsOneWidget,
    );
    expect(find.text('Volver a intentar'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'cambiar datos antes de confirmar no envía y conserva el formulario',
    (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      var llamadas = 0;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST' && options.path == ruta) llamadas++;
            handler.next(options);
          },
        ),
      );
      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: obligacionId,
            montoDelPeriodo: '125.00',
          ),
          dio: dio,
        ),
      );
      await tester.pump();
      await tester.enterText(find.byType(TextField), 'REF-EDITABLE');
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
      await tester.pumpAndSettle();

      expect(find.text('Revisá tu aporte'), findsOneWidget);
      expect(find.text('Bs 125,00'), findsOneWidget);
      expect(find.text('Billetera'), findsOneWidget);
      expect(find.text('REF-EDITABLE'), findsOneWidget);
      expect(llamadas, 0);
      await tester.tap(find.text('Cambiar datos'));
      await tester.pumpAndSettle();

      expect(find.byType(TextField), findsOneWidget);
      expect(
        tester.widget<TextField>(find.byType(TextField)).controller?.text,
        'REF-EDITABLE',
      );
      expect(llamadas, 0);
    },
  );

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('revisión a 360×760, texto 200 % y $brillo', (tester) async {
      tester.view.physicalSize = const Size(1080, 2280);
      tester.view.devicePixelRatio = 3;
      addTearDown(tester.view.reset);
      final (:dio, adaptador: _) = dioSimulado();
      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: obligacionId,
            montoDelPeriodo: '125.00',
          ),
          dio: dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await tester.pump();
      await tester.enterText(find.byType(TextField), 'REF-001-EXTENSA-123456');
      await tester.pumpAndSettle();
      await tester.ensureVisible(
        find.widgetWithText(FilledButton, 'Revisar aporte'),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
      await tester.pumpAndSettle();

      expect(find.text('Revisá tu aporte'), findsOneWidget);
      expect(find.text('REF-001-EXTENSA-123456'), findsOneWidget);
      await tester.ensureVisible(
        find.widgetWithText(FilledButton, 'Confirmar aporte'),
      );
      expect(find.text('Confirmar aporte'), findsOneWidget);
      expect(find.text('Cambiar datos'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }
}
