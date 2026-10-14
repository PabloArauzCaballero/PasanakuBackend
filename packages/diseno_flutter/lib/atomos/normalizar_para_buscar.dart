/// Normaliza texto de búsqueda sin tildes ni diferencias entre mayúsculas.
String normalizarParaBuscar(String texto) {
  const con = 'áéíóúüñÁÉÍÓÚÜÑ';
  const sin = 'aeiouunAEIOUUN';
  final b = StringBuffer();
  for (final c in texto.split('')) {
    final i = con.indexOf(c);
    b.write(i < 0 ? c : sin[i]);
  }
  return b.toString().toLowerCase().trim();
}
