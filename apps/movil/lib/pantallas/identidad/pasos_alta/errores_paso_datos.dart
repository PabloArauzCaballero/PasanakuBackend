import '../dominio/validaciones.dart';

Map<String, String?> erroresPasoDatos({
  required Set<String> tocados,
  required bool intentado,
  required String primerNombre,
  required String segundoNombre,
  required String apellidoPaterno,
  required String apellidoMaterno,
  required String telefono,
  required String documento,
  required String correo,
  required String? lugarExpedicion,
  required DateTime? fechaExpiracion,
  required DateTime? fechaNacimiento,
}) {
  String? error(String campo, String? Function() calcular) =>
      tocados.contains(campo) || intentado ? calcular() : null;

  return {
    'primerNombre': error(
      'primerNombre',
      () => errorNombres(primerNombre, segundoNombre),
    ),
    'segundoNombre': error(
      'segundoNombre',
      () => errorCampoDeNombre(segundoNombre),
    ),
    'apellidoPaterno': error(
      'apellidoPaterno',
      () => errorApellidos(apellidoPaterno, apellidoMaterno),
    ),
    'apellidoMaterno': error(
      'apellidoMaterno',
      () => errorCampoDeNombre(apellidoMaterno),
    ),
    'telefono': error('telefono', () => errorTelefono(telefono)),
    'correo': error('correo', () => errorCorreo(correo)),
    'documento': error('documento', () => errorDocumento(documento)),
    'lugar': error('lugar', () => errorLugarExpedicion(lugarExpedicion)),
    'vence': error('vence', () => errorVencimientoDocumento(fechaExpiracion)),
    'fecha': error('fecha', () => errorFechaNacimiento(fechaNacimiento)),
  };
}
