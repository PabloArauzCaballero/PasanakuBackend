/// Los textos del dominio identidad, en un solo lugar (regla de `disenar-frontend`:
/// cero literales sueltos en los widgets).
class TextosIdentidad {
  TextosIdentidad._();

  static const tituloBienvenida = 'Bienvenido a AportaYa';
  static const cargando = 'Un momento…';

  // Portada: lo primero que ve alguien que abre la app. Explica el producto en los
  // términos en que la gente ya lo conoce —el pasanaku de toda la vida— y dice qué
  // cambia acá, sin jerga financiera ni promesas de rendimiento.
  static const portadaTitular =
      'El pasanaku de siempre,\ncon las cuentas claras';
  static const portadaBajada =
      'Armá una rueda con tu gente, poné lo mismo cada turno y cobrá cuando te toca. '
      'Sin confiar de memoria.';
  static const portadaRuedaTitulo = 'Tu rueda, a la vista';
  static const portadaRuedaDetalle =
      'Quién puso, quién cobró y a quién le toca.';
  static const portadaSorteoTitulo = 'El turno no se arregla';
  static const portadaSorteoDetalle =
      'Se sortea, y cualquiera del grupo puede comprobarlo.';
  static const portadaCustodiaTitulo = 'La plata no la tenemos nosotros';
  static const portadaCustodiaDetalle =
      'Queda en Banco Unión, aparte del dinero de la empresa.';
  static const portadaSaltar = 'Saltar';
  static const tourSiguiente = 'Siguiente';
  // La bienvenida y las cuatro láminas del tour, con el texto exacto de la maqueta
  // (`docs/Views/AportaYa-Maqueta.html`: `bienvenida` y `const TOUR`). La segunda
  // línea de cada título es la que se destaca en verde.
  static const bienvenidaTitulo =
      'El pasanaku de siempre,\ncon la plata segura';
  static const bienvenidaTexto =
      'Aportá, cobrá tu turno y mirá el estado del grupo desde el celular. '
      'Cada movimiento queda registrado.';
  static const tour1Titulo = 'El pasanaku de siempre,';
  static const tour1Destacado = 'sin el cuaderno';
  static const tour1Texto =
      'Los mismos turnos, la misma confianza. Lo que cambia es que ya nadie tiene '
      'que juntar, guardar ni acordarse de nada.';
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

  // Alta (CU-01), ocho pasos.
  static const registroTitulo = 'Crear cuenta';
  static const telefonoAyuda = 'Ocho dígitos, sin el código de país.';
  static const documentoAyuda =
      'Tu cédula de identidad, sin puntos ni guiones.';
  static const lugarExpedicion = 'Expedido en';
  static const lugarExpedicionAyuda =
      'El departamento del carnet. El mismo número existe en dos departamentos, '
      'y esto los distingue.';
  static const lugarExpedicionFalta = 'Elegí dónde te expidieron el carnet.';
  static const fechaNacimientoAyuda =
      'Para abrir una billetera hay que ser mayor de edad.';
  static const registroPie =
      'Al continuar guardamos estos datos para abrir tu cuenta. '
      'Todavía no se envía nada al banco.';
  static const registroIntro =
      'Con estos datos abrimos tu billetera. Tienen que coincidir con tu '
      'documento: si no coinciden, la verificación se frena.';
  static const pasoDatos = 'Tus datos';
  static const pasoCelular = 'Confirmar celular';
  static const pasoAnverso = 'Documento (anverso)';
  static const pasoReverso = 'Documento (reverso)';
  static const pasoPruebaDeVida = 'Prueba de vida';
  static const pasoCotejo = 'Revisá tus datos';
  static const pasoPerfil = 'Tu actividad';
  static const pasoContrato = 'Contrato';

  static const nombres = 'Nombres';
  static const apellidos = 'Apellidos';
  static const fechaNacimiento = 'Fecha de nacimiento';
  static const telefono = 'Celular';
  static const tipoDocumento = 'Tipo de documento';
  static const numeroDocumento = 'Número de documento';
  static const codigoEnviado = 'Te enviamos un código por SMS';
  static const capturarAnverso = 'Fotografiá el frente de tu documento';
  static const capturarReverso = 'Fotografiá el reverso de tu documento';
  static const capturarSelfie = 'Mirá a la cámara y no te muevas';
  static const camaraSinPermiso =
      'No pudimos usar la cámara. Podés seguir escribiendo los datos a mano.';
  static const camaraPocaLuz =
      'Hay poca luz para una foto nítida. Podés reintentar o escribir los datos.';
  static const reintentarCaptura = 'Reintentar';
  static const escribirAMano = 'Escribir a mano';
  static const origenDeFondos = 'Origen de los fondos';
  static const actividadEconomica = 'Actividad económica';
  static const montoMensualEstimado = 'Monto mensual estimado (Bs)';

  // CU-03, declaración PEP.
  static const preguntaPep =
      '¿Ocupás o ocupaste un cargo público de alta jerarquía, o sos familiar '
      'directo o cercano de alguien que lo ocupa?';
  static const detallePep = 'Contanos el cargo y la institución';

  // CU-05, contrato de adhesión.
  static const tituloContrato = 'Contrato de adhesión y tarifario';
  static const avisoLeerContrato = 'Deslizá hasta el final para poder aceptar.';
  static const aceptoContrato = 'Acepto el contrato de adhesión';
  static const aceptoTarifario = 'Acepto el tarifario vigente';
  static const aceptoTratamientoDatos =
      'Acepto el tratamiento de mis datos personales';
  static const aceptarYContinuar = 'Aceptar y continuar';

  // CU-04, sesión.
  static const tituloSesion = 'Ingresar';
  static const sesionSaludo = 'Hola de nuevo';
  static const sesionTitular = 'Entrá a tu billetera';
  static const sesionCelularAyuda = 'El mismo con el que abriste tu cuenta';
  static const sesionOlvide = '¿Olvidaste tu contraseña?';
  // Sin número ni canal inventado: cuál es el de AportaYa es dato de producto, y
  // poner uno falso acá lo copiaría alguien.
  static const sesionOlvideAyuda =
      'Todavía no se puede recuperar desde la app. Comunicate con soporte de '
      'AportaYa y te ayudamos a entrar.';
  static const sesionSinCuenta = '¿Todavía no tenés cuenta?';
  static const sesionCustodia =
      'Tu plata queda en custodia en Banco Unión, separada del dinero de la empresa.';
  static const contrasena = 'Contraseña';
  static const ingresar = 'Ingresar';
  static const tituloMfa = 'Verificación en dos pasos';
  static const dispositivoDeConfianza =
      'Confiar en este dispositivo por 30 días';
  static const tusDispositivos = 'Tus dispositivos';
  static const revocar = 'Revocar';

  // CU-07 / CU-09.
  static const tituloPerfil = 'Tu perfil';
  static const guardar = 'Guardar';
  static const tituloContrasenaNueva = 'Cambiar contraseña';
  static const contrasenaActual = 'Contraseña actual';
  static const contrasenaNueva = 'Contraseña nueva';
  static const cerrarSesion = 'Cerrar sesión';
  static const tituloBaja = 'Dar de baja tu cuenta';
  static const avisoBaja =
      'Al confirmar, tu cuenta queda inhabilitada. Si tenés saldo o cupos '
      'activos, primero hay que resolverlos.';
  static const confirmarBaja = 'Confirmar baja';

  static const continuar = 'Continuar';
  static const atras = 'Atrás';
}
