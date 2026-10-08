/// El identificador y secreto firmados son un único segmento de ruta. Compartir
/// esta forma evita que el deep link y el retorno tras login acepten variantes
/// distintas del mismo enlace.
final RegExp formatoCodigoInvitacion = RegExp(
  r'^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([0-9a-f]{64})$',
);

/// Solo permite volver a un enlace de invitación interno después del ingreso.
/// Un valor recibido por URI nunca puede redirigir a un sitio externo.
String? retornoDeInvitacion(String? valor) {
  if (valor == null) return null;
  final uri = Uri.tryParse(valor);
  if (uri == null ||
      uri.hasScheme ||
      uri.hasAuthority ||
      uri.hasQuery ||
      uri.hasFragment) {
    return null;
  }
  final segmentos = uri.pathSegments;
  if (segmentos.length != 3 ||
      segmentos[0] != 'pasanaku' ||
      segmentos[1] != 'unirse' ||
      !formatoCodigoInvitacion.hasMatch(segmentos[2])) {
    return null;
  }
  return '/pasanaku/unirse/${segmentos[2]}';
}
