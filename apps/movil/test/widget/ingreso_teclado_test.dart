import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

void main() {
  testWidgets(
    'contraseña y acción siguen visibles a 320 dp, texto 200 % y teclado',
    (tester) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      tester.view.viewInsets = const FakeViewPadding(bottom: 300);
      addTearDown(tester.view.reset);

      await tester.pumpWidget(
        conApp(
          const PantallaDeSesion(),
          dio: dioSimulado().dio,
          escalaDeTexto: 2,
        ),
      );
      await tester.ensureVisible(find.byType(TextField).at(0));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(TextField).at(0));
      await tester.testTextInput.receiveAction(TextInputAction.next);
      await tester.pumpAndSettle();

      expect(
        tester.getTopLeft(find.text('Contraseña')).dy,
        greaterThanOrEqualTo(0),
      );
      expect(
        tester.getBottomRight(find.byType(TextField).at(1)).dy,
        lessThanOrEqualTo(460),
      );
      expect(
        tester.getBottomRight(find.text('Ingresar')).dy,
        lessThanOrEqualTo(460),
      );
      expect(find.text(TextosIdentidad.portadaCrearCuenta), findsNothing);
      await tester.ensureVisible(find.text(TextosIdentidad.sesionOlvide));
      await tester.pumpAndSettle();
      expect(
        tester.getBottomRight(find.text(TextosIdentidad.sesionOlvide)).dy,
        lessThanOrEqualTo(390),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('Siguiente pasa del teléfono a la contraseña', (tester) async {
    await tester.pumpWidget(
      conApp(const PantallaDeSesion(), dio: dioSimulado().dio),
    );

    final campos = find.byType(TextField);
    expect(campos, findsNWidgets(2));
    expect(find.text(TextosIdentidad.portadaCrearCuenta), findsOneWidget);
    expect(
      tester.widget<TextField>(campos.at(0)).textInputAction,
      TextInputAction.next,
    );
    expect(
      tester.widget<TextField>(campos.at(1)).textInputAction,
      TextInputAction.done,
    );

    await tester.tap(campos.at(0));
    await tester.pump();
    await tester.testTextInput.receiveAction(TextInputAction.next);
    await tester.pump();

    expect(tester.widget<TextField>(campos.at(1)).focusNode?.hasFocus, isTrue);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Listo no envía credenciales incompletas', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    var peticiones = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, gestor) {
          if (opciones.path.endsWith('/sesiones')) peticiones++;
          gestor.next(opciones);
        },
      ),
    );
    adaptador.onPost(
      '/sesiones',
      (servidor) => servidor.reply(422, {'codigo': 'AP-CU04-01'}),
      data: Matchers.any,
    );
    await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));

    final campos = find.byType(TextField);
    await tester.enterText(campos.at(0), '+59178123456');
    await tester.enterText(campos.at(1), 'corta');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await asentar(tester);

    expect(peticiones, 0);
    expect(
      tester.widget<TextField>(campos.at(0)).controller?.text,
      '+59178123456',
    );
    expect(tester.widget<TextField>(campos.at(1)).controller?.text, 'corta');
    expect(tester.takeException(), isNull);
  });

  testWidgets('Listo envía una sola vez con datos válidos', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    var peticiones = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, gestor) {
          if (opciones.path.endsWith('/sesiones')) peticiones++;
          gestor.next(opciones);
        },
      ),
    );
    adaptador.onPost(
      '/sesiones',
      (servidor) => servidor.reply(422, {'codigo': 'AP-CU04-01'}),
      data: Matchers.any,
    );
    await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));

    final campos = find.byType(TextField);
    await tester.enterText(campos.at(0), '+59178123456');
    await tester.enterText(campos.at(1), 'Secreta123');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await asentar(tester);

    expect(peticiones, 1);
    expect(tester.takeException(), isNull);
  });
}
