import 'dart:ui';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mi_estado.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    for (final ids in [
      (participante: '', usuario: '00000000-0000-4000-8000-000000000001'),
      (participante: '00000000-0000-4000-8000-000000000002', usuario: ''),
    ]) {
      testWidgets(
        'sin ID requerido no consulta red a 360×760/200 % ($brillo)',
        (tester) async {
          final original = tester.view.physicalSize;
          final pixelRatioOriginal = tester.view.devicePixelRatio;
          tester.view.physicalSize = const Size(360, 760);
          tester.view.devicePixelRatio = 1.0;
          addTearDown(() {
            tester.view.physicalSize = original;
            tester.view.devicePixelRatio = pixelRatioOriginal;
          });
          final dio = dioSimulado().dio;
          var peticiones = 0;
          dio.interceptors.add(
            InterceptorsWrapper(
              onRequest: (opciones, manejador) {
                peticiones++;
                manejador.next(opciones);
              },
            ),
          );
          await tester.pumpWidget(
            conApp(
              PantallaMiEstado(
                participanteId: ids.participante,
                usuarioId: ids.usuario,
              ),
              dio: dio,
              brillo: brillo,
              escalaDeTexto: 2,
            ),
          );
          await asentar(tester);
          expect(
            find.textContaining('falta identificar tu participación'),
            findsOneWidget,
          );
          await tester.ensureVisible(find.text('Ir a ayuda'));
          await asentar(tester);
          expect(peticiones, 0);
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
}
