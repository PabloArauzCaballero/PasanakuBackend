import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('un rechazo API envuelto por Dio conserva el mensaje de dominio', () {
    final error = DioException(
      requestOptions: RequestOptions(path: '/usuarios/verificacion'),
      error: ErrorDeApi(
        codigo: 'AP-SEG-01',
        trazaId: 'traza-sintetica',
        estado: 403,
      ),
    );

    expect(
      mensajeDeError(error),
      'Esta acción todavía no está disponible para tu cuenta.',
    );
    expect(trazaDeError(error), 'traza-sintetica');
  });

  test('un error de red envuelto por Dio conserva la ayuda de conexión', () {
    final error = DioException(
      requestOptions: RequestOptions(path: '/usuarios/verificacion'),
      error: ErrorDeRed(),
    );

    expect(mensajeDeError(error), contains('No hay conexión.'));
    expect(trazaDeError(error), isNull);
  });

  test('un fallo desconocido puede usar texto específico de la operación', () {
    const mensaje = 'No pudimos guardar la fecha del documento.';

    expect(
      mensajeDeError(StateError('fallo interno'), mensajeDesconocido: mensaje),
      mensaje,
    );
  });
}
