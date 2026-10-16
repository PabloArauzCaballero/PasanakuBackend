import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/rutas.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Hallazgo del carril H (emulador): con la app abierta en una invitación, un SEGUNDO
/// enlace (otro `codigo`) llegaba a la misma ruta y la pantalla seguía mostrando la
/// invitación anterior, sin consultar la nueva.
const _a =
    '12345678-1234-4234-8234-123456789abc.'
    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
const _b =
    '22345678-1234-4234-8234-123456789abc.'
    'fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210';

void main() {
  testWidgets(
    'un segundo enlace con la app abierta consulta y muestra el nuevo',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      final consultados = <String>[];
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (o, h) {
            final d = o.data;
            if (o.path == '/grupos/invitaciones/enlace/consultar' && d is Map) {
              consultados.add(d['tokenId'] as String);
            }
            h.next(o);
          },
        ),
      );
      adaptador.onPost(
        '/grupos/invitaciones/enlace/consultar',
        (s) => s.reply(200, {
          'grupoId': '11111111-1111-4111-8111-111111111111',
          'nombre': 'Grupo',
          'montoAporte': '100.00',
          'moneda': 'BOB',
          'periodicidad': 'MENSUAL',
          'reglamento': 'R',
          'hashReglamento': 'a' * 64,
        }),
        data: Matchers.any,
      );
      final almacen = AlmacenEnMemoria();
      await almacen.guardar('sesion.acceso', 'a.b.c');
      final enrutador = GoRouter(
        initialLocation: '/pasanaku/unirse/$_a',
        routes: rutasDeInvitacion,
      );
      await tester.pumpWidget(
        ProviderScope(
          retry: (r, e) => null,
          overrides: [
            almacenSeguroProvider.overrideWithValue(almacen),
            dioProvider.overrideWithValue(dio),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            routerConfig: enrutador,
          ),
        ),
      );
      await asentar(tester);
      expect(consultados, ['12345678-1234-4234-8234-123456789abc']);

      enrutador.go('/pasanaku/unirse/$_b');
      await asentar(tester);
      expect(consultados, [
        '12345678-1234-4234-8234-123456789abc',
        '22345678-1234-4234-8234-123456789abc',
      ]);
    },
  );
}
