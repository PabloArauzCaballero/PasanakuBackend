import 'dart:async';

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cu02_consultar_verificacion.dart';

/// Los cuatro estados finales/no-finales, igual que Atlas: `PENDIENTE` y
/// `EN_REVISION` siguen consultando solas; `APROBADA` y `RECHAZADA` detienen el
/// sondeo.
bool esEstadoFinal(EstadoDeVerificacionEstadoEnum estado) =>
    estado == EstadoDeVerificacionEstadoEnum.APROBADA ||
    estado == EstadoDeVerificacionEstadoEnum.RECHAZADA;

/// Consulta el estado de la verificación cada 2 s, hasta 20 intentos o hasta un
/// estado final — el mismo límite que usa Atlas para no sondear para siempre si
/// algo quedó colgado del lado del motor.
class VerificacionNotifier extends AsyncNotifier<EstadoDeVerificacion> {
  static const _intervalo = Duration(seconds: 2);
  static const _maximoDeIntentos = 20;

  String? _usuarioId;
  Timer? _temporizador;
  int _intentos = 0;

  @override
  Future<EstadoDeVerificacion> build() {
    ref.onDispose(() => _temporizador?.cancel());
    final usuarioId = _usuarioId;
    if (usuarioId == null) {
      // No hay nada que consultar todavía: `iniciar` lo fija antes de la primera
      // pantalla que lo mira.
      return Future.error(StateError('Todavía no hay usuario para consultar.'));
    }
    return ref.read(consultarVerificacionProvider).ejecutar(usuarioId);
  }

  /// Arranca el sondeo para un usuario. Se llama una sola vez, al entrar a la
  /// pantalla de estado.
  void iniciar(String usuarioId) {
    _usuarioId = usuarioId;
    _intentos = 0;
    unawaited(actualizar());
  }

  /// "Actualizar estado": lo dispara el sondeo automático y también el botón
  /// manual de la pantalla, igual que en Atlas.
  Future<void> actualizar() async {
    final usuarioId = _usuarioId;
    if (usuarioId == null) return;
    state = const AsyncLoading<EstadoDeVerificacion>();
    try {
      final expediente = await ref
          .read(consultarVerificacionProvider)
          .ejecutar(usuarioId);
      state = AsyncData(expediente);
      _intentos++;
      _temporizador?.cancel();
      if (!esEstadoFinal(expediente.estado) && _intentos < _maximoDeIntentos) {
        _temporizador = Timer(_intervalo, () => unawaited(actualizar()));
      }
    } catch (e, trazado) {
      state = AsyncError(e, trazado);
    }
  }
}

final verificacionProvider =
    AsyncNotifierProvider<VerificacionNotifier, EstadoDeVerificacion>(
      VerificacionNotifier.new,
    );
