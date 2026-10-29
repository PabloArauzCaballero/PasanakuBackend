import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import '../../comun.dart';
import 'inventario.dart';

/// Toda ruta declarada en `lib/**/rutas*.dart` (y las de `acceso_a_rutas.dart`) tiene su
/// captura o una exclusión con motivo: una pantalla nueva no se escapa de la evidencia.
void main() {
  test('el inventario de capturas cubre todas las rutas registradas', () {
    final lib = Directory('${raizDelRepositorio()}/apps/movil/lib');
    final declaradas = <String>{};
    final constantes = <String, String>{};
    final archivos = lib.listSync(recursive: true).whereType<File>().toList();
    for (final f in archivos) {
      final texto = f.readAsStringSync();
      for (final m in RegExp(r"const (\w+) = '(/[^']*)'").allMatches(texto)) {
        constantes[m.group(1)!] = m.group(2)!;
      }
    }
    for (final f in archivos) {
      final texto = f.readAsStringSync();
      if (!f.path.contains('rutas')) continue;
      for (final m in RegExp(r"path:\s*'([^']+)'").allMatches(texto)) {
        declaradas.add(m.group(1)!);
      }
      for (final m in RegExp(r'path:\s*(ruta\w+)').allMatches(texto)) {
        declaradas.add(constantes[m.group(1)!] ?? m.group(1)!);
      }
    }
    final cubiertas = {
      ...pantallasDeCaptura.map((p) => p.declarada),
      ...rutasExcluidas.keys,
    };
    expect(declaradas.difference(cubiertas), isEmpty);
    expect(cubiertas.difference(declaradas), isEmpty);
  });
}
