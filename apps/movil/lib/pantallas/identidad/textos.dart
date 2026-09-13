/// Los textos del dominio identidad, en un solo lugar (regla de `disenar-frontend`:
/// cero literales sueltos en los widgets).
class TextosIdentidad {
  TextosIdentidad._();

  static const tituloBienvenida = 'Bienvenido a AportaYa';
  static const cargando = 'Un momento…';

  // Portada: lo primero que ve alguien que abre la app. Explica el producto en los
  // términos en que la gente ya lo conoce —el pasanaku de toda la vida— y dice qué
  // cambia acá, sin jerga financiera ni promesas de rendimiento.
  static const portadaTitular = 'El pasanaku de siempre,\ncon las cuentas claras';
  static const portadaBajada =
      'Armá una rueda con tu gente, poné lo mismo cada turno y cobrá cuando te toca. '
      'Sin confiar de memoria.';
  static const portadaRuedaTitulo = 'Tu rueda, a la vista';
  static const portadaRuedaDetalle = 'Quién puso, quién cobró y a quién le toca.';
  static const portadaSorteoTitulo = 'El turno no se arregla';
  static const portadaSorteoDetalle =
      'Se sortea, y cualquiera del grupo puede comprobarlo.';
  static const portadaCustodiaTitulo = 'La plata no la tenemos nosotros';
  static const portadaCustodiaDetalle =
      'Queda en Banco Unión, aparte del dinero de la empresa.';
  static const portadaSaltar = 'Saltar';
  // Las cuatro láminas del tour (D-8 de la maqueta): qué es, qué se ve, cómo se
  // decide el orden, y dónde queda la plata. Una idea por lámina.
  static const tour1Titulo = 'El pasanaku de siempre,\ncon las cuentas claras';
  static const tour1Texto =
      'Armá una rueda con tu gente, poné lo mismo cada turno y cobrá cuando te toca.';
  static const tour2Titulo = 'Tu rueda, a la vista';
  static const tour2Texto =
      'Quién puso, quién cobró y a quién le toca. Sin confiar de memoria ni llevar '
      'la cuenta en un cuaderno.';
  static const tour3Titulo = 'El turno no se arregla';
  static const tour3Texto =
      'El orden sale sorteado, y cualquiera del grupo puede comprobar que el sorteo '
      'fue limpio.';
  static const tour4Titulo = 'La plata no la tenemos nosotros';
  static const tour4Texto =
      'Queda en custodia en Banco Unión, separada del dinero de la empresa. Aunque '
      'a AportaYa le vaya mal, tu plata sigue siendo tuya.';
  static const portadaCrearCuenta = 'Crear mi cuenta';
  static const portadaYaTengoCuenta = 'Ya tengo cuenta';

  // Alta (CU-01), ocho pasos.
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
