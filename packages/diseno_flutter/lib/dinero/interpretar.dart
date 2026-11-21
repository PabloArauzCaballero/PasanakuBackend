/// Lo que una persona escribe en un campo de importe → la cadena del contrato.
///
/// Reproduce, caso por caso, la referencia `packages/tokens/dinero/interpretar.ts` y sus
/// vectores `packages/tokens/vectores/monto-escrito.json` (F1.S4.M1 · L6 DINERO-002).
///
/// Regla (formato regional es-BO: miles con punto, centavos con coma):
/// - con coma, la coma es la decimal: «1.250,50» → `1250.50`;
/// - sin coma y con un solo punto seguido de 0 a 2 dígitos al final, ese punto es la
///   decimal (el teclado del teléfono muchas veces solo tiene punto): «12.50» → `12.50`;
/// - sin coma y con puntos en grupos de tres, son miles: «1.240» → `1240.00`;
/// - todo lo demás es ambiguo y devuelve `null`: «1.240.50», «1,250.50», «-5».
///
/// Nunca pasa por `double`: enteros y centavos se separan como texto.
library;

final _entero = RegExp(r'^(\d{1,3}(\.\d{3})+|\d+)$');
final _soloCifras = RegExp(r'^[\d.,]+$');
final _puntoDecimal = RegExp(r'^\d+\.\d{0,2}$');
final _centavos = RegExp(r'^\d{0,2}$');

String? interpretarMontoEscrito(String escrito) {
  final texto = escrito.replaceAll(RegExp(r'\s'), '');
  if (!_soloCifras.hasMatch(texto)) {
    return null;
  }

  final String entero;
  final String centavos;
  final partesPorComa = texto.split(',');
  if (partesPorComa.length > 2) {
    return null;
  }

  if (partesPorComa.length == 2) {
    if (!_entero.hasMatch(partesPorComa[0]) ||
        !_centavos.hasMatch(partesPorComa[1])) {
      return null;
    }
    entero = partesPorComa[0].replaceAll('.', '');
    centavos = partesPorComa[1];
  } else if (_puntoDecimal.hasMatch(texto)) {
    final partes = texto.split('.');
    entero = partes[0];
    centavos = partes[1];
  } else if (_entero.hasMatch(texto)) {
    entero = texto.replaceAll('.', '');
    centavos = '';
  } else {
    return null;
  }

  final sinCerosDelante = entero.replaceFirst(RegExp(r'^0+(?=\d)'), '');
  return '$sinCerosDelante.${centavos.padRight(2, '0')}';
}
