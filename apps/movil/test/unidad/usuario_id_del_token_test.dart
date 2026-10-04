import 'dart:convert';

import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_test/flutter_test.dart';

String _jwt(Map<String, Object?> carga) {
  String b64(Object o) => base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${b64({'alg': 'none'})}.${b64(carga)}.firma';
}

void main() {
  test('saca el sub del token de acceso', () {
    expect(usuarioIdDelToken(_jwt({'sub': 'u-123', 'rol': 'PARTICIPANTE'})), 'u-123');
  });

  test('sin token, o con un token que no es un JWT, no hay usuario', () {
    expect(usuarioIdDelToken(null), isNull);
    expect(usuarioIdDelToken(''), isNull);
    expect(usuarioIdDelToken('no-es-un-jwt'), isNull);
    expect(usuarioIdDelToken('a.%%%.c'), isNull);
  });

  test('un token sin sub, o con sub vacío o que no es texto, no hay usuario', () {
    expect(usuarioIdDelToken(_jwt({'rol': 'X'})), isNull);
    expect(usuarioIdDelToken(_jwt({'sub': ''})), isNull);
    expect(usuarioIdDelToken(_jwt({'sub': 7})), isNull);
  });
}
