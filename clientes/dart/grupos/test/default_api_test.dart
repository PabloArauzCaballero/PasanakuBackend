import 'package:test/test.dart';
import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteGrupos().getDefaultApi();

  group(DefaultApi, () {
    // Fecha límite en días hábiles, con los días que se saltearon
    //
    // El resultado se GUARDA donde se use y no se recalcula: si despues se declara un feriado, ese plazo no se mueve. Devuelve tambien que dias se saltearon, porque un plazo que no se puede explicar no se puede defender. Nota de ruta: el caso de uso dice `/calendario`, que no esta entre los prefijos reservados de `grupos` (/grupos, /turnos, /acuerdos). Va bajo /grupos/calendario para no invadir un prefijo sin dueno.
    //
    //Future<SalidaPlazoHabil> calcularPlazoHabil(DateTime desde, int dias, String alcance, { String referenciaId }) async
    test('test calcularPlazoHabil', () async {
      // TODO
    });

    // Publicar el compromiso del sorteo
    //
    // Publica SOLO el hash de la semilla. Sin esa separacion, quien ejecuta el sorteo podria probar semillas hasta que salga el orden que le conviene: el compromiso es lo que convierte «confia en nosotros» en «verificalo vos».
    //
    //Future<CompromisoDeSorteo> comprometerSorteo(String grupoId, String idempotencyKey, { EntradaCompromiso entradaCompromiso }) async
    test('test comprometerSorteo', () async {
      // TODO
    });

    // Si la persona participa hoy de algun grupo activo
    //
    // La pregunta `nucleo-financiero` antes de cerrar una billetera (CU-16): cerrar la cuenta de alguien que todavia debe aportar a un pasanaku deja al grupo sin de donde cobrarle.
    //
    //Future<ActividadEnGrupos> consultarActividad(String usuarioId) async
    test('test consultarActividad', () async {
      // TODO
    });

    // El paquete publicado de un sorteo, para poder recomputarlo
    //
    // La pregunta `transparencia` para CU-61. Trae lo que hace falta para rehacer el sorteo desde afuera: el hash comprometido, la semilla revelada, las entropias, el metodo y el orden publicado. Antes del revelado la semilla viaja nula, y ese es el punto: comprometer y revelar existen para que nadie —nosotros incluidos— pueda elegir el resultado despues de conocerlo.
    //
    //Future<PaqueteDelSorteo> consultarPaqueteDelSorteo(String sorteoId) async
    test('test consultarPaqueteDelSorteo', () async {
      // TODO
    });

    // Crear el grupo y disparar el congelamiento del tarifario
    //
    // El grupo nace BORRADOR y emite `grupos.grupo_creado`; `tarifas` congela el precio y `nucleo-financiero` abre la cuenta del grupo al consumirlo. Pasa a ABIERTO_A_INSCRIPCION cuando los dos respondieron. Que nazca en borrador no es un rodeo: un grupo sin precio congelado no puede recibir a nadie, porque el precio es justamente lo que cada participante acepta al firmar el reglamento. Dejarlo abierto antes seria pedirle a alguien que se comprometa a un costo que todavia no existe. La cuenta del grupo tiene por titular al GRUPO y nunca al organizador (R-GRP-04): si fuera del organizador, la plata del grupo estaria a nombre de una persona.
    //
    //Future<SalidaGrupo> crearGrupo(String idempotencyKey, EntradaGrupo entradaGrupo) async
    test('test crearGrupo', () async {
      // TODO
    });

    // Invitar a un contacto, con token de un solo uso
    //
    // Si el destinatario esta suprimido NO se envia nada y se responde como si hubiera salido bien: decir «esa persona pidio no recibir mensajes» ya cuenta algo de ella a quien no tiene por que saberlo. El mensaje dice quien invita y a que grupo, con el monto y la periodicidad — nadie deberia aceptar un compromiso de dinero sin saber cual— y no revela datos de los otros integrantes. El token lo emite `identidad`: aca se recibe su identificador.
    //
    //Future<SalidaInvitacion> invitarAlGrupo(String grupoId, String idempotencyKey, EntradaInvitacion entradaInvitacion) async
    test('test invitarAlGrupo', () async {
      // TODO
    });

    // Postular a un grupo, con el motivo legible del puntaje
    //
    // El postulante ve POR QUE se le propuso ese grupo —monto compatible, gente de su zona, riesgo parecido— y nunca los datos de los demas. Un puntaje sin explicacion es un numero que nadie puede discutir, y un emparejamiento que nadie puede discutir es uno que se acepta por resignacion. Los pesos del criterio son datos CON VIGENCIA, no constantes: hay que poder explicar con que pesos se armo un grupo de hace seis meses.
    //
    //Future<SalidaPostulacion> postularAlGrupo(String grupoId, String idempotencyKey, EntradaPostulacion entradaPostulacion) async
    test('test postularAlGrupo', () async {
      // TODO
    });

    // Proponer un acuerdo y abrir su votación
    //
    // Ningun efecto colectivo se ejecuta sin acuerdo aprobado, y el efecto va en la MISMA transaccion que la aprobacion: si el efecto falla, el acuerdo no queda aprobado sin su efecto — que seria un grupo convencido de que decidio algo que no ocurrio.
    //
    //Future<SalidaAcuerdo> proponerAcuerdo(String idempotencyKey, EntradaAcuerdo entradaAcuerdo) async
    test('test proponerAcuerdo', () async {
      // TODO
    });

    // A que persona apunta un alias
    //
    // La pregunta `nucleo-financiero` para transferir a un alias (CU-12). El alias vive en `grupos.participante` y ese esquema no se lee desde afuera (invariante 11). Devuelve el identificador de la persona y nada mas — ni el nombre, ni el telefono, ni la cuenta. La cuenta la resuelve quien la posee: `grupos` no conoce `nucleo_financiero.cuenta_billetera`, y esta ruta no es un directorio.
    //
    //Future<AliasResuelto> resolverAlias(String alias) async
    test('test resolverAlias', () async {
      // TODO
    });

    // Revelar la semilla y crear los turnos
    //
    // Verifica que SHA256(semilla || entropias) sea el hash publicado y recien entonces crea los turnos, todo en una transaccion: o hay turnos completos o no hay ninguno. Si el hash no cierra, el sorteo queda ANULADO — jamas se publica un resultado que no verifica.
    //
    //Future<RevelacionDeSorteo> revelarSorteo(String grupoId, String idempotencyKey, EntradaRevelacion entradaRevelacion) async
    test('test revelarSorteo', () async {
      // TODO
    });

    // Solicitar la permuta de dos turnos
    //
    // Sin aceptacion de la contraparte no hay nada: la solicitud nace PENDIENTE. Si alguna parte tiene deuda no se permuta — adelantar el cobro de un moroso lo paga el grupo entero.
    //
    //Future<SalidaPermuta> solicitarPermuta(String idempotencyKey, EntradaPermuta entradaPermuta) async
    test('test solicitarPermuta', () async {
      // TODO
    });

    // Solicitar el retiro y ver la posición exacta antes de confirmar
    //
    // El numero se calcula y se MUESTRA ANTES de confirmar, con su desglose. Un retiro donde el monto aparece despues es un retiro que alguien va a discutir, y con razon. Acreedora sin reemplazo se liquida AL CIERRE DEL CICLO, no antes: sacar plata de la bolsa a mitad de camino perjudica a los que faltan cobrar.
    //
    //Future<SalidaRetiro> solicitarRetiro(String grupoId, String idempotencyKey, EntradaRetiro entradaRetiro) async
    test('test solicitarRetiro', () async {
      // TODO
    });

    // Traspasar un cupo a otro participante
    //
    // El TURNO no se toca: la posicion en el calendario es del cupo, no de la persona. Si se moviera, traspasar seria una forma de adelantar el propio turno y el sorteo dejaria de significar nada. Y la deuda no viaja con el cupo: las obligaciones vencidas se quedan con quien las genero. Sin eso, un cupo seria una forma de venderle a alguien la deuda de otro.
    //
    //Future<SalidaTraspaso> traspasarCupo(String grupoId, String cupoId, String idempotencyKey, EntradaTraspaso entradaTraspaso) async
    test('test traspasarCupo', () async {
      // TODO
    });

    // Emitir el voto, con el peso de los cupos
    //
    // El afectado por el acuerdo vota y su voto QUEDA REGISTRADO, como abstencion con peso cero. Impedirle votar lo dejaria sin constancia de que se presento; dejarlo ponderar convertiria el quorum en una formalidad en los dos casos donde mas importa.
    //
    //Future<SalidaAcuerdo> votarAcuerdo(String acuerdoId, String idempotencyKey, EntradaVoto entradaVoto) async
    test('test votarAcuerdo', () async {
      // TODO
    });

  });
}
