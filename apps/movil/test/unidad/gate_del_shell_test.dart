import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Automatiza dos líneas del gate propio del carril F2 para que dejen de depender
/// de correrlas a mano antes de cada PR.
void main() {
  final lib = Directory('${raizDelRepositorio()}/apps/movil/lib');

  test('el paquete shared_preferences no se usa (ni se importa) en lib/', () {
    // Se busca el *uso real* — import o construcción de la clase — no la palabra
    // suelta: `sesion.dart` la nombra a propósito en un comentario para decir
    // justamente que no se usa.
    final ofensores = <String>[];
    final usoReal = RegExp(
      r"import\s+'package:shared_preferences|\bSharedPreferences\s*\(",
    );
    for (final f in lib.listSync(recursive: true).whereType<File>()) {
      if (!f.path.endsWith('.dart')) continue;
      if (usoReal.hasMatch(f.readAsStringSync())) {
        ofensores.add(f.path);
      }
    }
    expect(
      ofensores,
      isEmpty,
      reason:
          'Nada sensible va en SharedPreferences (flutter_secure_storage sí): '
          '$ofensores',
    );
  });

  test('Platform.is solo aparece dentro de infraestructura/', () {
    final ofensores = <String>[];
    for (final f in lib.listSync(recursive: true).whereType<File>()) {
      if (!f.path.endsWith('.dart')) continue;
      if (f.path.contains('/infraestructura/')) continue;
      if (RegExp(r'Platform\.is').hasMatch(f.readAsStringSync())) {
        ofensores.add(f.path);
      }
    }
    expect(
      ofensores,
      isEmpty,
      reason: 'Platform.is* solo en infraestructura/ (ADR-036): $ofensores',
    );
  });
}
