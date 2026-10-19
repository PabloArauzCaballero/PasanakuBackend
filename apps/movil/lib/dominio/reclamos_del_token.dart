import 'dart:convert';

/// El `sub` de un JWT, o `null` si no tiene forma utilizable. No verifica la firma.
String? usuarioIdDelToken(String? token) {
  final carga = _cargaDelToken(token);
  final sub = carga?['sub'];
  return sub is String && sub.isNotEmpty ? sub : null;
}

/// Orienta la navegación; cada operación vuelve a autorizarse en el backend.
bool? billeteraPermitidaDelToken(String? token) {
  final permisos = _cargaDelToken(token)?['permisos'];
  if (permisos is! List) return null;
  return permisos.whereType<String>().contains('BILLETERA_VER');
}

Map<String, dynamic>? _cargaDelToken(String? token) {
  if (token == null) return null;
  final partes = token.split('.');
  if (partes.length != 3) return null;
  try {
    final carga = jsonDecode(
      utf8.decode(base64Url.decode(base64Url.normalize(partes[1]))),
    );
    return carga is Map<String, dynamic> ? carga : null;
  } on FormatException {
    return null;
  }
}
