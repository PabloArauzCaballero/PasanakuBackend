bool llegarA({
  required String? ruta,
  required String actual,
  required void Function(String ruta)? navegar,
  required void Function(String problema) alFallar,
}) {
  if (ruta == null) return true;
  if (actual == ruta || actual.startsWith('$ruta/')) return true;
  if (navegar == null) {
    alFallar('No pudimos abrir «$ruta» desde acá.');
    return false;
  }
  navegar(ruta);
  return true;
}
