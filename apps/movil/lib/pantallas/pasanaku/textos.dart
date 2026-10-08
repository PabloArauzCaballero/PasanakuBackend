/// Textos del dominio pasanaku (grupo, turno, aporte, entrega, transparencia,
/// reclamo). Nada de literales sueltos en las pantallas — es la misma disciplina que
/// `identidad` y `billetera` (regla "cero literales de diseño").
library;

class TextosPasanaku {
  // Grupo — crear
  static const tituloCrearGrupo = 'Organizar un grupo';
  static const nombreDelGrupo = 'Nombre del grupo';
  static const montoAporte = 'Monto de cada aporte';
  static const periodicidad = 'Cada cuánto se aporta';
  static const cupos = 'Cuántos cupos tiene';
  static const diaCobro = 'Día del mes en que se cobra';
  static const modalidadTurnos = 'Cómo se ordenan los turnos';
  static const fechaDeInicio = 'Cuándo empieza';
  static const permitePermutas = 'Permite intercambiar turnos entre socios';
  static const crearGrupoBoton = 'Crear grupo';
  static const creandoGrupo = 'Creando el grupo…';
  static const grupoCreado =
      'Grupo creado. Está congelando el precio; en un momento se abre para inscripciones.';
  static const requisitosTitulo = 'Requisitos para organizar';
  static const requisitosAyuda =
      'Los umbrales los fija el contrato de habilitación de organizadores, no esta app.';
  static const capacitacionVencidaAviso =
      'Tu capacitación venció: no podés abrir grupos nuevos, pero los que ya tenés vigentes siguen en pie.';

  // Grupo — postular / pedir cupo
  static const tituloPedirCupo = 'Pedir mi cupo';
  static const pedirCupoBoton = 'Pedir mi cupo';
  static const cuposSolicitados = 'Cuántos cupos pedís';
  static const mensajeOpcional = 'Un mensaje para quien decide (opcional)';
  static const enviandoPedido = 'Enviando tu pedido…';
  static const pedidoEnviadoTitulo = 'Tu pedido de cupo';
  static const quienDecide =
      'Quién decide: el organizador del grupo, con el puntaje de emparejamiento como guía.';
  static const enCuanto =
      'En cuánto: no hay plazo fijo; te avisamos apenas responda.';
  static const queVeDeVos =
      'Qué ve de vos: tu puntaje y los motivos generales, nunca tus datos personales ni los de otros postulantes.';
  static const tresSalidas =
      'Tres salidas posibles: te acepta con el cupo, te rechaza con un motivo, o el grupo se cierra sin que responda.';
  static const cupoNoOcupadoAviso =
      'Pedir el cupo no lo ocupa todavía: tu lista de grupos no lo va a mostrar hasta que te acepten.';
  static const puntajeTitulo = 'Por qué te propusimos este puntaje';

  // Invitar (CU-69)
  static const tituloInvitar = 'Invitar a un contacto';
  static const telefonoInvitado = 'Celular de la persona (+591…)';
  static const nombreSugerido = 'Cómo la conocés (opcional)';
  static const canalInvitacion = 'Por dónde le llega';
  static const enviarInvitacion = 'Invitar';
  static const enviandoInvitacion = 'Enviando la invitación…';
  static const invitacionEnviada =
      'Listo. Le avisamos, con quién y a qué grupo.';

  // Permuta de turnos (CU-62)
  static const tituloPermuta = 'Cambiar turno con otro socio';
  static const turnoPropio = 'Tu turno';
  static const turnoDeseado = 'El turno que querés';
  static const contraparte = 'Con quién';
  static const motivoPermuta = 'Por qué (obligatorio)';
  static const pedirPermuta = 'Proponer el cambio';
  static const enviandoPermuta = 'Enviando la propuesta…';
  static const permutaPendiente =
      'Propuesta enviada. Queda pendiente hasta que la otra persona la acepte.';

  // Retiro (CU-65)
  static const tituloRetiro = 'Retirarme del grupo';
  static const motivoRetiro = 'Contanos por qué (obligatorio)';
  static const aceptoPlanDePago = 'Acepto un plan de pago si quedo debiendo';
  static const pedirRetiro = 'Solicitar el retiro';
  static const enviandoRetiro = 'Calculando tu posición…';
  static const posicionAcreedora =
      'Tu posición es ACREEDORA: se te liquida recién al cierre del ciclo, no antes.';
  static const posicionDeudora =
      'Tu posición es DEUDORA: tenés saldo pendiente con el grupo.';
  static const posicionNeutra = 'Tu posición es NEUTRA: no debés ni te deben.';

  // Turno
  static const tituloTurno = 'El turno del grupo';
  static const turnoSinConfirmarTitulo = 'Tu turno no está verificado';
  static const turnoSinConfirmarDetalle =
      'Podés revisar el sorteo publicado. No mostraremos una posición sin verificarla.';
  static const verificarPublicamente = 'Verificar el sorteo';
  static const sorteoTitulo = 'Cómo se sorteó el orden';
  static const calendarioTitulo = 'Calendario del ciclo';
  static const plazoGuardado =
      'Fecha límite guardada al calcularse; no se recalcula.';

  // Verificación pública (CU-61)
  static const tituloVerificacion = 'Verificación pública del sorteo';
  static const verificacionAyuda =
      'Cualquiera puede reproducir este sorteo con estos datos: nadie, ni la plataforma, pudo elegir el resultado después de conocerlo.';
  static const hashComprometido = 'Hash comprometido (antes de sortear)';
  static const semillaRevelada = 'Semilla revelada';
  static const ordenPublicado = 'Orden publicado';
  static const reproducir = 'Reproducir el sorteo';
  static const coincideSi = 'El orden reproducido coincide con el guardado.';
  static const coincideNo =
      'El orden reproducido NO coincide. Reportalo: esto es un hallazgo, no un detalle.';

