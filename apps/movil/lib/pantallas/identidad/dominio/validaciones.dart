/// Validación **de ayuda**, no de garantía (invariante 7 — el servidor protege).
/// Cada función devuelve el texto de error en voz de marca o `null`. Ninguna regla
/// de negocio (listas restrictivas, duplicados, licencia) se decide acá: eso lo
/// resuelve `identidad.yaml` y su implementación en `servicios/identidad`.
library;

String? errorNombre(String valor) {
  final v = valor.trim();
  if (v.length < 2) return 'Escribí al menos dos letras.';
  if (v.length > 60) return 'Es demasiado largo.';
  return null;
}

/// Un campo de nombre opcional: vacío es válido; su obligatoriedad se decide por
/// el rol del campo y la validación combinada del formulario.
String? errorCampoDeNombre(String valor) {
  final v = valor.trim().replaceAll(RegExp(r'\s+'), ' ');
  if (v.isEmpty) return null;
  if (v.length > 30) return 'No puede pasar de 30 letras.';
  if (!RegExp(r"^[\p{L}]+(?:[ '-][\p{L}]+)*$", unicode: true).hasMatch(v)) {
    return 'Usá solo letras, apóstrofo o guion.';
  }
  return null;
}

String? errorNombres(String primerNombre, String segundoNombre) {
  if (primerNombre.trim().isEmpty) return 'Escribí tu primer nombre.';
  final error = errorCampoDeNombre(primerNombre);
  if (error != null) return error;
  final total = [primerNombre, segundoNombre]
      .where((parte) => parte.trim().isNotEmpty)
      .map((parte) => parte.trim().replaceAll(RegExp(r'\s+'), ' '))
      .join(' ')
      .length;
  if (total < 2) return 'Escribí tu primer nombre.';
  if (total > 60) {
    return 'Entre los dos nombres no pueden pasar de 60 letras.';
  }
  return null;
}

String? errorApellidos(String paterno, String materno) {
  if (paterno.trim().isEmpty && materno.trim().isEmpty) {
    return 'Escribí al menos un apellido.';
  }
  for (final valor in [paterno, materno]) {
    final error = errorCampoDeNombre(valor);
    if (error != null) return error;
  }
  final total = [paterno, materno]
      .where((parte) => parte.trim().isNotEmpty)
      .map((parte) => parte.trim().replaceAll(RegExp(r'\s+'), ' '))
      .join(' ')
      .length;
  if (total < 2) return 'Escribí al menos un apellido.';
  if (total > 60) return 'Los apellidos juntos no pueden pasar de 60 letras.';
  return null;
}

String? errorTelefono(String valor) {
  final v = valor.trim();
  if (!RegExp(r'^\+591\d{8}$').hasMatch(v)) {
    return 'Un celular boliviano se escribe +591 y ocho dígitos.';
  }
  return null;
}

String? errorFechaNacimiento(DateTime? fecha) {
  if (fecha == null) return 'Elegí una fecha.';
  final edad = DateTime.now().difference(fecha).inDays / 365.25;
  if (edad < 18) return 'Hay que ser mayor de edad para abrir cuenta.';
  return null;
}

/// El vencimiento del carnet. Hoy todavia vale: el documento vence al terminar ese dia.
/// Es ayuda: el servidor lo vuelve a comprobar con su propio reloj (AP-CU01-07).
String? errorVencimientoDocumento(DateTime? fecha, {DateTime? hoy}) {
  if (fecha == null) return 'Elegí hasta cuándo vale tu carnet.';
  final h = hoy ?? DateTime.now();
  final dia = DateTime(h.year, h.month, h.day);
  if (DateTime(fecha.year, fecha.month, fecha.day).isBefore(dia)) {
    return 'Tu carnet está vencido. Para abrir la cuenta hace falta uno vigente.';
  }
  return null;
}

String? errorDocumento(String valor) {
  final numero = valor.trim();
  if (!RegExp(r'^\d{5,10}$').hasMatch(numero)) {
    return 'Escribí un número de CI de entre 5 y 10 dígitos.';
  }
  return null;
}

String? errorContrasena(String valor) {
  if (valor.length < 8) return 'Ocho caracteres como mínimo.';
  if (!RegExp(r'[0-9]').hasMatch(valor)) return 'Sumale al menos un número.';
  return null;
}

double fortalezaContrasena(String valor) {
  var puntos = 0;
  if (valor.length >= 8) puntos += 1;
  if (valor.length >= 12) puntos += 1;
  if (RegExp(r'[0-9]').hasMatch(valor)) puntos += 1;
  if (RegExp(r'[A-Z]').hasMatch(valor)) puntos += 1;
  if (RegExp(r'[^A-Za-z0-9]').hasMatch(valor)) puntos += 1;
  return puntos / 5;
}

/// El número de CI boliviano NO es único: se repite entre departamentos. Lo que lo
/// desambigua es la extensión, y sin ella el servidor no puede distinguir a dos
/// personas distintas con el mismo número.
String? errorLugarExpedicion(String? valor) {
  if (valor == null || valor.isEmpty) {
    return 'Elegí dónde te expidieron el carnet.';
  }
  return null;
}

/// Validación de ayuda, no de garantía: un correo solo se comprueba mandándole algo.
/// Acá se atajan los errores de tipeo, que son casi todos.
String? errorCorreo(String valor) {
  final v = valor.trim();
  if (v.isEmpty) return 'Escribí tu correo.';
  if (!RegExp(r'^[^@\s]+@[^@\s.]+\.[^@\s]+$').hasMatch(v)) {
    return 'Revisá el correo: le falta el arroba o el punto.';
  }
  return null;
}
