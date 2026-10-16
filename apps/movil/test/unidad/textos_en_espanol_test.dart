import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

final _palabrasProhibidas = RegExp(
  r'\b(?:e-?mail|login|log\s+in|sign\s*up|selfie|scan|upload|submit|next|back|continue|password|phone|retry|cancel|done|ok|loading|error|front|reverse|liveness)\b',
  caseSensitive: false,
);

// En archivos de textos, todas las cadenas son copy salvo los códigos de contrato
// en mayúsculas seguidos de `=>` (p. ej. `SELFIE => 'De frente'`).
final _cadenas = RegExp(r"'((?:\\.|[^'\\])*)'");
final _textoLiteral = RegExp(
  r"\bText\s*\(\s*(?:const\s+)?'((?:\\.|[^'\\])*)'",
  dotAll: true,
);
final _codigoDeContrato = RegExp(r"^\s*'[A-Z][A-Z0-9_]*'\s*=>");
final _comentarios = RegExp(r'//[^\r\n]*|/\*[\s\S]*?\*/');

void main() {
  test('el copy del alta y del ingreso no expone palabras en inglés', () {
    final raiz = Directory('lib');
    expect(raiz.existsSync(), isTrue, reason: 'Ejecutar desde apps/movil.');

    final textos = raiz.listSync(recursive: true).whereType<File>().where((
      archivo,
    ) {
      final nombre = archivo.uri.pathSegments.last.toLowerCase();
      return nombre.startsWith('textos') && nombre.endsWith('.dart');
    });
    final pantallas = Directory('lib/pantallas')
        .listSync(recursive: true)
        .whereType<File>()
        .where((archivo) => archivo.path.endsWith('.dart'));

    final hallazgos = <String>[];
    for (final archivo in textos) {
      final fuente = archivo.readAsStringSync().replaceAll(_comentarios, '');
      for (final linea in fuente.split(RegExp(r'\r?\n'))) {
        if (_codigoDeContrato.hasMatch(linea)) continue;
        for (final coincidencia in _cadenas.allMatches(linea)) {
          _registrar(hallazgos, archivo, coincidencia.group(1)!);
        }
      }
    }
    for (final archivo in pantallas) {
      final fuente = archivo.readAsStringSync().replaceAll(_comentarios, '');
      for (final coincidencia in _textoLiteral.allMatches(fuente)) {
        _registrar(hallazgos, archivo, coincidencia.group(1)!);
      }
    }

    expect(hallazgos, isEmpty, reason: hallazgos.join('\n'));
  });
}

void _registrar(List<String> hallazgos, File archivo, String cadena) {
  final coincidencia = _palabrasProhibidas.firstMatch(cadena);
  if (coincidencia == null) return;
  hallazgos.add(
    '${archivo.path}: «$cadena» contiene «${coincidencia.group(0)}»',
  );
}
