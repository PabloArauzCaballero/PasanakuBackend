import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:aportaya_diseno/errores.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';
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
    this.trazaId,
  });
  final EstadoDeUnaSubida estado;
  final String? error;
  final String? trazaId;

  SubidaDeUnaCara copiarCon({
    EstadoDeUnaSubida? estado,
    String? error,
    String? trazaId,
  }) => SubidaDeUnaCara(
    estado: estado ?? this.estado,
    error: error,
    trazaId: trazaId,
  );
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
  var _cancelarLote = false;

  @override
  EstadoDeLaSubida build() {
    ref.watch(generacionDeSesionProvider);
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
    String? tokenDeAlta,
  }) async {
    _cancelarLote = false;
    for (final cara in CapturasDelExpediente.orden) {
      if (_cancelarLote) return;
      if (state.porCara[cara]?.estado == EstadoDeUnaSubida.subida) continue;
      final captura = capturas.porCara[cara];
      if (captura == null) continue;
      await _subirUna(
        usuarioId: usuarioId,
        cara: cara,
        ruta: captura.ruta,
        tokenDeAlta: tokenDeAlta,
      );
      if (_cancelarLote) return;
    }
    if (state.completa) await capturas.eliminarArchivosLocales();
  }

  Future<void> _subirUna({
    required String usuarioId,
    required CaraDelCarril cara,
    required String ruta,
    String? tokenDeAlta,
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
            tokenDeAlta: tokenDeAlta,
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
      final problema = errorDeDominio(e);
      _actualizar(
        cara,
        SubidaDeUnaCara(
          estado: EstadoDeUnaSubida.fallida,
          error: mensajeDeError(problema),
          trazaId: problema is ErrorPresentable ? problema.trazaId : null,
        ),
      );
    }
  }

  /// "Cancelar la subida": corta la petición en curso de esa cara.
  void cancelar(CaraDelCarril cara) {
    _cancelaciones[cara]?.cancel();
  }

  /// Detiene también las caras que aún no se iniciaron cuando se abandona esta
  /// pantalla; cancelar solo una fila mantiene el comportamiento de seguir con las otras.
  void cancelarPendientes() {
    _cancelarLote = true;
    for (final cancelacion in _cancelaciones.values) {
      cancelacion.cancel();
    }
  }

  /// "Reintentar": mismo archivo, misma clave de idempotencia — un reintento del
  /// mismo intento, no una operación nueva.
  Future<void> reintentar({
    required String usuarioId,
    required CaraDelCarril cara,
    required String ruta,
    String? tokenDeAlta,
  }) => _subirUna(
    usuarioId: usuarioId,
    cara: cara,
    ruta: ruta,
    tokenDeAlta: tokenDeAlta,
  );

  /// "Repetir la foto": rota la clave de idempotencia — la foto nueva es, a
  /// propósito, una operación distinta de la que falló.
  void rotarClaveParaRepetir(CaraDelCarril cara) {
    ref.read(idempotenciaProvider.notifier).cerrar('alta-${cara.name}');
  }
}

final subidaProvider = NotifierProvider<SubidaNotifier, EstadoDeLaSubida>(
  SubidaNotifier.new,
);
