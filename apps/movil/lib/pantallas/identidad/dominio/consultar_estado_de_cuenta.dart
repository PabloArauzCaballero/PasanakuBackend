import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../proveedores/renovar_acceso.dart';
import '../../../proveedores/sesion.dart';
import 'kyc_del_titular.dart';
import 'verificacion_diferida.dart';

/// Lo que encontró «Consultar estado» (2026-10-19).
enum ResultadoDeConsulta {
  /// Ya la aprobaron y el acceso nuevo quedó activo: billetera y grupos habilitados.
  activada,

  /// Ya la aprobaron, pero no se pudo renovar el acceso todavía.
  aprobadaSinActivar,

  /// Una persona todavía la está revisando.
  enRevision,

  /// No mandó el carnet: falta verificarse.
  sinVerificar,

  /// Otro estado de la cuenta (bloqueada, suspendida…).
  otro,
}

/// Pregunta al servidor en qué está la cuenta y, si la identidad ya está aprobada,
/// renueva el acceso en el momento: la persona no tiene que salir y volver a entrar.
/// Los errores de red se propagan para que la pantalla los diga.
final consultarEstadoDeCuentaProvider =
    Provider<Future<ResultadoDeConsulta> Function()>((ref) {
      return () async {
        ref.invalidate(estadoDeCuentaProvider);
        final estado = await ref.read(estadoDeCuentaProvider.future);
        final usuarioId = ref.read(sesionProvider).usuarioIdActual;
        if (usuarioId != null) ref.invalidate(kycDelTitularProvider(usuarioId));
        switch (estado) {
          case EstadoDeCuenta.verificada:
            final listo = await ref.read(renovarAccesoProvider)();
            return listo
                ? ResultadoDeConsulta.activada
                : ResultadoDeConsulta.aprobadaSinActivar;
          case EstadoDeCuenta.enRevision:
            return ResultadoDeConsulta.enRevision;
          case EstadoDeCuenta.sinVerificar:
            return ResultadoDeConsulta.sinVerificar;
          case EstadoDeCuenta.otro:
            return ResultadoDeConsulta.otro;
        }
      };
    });
