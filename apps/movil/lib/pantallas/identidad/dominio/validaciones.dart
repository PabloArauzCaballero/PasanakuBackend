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

String? errorDocumento(String valor) {
  if (valor.trim().length < 5) return 'Revisá el número de documento.';
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
