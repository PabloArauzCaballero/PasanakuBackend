/// Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. La app no muestra el mensaje
/// del backend: el texto que lee una persona es decisión de producto.
library;

import 'package:aportaya_diseno/errores.dart';

const Map<String, String> _catalogo = {
  'AP-CU01-01':
      'Todavía no podemos abrir tu cuenta: falta verificar tus datos.',
  'AP-CU01-03': 'Ese celular ya tiene una cuenta. Si es tuyo, iniciá sesión.',
  'AP-CU01-04': 'Falta aceptar el contrato para abrir tu cuenta.',
  'AP-CU01-05': 'Por ahora no podemos abrir cuentas nuevas. Probá más tarde.',
  'AP-CU01-06':
      'Esa contraseña no sirve: tiene que ser más larga y no usar tu celular ni tu documento.',
  'AP-CU04-01': 'El código no coincide. Revisalo e intentá de nuevo.',
  'AP-CU04-04': 'Demasiados intentos. Esperá una hora y volvé a probar.',
  'AP-CU04-06': 'Tu segundo factor no está activado. Pedí ayuda a soporte.',
  'AP-CU21-03': 'No pudimos cobrar. Revisá tu saldo e intentá de nuevo.',
  'AP-VAL-03': 'Este vale ya se usó. Cada vale se canjea una sola vez.',
};

/// `AP-CU01-03` es uno solo («la cuenta ya existe») y el backend dice en
/// `detalle.campo` CUÁL dato ya tiene cuenta. Sin esto, un correo repetido se leía
/// como «ese celular ya tiene una cuenta» y la persona no tenía cómo corregirlo.
const Map<String, String> _cuentaYaExistePorCampo = {
  'telefono': 'Ese celular ya tiene una cuenta. Si es tuyo, iniciá sesión.',
  'documento':
      'Ese documento ya tiene una cuenta. Si es tuyo, iniciá sesión o recuperá el acceso.',
  'correo':
      'Ese correo ya tiene una cuenta. Volvé atrás y usá otro, o iniciá sesión si es tuyo.',
};

/// Lo que rechaza la base sin una regla de negocio detrás (`AP-BD-0n`, ver
/// `FallaDeLaBase` en `comun-web`). Son códigos estables: el cliente distingue un
/// duplicado de una caída, que antes salían iguales como 500.
const Map<String, String> _fallasDeLaBase = {
  'AP-BD-01': 'Ya hay un registro con esos datos. Revisalos antes de seguir.',
  'AP-BD-02':
      'Algo de lo que elegiste ya no existe o cambió. Actualizá y probá de nuevo.',
  'AP-BD-03': 'Uno de los datos no es válido. Revisalos antes de seguir.',
  'AP-BD-04': 'Se cruza con otro registro vigente. Revisá las fechas.',
  'AP-BD-05': 'Alguien cambió esto al mismo tiempo. Probá de nuevo.',
  'AP-BD-06': 'Tardó más de lo normal. Probá de nuevo en un momento.',
};

String mensajeDe(String codigo, {int? estado, String? campo}) {
  if (codigo == 'AP-CU01-03' && campo != null) {
    final porCampo = _cuentaYaExistePorCampo[campo];
    if (porCampo != null) return porCampo;
  }
  final conocido = _catalogo[codigo] ?? _fallasDeLaBase[codigo];
  if (conocido != null) return conocido;
  return switch (estado) {
    401 => 'Tu sesión venció. Volvé a ingresar.',
    403 => 'No tenés acceso a esto.',
    404 => 'No encontramos lo que buscabas.',
    409 =>
      'Esta operación ya se hizo o cambió mientras tanto. Revisá el estado antes de repetirla.',
    400 || 422 => 'Revisá los datos e intentá de nuevo.',
    _ => 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.',
  };
}

class ErrorDeApi implements Exception, ErrorPresentable {
  ErrorDeApi({
    required this.codigo,
    required this.trazaId,
    required this.estado,
    this.campo,
  }) : mensaje = mensajeDe(codigo, estado: estado, campo: campo);

  final String codigo;

  /// El campo que el backend señala en `detalle.campo`, si señala uno.
  final String? campo;
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
