/// Validadores compartidos, del lado del cliente, **espejo** de la restricción del
/// contrato — nunca la reemplazan: el servidor decide, esto solo evita un viaje de
/// red con un dato que ya se sabe inválido (`planes/10b` §4).
///
/// F2 declara el archivo y las reglas que usa el propio shell (documento, teléfono);
/// cada carril de pantallas agrega las suyas acá, junto al patrón que las genera
/// —no una función por CU dispersa por `pantallas/`— porque son del mismo nivel
/// (átomo de dominio, sin Flutter ni red) y compartirlas evita que dos pantallas
/// validen el mismo dato con dos reglas distintas.
library;

/// Teléfono boliviano: 8 dígitos, empieda con 6 o 7 (CU-04, ingreso).
bool telefonoValido(String valor) => RegExp(r'^[67]\d{7}$').hasMatch(valor);

/// Carnet de identidad: 5 a 10 dígitos, con o sin complemento alfabético al final.
bool carnetValido(String valor) =>
    RegExp(r'^\d{5,10}[A-Za-z]{0,2}$').hasMatch(valor);

/// Un monto en bolivianos, como cadena decimal (`Dinero` viaja así, nunca `double`):
/// hasta dos decimales, sin signo, mayor que cero.
bool montoValido(String valor) {
  final m = RegExp(r'^\d+(\.\d{1,2})?$').firstMatch(valor);
  if (m == null) return false;
  final n = double.parse(valor); // permitido: valida forma, no formatea
  return n > 0;
}
