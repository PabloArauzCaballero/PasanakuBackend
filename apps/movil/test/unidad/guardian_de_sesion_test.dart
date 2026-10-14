import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/infraestructura/capacidades.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/proveedores/capacidades.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('sin sesión, abrir la billetera muestra el ingreso', (
    tester,
  ) async {
    final red = dioSimulado();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(red.dio),
          capacidadesProvider.overrideWithValue(
            capacidadesDeLaPlataforma(forzarIOSParaPruebas: true),
          ),
        ],
        child: const AppAportaYa(inicial: '/billetera/inicio'),
      ),
    );
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();

    expect(find.byType(PantallaDeSaldo), findsNothing);
    expect(find.byType(PantallaDeSesion), findsOneWidget);
    expect(find.byKey(const ValueKey('pantalla-de-ingreso')), findsOneWidget);
  });

  testWidgets('un 401 cierra la sesión y lleva al ingreso', (tester) async {
    final almacen = AlmacenEnMemoria();
    final sesion = Sesion(almacen);
    await sesion.abrir(acceso: 'token-vencido', refresco: 'refresco-vencido');
    final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
      ..httpClientAdapter = _Siempre401();
    instalarInterceptores(dio, sesion);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(almacen),
          sesionProvider.overrideWithValue(sesion),
          dioProvider.overrideWithValue(dio),
          capacidadesProvider.overrideWithValue(
            capacidadesDeLaPlataforma(forzarIOSParaPruebas: true),
          ),
        ],
        child: const AppAportaYa(inicial: '/billetera/inicio'),
      ),
    );
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();

    expect(await sesion.tokenDeAcceso(), isNull);
    expect(find.byType(PantallaDeSaldo), findsNothing);
    expect(find.byType(PantallaDeSesion), findsOneWidget);
    expect(find.byKey(const ValueKey('pantalla-de-ingreso')), findsOneWidget);
  });
}

class _Siempre401 implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async => ResponseBody.fromBytes(
    utf8.encode('{}'),
    401,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );

  @override
  void close({bool force = false}) {}
}
