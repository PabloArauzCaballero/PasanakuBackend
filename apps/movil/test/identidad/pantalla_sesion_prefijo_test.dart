import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Hallazgo del carril H (emulador): el ingreso pedía escribir «+591» a mano mientras el
/// alta lo trae puesto; sin él, «Ingresar» quedaba apagado sin explicación.
void main() {
  Boton ingresar(WidgetTester tester) =>
      tester.widget<Boton>(find.widgetWithText(Boton, 'Ingresar'));

  testWidgets(
    'el prefijo +591 viene puesto: con ocho dígitos y clave se habilita',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
      await tester.pump();
      final camposTexto = tester.widgetList<TextField>(find.byType(TextField));
      expect(camposTexto, hasLength(2));
      expect(
        camposTexto.every((campo) => campo.decoration!.hintText!.isNotEmpty),
        isTrue,
      );
      expect(
        camposTexto.elementAt(1).decoration!.hintText,
        'Mínimo 8 caracteres',
      );
      expect(find.text('+591'), findsWidgets);
      expect(ingresar(tester).onPressed, isNull);

      final campos = find.byType(EditableText);
      await tester.enterText(campos.at(0), '70000001');
      await tester.enterText(campos.at(1), 'Clave-Segura-123');
      await tester.pump();
      expect(ingresar(tester).onPressed, isNotNull);
    },
  );

  testWidgets('con menos de ocho dígitos sigue apagado', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
    await tester.pump();
    final campos = find.byType(EditableText);
    await tester.enterText(campos.at(0), '7000');
    await tester.enterText(campos.at(1), 'Clave-Segura-123');
    await tester.pump();
    expect(ingresar(tester).onPressed, isNull);
  });

  testWidgets(
    'con «Entrar con mi correo» el celular se cambia por el correo y entra con él',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
      // Deja terminar la aparición escalonada: a mitad de camino el toque cae corrido.
      await tester.pump(const Duration(seconds: 1));

      await tester.tap(find.text('Entrar con mi correo'));
      await tester.pump();
      expect(find.byKey(const Key('campo-correo-de-ingreso')), findsOneWidget);
      expect(find.text('+591'), findsNothing);
      expect(find.text('Entrar con mi celular'), findsOneWidget);

      final campos = find.byType(EditableText);
      await tester.enterText(campos.at(0), 'ana@ejemplo');
      await tester.enterText(campos.at(1), 'Clave-Segura-123');
      await tester.pump();
      expect(ingresar(tester).onPressed, isNull, reason: 'correo incompleto');

      await tester.enterText(campos.at(0), 'ana@ejemplo.invalid');
      await tester.pump();
      expect(ingresar(tester).onPressed, isNotNull);
    },
  );

  testWidgets('credencial incorrecta tiembla tres veces y da haptic ligero', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/sesiones',
      (s) => s.reply(422, {
        'codigo': 'AP-CU04-01',
        'mensaje': 'tecnico',
        'trazaId': 'credencial-test',
      }),
      data: Matchers.any,
    );
    final hapticos = <Object?>[];
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (llamada) async {
        if (llamada.method == 'HapticFeedback.vibrate') {
          hapticos.add(llamada.arguments);
        }
        return null;
      },
    );
    addTearDown(
      () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.platform,
        null,
      ),
    );

    await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
    await tester.pump();
    final campos = find.byType(EditableText);
    await tester.enterText(campos.at(0), '70000001');
    await tester.enterText(campos.at(1), 'Clave-Segura-123');
    await tester.pump();
    await tester.tap(find.widgetWithText(Boton, 'Ingresar'));
    await tester.pump();
    final mensaje = find.text('El celular o la contraseña no coinciden.');
    for (
      var intento = 0;
      intento < 10 && mensaje.evaluate().isEmpty;
      intento++
    ) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    // Riverpod publica el rechazo al responder el adaptador; desde ese instante
    // avanzamos un tramo de la sacudida, no un punto fijo del reloj de red.
    await tester.pump(const Duration(milliseconds: 50));
    await tester.pump(const Duration(milliseconds: 50));

    final sacudida = tester.widget<AnimatedBuilder>(
      find.byKey(const ValueKey('sacudida-de-credencial')),
    );
    final animacion = sacudida.animation as Animation<double>;
    expect(animacion.value.abs(), greaterThan(1));
    expect(mensaje, findsOneWidget);
    expect(hapticos, ['HapticFeedbackType.lightImpact']);
  });

  testWidgets(
    'movimiento reducido mantiene el formulario quieto ante rechazo',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/sesiones',
        (s) => s.reply(422, {
          'codigo': 'AP-CU04-01',
          'mensaje': 'tecnico',
          'trazaId': 'credencial-quieta',
        }),
        data: Matchers.any,
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSesion(), dio: dio, movimientoReducido: true),
      );
      await tester.pump();
      final campos = find.byType(EditableText);
      await tester.enterText(campos.at(0), '70000001');
      await tester.enterText(campos.at(1), 'Clave-Segura-123');
      await tester.pump();
      await tester.tap(find.widgetWithText(Boton, 'Ingresar'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 150));

      final sacudida = tester.widget<AnimatedBuilder>(
        find.byKey(const ValueKey('sacudida-de-credencial')),
      );
      expect((sacudida.animation as Animation<double>).value, 0);
      expect(
        find.text('El celular o la contraseña no coinciden.'),
        findsOneWidget,
      );
    },
  );
}
