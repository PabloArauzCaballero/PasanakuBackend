import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/billetera/resumen_de_pasanakus.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

/// «Tus pasanakus» en el inicio (2026-10-21): avisos de lo que pide atención, «todo
/// al día» o cómo entrar a un grupo. Datos sintéticos.
Map<String, dynamic> _grupo(String id, String nombre, String estado) => {
  'grupoId': id,
  'participanteId': 'p-$id',
  'estado': estado,
  'nombreDelGrupo': nombre,
};

Future<GoRouter> _montar(WidgetTester tester, Dio dio) async {
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (_, _) => const Scaffold(
          body: SingleChildScrollView(child: ResumenDePasanakus()),
        ),
      ),
      GoRoute(
        path: '/pasanaku/grupos/:grupoId',
        builder: (_, s) =>
            Scaffold(body: Text('grupo ${s.pathParameters['grupoId']}')),
      ),
    ],
  );
  await tester.pumpWidget(
    ProviderScope(
      retry: (r, e) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
      child: MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        routerConfig: router,
      ),
    ),
  );
  await asentar(tester);
  return router;
}

void main() {
  group('Tus pasanakus en el inicio', () {
    testWidgets(
      'correcto: un aviso por grupo que pide algo; el activo no avisa',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/grupos/participaciones',
          (s) => s.reply(200, [
            _grupo('g1', 'Cumpleaños de leo', 'ACEPTADO_PENDIENTE_FIRMA'),
            _grupo('g2', 'Ahorro del barrio', 'EN_MORA'),
            _grupo('g3', 'Viaje', 'ACTIVO'),
          ]),
        );
        await _montar(tester, dio);

        expect(find.text('Cumpleaños de leo'), findsOneWidget);
        expect(find.text('Te falta firmar el reglamento'), findsOneWidget);
        expect(find.text('Ahorro del barrio'), findsOneWidget);
        expect(find.text('Tenés un aporte atrasado'), findsOneWidget);
        expect(find.text('Viaje'), findsNothing);
        expect(find.textContaining('Todavía no estás'), findsNothing);
      },
    );

    testWidgets('tocar un aviso abre ese grupo', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/grupos/participaciones',
        (s) => s.reply(200, [
          _grupo('g1', 'Cumpleaños de leo', 'ACEPTADO_PENDIENTE_FIRMA'),
        ]),
      );
      await _montar(tester, dio);

      await tester.tap(find.text('Cumpleaños de leo'));
      await asentar(tester);
      expect(find.text('grupo g1'), findsOneWidget);
    });

    testWidgets('límite: con todo al día, una sola línea y nada de la lista', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/grupos/participaciones',
        (s) => s.reply(200, [
          _grupo('g1', 'Uno', 'ACTIVO'),
          _grupo('g2', 'Dos', 'ACTIVO'),
        ]),
      );
      await _montar(tester, dio);

      expect(find.text('Estás en 2 grupos · todo al día'), findsOneWidget);
      expect(find.text('Ver mis grupos'), findsOneWidget);
      expect(find.text('Uno'), findsNothing);
    });

    testWidgets('sin grupos: lo dice y ofrece unirse o crear', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/grupos/participaciones',
        (s) => s.reply(200, <Object>[]),
      );
      await _montar(tester, dio);

      expect(
        find.textContaining('Todavía no estás en ningún pasanaku'),
        findsOneWidget,
      );
      expect(find.text('Unirme a un grupo'), findsOneWidget);
      expect(find.text('Crear un grupo'), findsOneWidget);
    });

    testWidgets(
      'inválido: si falla, ofrece volver a intentar sin romper el inicio',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/grupos/participaciones',
          (s) => s.reply(500, {
            'codigo': 'AP-SIS-500',
            'mensaje': 'No.',
            'trazaId': 't',
          }),
        );
        await _montar(tester, dio);

        expect(find.text('Volver a intentar'), findsOneWidget);
        expect(find.textContaining('Todavía no estás'), findsNothing);
      },
    );
  });
}
