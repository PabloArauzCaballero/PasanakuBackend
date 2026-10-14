import 'dart:convert';
import 'dart:typed_data';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu04_autenticar.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

const _telefonoSintetico = '+59170000001';
const _claveSintetica = 'SYNTHETIC_PASSWORD_H10_S4_M1';
const _accesoSintetico = 'SYNTHETIC_ACCESS_TOKEN_H10_S4_M1';

class _AlmacenEnMemoria implements AlmacenSeguro {
  final _valores = <String, String>{};

  @override
  Future<String?> leer(String clave) async => _valores[clave];

  @override
  Future<void> guardar(String clave, String valor) async {
    _valores[clave] = valor;
  }

  @override
  Future<void> borrar(String clave) async {
    _valores.remove(clave);
  }
}

class _RespuestaDeInicio implements HttpClientAdapter {
  int llamadas = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    llamadas++;
    return ResponseBody.fromString(
      jsonEncode({
        'sesionId': 'sesion-sintetica-h10-s4-m1',
        'expiraEn': '2026-10-14T18:00:00Z',
        'requiereFactorAdicional': false,
        'dispositivoConfiable': false,
        'tokenAcceso': _accesoSintetico,
        // La ausencia de tokenRefresco activa el único logger de producción.
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
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  test('H10.S4.M1 login sintetico no vuelca secretos en logs', () async {
    final respuesta = _RespuestaDeInicio();
    final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
      ..httpClientAdapter = respuesta;
    final contenedor = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(_AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(() {
      contenedor.dispose();
      dio.close(force: true);
    });

    final resultado = await contenedor
        .read(autenticacionProvider)
        .conCredenciales(
          telefonoE164: _telefonoSintetico,
          credencial: _claveSintetica,
        );

    expect(respuesta.llamadas, 1);
    expect(resultado, ResultadoDeAutenticacion.entro);
    expect(
      await contenedor.read(sesionProvider).tokenDeAcceso(),
      _accesoSintetico,
    );
    expect(await contenedor.read(sesionProvider).tokenDeRefresco(), isNull);
  });
}
