import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../../comun.dart';

/// Los estados que se fotografían de cada pantalla (regla 95.2) y de dónde sale cada uno.
enum EstadoDeCaptura {
  /// El ejemplo `ok` del contrato.
  datos('ok'),

  /// El ejemplo `vacio` del contrato.
  vacio('vacio'),

  /// El ejemplo `adverso` del contrato: nombres largos, montos grandes (`break-ui`).
  peorCaso('adverso'),

  /// Todo GET responde 503: el servicio no contesta.
  error(null),

  /// Ninguna respuesta llega nunca: la pantalla queda en su esqueleto.
  cargando(null);

  const EstadoDeCaptura(this.escenario);
  final String? escenario;
}

class _Operacion {
  _Operacion(this.metodo, this.patron, this.escenarios);
  final String metodo;
  final RegExp patron;
  final Map<String, dynamic> escenarios;
}

/// Responde cualquier pedido con el ejemplo de su operación en `packages/simulado/ejemplos`
/// (el MISMO JSON que sirve el simulado y que usan los tests), en el estado elegido.
/// Es un doble declarado: las capturas muestran datos SINTÉTICOS, nunca de personas.
class AdaptadorPorContrato implements HttpClientAdapter {
  AdaptadorPorContrato(this.estado) : _operaciones = _cargar();

  final EstadoDeCaptura estado;
  final List<_Operacion> _operaciones;

  /// Pedidos que ningún contrato describe; la captura los lista para el reporte.
  final sinContrato = <String>{};

  static List<_Operacion> _cargar() {
    final base = Directory(
      '${raizDelRepositorio()}/packages/simulado/ejemplos',
    );
    return [
      for (final archivo in base.listSync(recursive: true).whereType<File>())
        if (archivo.path.endsWith('.json'))
          _desde(
            jsonDecode(archivo.readAsStringSync()) as Map<String, dynamic>,
          ),
    ];
  }

  static _Operacion _desde(Map<String, dynamic> json) {
    final ruta = (json['ruta'] as String).replaceAllMapped(
      RegExp(r'\{[^}]+\}'),
      (_) => '[^/]+',
    );
    return _Operacion(
      (json['metodo'] as String).toUpperCase(),
      RegExp('$ruta\$'),
      json['escenarios'] as Map<String, dynamic>,
    );
  }

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    if (estado == EstadoDeCaptura.cargando) {
      return Completer<ResponseBody>().future;
    }
    final metodo = options.method.toUpperCase();
    if (estado == EstadoDeCaptura.error && metodo == 'GET') {
      return Future.value(
        _json(503, {
          'codigo': 'SERVICIO_NO_DISPONIBLE',
          'mensaje': 'El servicio no responde',
          'trazaId': 'traza-captura',
        }),
      );
    }
    final ruta = options.uri.path;
    for (final operacion in _operaciones) {
      if (operacion.metodo != metodo || !operacion.patron.hasMatch(ruta)) {
        continue;
      }
      final escenario =
          (operacion.escenarios[estado.escenario ?? 'ok'] ??
                  operacion.escenarios['ok'])
              as Map<String, dynamic>;
      return Future.value(
        _json(escenario['estado'] as int, escenario['cuerpo']),
      );
    }
    sinContrato.add('$metodo $ruta');
    return Future.value(_json(404, {'codigo': 'SIN_CONTRATO'}));
  }

  ResponseBody _json(int estado, Object? cuerpo) => ResponseBody.fromString(
    cuerpo == null ? '' : jsonEncode(cuerpo),
    estado,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );

  @override
  void close({bool force = false}) {}
}
