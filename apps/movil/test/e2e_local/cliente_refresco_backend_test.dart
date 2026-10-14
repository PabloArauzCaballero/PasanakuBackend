import 'dart:io' show Platform;

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _nombresDeEntorno = [
  'H1_S6_M8_BASE_URL',
  'H1_S6_M8_USER_ID',
  'H1_S6_M8_ACCESS_EXPIRADO',
  'H1_S6_M8_REFRESH',
];

void main() {
  final entorno = Platform.environment;
  final disponible = _nombresDeEntorno.every(
    (nombre) => entorno[nombre]?.isNotEmpty ?? false,
  );

  test(
    'access vencido renueva contra identidad y reintenta el recurso protegido',
    () async {
      final uri = Uri.parse(entorno['H1_S6_M8_BASE_URL']!);
      expect(uri.scheme, 'http');
      expect(uri.host, '127.0.0.1');
      expect(uri.port, 8085);

      final usuarioId = entorno['H1_S6_M8_USER_ID']!;
      final accesoVencido = entorno['H1_S6_M8_ACCESS_EXPIRADO']!;
      final refrescoInicial = entorno['H1_S6_M8_REFRESH']!;
      final ruta = '/usuarios/$usuarioId/kyc';

      final dioDeSonda = Dio(
        BaseOptions(
          baseUrl: uri.toString(),
          headers: {'Authorization': 'Bearer $accesoVencido'},
        ),
      );
      addTearDown(dioDeSonda.close);
      try {
        await dioDeSonda.get<Map<String, dynamic>>(ruta);
        fail('el access vencido debe recibir 401');
      } on DioException catch (error) {
        expect(error.response?.statusCode, 401);
        // ignore: avoid_print
        print('H1_S6_M8_EXPIRED_ACCESS_STATUS=401');
      }

      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.abrir(acceso: accesoVencido, refresco: refrescoInicial);

      final dio = Dio(BaseOptions(baseUrl: uri.toString()));
      instalarInterceptores(dio, sesion);
      addTearDown(dio.close);

      final respuesta = await dio.get<Map<String, dynamic>>(ruta);

      expect(respuesta.statusCode, 200);
      expect(respuesta.data?['usuarioId'], usuarioId);
      // ignore: avoid_print
      print('H1_S6_M8_CLIENT_RETRY_STATUS=${respuesta.statusCode}');
      final accesoRenovado = await sesion.tokenDeAcceso();
      final refrescoRotado = await sesion.tokenDeRefresco();
      expect(accesoRenovado, isNot(accesoVencido));
      expect(refrescoRotado, isNot(refrescoInicial));

      final cierre = await dio.delete<void>('/sesion');
      expect(cierre.statusCode, 204);
      // ignore: avoid_print
      print('H1_S6_M8_LOGOUT_STATUS=${cierre.statusCode}');

      final dioSinInterceptores = Dio(BaseOptions(baseUrl: uri.toString()));
      addTearDown(dioSinInterceptores.close);
      await expectLater(
        dioSinInterceptores.post<Map<String, dynamic>>(
          '/sesion/refrescar',
          data: {'refresco': refrescoRotado},
        ),
        throwsA(
          isA<DioException>().having(
            (error) => error.response?.statusCode,
            'statusCode',
            401,
          ),
        ),
      );
      // ignore: avoid_print
      print('H1_S6_M8_REFRESH_AFTER_LOGOUT_STATUS=401');
    },
    skip: disponible
        ? false
        : 'requiere backend identidad local y JWT sintético vencido',
  );
}
