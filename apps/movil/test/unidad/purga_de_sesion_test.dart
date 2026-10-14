import 'dart:convert';
import 'dart:typed_data';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/cu13_consultar_saldo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _AdaptadorSaldo implements HttpClientAdapter {
  int consultas = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    consultas++;
    return ResponseBody.fromString(
      jsonEncode({
        'cuentaId': 'cuenta-sintetica',
        'disponible': {'monto': '100.00', 'moneda': 'BOB'},
        'retenido': {'monto': '0.00', 'moneda': 'BOB'},
        'alCorteDe': '2026-10-12T12:00:00Z',
      }),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  test(
    'el saldo de una sesión anterior se vuelve a pedir tras cerrar y entrar otra',
    () async {
      final ahora = DateTime.utc(2026, 10, 12, 12);
      final sesion = Sesion(AlmacenEnMemoria(), () => ahora);
      await sesion.abrir(acceso: 'access-A', refresco: 'refresh-A');
      final dio = Dio(
        BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'),
      );
      final adaptador = _AdaptadorSaldo();
      dio.httpClientAdapter = adaptador;
      instalarInterceptores(dio, sesion);
      final container = ProviderContainer(
        overrides: [
          sesionProvider.overrideWithValue(sesion),
          dioProvider.overrideWithValue(dio),
        ],
      );
      final suscripcion = container.listen(
        saldoProvider('cuenta-sintetica'),
        (_, _) {},
      );

      await container.read(saldoProvider('cuenta-sintetica').future);
      expect(adaptador.consultas, 1);

      await sesion.cerrar();
      await sesion.abrir(acceso: 'access-B', refresco: 'refresh-B');
      await container.read(saldoProvider('cuenta-sintetica').future);

      expect(adaptador.consultas, 2);
      suscripcion.close();
      container.dispose();
    },
  );
}
