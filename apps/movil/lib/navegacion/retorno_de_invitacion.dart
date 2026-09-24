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
  if (!RegExp(
    r'^/pasanaku/unirse/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.[0-9a-f]{64}$',
  ).hasMatch(uri.path)) {
    return null;
  }
  return uri.path;
}
