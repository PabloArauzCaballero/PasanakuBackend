import 'package:test/test.dart';
import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteTransparencia().getDefaultApi();

  group(DefaultApi, () {
    // Cerrar una alerta con su desenlace
    //
    // Cerrar exige desenlace: una alerta que se cierra sin decir si era cierta no deja calibrar nada, y el modelo se queda con el mismo error para siempre.
    //
    //Future cerrarAlertaRiesgo(String alertaId, EntradaCierreAlerta entradaCierreAlerta) async
    test('test cerrarAlertaRiesgo', () async {
      // TODO
    });

    // Compensar un evento que sumo o resto puntos
    //
    // No se borra el original ni se lo edita: evento_reputacion es append-only. Entra una fila nueva con el impacto exacto invertido y `revertidoPorId` apuntando al original.
    //
    //Future<SalidaEventoReputacion> compensarEventoReputacion(String eventoId, EntradaCompensacion entradaCompensacion) async
    test('test compensarEventoReputacion', () async {
      // TODO
    });

    // El puntaje de reputacion de alguien
    //
    // Lo preguntan CU-64 y CU-68 de `grupos`: aceptar a alguien o dejarle tomar un cupo depende de su historial, y el historial es de este servicio. Quien todavia no tiene puntaje NO tiene cero. Cero seria el peor puntaje posible y quien recien llega no hizo nada malo: se responde SIN_HISTORIAL y quien pregunta decide. Confundir «no se sabe» con «malo» le cierra la puerta a todo el que empieza.
    //
    //Future<PuntajeDelUsuario> consultarPuntaje(String usuarioId) async
    test('test consultarPuntaje', () async {
      // TODO
    });

    // Emitir un certificado de reputacion verificable
    //
    // El titular elige que incluir campo por campo, y lo que no eligio no entra ni al hash. Solo el titular emite el suyo (R-SEG-03).
    //
    //Future<SalidaCertificado> emitirCertificado(String idempotencyKey, EntradaCertificado entradaCertificado) async
    test('test emitirCertificado', () async {
      // TODO
    });

    // Evaluar y otorgar las insignias que un hecho desbloqueo
    //
    // NO hay otorgamiento manual. Se gana contra los datos o no se gana: una insignia que alguien puede conceder a dedo no dice nada de quien la lleva.
    //
    //Future<List<SalidaInsignia>> evaluarInsignias(String idempotencyKey, EntradaEvaluacionInsignias entradaEvaluacionInsignias) async
    test('test evaluarInsignias', () async {
      // TODO
    });

    // Anticipar el riesgo con alertas tempranas
    //
    // Sin historial el nivel es SIN_DATOS, nunca riesgo alto. Al participante nunca se le muestra un puntaje: el mensaje habla de hechos concretos, con numeros, y de que puede hacer.
    //
    //Future<SalidaEvaluacionRiesgo> evaluarRiesgo(String idempotencyKey, EntradaEvaluacionRiesgo entradaEvaluacionRiesgo) async
    test('test evaluarRiesgo', () async {
      // TODO
    });

    // Los bloques publicados de un grupo
    //
    // Ruta PUBLICA, sin sesion. Es el paquete que cualquiera descarga para verificar por fuera.
    //
    //Future<List<BloquePublicado>> listarBloques(String grupoId) async
    test('test listarBloques', () async {
      // TODO
    });

    // Moderar una resena
    //
    // Nada se publica sin moderar. Lo que trae datos personales se retiene, no se recorta: recortar deja el dato en la base y la sospecha en el aire.
    //
    //Future<SalidaResena> moderarResena(String resenaId, EntradaModeracion entradaModeracion) async
    test('test moderarResena', () async {
      // TODO
    });

    // Resenar a un participante
    //
    // Solo resena quien convivio y una vez por autor, evaluado, grupo y dimension (R-REP-06). Se resena al cerrar el ciclo, no durante: resenar a quien todavia te debe plata convierte la resena en una herramienta de presion.
    //
    //Future<SalidaResena> publicarResena(String idempotencyKey, EntradaResena entradaResena) async
    test('test publicarResena', () async {
      // TODO
    });

    // Recalcular el puntaje de reputacion
    //
    // Un solo puntaje vigente por usuario (R-REP-02) y el total es la suma de sus componentes (R-REP-03). Con menos eventos que el minimo del modelo el nivel es SIN_HISTORIAL: no es un castigo, es decir la verdad.
    //
    //Future<SalidaPuntaje> recalcularPuntaje(String idempotencyKey, EntradaRecalculo entradaRecalculo) async
    test('test recalcularPuntaje', () async {
      // TODO
    });

    // Registrar un evento de reputacion
    //
    // Un evento por hecho (R-REP-01). El impacto sale del catalogo del modelo, no de constantes: sin regla no se puntua, porque inventarle un impacto a un tipo de evento es decidir a mano cuanto vale la conducta de alguien.
    //
    //Future<SalidaEventoReputacion> registrarEventoReputacion(String idempotencyKey, EntradaEventoReputacion entradaEventoReputacion) async
    test('test registrarEventoReputacion', () async {
      // TODO
    });

    // Revocar un certificado
    //
    // Lo usa tambien el derecho de supresion (CU-07): al suprimir, la URL deja de resolver.
    //
    //Future revocarCertificado(String codigo, EntradaRevocacionCertificado entradaRevocacionCertificado) async
    test('test revocarCertificado', () async {
      // TODO
    });

    // Revocar una insignia, sin borrarla
    //
    // Revocar no borra (R-REP-05). Queda la fila, la fecha y el motivo: borrarla dejaria a la persona sin poder saber por que perdio algo que tenia.
    //
    //Future<SalidaInsignia> revocarInsignia(String otorgadaId, EntradaRevocacionInsignia entradaRevocacionInsignia) async
    test('test revocarInsignia', () async {
      // TODO
    });

    // Sellar el bloque de transparencia
    //
    // Con excepciones de conciliacion abiertas NO se sella (R-BIL-12). Un hecho ya sellado no se reescribe: la correccion entra al bloque siguiente como movimiento compensatorio y los dos quedan visibles.
    //
    //Future<SalidaSellado> sellarBloque(String idempotencyKey, EntradaSellado entradaSellado) async
    test('test sellarBloque', () async {
      // TODO
    });

    // Congelar una foto del puntaje
    //
    //Future<SalidaSnapshot> tomarSnapshotReputacion(EntradaSnapshot entradaSnapshot) async
    test('test tomarSnapshotReputacion', () async {
      // TODO
    });

    // Verificar la cadena de transparencia
    //
    // Ruta PUBLICA, sin sesion. Recorre los bloques y devuelve el primero que falla y que componente no cuadra. Un grupo sin bloques no es un error de integridad.
    //
    //Future<SalidaVerificacionCadena> verificarCadena(String grupoId) async
    test('test verificarCadena', () async {
      // TODO
    });

    // Verificar un certificado por su codigo
    //
    // Ruta PUBLICA, sin sesion. Un codigo inexistente responde igual que uno revocado: NO_VALIDO, sin fechas. Distinguirlos le diria a quien prueba codigos al azar cuando acerto.
    //
    //Future<SalidaVerificacionCertificado> verificarCertificado(String codigo) async
    test('test verificarCertificado', () async {
      // TODO
    });

    // Verificar publicamente el sorteo
    //
    // Ruta PUBLICA, sin sesion. Recomputa el compromiso y el barajado con el mismo atomo que uso CU-60 para sortear: una sola implementacion para generar y para verificar. Antes del revelado no hay nada que verificar, y ese es el punto.
    //
    //Future<SalidaVerificacionSorteo> verificarSorteo(String sorteoId) async
    test('test verificarSorteo', () async {
      // TODO
    });

  });
}
