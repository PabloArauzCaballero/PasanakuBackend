import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/superficie_viva.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _obligacion = '68a98585-2854-4b71-80fd-5ef490fe60f6';
const _ruta = '/aportes/obligaciones/$_obligacion/pagos';

/// CU-21 con el diseño de ADR-053: el importe lo fija el reglamento (no se edita), «Aportar» es
/// la única acción en naranja (A2: pagar algo que vence) y la confirmación habla en palabras.
void main() {
  Future<void> abrir(WidgetTester tester, dio) async {
    tester.view.physicalSize = const Size(1080, 2280);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: _obligacion,
          montoDelPeriodo: '4.44',
        ),
        dio: dio,
      ),
    );
    await asentar(tester);
  }

  testWidgets('el importe es una cifra fija, no un campo editable', (
    tester,
  ) async {
    final (:dio, adaptador: _) = dioSimulado();
    await abrir(tester, dio);
    expect(find.textContaining('4,44'), findsWidgets);
    expect(find.text('Lo fija el reglamento de tu grupo.'), findsOneWidget);
    // El único campo de texto es la referencia del pago.
    expect(find.byType(TextField), findsOneWidget);
  });

  testWidgets(
    '«Aportar» espera la referencia y, habilitado, es naranja de urgencia',
    (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      await abrir(tester, dio);
      const tokens = Tokens.claro;
      final boton = find.widgetWithText(Boton, 'Aportar Bs 4,44');
      expect(boton, findsOneWidget);
      // Sin referencia, el botón no hace nada.
      await tester.tap(boton);
      await tester.pump();
      expect(find.text('Tu aporte quedó registrado'), findsNothing);
      await tester.enterText(find.byType(TextField), 'REF-123');
      await tester.pump();
      // Habilitado, es el único botón con superficie viva de la pantalla: naranja (A2).
      expect(
        tester.widget<SuperficieViva>(find.byType(SuperficieViva).first).color,
        tokens.accent,
      );
    },
  );

  testWidgets('confirmado: dice el estado en palabras, no el nombre del enum', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      _ruta,
      (s) => s.reply(200, ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo']),
    );
    await abrir(tester, dio);
    await tester.enterText(find.byType(TextField), 'REF-123');
    await tester.pump();
    await tester.tap(find.widgetWithText(Boton, 'Aportar Bs 4,44'));
    await asentar(tester);
    expect(find.text('Tu aporte quedó registrado'), findsOneWidget);
    expect(find.text('Pendiente'), findsOneWidget);
    expect(find.textContaining('PENDIENTE'), findsNothing);
    expect(find.textContaining('misma clave'), findsNothing);
  });
}
