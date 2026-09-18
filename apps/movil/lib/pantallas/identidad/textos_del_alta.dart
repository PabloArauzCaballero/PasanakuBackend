/// Los textos de los pasos del alta (CU-01) y de la consulta de contratos de CU-05.
///
/// Viven aparte de [TextosIdentidad] y no por gusto: ese archivo junta los textos de
/// las trece pantallas del dominio y ya pasaba las doscientas líneas que el gate del
/// frontend permite. El corte no es arbitrario — lo que está acá es lo que se lee
/// mientras se crea una cuenta, y solo lo leen los pasos del asistente.
class TextosDelAlta {
  TextosDelAlta._();

  // Paso 2: la contraseña. Las reglas son las que aplica el servidor
  // (`PoliticaDeClave`), no unas inventadas acá.
  static const claveIntro =
      'Con esta contraseña vas a entrar a tu billetera. Elegí una que puedas '
      'recordar y que no uses en otro lado.';
  static const claveNueva = 'Contraseña';
  static const claveRepetir = 'Repetila';
  static const claveAyuda = 'Ocho caracteres o más.';
  static const claveCorta = 'Todavía es corta: tienen que ser ocho o más.';
  static const claveConTelefono =
      'No uses tu celular adentro de la contraseña.';
  static const claveConDocumento =
      'No uses tu número de documento adentro de la contraseña.';
  static const clavesNoCoinciden = 'Las dos no coinciden. Revisá la segunda.';
  static const claveAvisoTitulo = 'Nadie de AportaYa te la va a pedir';
  static const claveAvisoDetalle =
      'Ni por teléfono, ni por correo, ni por WhatsApp. Quien te la pida está '
      'intentando estafarte.';

  static const origenDeFondos = 'Origen de los fondos';
  static const origenDeFondosAyuda =
      'De dónde sale la plata que vas a mover por acá.';
  static const origenFalta = 'Elegí de dónde salen tus fondos.';
  static const origenDetalle = 'Contanos de dónde';
  static const origenDetalleAyuda =
      'Una línea alcanza: «venta de mi moto», «premio de un sorteo».';
  static const origenDetalleFalta = 'Escribí de dónde salen tus fondos.';
  static const actividadEconomica = 'Actividad económica';
  static const actividadEconomicaAyuda = 'A qué te dedicás hoy.';
  static const actividadFalta = 'Elegí a qué te dedicás.';
  static const elegirOpcion = 'Elegir';
  static const montoMensualEstimado = 'Monto mensual estimado';
  static const montoMensualAyuda =
      'Cuánto calculás mover por mes. Es una estimación, no un límite: '
      'podés dejarlo vacío si todavía no sabés.';
  static const montoInvalido = 'Escribí un monto mayor a cero, o dejalo vacío.';

  static const contratosCargando = 'Buscando el contrato vigente…';
  static const contratosSinRespuesta =
      'No pudimos traer el contrato vigente. Sin él no se puede abrir la cuenta: '
      'revisá tu conexión y volvé a intentar.';
  static const contratosIncompletos =
      'El servidor no está publicando los tres documentos que hay que aceptar. '
      'No es algo que puedas resolver desde acá: escribinos a soporte.';
  static const reintentar = 'Volver a intentar';
  static const altaEnviando = 'Creando tu cuenta…';
  static const avisoLeerContrato = 'Deslizá hasta el final para poder aceptar.';
  static const aceptoContrato = 'Acepto el contrato de adhesión';
  static const aceptoTarifario = 'Acepto el tarifario vigente';
  static const aceptoTratamientoDatos =
      'Acepto el tratamiento de mis datos personales';
  static const aceptarYContinuar = 'Aceptar y continuar';
}
