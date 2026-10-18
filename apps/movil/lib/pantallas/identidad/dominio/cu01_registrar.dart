import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-01 · alta de usuario. `POST /usuarios`, en una sola petición: los datos reunidos en el asistente
/// del formulario juntan datos y recién al final —con los contratos aceptados— se
/// crea la persona. Antes de eso no existe nada del lado del servidor, que es lo que
/// permite abandonar el alta a mitad sin dejar una cuenta a medio hacer.
class Registro {
  Registro(this._ref);
  final Ref _ref;

  /// Devuelve el id de la cuenta de billetera que el backend abre junto al usuario,
  /// o `null` si el alta quedó pendiente de verificación y todavía no hay billetera.
  /// La clave de idempotencia vive por «formulario de alta», no por intento: si el
  /// envío se corta y la persona vuelve a tocar, el servidor reconoce que es el mismo
  /// alta y no crea dos personas con el mismo documento.
  static const formularioId = 'cu01-alta';

  /// Sin documento (`tipoDocumento == null`) la cuenta se crea sin verificar: la
  /// persona saltó ese paso y se verifica después, cuando quiere operar con dinero.
  ///
  /// Va por Dio directo y no por el cliente generado: ese cliente todavía exige el
  /// documento y no conoce el estado `INCOMPLETO_KYC` (contrato del 2026-10-17).
  Future<AltaCreada> crear({
    required String telefonoE164,
    required String nombres,
    required String apellidos,
    required DateTime fechaNacimiento,
    required String? tipoDocumento,
    required String numeroDocumento,
    required String? lugarExpedicion,
    required DateTime? fechaExpiracion,
    required String correo,
    required String canalVerificacion,
    required String? verificacionCorreoId,
    required String contrasena,
    required List<String> contratosAceptados,
  }) async {
    if (verificacionCorreoId == null) {
      throw const VerificacionCorreoFaltante();
    }
    try {
      final r = await _ref
          .read(dioProvider)
          .post<Map<String, dynamic>>(
            '/usuarios',
            options: Options(
              contentType: Headers.jsonContentType,
              headers: {
                'Idempotency-Key': _ref
                    .read(idempotenciaProvider.notifier)
                    .claveDe(formularioId),
              },
            ),
            // Serializado aca, como lo hacia el cliente generado.
            data: jsonEncode({
              'telefonoE164': telefonoE164,
              'nombres': nombres,
              'apellidos': apellidos,
              'fechaNacimiento': fechaIso(fechaNacimiento),
              'correo': correo,
              'verificacionCorreoId': verificacionCorreoId,
              'canalVerificacion': canalVerificacion,
              if (tipoDocumento != null)
                'documento': documentoComoJson(
                  tipo: tipoDocumento,
                  numero: numeroDocumento,
                  lugarExpedicion: lugarExpedicion,
                  fechaExpiracion: fechaExpiracion,
                ),
              'contrasena': contrasena,
              'aceptaContratos': contratosAceptados,
            }),
          );
      final cuerpo = r.data ?? const <String, dynamic>{};
      // El `usuarioId` es lo que permite subir las fotos del expediente: se sacan
      // antes, cuando la persona todavia no existe, y se mandan recien ahora.
      return AltaCreada(
        usuarioId: cuerpo['usuarioId'] as String?,
        cuentaBilleteraId: cuerpo['cuentaBilleteraId'] as String?,
        tokenDeAlta: cuerpo['tokenDeAlta'] as String?,
      );
    } on DioException catch (e) {
      throw errorDeDominio(e);
    }
  }
}

/// `2026-10-17`: el formato `date` del contrato, sin hora ni zona.
String fechaIso(DateTime f) =>
    '${f.year.toString().padLeft(4, '0')}-${f.month.toString().padLeft(2, '0')}-${f.day.toString().padLeft(2, '0')}';

/// El `Documento` del contrato. Lo comparten el alta y la verificación diferida.
Map<String, Object?> documentoComoJson({
  required String tipo,
  required String numero,
  required String? lugarExpedicion,
  required DateTime? fechaExpiracion,
}) => {
  'tipo': switch (tipo.toUpperCase()) {
    'CEX' => 'CEX',
    'PASAPORTE' => 'PASAPORTE',
    _ => 'CI',
  },
  'numero': numero,
  'lugarExpedicion': ?lugarExpedicion,
  if (fechaExpiracion != null) 'fechaExpiracion': fechaIso(fechaExpiracion),
};

/// Lo que vuelve del alta. La billetera puede no existir todavia —el alta responde
/// 202 y la abre otro servicio al consumir el evento—, pero el usuario si.
class AltaCreada {
  const AltaCreada({
    required this.usuarioId,
    required this.cuentaBilleteraId,
    this.tokenDeAlta,
  });
  final String? usuarioId;
  final String? cuentaBilleteraId;

  /// El permiso de UN solo proposito para subir las fotos de ESTE usuario: la subida ya
  /// no se acepta con solo conocer el `usuarioId` (cabecera `Token-De-Alta`).
  final String? tokenDeAlta;
}

final registroProvider = Provider<Registro>(Registro.new);

class VerificacionCorreoFaltante implements Exception {
  const VerificacionCorreoFaltante();

  @override
  String toString() =>
      'Confirma el codigo de tu correo antes de crear la cuenta.';
}
