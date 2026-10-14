import 'dart:async';
import 'dart:typed_data';

import 'package:aportaya_movil/dominio/cierre_de_sesion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:fake_async/fake_async.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _AdaptadorColgado implements HttpClientAdapter {
  int llamadas = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    llamadas++;
    return Completer<ResponseBody>().future;
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  test(
    'servidor colgado: intenta como máximo dos veces y cierra localmente en cinco segundos',
    () async {
      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.abrir(acceso: 'acceso', refresco: 'refresco');
      final dio = Dio(BaseOptions(baseUrl: 'https://api.pruebas.test/api/v1'));
      final adaptador = _AdaptadorColgado();
      dio.httpClientAdapter = adaptador;
      final cierre = CierreDeSesion(sesion, dio);
      var terminado = false;

      fakeAsync((reloj) {
        cierre.ejecutar().then((_) => terminado = true);
        reloj.flushMicrotasks();
        reloj.elapse(const Duration(seconds: 5));
        reloj.flushMicrotasks();
      });

      expect(terminado, isTrue);
      expect(adaptador.llamadas, 2);
      expect(sesion.estado, EstadoDeSesion.anonima);
      dio.close(force: true);
      sesion.dispose();
    },
  );
}
