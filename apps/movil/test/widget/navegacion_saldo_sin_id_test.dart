import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/proveedores/conexion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

void main() {
  const cuenta = '11111111-1111-4111-8111-111111111111';

  for (final (etiqueta, nombre, ruta) in [
    ('Recargar', 'billetera.recargar', '/billetera/recargar'),
    ('Retirar', 'billetera.retirar', '/billetera/retirar'),
    ('Generar extracto', 'billetera.extracto', '/billetera/extracto'),
  ]) {
    testWidgets('$etiqueta no pone el ID financiero en la URL', (tester) async {
      tester.view.physicalSize = const Size(1080, 2280);
      tester.view.devicePixelRatio = 3;
      addTearDown(tester.view.reset);
      final router = GoRouter(
        initialLocation: '/billetera/inicio',
        routes: [
          GoRoute(
            path: '/billetera/inicio',
            name: 'billetera.inicio',
            builder: (_, _) => const PantallaDeSaldo(cuentaId: cuenta),
          ),
          GoRoute(
            path: ruta,
            name: nombre,
            builder: (_, _) => const Scaffold(body: Text('Destino')),
          ),
        ],
      );
      addTearDown(router.dispose);
      final simulado = dioSimulado();
      simulado.adaptador.onGet(
        '/billetera/$cuenta/saldo',
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await tester.pumpWidget(
        ProviderScope(
          retry: (retryCount, error) => null,
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(simulado.dio),
            conectividadProvider.overrideWithValue(
              const ConexionDePruebaEnLinea(),
            ),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            routerConfig: router,
          ),
        ),
      );
      await asentar(tester);
      await tester.ensureVisible(find.text(etiqueta));
      await tester.pumpAndSettle();
      await tester.tap(find.text(etiqueta));
      await tester.pumpAndSettle();

      expect(router.state.uri.toString(), ruta);
      expect(router.state.uri.query, isEmpty);
      expect(router.state.uri.toString(), isNot(contains(cuenta)));
      expect(tester.takeException(), isNull);
    });
  }
}
