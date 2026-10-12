/// Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. La app no muestra el mensaje
/// del backend: el texto que lee una persona es decisión de producto.
library;

import 'package:aportaya_diseno/errores.dart';

const Map<String, String> _catalogo = {
  'AP-CU01-01':
      'Todavía no podemos abrir tu cuenta: falta verificar tus datos.',
  'AP-CU01-03': 'Ese celular ya tiene una cuenta. Si es tuyo, iniciá sesión.',
  // CU-01 · código de correo previo al alta (servicio identidad: AP-CU01-07..11).
  'AP-CU01-07': 'No pudimos enviar el correo. Probá de nuevo en un momento.',
  'AP-CU01-08': 'Pediste demasiados códigos. Esperá un rato y volvé a probar.',
  'AP-CU01-09': 'El código no es correcto. Revisalo o pedí uno nuevo.',
  'AP-CU01-10': 'El código venció. Pedí uno nuevo.',
  'AP-CU01-11': 'Confirmá el código de este correo antes de crear tu cuenta.',
  // CU-04 · el servidor distingue cada rechazo (identidad.yaml: 01 credencial, 02 bloqueo,
  // 04 código del segundo factor, 05 demasiados intentos, 06/07 factor). La app los dice por separado.
  'AP-CU04-01':
      'El celular o la contraseña no coinciden. Revisalos e intentá de nuevo.',
  'AP-CU04-02':
      'Tu cuenta está bloqueada un rato por seguridad. Probá más tarde.',
  'AP-CU04-04':
      'El código no coincide o ya no sirve. Revisalo o pedí uno nuevo.',
  'AP-CU04-05': 'Demasiados intentos. Esperá un rato y volvé a probar.',
  'AP-CU04-06': 'Tu segundo factor no está activado. Pedí ayuda a soporte.',
  'AP-CU04-07':
      'Ese tipo de segundo factor no sirve para tu cuenta. Pedí ayuda a soporte.',
  // CU-13 · el saldo. Con TEST real, la cuenta que la app consulta no existe (422) y se leía como falla del servidor.
  'AP-CU13-01': 'Tu billetera todavía no está activa.',
  // CU-20 · crear grupo. Con el servidor TEST real, AP-CU20-04 (licencia sin otorgar, R-LIC-01) llegaba como
  // «Algo salió mal de nuestro lado»: era un rechazo de negocio explicable, no una falla.
  'AP-CU20-01':
      'Todavía no hay una tarifa vigente para crear grupos. Probá más adelante.',
  'AP-CU20-02': 'Tu cuenta todavía no está habilitada para organizar grupos.',
  'AP-CU20-03':
      'El precio de este grupo ya estaba fijado. Revisá tus grupos antes de repetirlo.',
  'AP-CU20-04':
      'Organizar grupos todavía no está disponible en AportaYa. Probá más adelante.',
  'AP-CU20-05':
      'Necesitás subir tu nivel de verificación antes de organizar un grupo.',
  // CU-69 · invitar y unirse por enlace (grupos.yaml: 01 cupos, 03 ya participa, 04 reenvíos, 05 enlace
  // inválido/revocado/vencido, 07 emisor, 08 límite diario, 09 vigente, 10 ingreso con revisión humana).
  'AP-CU69-01': 'Ya no hay cupos libres en este grupo.',
  'AP-CU69-03': 'Esa persona ya forma parte del grupo.',
  'AP-CU69-04': 'Ya reenviaste esta invitación demasiadas veces.',
  'AP-CU69-05':
      'Esta invitación ya no es válida: pudo vencer, usarse o ser revocada por quien te invitó.',
  'AP-CU69-07':
      'No podés invitar a este grupo: solo quien lo organiza puede hacerlo.',
  'AP-CU69-08': 'Llegaste al límite de invitaciones de hoy. Probá mañana.',
  'AP-CU69-09':
      'Ya hay una invitación vigente para ese celular. Revocala antes de enviar otra.',
  'AP-CU69-10':
      'Este grupo revisa cada ingreso antes de aceptarlo y por enlace no se puede entrar todavía. Pedile ayuda a quien te invitó.',
  // CU-12 · QR interno. Cada rechazo del QR dice una cosa distinta; en particular el bancario/interoperable
  // NO se presenta como «QR inválido» ni como si pudiera pagarse acá: todavía no hay proveedor habilitado.
  'AP-CU12-07':
      'Ese QR no es válido. Pedí que te generen uno nuevo desde AportaYa.',
  'AP-CU12-08': 'Ese QR venció. Pedí que te generen uno nuevo.',
  'AP-CU12-09': 'Ese QR ya se usó. Cada QR se paga una sola vez.',
  'AP-CU12-10':
      'Este QR es de un banco o de otra red de pagos y todavía no aceptamos pagos con QR interoperable. '
      'Podés pagar con un QR de AportaYa o con una transferencia.',
  'AP-CU21-03': 'No pudimos cobrar. Revisá tu saldo e intentá de nuevo.',
  'AP-VAL-03': 'Este vale ya se usó. Cada vale se canjea una sola vez.',
};

/// ¿La app tiene un texto propio para este código? Las pantallas lo usan para no tapar un rechazo
/// explicable con el genérico por estado.
bool tieneMensajePropio(String codigo) => _catalogo.containsKey(codigo);

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
