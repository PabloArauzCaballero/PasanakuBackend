import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_movil/pantallas/identidad/aviso_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets(
    'una foto fallida se anuncia como pendiente, no como éxito pleno',
    (tester) async {
      final dio = dioSimulado().dio;
      await tester.pumpWidget(
        conApp(const AvisoDeAlta(fotosPendientes: true), dio: dio),
      );

      expect(
        find.text(TextosIdentidad.altaFotosPendientesTitulo),
        findsOneWidget,
      );
      expect(
        find.text(TextosIdentidad.altaFotosPendientesDetalle),
        findsOneWidget,
      );
      expect(tester.widget<Alerta>(find.byType(Alerta)).tono, Tono.aviso);
      expect(find.text(TextosIdentidad.altaListaTitulo), findsNothing);
    },
  );

  testWidgets('el aviso y la acción de ingreso caben con texto al 200 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(
      conApp(
        const PantallaDeSesion(cuentaRecienCreada: true, fotosPendientes: true),
        dio: dioSimulado().dio,
        escalaDeTexto: 2,
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text(TextosIdentidad.altaFotosPendientesTitulo),
      findsOneWidget,
    );
    expect(
      tester.getBottomRight(find.text(TextosIdentidad.ingresar)).dy,
      lessThanOrEqualTo(760),
    );
    expect(tester.takeException(), isNull);
  });
}