  // Aporte (CU-21)
  static const tituloAportar = 'Aportar este período';
  static const aporteSinMontoVerificado =
      'No podemos confirmar cuánto te toca aportar. Por seguridad, no '
      'enviaremos un pago desde este enlace. Pedí ayuda para continuar.';
  static const revisarAporte = 'Revisar aporte';
  static const tituloRevisionAporte = 'Revisá tu aporte';
  static const confirmarAporte = 'Confirmar aporte';
  static const cambiarDatosAporte = 'Cambiar datos';
  static const aporteMedioResumen = 'Medio de pago';
  static const aporteRevisionAyuda =
      'Todavía no enviamos nada. Revisá el monto, el medio y la referencia antes de confirmar.';
  static const aporteEnviandoAyuda =
      'Estamos enviando este aporte. Esperá la respuesta antes de volver a intentar.';
  static const aporteReintentoAyuda =
      'El intento anterior podría haber llegado. Revisá el estado antes de reintentar; se usarán los mismos datos.';
  static const aporteSinConexion =
      'Aporte pausado. Tus datos siguen aquí. Si ya lo intentaste, revisá el estado al volver.';
  static const aporteMontoTitulo = 'Monto de este período';
  static const aporteMontoAyuda =
      'Lo define el reglamento del grupo y no se puede cambiar en este pago.';
  static const aporteCanal = '¿Cómo vas a pagar?';
  static const aporteReferencia = 'Referencia del pago';
  static const aporteReferenciaAyuda =
      'Copiá el código o número que te dio el medio de pago.';
  static const aporteReferenciaFaltante =
      'Ingresá la referencia para poder identificar el pago.';
  static const bolsaAcumulada = 'Bolsa acumulada';
  static const comision = 'Comisión';
  static const descuentoPorNivel = 'Descuento por tu nivel';
  static const neto = 'Neto a recibir el turno';
  static const aportando = 'Registrando tu aporte…';
  static const aporteConfirmado = 'Aporte registrado';
  static const aporteConfirmadoDetalle =
      'El pago quedó guardado. La obligación se actualizará cuando el medio de pago lo confirme.';
  static const aporteYaRegistradoDetalle =
      'Reconocimos tu intento anterior: no generamos un segundo pago.';

  // Entrega (CU-22)
  static const tituloEntrega = 'Recibir el fondo';
  static const cuentaDestino = 'Cuenta de destino';
  static const solicitarEntrega = 'Solicitar la entrega';
  static const entregaEnCurso = 'Tu entrega está en curso.';

  // Mora / incumplimiento — SIEMPRE en hechos, nunca en probabilidad
  static const tituloMora = 'Estado de tu aporte';
  static const faltaIdentificarParticipacion =
      'No podemos consultar tu estado porque falta identificar tu participación. '
      'Por seguridad, no hagas un aporte desde esta pantalla; pedí ayuda.';
  static const irAAyuda = 'Ir a ayuda';
  static String diasDesdeVencimiento(int dias) => dias <= 0
      ? 'Tu aporte vence hoy.'
      : 'Tu aporte está a $dias día${dias == 1 ? '' : 's'} de su plazo, vencido el';
  static const comoRegularizar = 'Cómo regularizar';
  static const alDia = 'Estás al día con este grupo.';
  static String deudaVigente(String monto) =>
      'Tenés una deuda vigente de $monto con el grupo.';
  static String porAportarAviso(String monto) =>
      'Por aportar este período: $monto.';
  static const sinRestriccion = 'No tenés restricciones vigentes.';
  static String restriccionVigente(String monto) =>
      'Tenés una restricción vigente. Se levanta pagando $monto.';
  static const tituloMiPuntaje = 'Tu puntaje de reputación';
  static const faltaIdentificarPuntaje =
      'No podemos consultar tu puntaje desde este enlace porque no confirma tu identidad. Pedí ayuda para continuar.';
  static const sinHistorial =
      'Todavía no tenés historial suficiente para un puntaje — no es una sanción, es que recién empezás.';

  // Transparencia (CU-72/73)
  static const tituloTransparencia = 'Transparencia del grupo';
  static const cadenaVerificada = 'La cadena de bloques del grupo verifica.';
  static const cadenaRota =
      'La cadena no verifica desde el bloque señalado. Es un hallazgo, se escaló.';
  static const verificarCadena = 'Verificar la cadena';

  // Reclamo (CU-52/53)
  static const tituloReclamo = 'Hacer un reclamo';
  static const numeroDeReclamo = 'Reclamo número';
  static const fechaLimiteReclamo = 'Fecha límite de respuesta';
  static const segundaInstanciaDisponible =
      'Si no estás conforme con la respuesta, podés pedir una segunda instancia, y después acudir a la ASFI.';
  static const enviarReclamo = 'Enviar reclamo';
  static const motivoDelReclamo = 'Contanos qué pasó';
  static const contratoPendienteHueco =
      'Todavía no podemos enviar tu reclamo: el contrato de esta operación no está publicado. Contactá a soporte.';

  // Mercado / D-20
  static const topeDelMercado =
      'Este grupo está a punto de superar el 5 % del mercado permitido.';
  static const puntajeInsuficiente =
      'Tu puntaje no alcanza el mínimo para ver este mercado.';
  static const verTuNivel = 'Ver tu nivel';

  // Comunes
  static const volverAIntentar = 'Volver a intentar';
  static const cargando = 'Cargando…';
}
