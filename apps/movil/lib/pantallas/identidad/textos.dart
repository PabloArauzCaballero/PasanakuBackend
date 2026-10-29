/// Los textos del dominio identidad, en un solo lugar (regla de `disenar-frontend`:
/// cero literales sueltos en los widgets).
class TextosIdentidad {
  TextosIdentidad._();

  static const tituloBienvenida = 'Tu cuenta en AportaYa';
  // Sin nombre (la ruta no recibe datos): el panel dice «Bienvenida, <esto>».
  static const bienvenidaSinNombre = 'tu cuenta está lista';
  static const bienvenidaIrABilletera = 'Ver mi billetera';
  static const bienvenidaCompletarPerfil = 'Completar mi perfil';
  static const cargando = 'Un momento…';

  static const portadaSaltar = 'Crear cuenta';
  static const portadaComoFunciona = '¿Cómo funciona?';
  static const tourSiguiente = 'Siguiente';
  static const tourEmpezar = 'Crear mi cuenta';
  // La bienvenida y las cuatro láminas del tour (`docs/Views/AportaYa-Maqueta.html`).
  // Titular verificable (2026-10-29, skill copywriting): «cuentas claras» lo prueba
  // el registro de cada movimiento; «plata segura» no se puede comprobar en una línea.
  static const bienvenidaTitulo =
      'El pasanaku de siempre,\ncon las cuentas claras';
  static const bienvenidaTexto =
      'Aportá, recibí tu entrega cuando te toca y mirá el estado del grupo '
      'desde el celular. Cada movimiento queda registrado.';
  static const tour1Titulo = 'El pasanaku de siempre,';
  static const tour1Destacado = 'sin el cuaderno';
  static const tour1Texto =
      'Los mismos turnos y la misma gente. Lo que cambia es que nadie tiene que '
      'juntar ni guardar la plata, y cada aporte queda anotado.';
  static const tour2Titulo = 'Tu plata está';
  static const tour2Destacado = 'en custodia';
  static const tour2Texto =
      'No la guarda un vecino ni la empresa: vive en una cuenta bancaria separada. '
      'Cada movimiento queda con su comprobante, y podés bajar el extracto cuando '
      'quieras.';
  static const tour3Titulo = 'El turno se sortea';
  static const tour3Destacado = 'a la vista de todos';
  static const tour3Texto =
      'El orden no lo decide nadie. Sale de una semilla pública que se toma después '
      'de cerrar la lista, y cualquiera puede repetir el sorteo y llegar al mismo '
      'resultado.';
  static const tour4Titulo = 'Si alguien no aporta,';
  static const tour4Destacado = 'el grupo no se frena';
  static const tour4Texto =
      'Hay un fondo de garantía que cubre la cuota para que el turno se entregue '
      'igual. La deuda no se perdona: queda viva y el fondo pasa a ser el acreedor.';
  static const portadaCrearCuenta = 'Crear mi cuenta';
  static const portadaYaTengoCuenta = 'Ya tengo cuenta';

  // Las garantías de la bienvenida: las mismas tres ideas de las láminas 2, 1 y 3 del
  // tour, para quien decide sin abrirlo.
  static const portadaGarantiaCustodia = 'Plata en custodia';
  static const portadaGarantiaRegistro = 'Todo queda registrado';
  static const portadaGarantiaTurno = 'Tu turno a la vista';

  // Alta (CU-01), seis pasos en cuatro fases.
  static const registroTitulo = 'Crear cuenta';
  static const telefonoAyuda = 'Ocho dígitos, sin el código de país.';
  static const documentoAyuda =
      'El número de tu cédula de identidad, solo los dígitos y sin el complemento. '
      'Ej.: 8452136. Tiene que ser el mismo que se ve en la foto del carnet.';
  static const lugarExpedicion = 'Expedido en';
  static const lugarExpedicionAyuda = 'El departamento del carnet.';
  static const lugarExpedicionVacio = 'Elegir';
  static const correo = 'Correo electrónico';
  static const correoAyuda = 'Acá te mandamos los comprobantes y el extracto.';
  static const canalVerificacion = '¿Por dónde te mandamos el código?';
  static const canalSms = 'SMS al celular';
  static const canalCorreo = 'Correo electrónico';
  static const canalAyuda =
      'Elegí cuál de tus dos contactos confirmás primero. Podés confirmar el otro '
      'más adelante desde tu perfil.';
  static const fechaExpiracion = 'Vence el';
  static const fechaExpiracionAyuda =
      'La fecha de vencimiento que figura en tu carnet.';
  static const lugarExpedicionFalta = 'Elegí dónde te expidieron el carnet.';
  static const fechaNacimientoAyuda =
      'Para abrir una billetera hay que ser mayor de edad.';
  static const registroPie =
      'Al continuar guardamos estos datos para abrir tu cuenta. '
      'Todavía no se envía nada al banco.';
  // Paso 1: qué viene después, y que el carnet se puede postergar (2026-10-17).
  static const registroIntro =
      'Primero, tu contacto y una contraseña. El carnet viene después, y lo '
      'podés dejar para más tarde.';
  static const pasoDatos = 'Tus datos';
  static const pasoContrasena = 'Tu contraseña';
  static const pasoCorreo = 'Confirmar correo';
  static const pasoCapturas = 'Tus capturas';
  static const pasoAnverso = 'Documento (anverso)';
  static const pasoReverso = 'Documento (reverso)';
  static const pasoPruebaDeVida = 'Prueba de vida';
  static const pasoCotejo = 'Revisá tus datos';
  static const pasoPerfil = 'Tu actividad';
  static const pasoContrato = 'Contrato';

