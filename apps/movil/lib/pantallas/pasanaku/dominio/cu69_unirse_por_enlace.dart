import 'package:dio/dio.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';

/// Las dos mitades de un enlace de invitación `aportaya://unirse/<tokenId>.<token>`.
///
/// El token es un secreto: este tipo nunca lo escribe en un log ni lo devuelve en un texto
/// (regla 90.2.9). [leer] devuelve `null` si el código no tiene la forma exacta
/// (UUID + punto + 64 hexadecimales en minúscula).
class EnlaceDeInvitacion {
  const EnlaceDeInvitacion._(this.tokenId, this.token);

  static final _formato = RegExp(
    r'^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([0-9a-f]{64})$',
  );

  static EnlaceDeInvitacion? leer(String codigo) {
    final partes = _formato.firstMatch(codigo);
    return partes == null
        ? null
        : EnlaceDeInvitacion._(partes.group(1)!, partes.group(2)!);
  }

  final String tokenId;
  final String token;

  /// CU-69 · `POST /grupos/invitaciones/enlace/consultar`: qué grupo es y qué reglamento hay que
  /// aceptar. Solo responde si el teléfono autenticado es el destinatario de la invitación.
  Future<Map<String, dynamic>?> consultar(Dio dio) async {
    final r = await dio.post<Map<String, dynamic>>(
      '/grupos/invitaciones/enlace/consultar',
      data: {'tokenId': tokenId, 'token': token},
    );
    return r.data;
  }

  /// CU-69 · `POST /grupos/invitaciones/enlace/aceptar`. Devuelve el `grupoId` del ingreso.
  /// La clave de idempotencia la pone quien llama: el mismo formulario reintentado repite la clave.
  Future<String?> aceptar(
    Dio dio, {
    required Object? hashReglamento,
    required String claveDeIdempotencia,
  }) async {
    final r = await dio.post<Map<String, dynamic>>(
      '/grupos/invitaciones/enlace/aceptar',
      data: {
        'tokenId': tokenId,
        'token': token,
        'hashReglamento': hashReglamento,
        'aceptaReglamento': true,
      },
      options: Options(headers: {'Idempotency-Key': claveDeIdempotencia}),
    );
    return r.data?['grupoId'] as String?;
  }
}

/// Si el servidor rechazó con un código que la app sabe explicar (enlace revocado, vencido, ya
/// usado, ingreso con revisión humana…), se dice eso; si no (sin red, falla), el texto [generico].
String textoDelRechazoDeInvitacion(Object e, String generico) {
  final motivo = errorDeDominio(e);
  if (motivo is ErrorDeApi && tieneMensajePropio(motivo.codigo)) {
    return motivo.mensaje;
  }
  return generico;
}
