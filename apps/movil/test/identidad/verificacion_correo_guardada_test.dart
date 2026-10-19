import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/verificacion_correo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';

import '../comun.dart';

/// Doble del servidor de verificación de correo. Responde como el contrato de
/// `identidad.yaml`: la misma Idempotency-Key devuelve la misma verificación sin
/// emitir otra; una clave nueva emite (y cuenta contra el tope).
class _ServidorDeCorreo implements HttpClientAdapter {
  _ServidorDeCorreo({this.expiraEn});

  final DateTime? expiraEn;
  final emisiones = <String>[];
  final confirmaciones = <String>[];
  final _porClave = <String, String>{};
  bool rechazarClavesConocidas = false;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (options.path.endsWith('/confirmacion')) {
      confirmaciones.add(options.path);
      return _json(200, {'verificada': true});
    }
    final clave = '${options.headers['Idempotency-Key']}';
    final conocida = _porClave[clave];
    if (conocida != null && rechazarClavesConocidas) {
      return _json(422, {
        'codigo': 'AP-CU01-07',
        'mensaje': 'La solicitud anterior ya no es util. Pedi un codigo nuevo.',
      });
    }
    final id = conocida ?? 'ver-${_porClave.length + 1}';
    if (conocida == null) {
      _porClave[clave] = id;
      emisiones.add(id);
    }
    return _json(201, {
      'verificacionId': id,
      'destinoEnmascarado': 'a***@ejemplo.invalid',
      'expiraEn': (expiraEn ?? DateTime.now().add(const Duration(minutes: 10)))
          .toUtc()
          .toIso8601String(),
    });
  }

  ResponseBody _json(int estado, Map<String, Object> cuerpo) =>
      ResponseBody.fromString(
        jsonEncode(cuerpo),
        estado,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType],
        },
      );

  @override
  void close({bool force = false}) {}
}

const _correo = 'persona@ejemplo.invalid';

/// Una "apertura" de la app: contenedor nuevo (memoria limpia), mismo almacén.
ProviderContainer _abrirApp(
  AlmacenEnMemoria almacen,
  _ServidorDeCorreo servidor,
) {
  final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
    ..httpClientAdapter = servidor;
  final c = ProviderContainer(
    overrides: [
      dioProvider.overrideWithValue(dio),
      almacenSeguroProvider.overrideWithValue(almacen),
    ],
  );
  addTearDown(c.dispose);
  return c;
}

void main() {
  test(
    'correcto: tras cerrar la app, el mismo correo reutiliza la verificación '
    'pendiente sin emitir otro código',
    () async {
      final almacen = AlmacenEnMemoria();
      final servidor = _ServidorDeCorreo();

      final primera = await _abrirApp(
        almacen,
        servidor,
      ).read(verificacionCorreoProvider).solicitar(_correo);
      final tras = await _abrirApp(
        almacen,
        servidor,
      ).read(verificacionCorreoProvider).solicitar(' Persona@Ejemplo.invalid ');

      expect(tras.id, primera.id);
      expect(tras.confirmada, isFalse);
      expect(servidor.emisiones, hasLength(1));
    },
  );

  test(
    'límite: si ya había confirmado el código, al volver no se pide otro',
    () async {
      final almacen = AlmacenEnMemoria();
      final servidor = _ServidorDeCorreo();
      final app = _abrirApp(almacen, servidor);
      final s = await app.read(verificacionCorreoProvider).solicitar(_correo);
      await app
          .read(verificacionCorreoProvider)
          .confirmar(id: s.id, correo: _correo, codigo: '123456');

      final tras = await _abrirApp(
        almacen,
        servidor,
      ).read(verificacionCorreoProvider).solicitar(_correo);

      expect(tras.id, s.id);
      expect(tras.confirmada, isTrue);
      expect(servidor.emisiones, hasLength(1));
    },
  );

  test('límite: una verificación vencida no se reutiliza', () async {
    final almacen = AlmacenEnMemoria();
    final vencido = _ServidorDeCorreo(
      expiraEn: DateTime.now().subtract(const Duration(seconds: 1)),
    );
    await _abrirApp(
      almacen,
      vencido,
    ).read(verificacionCorreoProvider).solicitar(_correo);

    final servidor = _ServidorDeCorreo();
    final tras = await _abrirApp(
      almacen,
      servidor,
    ).read(verificacionCorreoProvider).solicitar(_correo);

    expect(tras.confirmada, isFalse);
    expect(servidor.emisiones, hasLength(1));
  });

  test('inválido: otro correo no hereda la verificación guardada', () async {
    final almacen = AlmacenEnMemoria();
    final servidor = _ServidorDeCorreo();
    final primera = await _abrirApp(
      almacen,
      servidor,
    ).read(verificacionCorreoProvider).solicitar(_correo);

    final otra = await _abrirApp(
      almacen,
      servidor,
    ).read(verificacionCorreoProvider).solicitar('otra@ejemplo.invalid');

    expect(otra.id, isNot(primera.id));
    expect(servidor.emisiones, hasLength(2));
  });

  test('inválido: si el servidor ya no acepta la anterior, se pide una nueva '
      'y no queda el error', () async {
    final almacen = AlmacenEnMemoria();
    final servidor = _ServidorDeCorreo();
    final primera = await _abrirApp(
      almacen,
      servidor,
    ).read(verificacionCorreoProvider).solicitar(_correo);
    servidor.rechazarClavesConocidas = true;

    final tras = await _abrirApp(
      almacen,
      servidor,
    ).read(verificacionCorreoProvider).solicitar(_correo);

    expect(tras.id, isNot(primera.id));
    expect(servidor.emisiones, hasLength(2));
  });

  test(
    'inválido: lo guardado ilegible se descarta sin romper el alta',
    () async {
      final almacen = AlmacenEnMemoria();
      await almacen.guardar(VerificacionCorreo.claveGuardada, '{roto');
      final servidor = _ServidorDeCorreo();

      final s = await _abrirApp(
        almacen,
        servidor,
      ).read(verificacionCorreoProvider).solicitar(_correo);

      expect(s.id, 'ver-1');
      expect(servidor.emisiones, hasLength(1));
    },
  );

  test('pedir uno nuevo a propósito borra lo guardado y emite otro', () async {
    final almacen = AlmacenEnMemoria();
    final servidor = _ServidorDeCorreo();
    final app = _abrirApp(almacen, servidor);
    await app.read(verificacionCorreoProvider).solicitar(_correo);

    await app.read(verificacionCorreoProvider).solicitar(_correo, nueva: true);

    expect(servidor.emisiones, hasLength(2));
  });
}
