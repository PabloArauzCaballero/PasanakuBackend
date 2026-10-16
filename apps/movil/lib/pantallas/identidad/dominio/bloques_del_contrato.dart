/// El contrato partido en bloques para poder leerlo como un documento.
///
/// El texto legal vive entero en `texto_del_contrato.dart` y **no se toca acá**:
/// esto solo reconoce su estructura —el título, las cláusulas «PRIMERA — PARTES», los
/// incisos «a)», las viñetas «·», las rayas que separan el tarifario y el pie— para
/// pintarla con jerarquía. Antes se mostraba como un único `Text` con encabezados en
/// mayúsculas y rayas de caracteres, y leerlo en un celular era leer un volcado.
///
/// Ninguna palabra se agrega ni se quita: `bloques_del_contrato_test.dart` lo
/// comprueba reconstruyendo el texto desde los bloques.
library;

enum TipoDeBloque { titulo, seccion, parrafo, introduccion, item, pie }

class BloqueDelContrato {
  const BloqueDelContrato(
    this.tipo,
    this.texto, {
    this.ordinal,
    this.marca,
    this.enTarifario = false,
  });

  final TipoDeBloque tipo;

  /// El texto tal como está en el contrato (sin la marca del inciso).
  final String texto;

  /// «PRIMERA», «DÉCIMA SEGUNDA»… Solo en las cláusulas numeradas.
  final String? ordinal;

  /// «a)» o «·». Solo en los incisos.
  final String? marca;

  /// El tarifario va en un bloque propio: es lo que la persona vuelve a buscar.
  final bool enTarifario;
}

final _clausula = RegExp(r'^([A-ZÁÉÍÓÚÑ ]+?) — ([A-ZÁÉÍÓÚÑ ,]+)$');
final _encabezado = RegExp(r'^[A-ZÁÉÍÓÚÑ ]{4,}$');
final _inciso = RegExp(r'^([a-z]\)|·) (.+)$');
final _raya = RegExp(r'^─+$');

List<BloqueDelContrato> bloquesDelContrato(String texto) {
  final lineas = texto
      .split('\n')
      .map((l) => l.trim())
      .where((l) => l.isNotEmpty);
  final bloques = <BloqueDelContrato>[];
  var rayas = 0;
  var tarifario = false;
  for (final linea in lineas) {
    if (_raya.hasMatch(linea)) {
      // Primera raya: empieza el tarifario. Segunda: termina y empieza el pie.
      rayas++;
      tarifario = false;
      continue;
    }
    if (bloques.isEmpty) {
      bloques.add(BloqueDelContrato(TipoDeBloque.titulo, linea));
      continue;
    }
    if (rayas >= 2) {
      bloques.add(BloqueDelContrato(TipoDeBloque.pie, linea));
      continue;
    }
    final clausula = _clausula.firstMatch(linea);
    if (clausula != null) {
      bloques.add(
        BloqueDelContrato(
          TipoDeBloque.seccion,
          clausula.group(2)!,
          ordinal: clausula.group(1),
        ),
      );
      continue;
    }
    if (_encabezado.hasMatch(linea)) {
      tarifario = rayas == 1;
      bloques.add(
        BloqueDelContrato(TipoDeBloque.seccion, linea, enTarifario: tarifario),
      );
      continue;
    }
    final inciso = _inciso.firstMatch(linea);
    if (inciso != null) {
      bloques.add(
        BloqueDelContrato(
          TipoDeBloque.item,
          inciso.group(2)!,
          marca: inciso.group(1),
          enTarifario: tarifario,
        ),
      );
      continue;
    }
    bloques.add(
      BloqueDelContrato(
        linea.endsWith(':') ? TipoDeBloque.introduccion : TipoDeBloque.parrafo,
        linea,
        enTarifario: tarifario,
      ),
    );
  }
  return bloques;
}

/// «OBLIGACIONES DE APORTAYA» → «Obligaciones de AportaYa». Solo cambia mayúsculas:
/// la marca y las siglas conservan su forma.
String enTipoOracion(String encabezado) {
  const propios = {'aportaya': 'AportaYa', 'asfi': 'ASFI', 'uif': 'UIF'};
  final palabras = encabezado.toLowerCase().split(' ');
  return [
    for (var i = 0; i < palabras.length; i++)
      propios[palabras[i]] ??
          (i == 0 && palabras[i].isNotEmpty
              ? palabras[i][0].toUpperCase() + palabras[i].substring(1)
              : palabras[i]),
  ].join(' ');
}
