import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/cu10_recargar_saldo.dart';
import 'package:aportaya_movil/proveedores/idempotencia.dart';

/// DOBLE declarado del servidor de `nucleo-financiero` (regla 65): responde por
/// Idempotency-Key como promete el contrato. La misma clave devuelve la misma orden
/// sin crear otra; cada respuesta programada se consume en orden.
class _Servidor implements HttpClientAdapter {
  final claves = <String>[];
  final ordenesCreadas = <String>{};
  final respuestas = <Object>[]; // int (estado) o DioExceptionType

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final clave = '${options.headers['Idempotency-Key']}';
    claves.add(clave);
    final siguiente = respuestas.isEmpty ? 201 : respuestas.removeAt(0);
    if (siguiente is DioExceptionType) {
      // Resultado desconocido: el servidor pudo haber procesado (lo registramos).
      ordenesCreadas.add(clave);
      throw DioException(requestOptions: options, type: siguiente);
    }
    final estado = siguiente as int;
    if (estado >= 200 && estado < 300) ordenesCreadas.add(clave);
    if (estado >= 500) ordenesCreadas.add(clave);
    final cuerpo = estado < 300
        ? {
            'ordenRecargaId':
                '00000000-0000-4000-8000-00000000000${ordenesCreadas.length}',
            'estado': 'PENDIENTE',
            'expiraEn': '2026-11-21T20:00:00Z',
            'acreditara': {'monto': '100.00', 'moneda': 'BOB'},
          }
        : {'codigo': 'AP-CU10-02', 'mensaje': 'Monto fuera de los límites.'};
    return ResponseBody.fromString(
      jsonEncode(cuerpo),
      estado,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

const _cuenta = 'c0000000-0000-4000-8000-0000000000c1';

void main() {
  late _Servidor servidor;
  late ProviderContainer c;

  setUp(() async {
    servidor = _Servidor();
    final dio = Dio(BaseOptions(baseUrl: 'http://api.invalid'))
      ..httpClientAdapter = servidor;
    c = ProviderContainer(overrides: [dioProvider.overrideWithValue(dio)]);
    // El notifier construye en asíncrono; mientras tanto `enviar` se niega (isLoading).
    await c.read(recargarSaldoProvider.future);
  });
  tearDown(() => c.dispose());

  Future<void> recargar(String monto) => c
      .read(recargarSaldoProvider.notifier)
      .enviar(cuentaId: _cuenta, monto: monto, medio: 'QR');

  group('correcto', () {
    test(
      'el éxito cierra la clave: la próxima recarga es otra operación',
      () async {
        await recargar('100.00');
        await recargar('100.00');
        expect(servidor.claves, hasLength(2));
        expect(servidor.claves.toSet(), hasLength(2));
      },
    );
  });

  group('límite: resultado desconocido conserva la clave', () {
    test(
      'timeout y reintento del mismo importe → misma clave, una sola orden',
      () async {
        servidor.respuestas.add(DioExceptionType.receiveTimeout);
        await recargar('100.00');
        expect(c.read(recargarSaldoProvider).hasError, isTrue);
        await recargar('100.00');
        expect(servidor.claves, hasLength(2));
        expect(servidor.claves[0], servidor.claves[1]);
        expect(servidor.ordenesCreadas, hasLength(1));
      },
    );

    test('un 503 también conserva la clave', () async {
      servidor.respuestas.add(503);
      await recargar('100.00');
      await recargar('100.00');
      expect(servidor.claves[0], servidor.claves[1]);
    });

    test('un 429 («probá de nuevo») conserva la clave', () async {
      servidor.respuestas.add(429);
      await recargar('100.00');
      await recargar('100.00');
      expect(servidor.claves[0], servidor.claves[1]);
    });
  });

  group('inválido: el rechazo definitivo libera la clave', () {
    test(
      '422 por importe, corrección y reenvío → se procesa con otra clave',
      () async {
        servidor.respuestas.add(422);
        await recargar('999999.00');
        expect(c.read(recargarSaldoProvider).hasError, isTrue);
        await recargar('100.00');
        expect(c.read(recargarSaldoProvider).value, isNotNull);
        expect(servidor.claves[0], isNot(servidor.claves[1]));
        expect(servidor.ordenesCreadas, hasLength(1));
      },
    );

    test(
      '422 y reenvío del MISMO contenido → clave nueva (la vieja se liberó)',
      () async {
        servidor.respuestas.add(422);
        await recargar('100.00');
        await recargar('100.00');
        expect(servidor.claves[0], isNot(servidor.claves[1]));
      },
    );
  });

  test(
    'esRechazoDefinitivo: 4xx sí; 408, 425, 429, 5xx y sin respuesta, no',
    () {
      DioException con(int? estado) => DioException(
        requestOptions: RequestOptions(),
        response: estado == null
            ? null
            : Response(requestOptions: RequestOptions(), statusCode: estado),
      );
      expect(esRechazoDefinitivo(con(400)), isTrue);
      expect(esRechazoDefinitivo(con(409)), isTrue);
      expect(esRechazoDefinitivo(con(422)), isTrue);
      for (final duda in [408, 425, 429, 500, 503, null]) {
        expect(esRechazoDefinitivo(con(duda)), isFalse, reason: '$duda');
      }
      expect(esRechazoDefinitivo(StateError('x')), isFalse);
    },
  );
}
