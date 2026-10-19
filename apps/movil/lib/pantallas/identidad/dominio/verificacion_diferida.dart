import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/sesion.dart';
import 'cu01_registrar.dart' show documentoComoJson;

/// Verificación de identidad diferida (2026-10-17): la cuenta se puede crear sin
/// carnet, y la verificación se pide cuando la persona quiere mover dinero o entrar a
/// un pasanaku. Ni la operación nueva ni `estadoDeCuenta` están todavía en el cliente
/// Dart generado: se llaman con el mismo `Dio` de la app.
enum EstadoDeCuenta {
  /// Creó la cuenta sin carnet: todavía no envió nada.
  sinVerificar,

  /// Envió su carnet y una persona lo está revisando.
  enRevision,

  /// Verificada: opera.
  verificada,

  /// Cualquier otro estado (bloqueada, suspendida…): la app no ofrece verificar.
  otro;

  static EstadoDeCuenta desde(String? valor) => switch (valor) {
    'INCOMPLETO_KYC' => sinVerificar,
    'PENDIENTE_VERIFICACION' => enRevision,
    'ACTIVO' => verificada,
    _ => otro,
  };
}

/// El estado de la cuenta de la sesión (`GET /usuarios/yo/perfil`).
final estadoDeCuentaProvider = FutureProvider.autoDispose<EstadoDeCuenta>((
  ref,
) async {
  ref.watch(generacionDeSesionProvider);
  try {
    final r = await ref
        .read(dioProvider)
        .get<Map<String, dynamic>>('/usuarios/yo/perfil');
    return EstadoDeCuenta.desde(r.data?['estadoDeCuenta'] as String?);
  } catch (e) {
    throw errorDeDominio(e);
  }
});

/// `POST /usuarios/{usuarioId}/verificacion`: envía el carnet y abre la verificación.
/// Si ya estaba abierta (`AP-CU01-12`, por ejemplo un reintento después de un corte),
/// se sigue igual: lo que falta son las fotos.
class IniciarVerificacion {
  IniciarVerificacion(this._ref);
  final Ref _ref;

  static const yaIniciada = 'AP-CU01-12';

  Future<void> ejecutar({
    required String usuarioId,
    required String tipo,
    required String numero,
    required String? lugarExpedicion,
    required DateTime? fechaExpiracion,
  }) async {
    try {
      await _ref
          .read(dioProvider)
          .post<Map<String, dynamic>>(
            '/usuarios/$usuarioId/verificacion',
            data: documentoComoJson(
              tipo: tipo,
              numero: numero,
              lugarExpedicion: lugarExpedicion,
              fechaExpiracion: fechaExpiracion,
            ),
          );
    } on DioException catch (e) {
      final error = errorDeDominio(e);
      if (error is ErrorDeApi && error.codigo == yaIniciada) return;
      throw error;
    }
  }
}

final iniciarVerificacionProvider = Provider<IniciarVerificacion>(
  IniciarVerificacion.new,
);
