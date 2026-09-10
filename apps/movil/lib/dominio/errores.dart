/// Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. La app no muestra el mensaje
/// del backend: el texto que lee una persona es decisión de producto.
library;

import 'package:aportaya_diseno/errores.dart';

const Map<String, String> _catalogo = {
  'AP-CU01-01':
      'Todavía no podemos abrir tu cuenta: falta verificar tus datos.',
  'AP-CU01-03': 'Ese celular ya tiene una cuenta. Si es tuyo, iniciá sesión.',
  'AP-CU04-01': 'El código no coincide. Revisalo e intentá de nuevo.',
  'AP-CU04-04': 'Demasiados intentos. Esperá una hora y volvé a probar.',
  'AP-CU04-06': 'Tu segundo factor no está activado. Pedí ayuda a soporte.',
  'AP-CU21-03': 'No pudimos cobrar. Revisá tu saldo e intentá de nuevo.',
  'AP-VAL-03': 'Este vale ya se usó. Cada vale se canjea una sola vez.',
};

String mensajeDe(String codigo, {int? estado}) {
  final conocido = _catalogo[codigo];
  if (conocido != null) return conocido;
  return switch (estado) {
    401 => 'Tu sesión venció. Volvé a ingresar.',
    403 => 'No tenés acceso a esto.',
    404 => 'No encontramos lo que buscabas.',
    409 =>
      'Esta operación ya se hizo o cambió mientras tanto. Revisá el estado antes de repetirla.',
    _ => 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.',
  };
}

class ErrorDeApi implements Exception, ErrorPresentable {
  ErrorDeApi({
    required this.codigo,
    required this.trazaId,
    required this.estado,
  }) : mensaje = mensajeDe(codigo, estado: estado);

  final String codigo;
  @override
  final String trazaId;
  final int estado;
  @override
  final String mensaje;

  @override
  bool get sinConexion => false;

  @override
  String toString() => 'ErrorDeApi($codigo, $estado, traza $trazaId)';
}

class ErrorDeRed implements Exception, ErrorPresentable {
  @override
  String get mensaje =>
      'No hay conexión. Te mostramos lo último que vimos; para operar hace falta señal.';

  @override
  String? get trazaId => null;

  @override
  bool get sinConexion => true;

  @override
  String toString() => 'ErrorDeRed';
}
