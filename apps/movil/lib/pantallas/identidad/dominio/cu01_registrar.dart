import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-01 · alta de usuario. `POST /usuarios`, en una sola petición: los ocho pasos
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

  Future<AltaCreada> crear({
    required String telefonoE164,
    required String nombres,
    required String apellidos,
    required DateTime fechaNacimiento,
    required String tipoDocumento,
    required String numeroDocumento,
    required String? lugarExpedicion,
    required String correo,
    required String canalVerificacion,
    required List<String> contratosAceptados,
  }) async {
    try {
      final r = await DefaultApi(_ref.read(dioProvider)).registrarUsuario(
        idempotencyKey: _ref
            .read(idempotenciaProvider.notifier)
            .claveDe(formularioId),
        entradaRegistro: EntradaRegistro(
          telefonoE164: telefonoE164,
          nombres: nombres,
          apellidos: apellidos,
          fechaNacimiento: fechaNacimiento,
          correo: correo.isEmpty ? null : correo,
          canalVerificacion: EntradaRegistroCanalVerificacionEnum.values
              .firstWhere((e) => e.value == canalVerificacion),
          documento: Documento(
            tipo: _tipo(tipoDocumento),
            numero: numeroDocumento,
            lugarExpedicion: lugarExpedicion == null
                ? null
                : DocumentoLugarExpedicionEnum.values.firstWhere(
                    (e) => e.name == lugarExpedicion,
                  ),
          ),
          aceptaContratos: contratosAceptados,
        ),
      );
      // El `usuarioId` es lo que permite subir las fotos del expediente: se sacan
      // antes, cuando la persona todavia no existe, y se mandan recien ahora.
      return AltaCreada(
        usuarioId: r.data?.usuarioId.toString(),
        cuentaBilleteraId: r.data?.cuentaBilleteraId?.toString(),
      );
    } on DioException catch (e) {
      throw errorDeDominio(e);
    }
  }

  DocumentoTipoEnum _tipo(String nombre) => switch (nombre.toUpperCase()) {
    'CEX' => DocumentoTipoEnum.CEX,
    'PASAPORTE' => DocumentoTipoEnum.PASAPORTE,
    _ => DocumentoTipoEnum.CI,
  };
}

/// Lo que vuelve del alta. La billetera puede no existir todavia —el alta responde
/// 202 y la abre otro servicio al consumir el evento—, pero el usuario si.
class AltaCreada {
  const AltaCreada({required this.usuarioId, required this.cuentaBilleteraId});
  final String? usuarioId;
  final String? cuentaBilleteraId;
}

final registroProvider = Provider<Registro>(Registro.new);