  static const nombres = 'Nombres';
  static const apellidos = 'Apellidos';
  static const primerNombre = 'Primer nombre';
  static const segundoNombre = 'Segundo nombre (opcional)';
  static const apellidoPaterno = 'Apellido paterno';
  static const apellidoMaterno = 'Apellido materno';
  static const fechaNacimiento = 'Fecha de nacimiento';
  static const telefono = 'Celular';
  static const tipoDocumento = 'Tipo de documento';
  static const numeroDocumento = 'Número de CI';
  static const codigoPorSms = 'Te enviamos un código por SMS al';
  static const codigoPorCorreo = 'Te enviamos un código al correo';
  static const codigoNoLlega =
      'El código dura 10 minutos. Si no llega, pedí una combinación nueva.';
  static const reenviarCodigo = 'Enviar una combinación nueva';
  static const enviandoCodigo = 'Enviando código...';
  static const verificandoCodigo = 'Comprobando código...';
  static const capturarAnverso = 'Fotografiá el frente de tu documento';
  static const capturarReverso = 'Fotografiá el reverso de tu documento';
  static const capturarSelfie = 'Mirá a la cámara y no te muevas';
  static const elegirDeLasFotos = 'Elegir de la galería';

  // CU-03, declaración PEP.
  static const preguntaPep =
      '¿Ocupás o ocupaste un cargo público de alta jerarquía, o sos familiar '
      'directo o cercano de alguien que lo ocupa?';
  static const detallePep = 'Contanos el cargo y la institución';

  // CU-05, contrato de adhesión.
  static const tituloContrato = 'Contrato de adhesión y tarifario';

  // CU-04, sesión.
  static const altaListaTitulo = 'Tu cuenta quedó creada';
  // El ingreso identifica a la cuenta por el celular; la clave elegida en el alta
  // se usa junto con ese dato para autenticarse.
  static const altaListaDetalle =
      'Ingresá con tu celular para entrar a tu billetera. Si tu verificación '
      'sigue en curso, te avisamos por correo cuando termine.';
  static const tituloSesion = 'Ingresar';
  static const sesionSaludo = 'Hola de nuevo';
  static const sesionTitular = 'Entrá a tu billetera';
  static const sesionCelularAyuda = 'El mismo con el que abriste tu cuenta';
  static const sesionCorreoAyuda = 'El correo que pusiste al abrir tu cuenta';
  static const sesionUsarCorreo = 'Entrar con mi correo';
  static const sesionUsarCelular = 'Entrar con mi celular';
  static const sesionOlvide = '¿Olvidaste tu contraseña?';
  // Sin número ni canal inventado: cuál es el de AportaYa es dato de producto, y
  // poner uno falso acá lo copiaría alguien.
  static const sesionOlvideAyuda =
      'Todavía no se puede recuperar desde la app. Comunicate con soporte de '
      'AportaYa y te ayudamos a entrar.';
  static const sesionSinCuenta = '¿Todavía no tenés cuenta?';
  static const sesionCustodia =
      'Tu plata queda en una cuenta de custodia, separada del dinero de la empresa.';
  static const contrasena = 'Contraseña';
  static const ingresar = 'Ingresar';
  static const tituloMfa = 'Verificación en dos pasos';
  static const mfaSinCodigoAyuda =
      'El código se envía al correo verificado de tu cuenta. Si no tenés acceso a '
      'ese correo, comunicate con soporte de AportaYa.';
  static const explicacionMfa =
      'Para cuidar tu cuenta te pedimos un código de 6 dígitos además de tu '
      'contraseña. Escribilo para terminar de entrar.';
  // Solo cuando el servidor confirmó que lo mandó por correo.
  static const explicacionMfaCorreo =
      'Para cuidar tu cuenta te mandamos un código de 6 dígitos al correo de tu '
      'cuenta. Escribilo para terminar de entrar.';
  static const confirmarMfa = 'Confirmar';
  static const etiquetaCodigoMfa = 'Código de verificación';
  static const mfaReenviar = 'Reenviar código por correo';
  static const mfaReenvioAceptado =
      'Te mandamos un código nuevo. Revisá Recibidos y también Spam.';
  static const mfaReenvioSinConfirmar =
      'No pudimos confirmar que el correo salió. Probá de nuevo en un momento o '
      'escribinos a soporte.';
  static const ingresarFaltaDato =
      'Escribí tu celular (o correo) y tu contraseña para ingresar.';
  static const dispositivoDeConfianza =
      'Confiar en este dispositivo por 30 días';
  static const tusDispositivos = 'Tus dispositivos';
  static const revocar = 'Revocar';

  // CU-07 / CU-09.
  static const tituloPerfil = 'Tu perfil';
  static const guardar = 'Guardar';
  static const correoNoEditable =
      'El correo identifica tu cuenta y no se puede cambiar desde la app.';
  static const tituloContrasenaNueva = 'Cambiar contraseña';
  static const contrasenaActual = 'Contraseña actual';
  static const contrasenaNueva = 'Contraseña nueva';
  static const cerrarSesion = 'Cerrar sesión';
  static const verGuiaOtraVez = 'Ver la guía otra vez';
  static const tituloBaja = 'Dar de baja tu cuenta';
  static const avisoBaja =
      'Al confirmar, tu cuenta queda inhabilitada. Si tenés saldo o cupos '
      'activos, primero hay que resolverlos.';
  static const confirmarBaja = 'Confirmar baja';

  static const continuar = 'Continuar';
  static const atras = 'Atrás';
}
