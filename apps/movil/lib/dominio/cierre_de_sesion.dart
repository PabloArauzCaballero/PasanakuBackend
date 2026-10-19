import 'dart:async';

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../proveedores/sesion.dart';
import 'cliente.dart' show dioProvider;

/// Revoca en el servidor cuando hay red y siempre borra localmente, incluso si
/// DELETE queda fuera de línea o agota el plazo total.
class CierreDeSesion {
  CierreDeSesion(this._sesion, this._dio);

  final Sesion _sesion;
  final Dio _dio;

  /// Con [esperarAlServidor] en `false` (el candado: «Entrar con mi contraseña») se
  /// cierra localmente al instante y la revocación sigue sola: antes, con la red
  /// lenta al prender la pantalla, el botón parecía no hacer nada durante 5 s
  /// (2026-10-19).
  Future<void> ejecutar({
    MotivoDeSalida? motivo = MotivoDeSalida.cerrada,
    bool esperarAlServidor = true,
  }) async {
    Future<void> revocacion = Future<void>.value();
    try {
      final acceso = await _sesion.tokenDeAcceso();
      if (acceso != null && acceso.isNotEmpty) {
        revocacion = _revocar(acceso).catchError((Object _) {});
      }
      if (esperarAlServidor) await revocacion;
    } on Object {
      // Cerrar sesión es una operación best-effort remota y obligatoria localmente.
    } finally {
      try {
        await _sesion.cerrar(motivo: motivo);
      } on Object {
        // No propagar errores del transporte ni del almacén a la pantalla de salida.
      }
    }
    if (!esperarAlServidor) unawaited(revocacion);
  }

  Future<void> _revocar(String acceso) async {
    final cronometro = Stopwatch()..start();
    final api = DefaultApi(_dio);
    for (var intento = 0; intento < 2; intento++) {
      final restante = const Duration(seconds: 5) - cronometro.elapsed;
      if (restante <= Duration.zero) return;
      final tokenCancelacion = CancelToken();
      final plazoIntento = restante < const Duration(milliseconds: 2400)
          ? restante
          : const Duration(milliseconds: 2400);
      try {
        await api
            .cerrarSesion(
              headers: {'Authorization': 'Bearer $acceso'},
              cancelToken: tokenCancelacion,
            )
            .timeout(plazoIntento);
        return;
      } on Object {
        tokenCancelacion.cancel('plazo de cierre agotado');
        if (intento == 1) return;
      }
    }
  }
}

final cierreDeSesionProvider = Provider<CierreDeSesion>(
  (ref) => CierreDeSesion(ref.watch(sesionProvider), ref.watch(dioProvider)),
);
