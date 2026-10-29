/// Textos del enlace del grupo (CU-69, 2026-10-19): el organizador comparte un enlace
/// —o su QR— y cada persona pide unirse; el organizador la acepta o la rechaza.
class TextosEnlaceDelGrupo {
  TextosEnlaceDelGrupo._();

  // Organizador
  static const titulo = 'Invitar al grupo';
  static const sinEnlaceTitulo = 'Un enlace para todo el grupo';
  static const sinEnlaceDetalle =
      'Creá un enlace y compartilo por WhatsApp o mostrá su QR. Quien lo abra pide '
      'unirse y vos decidís a quién aceptar. Sirve 7 días o hasta que se llene el grupo.';
  static const crearEnlace = 'Crear enlace del grupo';
  static const preparando = 'Preparando la invitación…';
  static const qrEtiqueta = 'Código QR para unirse al grupo';
  static const porQr = 'Por QR';
  static const porQrDetalle =
      'Mandá esta imagen al grupo de WhatsApp. Quien la reciba la guarda y la sube '
      'en Aporteo: Grupos → Unirse a un grupo.';
  static const compartirQr = 'Compartir QR por WhatsApp';
  static const porEnlaceTitulo = 'Por enlace';
  static const porEnlaceDetalle =
      'Quien lo reciba lo copia y lo pega en Aporteo: Grupos → Unirse a un grupo.';
  static const compartirEnlace = 'Compartir enlace por WhatsApp';
  static const noSePudoArmarQr =
      'No pudimos preparar la imagen del QR. Compartí el enlace.';
  static String mensajeDelQr(String? nombreDelGrupo) {
    final grupo = nombreDelGrupo == null || nombreDelGrupo.trim().isEmpty
        ? 'mi pasanaku'
        : 'mi pasanaku «${nombreDelGrupo.trim()}»';
    return 'Te invito a $grupo en Aporteo. Guardá esta imagen y subila en '
        'Grupos → Unirse a un grupo.';
  }

  static String vence(String cuando) =>
      'Sirve hasta el $cuando o hasta que se llene el grupo.';
  static const compartirPorWhatsApp = 'Compartir por WhatsApp';
  static const compartirConOtraApp = 'Compartir con otra app';
  static const copiarEnlace = 'Copiar enlace';
  static const enlaceCopiado = 'Enlace copiado.';
  static const verSolicitudes = 'Ver quién pidió unirse';
  static const generarOtro = 'Crear un enlace nuevo';
  static const generarOtroConfirmar =
      'El enlace anterior deja de servir. Quien lo tenga ya no va a poder pedir unirse.';
  static const anular = 'Anular el enlace';
  static const anularConfirmar =
      'Nadie más va a poder pedir unirse con este enlace. Podés crear otro cuando quieras.';
  static const confirmar = 'Sí, seguir';
  static const cancelar = 'Cancelar';
  static const anulado = 'Listo, el enlace ya no sirve.';
  static const invitarAUnNumero = 'Invitar a un número en particular';
  static const noSePudoAbrir =
      'No pudimos abrir esa app. Copiá el enlace y pegalo en el chat que quieras.';

  // Solicitudes
  static const tituloSolicitudes = 'Quién pidió unirse';
  static const tituloSinSolicitudes = 'Nadie pidió unirse todavía';
  static String conPuntaje(String p) => 'Puntaje $p';
  static String cuposPedidos(int n) => n == 1 ? '1 cupo' : '$n cupos';
  static const sinSolicitudes =
      'Todavía nadie pidió unirse. Cuando alguien abra el enlace, aparece acá.';
  static const sinNombre = 'Persona todavía sin nombre';
  static const porEnlace = 'Pidió con el enlace del grupo';
  static String pidioEl(String cuando) => 'Pidió el $cuando';
  static const aceptar = 'Aceptar';
  static const rechazar = 'Rechazar';
  static const aceptada = 'Aceptada: ya tiene su cupo en el grupo.';
  static const rechazada = 'Solicitud rechazada.';

  // Quien recibe el enlace
  static const tituloUnirse = 'Te invitaron a un pasanaku';
  static String aporte(String monto, String periodicidad) =>
      'Aporte de $monto · ${periodicidad.toLowerCase()}';
  static String cuposLibres(int n) =>
      n == 1 ? 'Queda 1 cupo libre' : 'Quedan $n cupos libres';
  static const grupoLleno =
      'Este grupo ya está completo. Pedile al organizador que te avise si se libera un cupo.';
  static const pedirUnirme = 'Pedir unirme';
  static const avisoDeDatos =
      'Al pedir unirte, el organizador ve tu nombre completo para saber quién sos.';
  static const pedidoEnviado = 'Listo, le avisamos al organizador';
  static const pedidoEnviadoDetalle =
      'El organizador revisa tu pedido y te acepta o no. Cuando te acepte, el grupo '
      'aparece en «Grupos».';
  static const verMisGrupos = 'Ir a mis grupos';
  static const verificarPrimero =
      'Para pedir unirte a un grupo con plata necesitamos verificar tu identidad.';
  static const verificarMiIdentidad = 'Verificar mi identidad';
  static const enlaceInvalido =
      'Este enlace ya no sirve: venció o el organizador lo cambió. Pedile uno nuevo.';
}

/// «Unirse a un grupo» (2026-10-20): con la imagen del QR que llegó por WhatsApp o con
/// el enlace pegado. La app valida el código con el servidor antes de pedir unirse.
class TextosUnirseAUnGrupo {
  TextosUnirseAUnGrupo._();

  static const titulo = 'Unirse a un grupo';
  static const ayuda =
      'Pedile al organizador el QR o el enlace del grupo. Después él decide si te acepta.';
  static const conQr = 'Con el QR';
  static const conQrDetalle =
      'Elegí de tu galería la imagen del QR que te mandaron por WhatsApp.';
  static const subirQr = 'Subir la imagen del QR';
  static const conEnlace = 'Con el enlace';
  static const enlaceEtiqueta = 'Enlace del grupo';
  static const buscar = 'Buscar el grupo';
  static const usarCamara = 'Escanear con la cámara';
  static const sinQr =
      'No encontramos un QR en esa imagen. Probá con otra o pegá el enlace.';
  static const qrAjeno = 'Ese QR no es de un grupo de Aporteo.';
  static const enlaceVacio = 'Pegá el enlace que te mandó el organizador.';
  static const enlaceAjeno =
      'Ese enlace no es de un grupo de Aporteo. Copialo entero del mensaje del organizador.';
}
