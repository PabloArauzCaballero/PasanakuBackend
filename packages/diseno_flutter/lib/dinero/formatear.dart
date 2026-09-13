/// El formateo de importes, en un solo lugar de la app.
///
/// Invariante 5: ningún importe se formatea a mano. `NumberFormat` y
/// `toStringAsFixed` están prohibidos fuera de este archivo y del átomo `Monto`.
/// Es puro trabajo de cadenas: el contrato define el importe como **cadena**
/// (`^-?\d+\.\d{2}$`) y pasar por `double` reintroduce el error que el contrato evita.
/// Pasa los mismos vectores que la referencia TypeScript (`packages/tokens/vectores`).
library;

final RegExp _formaDelContrato = RegExp(r'^-?\d+\.\d{2}$');

const Map<String, String> _prefijo = {'BOB': 'Bs', 'USD': 'USD'};

String prefijoDe(String moneda) => _prefijo[moneda] ?? moneda;

String _agruparMiles(String enteros) {
  final salida = StringBuffer();
  for (var i = 0; i < enteros.length; i += 1) {
    final desdeLaDerecha = enteros.length - i;
    if (i > 0 && desdeLaDerecha % 3 == 0) salida.write('.');
    salida.write(enteros[i]);
  }
  return salida.toString();
}

/// `('200.00', 10)` → `'2000.00'`: el importe por la cantidad de veces, **en
/// centavos enteros**, sin pasar por `double` ni una sola vez.
///
/// Sirve para adelantarle a la persona la consecuencia de lo que está escribiendo
/// («con 10 personas, cada turno junta…»). Un `200.00 * 10` en coma flotante da
/// `2000.0000000000002`, y una billetera que muestra eso pierde la confianza que
/// tarda meses en ganar. Devuelve la forma del contrato, lista para [formatearMonto].
String multiplicarMonto({required String monto, required int veces}) {
  if (!_formaDelContrato.hasMatch(monto)) {
    throw FormatException('Importe fuera del contrato: "$monto".');
  }
  if (veces < 0) throw ArgumentError.value(veces, 'veces', 'no puede ser negativo');
  final negativo = monto.startsWith('-');
  final sinSigno = negativo ? monto.substring(1) : monto;
  final centavos = int.parse(sinSigno.replaceFirst('.', '')) * veces;
  final enteros = centavos ~/ 100;
  final resto = (centavos % 100).toString().padLeft(2, '0');
  return '${negativo && centavos > 0 ? '-' : ''}$enteros.$resto';
}

/// `('1240.00', 'BOB')` → `'Bs 1.240,00'`. Lanza si el importe no tiene la forma
/// del contrato: mostrarlo a medias en una billetera es peor que fallar.
String formatearMonto({required String monto, required String moneda}) {
  if (!_formaDelContrato.hasMatch(monto)) {
    throw FormatException(
      'Importe fuera del contrato: "$monto". El esquema Dinero exige ^-?\\d+\\.\\d{2}\$.',
    );
  }
  final negativo = monto.startsWith('-');
  final sinSigno = negativo ? monto.substring(1) : monto;
  final punto = sinSigno.indexOf('.');
  final enteros = sinSigno.substring(0, punto);
  final centavos = sinSigno.substring(punto + 1);
  return '${negativo ? '-' : ''}${prefijoDe(moneda)} ${_agruparMiles(enteros)},$centavos';
}

/// El camino de vuelta, solo para la prueba de propiedad.
String desformatearMonto(String texto) {
  final negativo = texto.startsWith('-');
  final cuerpo = (negativo ? texto.substring(1) : texto).replaceFirst(
    RegExp(r'^\S+\s'),
    '',
  );
  final coma = cuerpo.indexOf(',');
  final enteros = cuerpo.substring(0, coma).replaceAll('.', '');
  final centavos = cuerpo.substring(coma + 1);
  return '${negativo ? '-' : ''}$enteros.$centavos';
}

/// Cómo se lee en voz alta: con su concepto y la moneda dicha entera.
String montoParaLectura({
  required String monto,
  required String moneda,
  String? etiqueta,
}) {
  final texto = formatearMonto(monto: monto, moneda: moneda);
  return etiqueta == null ? texto : '$etiqueta: $texto';
}
