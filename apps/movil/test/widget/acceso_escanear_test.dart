import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_escanear.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:aportaya_movil/proveedores/visor_qr.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

const _cuenta = '11111111-1111-4111-8111-111111111111';

void main() {
  testWidgets(
    'correcto: el ícono de la cabecera del inicio abre el escáner de invitaciones',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/billetera/$_cuenta/saldo',
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      final enrutador = GoRouter(
        initialLocation: '/billetera/inicio',
        routes: [
          GoRoute(
            path: '/billetera/inicio',
            builder: (_, _) => const PantallaDeSaldo(cuentaId: _cuenta),
          ),
          GoRoute(
            path: '/pasanaku/escanear',
            builder: (_, _) => const PantallaEscanear(),
          ),
        ],
      );
      tester.view.physicalSize = const Size(1080, 2280);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
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
            routerConfig: enrutador,
          ),
        ),
      );
      await asentar(tester);

      await tester.tap(find.bySemanticsLabel('Escanear invitación'));
      await tester.pumpAndSettle();
      expect(find.byType(PantallaEscanear), findsOneWidget);
    },
  );
}
