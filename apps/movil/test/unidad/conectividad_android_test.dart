import 'dart:async';
import 'dart:io';

import 'package:aportaya_movil/infraestructura/android/conectividad_android.dart';
import 'package:aportaya_movil/proveedores/conexion.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

void main() {
  const base = 'https://pruebas.aportaya.bo/api/v1';

  for (final (estadoHttp, esperado) in [
    (200, true),
    (404, true),
    (500, true),
  ]) {
    test('HTTP $estadoHttp significa gateway alcanzable, no offline', () async {
      final dio = Dio(BaseOptions(baseUrl: base));
      final adaptador = DioAdapter(
        dio: dio,
        matcher: const UrlRequestMatcher(),
      );
      adaptador.onGet(
        '$base/version',
        (s) => s.reply(estadoHttp, {'estado': 'ok'}),
      );
      var peticiones = 0;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (opciones, manejador) {
            peticiones++;
            expect(opciones.uri.toString(), '$base/version');
            manejador.next(opciones);
          },
        ),
      );
      final conectividad = ConectividadAndroid(
        sonda: dio,
        baseDeSonda: base,
        comprobarRed: () async => [ConnectivityResult.wifi],
      );

      expect(await conectividad.hayConexion(), esperado);
      expect(peticiones, 1);
    });
  }

  test('sin red del sistema no sondea el gateway', () async {
    final dio = Dio();
    var peticiones = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, manejador) {
          peticiones++;
          manejador.next(opciones);
        },
      ),
    );
    final conectividad = ConectividadAndroid(
      sonda: dio,
      baseDeSonda: base,
      comprobarRed: () async => [ConnectivityResult.none],
    );

    expect(await conectividad.hayConexion(), isFalse);
    expect(peticiones, 0);
  });

  test(
    'error de conexión al gateway no afirma que el teléfono esté offline',
    () async {
      final dio = Dio();
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (opciones, manejador) => manejador.reject(
            DioException(
              requestOptions: opciones,
              type: DioExceptionType.connectionError,
            ),
          ),
        ),
      );
      final conectividad = ConectividadAndroid(
        sonda: dio,
        baseDeSonda: base,
        comprobarRed: () async => [ConnectivityResult.wifi],
      );

      await expectLater(
        conectividad.hayConexion(),
        throwsA(isA<SondaNoComprobada>()),
      );
    },
  );

  test(
    'gateway que acepta la conexión pero no responde no deja la sonda colgada',
    () async {
      final servidor = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      addTearDown(() => servidor.close(force: true));
      final peticionRecibida = Completer<void>();
      servidor.listen((_) {
        if (!peticionRecibida.isCompleted) peticionRecibida.complete();
        // Simula un gateway que acepta el socket y nunca envía HTTP.
      });
      final conectividad = ConectividadAndroid(
        baseDeSonda: 'http://127.0.0.1:${servidor.port}',
        comprobarRed: () async => [ConnectivityResult.wifi],
        limiteSonda: const Duration(milliseconds: 100),
      );

      final resultado = conectividad.hayConexion();
      await peticionRecibida.future.timeout(const Duration(seconds: 2));
      await expectLater(
        resultado.timeout(const Duration(seconds: 2)),
        throwsA(isA<SondaNoComprobada>()),
      );
    },
  );

  test(
    'sonda vencida durante la consulta al sistema no inicia HTTP tardío',
    () async {
      final redDelSistema = Completer<List<ConnectivityResult>>();
      final dio = Dio();
      var peticiones = 0;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (opciones, manejador) {
            peticiones++;
            manejador.next(opciones);
          },
        ),
      );
      final conectividad = ConectividadAndroid(
        sonda: dio,
        baseDeSonda: base,
        comprobarRed: () => redDelSistema.future,
        limiteSonda: const Duration(milliseconds: 100),
      );

      await expectLater(
        conectividad.hayConexion().timeout(const Duration(seconds: 2)),
        throwsA(isA<SondaNoComprobada>()),
      );
      redDelSistema.complete([ConnectivityResult.wifi]);
      await Future<void>.delayed(Duration.zero);
      expect(peticiones, 0);
    },
  );

  test(
    'gateway recuperado tras timeout habilita una sonda posterior',
    () async {
      final servidor = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      addTearDown(() => servidor.close(force: true));
      var peticiones = 0;
      servidor.listen((solicitud) async {
        peticiones++;
        if (peticiones == 1) return; // Primer GET aceptado, sin respuesta.
        solicitud.response.statusCode = 200;
        await solicitud.response.close();
      });
      final cambiosDeRed = StreamController<List<ConnectivityResult>>();
      addTearDown(cambiosDeRed.close);
      final conectividad = ConectividadAndroid(
        baseDeSonda: 'http://127.0.0.1:${servidor.port}',
        comprobarRed: () async => [ConnectivityResult.wifi],
        cambiosDeRed: cambiosDeRed.stream,
        intervaloDeSonda: const Duration(milliseconds: 20),
        limiteSonda: const Duration(milliseconds: 100),
      );
      final fallo = Completer<void>();
      final recuperacion = Completer<void>();
      final suscripcion = conectividad.cambios.listen(
        (conectado) {
          if (conectado && !recuperacion.isCompleted) recuperacion.complete();
        },
        onError: (Object error, StackTrace traza) {
          expect(error, isA<SondaNoComprobada>());
          if (!fallo.isCompleted) fallo.complete();
        },
      );
      addTearDown(suscripcion.cancel);

      cambiosDeRed.add([ConnectivityResult.wifi]);
      await fallo.future.timeout(const Duration(seconds: 2));
      await recuperacion.future.timeout(const Duration(seconds: 2));
      expect(peticiones, greaterThanOrEqualTo(2));
    },
  );

  test('recupera el gateway aunque no cambie el wifi', () async {
    final cambiosDeRed = StreamController<List<ConnectivityResult>>();
    final dio = Dio();
    var responde = false;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, manejador) {
          if (responde) {
            manejador.resolve(
              Response(requestOptions: opciones, statusCode: 200),
            );
          } else {
            manejador.reject(
              DioException(
                requestOptions: opciones,
                type: DioExceptionType.connectionError,
              ),
            );
          }
        },
      ),
    );
    final conectividad = ConectividadAndroid(
      sonda: dio,
      baseDeSonda: base,
      comprobarRed: () async => [ConnectivityResult.wifi],
      cambiosDeRed: cambiosDeRed.stream,
      intervaloDeSonda: const Duration(milliseconds: 20),
    );
    final fallo = Completer<void>();
    final recuperacion = Completer<void>();
    final suscripcion = conectividad.cambios.listen(
      (conectado) {
        if (conectado && !recuperacion.isCompleted) recuperacion.complete();
      },
      onError: (Object error, StackTrace traza) {
        expect(error, isA<SondaNoComprobada>());
        if (!fallo.isCompleted) fallo.complete();
      },
    );

    cambiosDeRed.add([ConnectivityResult.wifi]);
    await fallo.future.timeout(const Duration(seconds: 1));
    responde = true;
    await recuperacion.future.timeout(const Duration(seconds: 1));

    await suscripcion.cancel();
    await cambiosDeRed.close();
  });

  test(
    'fallo inicial queda como error y la sonda periódica recupera el acceso',
    () async {
      final cambiosDeRed = StreamController<List<ConnectivityResult>>();
      addTearDown(cambiosDeRed.close);
      final dio = Dio();
      var responde = false;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (opciones, manejador) {
            if (responde) {
              manejador.resolve(
                Response(requestOptions: opciones, statusCode: 200),
              );
            } else {
              manejador.reject(
                DioException(
                  requestOptions: opciones,
                  type: DioExceptionType.connectionError,
                ),
              );
            }
          },
        ),
      );
      final conectividad = ConectividadAndroid(
        sonda: dio,
        baseDeSonda: base,
        comprobarRed: () async => [ConnectivityResult.wifi],
        cambiosDeRed: cambiosDeRed.stream,
        intervaloDeSonda: const Duration(milliseconds: 20),
      );
      final contenedor = ProviderContainer(
        overrides: [conectividadProvider.overrideWithValue(conectividad)],
      );
      addTearDown(contenedor.dispose);
      final fallo = Completer<void>();
      final recuperacion = Completer<void>();
      contenedor.listen(hayConexionProvider, (_, siguiente) {
        if (siguiente.hasError && !fallo.isCompleted) fallo.complete();
        if (siguiente.permiteOperar && !recuperacion.isCompleted) {
          recuperacion.complete();
        }
      });

      await fallo.future.timeout(const Duration(seconds: 2));
      expect(contenedor.read(hayConexionProvider).permiteOperar, isFalse);
      responde = true;
      await recuperacion.future.timeout(const Duration(seconds: 2));
      expect(contenedor.read(hayConexionProvider).permiteOperar, isTrue);
    },
  );
}
