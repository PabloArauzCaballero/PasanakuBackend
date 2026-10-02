import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../proveedores/idempotencia.dart';
import 'capturas_del_expediente.dart';
import 'cu02_subir_foto.dart';
import 'estado_sesion.dart' show mensajeDeError;

/// El estado de la subida de UNA cara — el mismo vocabulario que Atlas: pendiente,
/// subiendo, lenta (más de 10 s), subida, o fallida con su error.
enum EstadoDeUnaSubida { pendiente, subiendo, lenta, subida, fallida }

class SubidaDeUnaCara {
  const SubidaDeUnaCara({
    this.estado = EstadoDeUnaSubida.pendiente,
    this.error,
  });
  final EstadoDeUnaSubida estado;
  final String? error;

  SubidaDeUnaCara copiarCon({EstadoDeUnaSubida? estado, String? error}) =>
      SubidaDeUnaCara(estado: estado ?? this.estado, error: error);
}

class EstadoDeLaSubida {
  const EstadoDeLaSubida({this.porCara = const {}});
  final Map<CaraDelCarril, SubidaDeUnaCara> porCara;

  bool get completa => CapturasDelExpediente.orden.every(
    (c) => porCara[c]?.estado == EstadoDeUnaSubida.subida,
  );

  bool get subiendoAlguna => porCara.values.any(
    (s) =>
        s.estado == EstadoDeUnaSubida.subiendo ||
        s.estado == EstadoDeUnaSubida.lenta,
  );
}

/// Sube las cinco fotos ya validadas durante el paso de capturas, una por una, con
/// la misma máquina de estados que Atlas: "Subiendo…" y, a los 10 s, "Está
/// tardando más de lo normal. Puedes esperar o cancelar…".
class SubidaNotifier extends Notifier<EstadoDeLaSubida> {
  final _cancelaciones = <CaraDelCarril, CancelToken>{};
  final _temporizadores = <CaraDelCarril, Timer>{};

  @override
  EstadoDeLaSubida build() {
    ref.onDispose(() {
      for (final t in _temporizadores.values) {
        t.cancel();
      }
    });
    return const EstadoDeLaSubida();
  }

  void _actualizar(CaraDelCarril cara, SubidaDeUnaCara valor) {
    state = EstadoDeLaSubida(porCara: {...state.porCara, cara: valor});
  }

  /// Sube las que todavía no están `subida`, en orden. Si una falla, sigue con las
  /// demás — igual que el resto de la app: una foto que no sube no tapa a las otras.
  Future<void> subirPendientes({
    required String usuarioId,
    required CapturasDelExpediente capturas,
  }) async {
    for (final cara in CapturasDelExpediente.orden) {
      if (state.porCara[cara]?.estado == EstadoDeUnaSubida.subida) continue;
      final captura = capturas.porCara[cara];
      if (captura == null) continue;
      await _subirUna(usuarioId: usuarioId, cara: cara, ruta: captura.ruta);
    }
  }

  Future<void> _subirUna({
    required String usuarioId,
    required CaraDelCarril cara,
    required String ruta,
  }) async {
    final cancelacion = CancelToken();
    _cancelaciones[cara] = cancelacion;
    _actualizar(
      cara,
      const SubidaDeUnaCara(estado: EstadoDeUnaSubida.subiendo),
    );
    _temporizadores[cara]?.cancel();
    _temporizadores[cara] = Timer(const Duration(seconds: 10), () {
      if (state.porCara[cara]?.estado == EstadoDeUnaSubida.subiendo) {
        _actualizar(
          cara,
          const SubidaDeUnaCara(estado: EstadoDeUnaSubida.lenta),
        );
      }
    });
    try {
      await ref
          .read(subidaDeFotosProvider)
          .subir(
            usuarioId: usuarioId,
            cara: cara,
            rutaLocal: ruta,
            formularioId: 'alta-${cara.name}',
            cancelToken: cancelacion,
          );
      _temporizadores[cara]?.cancel();
      _actualizar(
        cara,
        const SubidaDeUnaCara(estado: EstadoDeUnaSubida.subida),
      );
    } on DioException catch (e) {
      _temporizadores[cara]?.cancel();
      if (CancelToken.isCancel(e)) {
        _actualizar(
          cara,
          const SubidaDeUnaCara(estado: EstadoDeUnaSubida.pendiente),
        );
        return;
      }
      _actualizar(
        cara,
        SubidaDeUnaCara(
          estado: EstadoDeUnaSubida.fallida,
          error: mensajeDeError(e),
        ),
      );
    }
  }

  /// "Cancelar la subida": corta la petición en curso de esa cara.
  void cancelar(CaraDelCarril cara) {
    _cancelaciones[cara]?.cancel();
  }

  /// "Reintentar": mismo archivo, misma clave de idempotencia — un reintento del
  /// mismo intento, no una operación nueva.
  Future<void> reintentar({
    required String usuarioId,
    required CaraDelCarril cara,
    required String ruta,
  }) => _subirUna(usuarioId: usuarioId, cara: cara, ruta: ruta);

  /// "Repetir la foto": rota la clave de idempotencia — la foto nueva es, a
  /// propósito, una operación distinta de la que falló.
  void rotarClaveParaRepetir(CaraDelCarril cara) {
    ref.read(idempotenciaProvider.notifier).cerrar('alta-${cara.name}');
  }
}

final subidaProvider = NotifierProvider<SubidaNotifier, EstadoDeLaSubida>(
  SubidaNotifier.new,
);
