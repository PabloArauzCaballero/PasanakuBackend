import 'package:aportaya_movil/pantallas/identidad/dominio/bloques_del_contrato.dart';
import 'package:aportaya_movil/pantallas/identidad/texto_del_contrato.dart';
import 'package:flutter_test/flutter_test.dart';

/// Partir el contrato en bloques no puede cambiar el contrato: lo que la persona
/// acepta es el texto, no su presentación.
void main() {
  final bloques = bloquesDelContrato(textoDelContrato);

  List<String> palabras(String s) => s
      .replaceAll(RegExp('─+'), ' ')
      .toLowerCase()
      .split(RegExp(r'\s+'))
      .where((p) => p.isNotEmpty)
      .toList();

  test('ninguna palabra se agrega ni se pierde', () {
    final reconstruido = [
      for (final b in bloques)
        [
          if (b.ordinal != null) '${b.ordinal} —',
          if (b.marca != null) b.marca!,
          b.texto,
        ].join(' '),
    ].join('\n');
    expect(palabras(reconstruido), palabras(textoDelContrato));
  });

  test('las veintitrés cláusulas quedan como sección con su ordinal', () {
    final clausulas = bloques
        .where((b) => b.tipo == TipoDeBloque.seccion && b.ordinal != null)
        .toList();
    expect(clausulas, hasLength(23));
    expect(clausulas.first.ordinal, 'PRIMERA');
    expect(clausulas.first.texto, 'PARTES');
    expect(clausulas.last.ordinal, 'VIGÉSIMA TERCERA');
  });

  test('el tarifario y el pie quedan marcados aparte', () {
    expect(bloques.first.tipo, TipoDeBloque.titulo);
    expect(
      bloques.where((b) => b.enTarifario && b.tipo == TipoDeBloque.item),
      hasLength(11),
    );
    expect(
      bloques.where((b) => b.tipo == TipoDeBloque.pie).first.texto,
      startsWith('Versión del contrato: CTO-BILLETERA v1'),
    );
  });

  test('los incisos conservan su marca', () {
    final incisos = bloques.where((b) => b.tipo == TipoDeBloque.item);
    expect(incisos.map((b) => b.marca).toSet(), containsAll(['a)', '·']));
  });

  test('tipo oración respeta la marca', () {
    expect(
      enTipoOracion('OBLIGACIONES DE APORTAYA'),
      'Obligaciones de AportaYa',
    );
    expect(enTipoOracion('PARTES'), 'Partes');
  });
}
