/// Las cuentas de la etapa, **en enteros** (regla 91.1.1): el contrato da el puntaje,
/// los umbrales y el avance como cadenas («575.00», «0.5000») justamente para que
/// nadie los pase por coma flotante. Acá se convierten a centésimos y diezmilésimos y
/// se restan como enteros; el único `double` es el que la barra divide para dibujar.
library;

final RegExp _avance = RegExp(r'^(0\.[0-9]{4}|1\.0000)$');
final RegExp _decimal = RegExp(r'^(-?)([0-9]+)(?:\.([0-9]{1,2}))?$');

/// `'0.5000'` → `5000`. `null` si llega algo fuera del contrato: la barra no se
/// inventa un avance.
int? diezmilesimosDe(String progreso) {
  if (!_avance.hasMatch(progreso)) return null;
  return int.parse(progreso.replaceFirst('.', ''));
}

/// `'575.00'` → `57500`; también acepta `'575'` y `'575.5'`. `null` si no es un número.
int? centesimosDe(String valor) {
  final m = _decimal.firstMatch(valor.trim());
  if (m == null) return null;
  final enteros = int.parse(m.group(2)!);
  final decimales = int.parse((m.group(3) ?? '').padRight(2, '0'));
  final absoluto = enteros * 100 + decimales;
  return m.group(1) == '-' ? -absoluto : absoluto;
}

/// `60000` → `'600'`; `60050` → `'600,50'`. Los puntos se leen sin decimales cuando
/// no los tienen, que es casi siempre.
String puntosLegibles(int centesimos) {
  final negativo = centesimos < 0;
  final absoluto = centesimos.abs();
  final resto = absoluto % 100;
  final cuerpo = resto == 0
      ? '${absoluto ~/ 100}'
      : '${absoluto ~/ 100},${resto.toString().padLeft(2, '0')}';
  return negativo ? '-$cuerpo' : cuerpo;
}

/// El umbral de una etapa para mostrar: `'600.00'` → `'600'`. Si el contrato trae algo
/// raro, se muestra tal cual en vez de esconderlo.
String umbralLegible(String desde) {
  final c = centesimosDe(desde);
  return c == null ? desde : puntosLegibles(c);
}

/// Cuántos puntos faltan para la etapa de arriba, en centésimos. `null` si alguno de
/// los dos no se puede leer. Nunca negativo.
int? centesimosQueFaltan({required String puntaje, required String desde}) {
  final actual = centesimosDe(puntaje);
  final umbral = centesimosDe(desde);
  if (actual == null || umbral == null) return null;
  final falta = umbral - actual;
  return falta < 0 ? 0 : falta;
}
