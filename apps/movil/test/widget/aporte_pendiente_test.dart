import 'package:aportaya_movil/pantallas/pasanaku/dominio/clave_persistida_de_aporte.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('el aporte pendiente explica cómo salir con texto al 200 %', (
    tester,
  ) async {
    const obligacion = '68a98585-2854-4b71-80fd-5ef490fe60f6';
    final almacen = AlmacenEnMemoria();
    final preparacion = ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(almacen)],
    );
    await preparacion
        .read(clavePersistidaDeAporteProvider)
        .abrir(
          obligacionId: obligacion,
          monto: '125.00',
          canal: 'BILLETERA_MOVIL',
          referenciaProveedor: 'REF-INICIAL',
        );
    preparacion.dispose();

    tester.view.physicalSize = const Size(360, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: obligacion,
          montoDelPeriodo: '125.00',
        ),
        dio: dioSimulado().dio,
        almacenSeguro: almacen,
        escalaDeTexto: 2,
      ),
    );
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'REF-DISTINTA');
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
      find.textContaining('Hay un intento anterior de aporte con otros datos'),
      findsOneWidget,
    );
    expect(find.textContaining('consultá a soporte'), findsOneWidget);
    expect(find.text('Volver a intentar'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
