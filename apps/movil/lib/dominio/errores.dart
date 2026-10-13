/// Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. La app no muestra el mensaje
/// del backend: el texto que lee una persona es decisión de producto.
library;

import 'package:aportaya_diseno/errores.dart';

const Map<String, String> _catalogo = {
  'AP-CU01-01':
      'Todavía no podemos abrir tu cuenta: falta verificar tus datos.',
  'AP-CU01-02':
      'No pudimos completar la verificación de identidad. Contactá a soporte para revisar tu caso.',
  // El backend usa el mismo código para celular y para documento ya registrados
  // (CU01RegistrarUsuario): el texto no puede elegir uno de los dos.
  'AP-CU01-03':
      'Ya hay una cuenta con ese celular o ese documento. Si es tuya, iniciá sesión.',
  'AP-CU01-04': 'Para continuar, aceptá los contratos vigentes.',
  'AP-CU01-05':
      'Todavía no podemos abrir cuentas en tu zona. Te avisamos apenas se pueda.',
  'AP-CU01-06':
      'Esa contraseña no sirve: es muy corta o usa tu celular o tu documento. Elegí otra.',
  // El servidor compara el vencimiento con su propio reloj (hora de Bolivia): el
  // aviso del paso 1 es ayuda, esto es la regla.
  'AP-CU01-07':
      'Tu carnet está vencido. Para abrir la cuenta hace falta uno vigente.',
  // Los AP-CU04-* siguen el contrato (identidad.yaml, POST /sesiones).
  'AP-CU04-01': 'El celular o la contraseña no coinciden.',
  'AP-CU04-04': 'Ese código no sirve: revisalo o pedí uno nuevo.',
  'AP-CU04-05':
      'Demasiados intentos. Esperá unos minutos antes de volver a probar.',
  'AP-CU01-08':
      'Este expediente ya se resolvió. Actualizá la bandeja antes de intentar otra vez.',
  'AP-CU01-09':
      'Faltan fotos del documento o de la prueba de vida. Completá la carga antes de aprobar.',
  'AP-CU01-10':
      'El documento no indica una fecha de vencimiento. Revisá la imagen y volvé a cargarla.',
  'AP-CU01-11': 'El documento venció y no se puede aprobar.',
  'AP-CU04-02':
      'Tu cuenta está bloqueada un rato por seguridad. Probá más tarde.',
  'AP-CU04-06': 'Tu segundo factor no está activado. Pedí ayuda a soporte.',
  'AP-CU04-07':
      'Ese tipo de segundo factor no sirve para tu cuenta. Pedí ayuda a soporte.',
  'AP-CU04-08': 'No pudimos mantener tu sesión. Volvé a entrar.',
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

/// Campos que el contrato permite deducir sin interpretar el `detalle` opaco.
enum CampoDeErrorApi { contrasena, fechaExpiracion }

String mensajeDe(String codigo, {int? estado}) {
  final conocido = _catalogo[codigo];
  if (conocido != null) return conocido;
  return switch (estado) {
    401 => 'Tu sesión venció. Volvé a ingresar.',
    403 => 'No tenés acceso a esto.',
    404 => 'No encontramos lo que buscabas.',
    409 =>
      'Esta operación ya se hizo o cambió mientras tanto. Revisá el estado antes de repetirla.',
    // El gateway o el servicio no están (TEST respondió `503 no available server`
    // a todo el 2026-10-03): no es la conexión ni los datos de la persona, y decirlo
    // evita que corrija lo que estaba bien o desinstale la app.
    502 || 503 || 504 =>
      'El servicio no está disponible en este momento. No es tu conexión ni tus '
          'datos: probá de nuevo en unos minutos.',
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

  CampoDeErrorApi? get campo => switch (codigo) {
    'AP-CU01-06' => CampoDeErrorApi.contrasena,
    'AP-CU01-07' => CampoDeErrorApi.fechaExpiracion,
    _ => null,
  };

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
