import 'package:aportaya_movil/pantallas/identidad/pantalla_contrato.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_del_alta.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// El gate propio del carril: "el botón de aceptar deshabilitado hasta hacer
/// scroll completo, o equivalente verificable" (CU-05).
void main() {
  testWidgets(
    'aceptar sigue deshabilitado con scroll parcial, y se habilita al llegar '
    'al final y marcar los tres consentimientos',
    (tester) async {
      final s = dioSimulado();
      // Sin los contratos vigentes las casillas no tienen ids que mandar y el botón
      // queda apagado con razón: es justo lo que la pantalla tiene que hacer.
      conContratosVigentes(s.adaptador);
      await tester.pumpWidget(conApp(const PantallaDeContrato(), dio: s.dio));
      await asentar(tester);

      Finder boton() =>
          find.widgetWithText(FilledButton, 'Aceptar y continuar');
      expect(tester.widget<FilledButton>(boton()).onPressed, isNull);

      // Scroll parcial: sigue deshabilitado.
      await tester.drag(find.byType(Scrollable).first, const Offset(0, -200));
      await tester.pump();
      expect(tester.widget<FilledButton>(boton()).onPressed, isNull);

      // Scroll hasta el final. El contrato es largo —es un contrato de adhesión de
      // verdad, no un relleno—, así que se baja hasta que el `ScrollController` deja
      // de avanzar en vez de confiar en que un solo `fling` alcance.
      final scroll = find.byType(Scrollable).first;
      var anterior = -1.0;
      for (var i = 0; i < 80; i++) {
        final posicion = tester.widget<Scrollable>(scroll).controller!.position;
        if (posicion.pixels >= posicion.maxScrollExtent - 1) break;
        if (posicion.pixels == anterior) break;
        anterior = posicion.pixels;
        await tester.drag(scroll, const Offset(0, -600));
        await asentar(tester);
      }
      await asentar(tester);

      for (final texto in [
        'Acepto el contrato de adhesión',
        'Acepto el tarifario vigente',
        'Acepto el tratamiento de mis datos personales',
      ]) {
        await tester.tap(find.text(texto));
        await tester.pump();
      }

      expect(tester.widget<FilledButton>(boton()).onPressed, isNotNull);
    },
  );

  testWidgets(
    'si el servidor no publica los contratos, no hay nada que aceptar y se dice',
    (tester) async {
      final s = dioSimulado();
      // `aceptaContratos` de CU-01 son UUID y salen de esta consulta. Sin ella, marcar
      // tres casillas no produce ningún id: dejar leer el contrato y ofrecer «Aceptar
      // y continuar» sería hacer leerlo entero para fallar recién al enviar el alta.
      s.adaptador.onGet(
        '/cumplimiento/contratos/vigentes',
        (r) => r.reply(503, {'codigo': 'AP-INFRA-01'}),
      );
      await tester.pumpWidget(conApp(const PantallaDeContrato(), dio: s.dio));
      await asentar(tester);

      expect(
        find.widgetWithText(FilledButton, 'Aceptar y continuar'),
        findsNothing,
        reason: 'no se ofrece aceptar lo que no se pudo traer',
      );
      expect(find.text('Acepto el contrato de adhesión'), findsNothing);
      expect(find.text(TextosDelAlta.reintentar), findsOneWidget);
    },
  );

  testWidgets('si el servidor publica solo dos de los tres, tampoco se acepta', (
    tester,
  ) async {
    final s = dioSimulado();
    // Firmar dos de tres sin saberlo es peor que no poder firmar: el consentimiento
    // de datos personales no se puede dar por arrastre de los otros dos.
    s.adaptador.onGet(
      '/cumplimiento/contratos/vigentes',
      (r) => r.reply(
        200,
        contratosVigentesDePrueba()
            .where((c) => c['tipo'] != 'TRATAMIENTO_DATOS')
            .toList(),
      ),
    );
    await tester.pumpWidget(conApp(const PantallaDeContrato(), dio: s.dio));
    await asentar(tester);

    expect(
      find.widgetWithText(FilledButton, 'Aceptar y continuar'),
      findsNothing,
    );
    expect(find.text(TextosDelAlta.contratosIncompletos), findsOneWidget);
  });
}
