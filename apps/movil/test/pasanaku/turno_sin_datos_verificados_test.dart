import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/rutas.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

void main() {
  const sorteoId = '11111111-1111-4111-8111-111111111111';
  const ruta = '/pasanaku/sorteos/$sorteoId/turno';

  for (final (brillo, escala) in [
    (Brightness.light, 1.0),
    (Brightness.dark, 2.0),
  ]) {
    testWidgets('un enlace adulterado no inventa el turno ($brillo)', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final simulado = dioSimulado();
      simulado.adaptador.onGet(
        '/grupos/sorteos/$sorteoId/paquete',
        (s) => s.reply(
          200,
          ejemplo('grupos', 'consultarPaqueteDelSorteo', 'ok')['cuerpo'],
        ),
      );
      simulado.adaptador.onGet(
        '/publico/sorteos/$sorteoId/verificacion',
        (s) => s.reply(
          200,
          ejemplo('transparencia', 'verificarSorteo', 'ok')['cuerpo'],
        ),
      );
      final router = GoRouter(
        initialLocation: '$ruta?total=abc&mio=999&actual=-1',
        routes: rutasPasanaku,
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(
        ProviderScope(
          retry: (retryCount, error) => null,
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(simulado.dio),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: brillo == Brightness.dark
                ? ThemeMode.dark
                : ThemeMode.light,
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(
                context,
              ).copyWith(textScaler: TextScaler.linear(escala)),
              child: child!,
            ),
            routerConfig: router,
          ),
        ),
      );
      await asentar(tester);

      expect(router.state.uri.toString(), ruta);
      expect(find.text(TextosPasanaku.turnoSinConfirmarTitulo), findsOneWidget);
      expect(
        find.text(TextosPasanaku.turnoSinConfirmarDetalle),
        findsOneWidget,
      );
      expect(find.text('Compromiso publicado'), findsOneWidget);
      expect(find.text('Semilla revelada'), findsOneWidget);
      expect(
        find.text('El orden reproducido coincide con el guardado'),
        findsNothing,
      );
      expect(find.textContaining('999'), findsNothing);
      expect(tester.takeException(), isNull);

      await tester.ensureVisible(find.text('Verificar el sorteo'));
      await tester.tap(find.text('Verificar el sorteo'));
      await asentar(tester);
      expect(router.state.uri.path, '/pasanaku/sorteos/$sorteoId/verificar');
      expect(tester.takeException(), isNull);
    });
  }
}
