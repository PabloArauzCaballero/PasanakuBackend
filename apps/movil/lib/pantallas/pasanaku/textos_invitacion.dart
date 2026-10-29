/// Textos de la invitación a un grupo (CU-69): quien invita, quien recibe el enlace y
/// el escaneo del QR. Salen de `textos.dart` solo para que ningún archivo pase de 200
/// líneas; la disciplina es la misma (cero literales sueltos en las pantallas).
library;

class TextosInvitacion {
  // Invitar (CU-69)
  static const tituloInvitar = 'Invitar a un contacto';
  static const accionInvitar = 'Invitar a alguien a mi grupo';
  static const accionCrearGrupo = 'Organizar un grupo nuevo';
  static const accionUnirseAGrupo = 'Unirse a un grupo';
  static String organizarAlTope(int tope) =>
      'Ya organizás $tope grupos, el máximo. Cuando uno termine vas a poder organizar otro.';
  static String participarAlTope(int tope) =>
      'Ya participás en $tope grupos, el máximo. Cuando uno termine vas a poder unirte a otro.';
  static const telefonoInvitado = 'Celular de la persona';
  static const nombreSugerido = 'Cómo la conocés (opcional)';
  static const canalInvitacion = 'Por dónde se la vas a mandar';
  static const enviarInvitacion = 'Crear la invitación';
  static const invitacionCreadaTitulo = 'Invitación creada';
  // AportaYa no manda SMS ni WhatsApp por su cuenta: el enlace sale del teléfono de
  // quien invita (2026-10-18). Decir «le va a llegar» prometía un envío que no existe.
  static String invitacionCreadaPara(String telefono) =>
      'Mandale el enlace a $telefono. Solo esa persona puede usarlo para unirse.';
  static const enviarPorWhatsApp = 'Enviar por WhatsApp';
  static const enviarPorSms = 'Enviar por SMS';
  static const compartirConOtraApp = 'Compartir con otra app';
  static const noSePudoAbrir =
      'No pudimos abrir esa app. Copiá el enlace y pegalo en el chat que quieras.';
  static String invitacionVence(String cuando) => 'Vence el $cuando.';
  static const qrEtiqueta = 'Código QR de la invitación';
  static const mostrarElQr =
      'Si está con vos, mostrale este QR: lo lee desde «Escanear invitación».';
  static const enlaceDeInvitacion = 'Enlace de la invitación';
  static const copiarEnlace = 'Copiar enlace';
  static const copiado = 'Copiado';
  static const enlaceCopiado =
      'Enlace copiado. Pegalo donde se lo quieras mandar.';
  static const invitarAOtra = 'Invitar a otra persona';
  // Unirse por enlace (CU-69, lado de quien recibe)
  static const tituloUnirse = 'Invitación al grupo';
  static const verificandoInvitacion = 'Verificando tu invitación…';
  static const ingresaParaVerGrupo =
      'Ingresá con el teléfono que recibió la invitación para ver el grupo.';
  static const ingresar = 'Ingresar';
  static const crearCuenta = 'Crear cuenta';
  static const reglamentoDelGrupo = 'Reglamento del grupo';
  static const aceptoReglamento = 'Leí y acepto este reglamento';
  static const unirmeAlGrupo = 'Unirme al grupo';
  static const yaFormasParte = 'Ya formás parte del grupo.';
  static const verMisGrupos = 'Ver mis grupos';
  static const reintentar = 'Reintentar';
  static const enviandoInvitacion = 'Enviando la invitación…';

  // Escanear una invitación
  static const tituloEscanear = 'Escanear invitación';
  static const escanearAyuda =
      'Apuntá la cámara al QR de la invitación que te mandó el organizador.';
  static const escanearAbriendo = 'Abriendo la cámara…';
  static const escanearRechazo =
      'Ese QR no es una invitación de AportaYa. Probá con otro.';
  static const camaraDenegadaTitulo = 'No podemos usar la cámara';
  static const camaraDenegadaDetalle =
      'Le negaste el permiso a AportaYa. Habilitalo desde los ajustes del teléfono, o pegá el enlace de la invitación acá abajo.';
  static const camaraNoDisponibleDetalle =
      'Este dispositivo no puede escanear. Pegá el enlace de la invitación acá abajo.';
  static const camaraOtroDetalle =
      'La cámara no arrancó. Volvé a intentar o pegá el enlace de la invitación acá abajo.';
  static const abrirAjustes = 'Abrir ajustes';
  static const pegarEnlaceEtiqueta = 'Enlace de la invitación';
  static const usarEnlace = 'Usar este enlace';
  static const enlaceInvalido =
      'Ese enlace no es una invitación de AportaYa. Revisalo y probá de nuevo.';
}
