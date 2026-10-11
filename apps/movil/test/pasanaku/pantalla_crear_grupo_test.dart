import 'dart:convert';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_crear_grupo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/cu20_crear_grupo.dart';

import '../comun.dart';

/// Hallazgo del carril H (prueba en emulador): «Crear grupo» no se habilitaba nunca,
/// porque `completo` exigía una fecha de inicio que el formulario no pregunta y que solo
/// se completaba al apretar el propio botón. Estas pruebas fijan que con nombre y monto
/// el botón se habilita, que envía una sola petición con la fecha por omisión, y que sin
/// monto sigue apagado.
void main() {
  Boton boton(WidgetTester tester) =>
      tester.widget<Boton>(find.widgetWithText(Boton, 'Crear grupo'));

  testWidgets('sin monto el botón sigue apagado', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    await tester.pumpWidget(conApp(const PantallaCrearGrupo(), dio: dio));
    await tester.pump();
    await tester.enterText(find.byType(EditableText).first, 'Grupo de prueba');
    await tester.pump();
    expect(boton(tester).onPressed, isNull);
  });

  testWidgets(
    'con nombre y monto el botón se habilita y envía una sola petición con fecha de inicio',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      final cuerpos = <Map<String, dynamic>>[];
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST' && options.path == '/grupos') {
              final d = options.data;
              cuerpos.add(
                Map<String, dynamic>.from(
                  (d is String ? jsonDecode(d) : d) as Map,
                ),
              );
            }
            handler.next(options);
          },
        ),
      );
      adaptador.onPost(
        '/grupos',
        (s) => s.reply(201, ejemplo('grupos', 'crearGrupo', 'ok')['cuerpo']),
        data: Matchers.any,
      );

      await tester.pumpWidget(conApp(const PantallaCrearGrupo(), dio: dio));
      await tester.pump();
      final campos = find.byType(EditableText);
      await tester.enterText(campos.at(0), 'Grupo de prueba');
      await tester.enterText(campos.at(1), '100,00');
      await tester.pump();

      expect(boton(tester).onPressed, isNotNull);
      await tester.tap(find.widgetWithText(Boton, 'Crear grupo'));
      await asentar(tester);

      final estado = ProviderScope.containerOf(
        tester.element(find.byType(PantallaCrearGrupo)),
      ).read(crearGrupoProvider);
      expect(cuerpos, hasLength(1), reason: 'estado: $estado');
      expect(cuerpos.single['nombre'], 'Grupo de prueba');
      expect(cuerpos.single['fechaDeInicio'], isNotNull);
    },
  );
}
