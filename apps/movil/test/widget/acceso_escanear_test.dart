import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_escanear.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:aportaya_movil/proveedores/visor_qr.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets(
    'correcto: el botón del medio de la barra abre el escáner de invitaciones '
    '(ADR-053, A6: escanear dejó la cabecera del inicio)',
    (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      tester.view.physicalSize = const Size(1080, 2280);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final router = crearEnrutador(
        inicial: '/billetera/inicio',
        sesion: await sesionActivaDePrueba(),
      );
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            dioProvider.overrideWithValue(dio),
            catalogoDeTutorialesProvider.overrideWithValue(
              const <TutorialDefinicion>[],
            ),
            visorQrProvider.overrideWithValue(
              ({required alLeer, required problema}) => const Text('visor'),
            ),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            routerConfig: router,
          ),
        ),
      );
      await asentar(tester);

      await tester.tap(find.bySemanticsLabel('Escanear invitación'));
      await tester.pumpAndSettle();
      expect(find.byType(PantallaEscanear), findsOneWidget);
      expect(router.state.uri.toString(), '/pasanaku/escanear');
    },
  );
}
