import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_actualizar_fecha_expiracion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test(
    'envía fecha civil yyyy-MM-dd, sin hora ni desplazamiento de zona',
    () async {
      final dio = Dio(BaseOptions(baseUrl: 'https://api.example.test/api/v1'))
        ..interceptors.add(
          InterceptorsWrapper(
            onRequest: (options, handler) {
              expect(options.method, 'PUT');
              expect(
                options.path,
                '/usuarios/usuario-sintetico/documento/fecha-expiracion',
              );
              expect(options.data, {'fechaExpiracionDocumento': '2031-02-03'});
              handler.resolve(
                Response<void>(requestOptions: options, statusCode: 204),
              );
            },
          ),
        );
      addTearDown(dio.close);

      final container = ProviderContainer(
        overrides: [dioProvider.overrideWithValue(dio)],
      );
      addTearDown(container.dispose);

      await container
          .read(actualizarFechaExpiracionDocumentoProvider)
          .ejecutar(
            usuarioId: 'usuario-sintetico',
            fecha: DateTime(2031, 2, 3, 23, 59),
          );
    },
  );
}
