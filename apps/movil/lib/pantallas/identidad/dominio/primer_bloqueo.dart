/// Devuelve la primera instrucción pendiente, conservando el orden visual del paso.
/// El primer elemento de cada par indica si ese requisito ya está resuelto.
String? primerBloqueo(List<(bool ok, String motivo)> requisitos) {
  for (final (ok, motivo) in requisitos) {
    if (!ok && motivo.trim().isNotEmpty) return motivo.trim();
  }
  return null;
}
