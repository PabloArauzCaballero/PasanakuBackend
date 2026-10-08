import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_cuenta_no_disponible.dart';
import 'package:aportaya_movil/pantallas/billetera/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  const rutas = [
    '/billetera/inicio',
    '/billetera/recargar',
    '/billetera/retirar',
    '/billetera/transferir',
    '/billetera/extracto',
  ];

  for (final ruta in rutas) {
    for (final sufijo in [
      '',
      '?cuenta=incorrecta',
      '?cuenta=22222222-2222-4222-8222-222222222222',
    ]) {
      testWidgets('$ruta$sufijo no consulta ni opera sin titularidad', (
        tester,
      ) async {
        tester.view.physicalSize = const Size(1080, 2280);
        tester.view.devicePixelRatio = 3;
        addTearDown(tester.view.reset);
        final router = crearEnrutador(inicial: '$ruta$sufijo');
        addTearDown(router.dispose);
        final simulado = dioSimulado();
        var solicitudes = 0;
        simulado.dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (options, handler) {
              solicitudes++;
              handler.next(options);
            },
          ),
        );
        await tester.pumpWidget(
          ProviderScope(
            retry: (retryCount, error) => null,
            overrides: [
              almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
              dioProvider.overrideWithValue(simulado.dio),
            ],
            child: MaterialApp.router(
              theme: temaDesde(Tokens.claro, Brightness.light),
              routerConfig: router,
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.byType(PantallaCuentaNoDisponible), findsOneWidget);
        expect(find.text(TextosBilletera.cuentaNoDisponible), findsOneWidget);
        expect(solicitudes, 0);
      });
    }
  }

  testWidgets('la falta de cuenta ofrece Ayuda como salida real', (
    tester,
  ) async {
    final router = crearEnrutador(inicial: '/billetera/inicio');
    addTearDown(router.dispose);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text(TextosBilletera.irAAyuda));
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), '/soporte/ayuda');
  });

  testWidgets('mensaje usable con texto al 200 % y tema oscuro', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1080, 2280);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final router = crearEnrutador(inicial: '/billetera/inicio');
    addTearDown(router.dispose);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.oscuro, Brightness.dark),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: const TextScaler.linear(2)),
            child: child!,
          ),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text(TextosBilletera.cuentaNoDisponible), findsOneWidget);
    expect(find.text(TextosBilletera.irAAyuda), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
