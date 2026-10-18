import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _ruta = '/usuarios/yo/perfil';

String _campoCorreo(WidgetTester tester) => tester
    .widget<TextField>(
      find.descendant(
        of: find
            .ancestor(of: find.text('Correo'), matching: find.byType(Column))
            .first,
        matching: find.byType(TextField),
      ),
    )
    .controller!
    .text;

void main() {
  testWidgets('correcto: el campo Correo trae el correo de la cuenta', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _ruta,
      (s) => s.reply(200, {
        'usuarioId': 'b8000000-0000-4000-8000-000000000077',
        'correo': 'ana@correo.test',
        'correoVerificado': true,
      }),
    );

    await tester.pumpWidget(conApp(const PantallaDePerfil(), dio: dio));
    await asentar(tester);

    expect(_campoCorreo(tester), 'ana@correo.test');
  });

  testWidgets('límite: una cuenta sin correo deja el campo vacío, sin inventar '
      'uno', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _ruta,
      (s) => s.reply(200, {
        'usuarioId': 'b8000000-0000-4000-8000-000000000077',
        'correo': null,
        'correoVerificado': false,
      }),
    );

    await tester.pumpWidget(conApp(const PantallaDePerfil(), dio: dio));
    await asentar(tester);

    expect(_campoCorreo(tester), isEmpty);
  });

  testWidgets('inválido: si la consulta falla el perfil sigue usable y el '
      'campo queda para escribir', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _ruta,
      (s) => s.reply(503, {
        'codigo': 'AP-GW-503',
        'mensaje': 'no',
        'trazaId': 't-perfil',
      }),
    );

    await tester.pumpWidget(conApp(const PantallaDePerfil(), dio: dio));
    await asentar(tester);

    expect(_campoCorreo(tester), isEmpty);
    expect(find.text('Cerrar sesión'), findsOneWidget);
  });
}
