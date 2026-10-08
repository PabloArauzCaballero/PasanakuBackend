import 'package:test/test.dart';
import 'package:aportaya_cliente_organizador/aportaya_cliente_organizador.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteOrganizador().getDefaultApi();

  group(DefaultApi, () {
    // Encender la regla
    //
    // Se revuelve a comprobar la coherencia y la prioridad: dos reglas activas con el mismo disparador y prioridad hacen que el orden de ejecucion dependa de como la base devuelva las filas.
    //
    //Future<SalidaRegla> activarRegla(String reglaId) async
    test('test activarRegla', () async {
      // TODO
    });

    // Registrar el resultado de una ejecucion
    //
    // La accion real la ejecuta el servicio dueno del efecto; aca se registra que paso. Ejecutarla desde este servicio seria escribir esquemas ajenos (invariante 11).
    //
    //Future<SalidaEjecucion> anotarEjecucion(String tareaId, EntradaEjecucion entradaEjecucion) async
    test('test anotarEjecucion', () async {
      // TODO
    });

    // Presentar la apelacion
    //
    // La sancion pasa a APELADA y no se puede dar por cumplida. La FILA de la apelacion se escribe al resolverla: la base no admite una apelacion abierta (hueco H-7 del informe del carril).
    //
    //Future<SalidaApelacion> apelarSancion(String sancionId, EntradaApelacion entradaApelacion) async
    test('test apelarSancion', () async {
      // TODO
    });

    // Aprobar la postulacion y crear al organizador
    //
    // Quien aprueba no puede ser quien postulo (R-SEG-04). El organizador nace en CAPACITACION_PENDIENTE: aprobar y entregar el acceso al fondo en el mismo acto saltaria el paso donde se le explica que responsabilidad esta tomando.
    //
    //Future<SalidaHabilitacion> aprobarPostulacion(String solicitudId, EntradaAprobacion entradaAprobacion) async
    test('test aprobarPostulacion', () async {
      // TODO
    });

    // La confirmacion humana que exige una accion sensible
    //
    //Future<SalidaProgramacion> aprobarTarea(String tareaId) async
    test('test aprobarTarea', () async {
      // TODO
    });

    // Si el organizador esta habilitado, y hasta donde
    //
    // La pregunta CU-20 antes de dejar crear un grupo: un organizador suspendido no abre pasanakus nuevos. `grupos` no puede leer este esquema (invariante 11). Devuelve tambien los limites del nivel porque quien crea el grupo los necesita en el mismo momento, y dos viajes para dos datos del mismo renglon es un viaje de mas.
    //
    //Future<Habilitacion> consultarHabilitacion(String organizadorId) async
    test('test consultarHabilitacion', () async {
      // TODO
    });

    // Definir una regla de automatizacion
    //
    // Las acciones que mueven plata ajena exigen confirmacion humana (R-ORG-06). La regla nace INACTIVA: publicarla y encenderla en el mismo acto no deja momento para revisar que la condicion diga lo que se cree que dice.
    //
    //Future<SalidaRegla> definirRegla(EntradaRegla entradaRegla) async
    test('test definirRegla', () async {
      // TODO
    });

    // Emitir el contrato de organizador
    //
    // Un solo contrato vigente por organizador (R-ORG-02). Dos permiten invocar el texto que mas convenga ante un incumplimiento.
    //
    //Future<SalidaContrato> emitirContrato(String organizadorId, EntradaContrato entradaContrato) async
    test('test emitirContrato', () async {
      // TODO
    });

    // Evaluar el desempeno de un periodo
    //
    // Una evaluacion por organizador y periodo (R-ORG-04). El puntaje sale de metricas con su peso, y las metricas se guardan: si alguien va a perder su habilitacion por un numero, ese numero tiene que poder abrirse. El descenso se aplica; el ascenso queda SUGERIDO.
    //
    //Future<SalidaEvaluacion> evaluarDesempeno(String organizadorId, EntradaEvaluacion entradaEvaluacion) async
    test('test evaluarDesempeno', () async {
      // TODO
    });

    // Firmar el contrato
    //
    // Lo firma el propio organizador: una firma puesta por otro no obliga a nadie. Firmar dos veces no sobrescribe la evidencia de cuando y con que token se firmo.
    //
    //Future<SalidaFirma> firmarContrato(String contratoId, FirmarContratoRequest firmarContratoRequest) async
    test('test firmarContrato', () async {
      // TODO
    });

    // Habilitar tras completar la capacitacion
    //
    //Future<SalidaHabilitacion> habilitarOrganizador(String organizadorId) async
    test('test habilitarOrganizador', () async {
      // TODO
    });

    // Postular para administrar grupos
    //
    // Un organizador administra la plata de otros: la habilitacion es la ultima puerta antes de que alguien tenga acceso al fondo de un grupo entero. Los requisitos son catalogo, y un obligatorio SIN DATO no se da por cumplido.
    //
    //Future<SalidaPostulacion> postularOrganizador(EntradaPostulacion entradaPostulacion) async
    test('test postularOrganizador', () async {
      // TODO
    });

    // Programar una tarea automatizada
    //
    // Una tarea por clave de idempotencia (R-ORG-07). La clave es determinista —regla, grupo y minuto—: si dependiera del reloj de la corrida, cada reintento del planificador generaria una tarea nueva.
    //
    //Future<SalidaProgramacion> programarTarea(EntradaProgramacion entradaProgramacion) async
    test('test programarTarea', () async {
      // TODO
    });

    // Rescindir el contrato
    //
    // No se rescinde con grupos en curso: irse dejando participantes a mitad del ciclo es lo que el contrato existe para que no pase sin consecuencias.
    //
    //Future<SalidaRescision> rescindirContrato(String contratoId, RescindirContratoRequest rescindirContratoRequest) async
    test('test rescindirContrato', () async {
      // TODO
    });

    // Resolver la apelacion
    //
    // La resuelve quien NO aplico la sancion (R-ORG-05). Sin eso, apelar es pedirle a la misma persona que se desdiga.
    //
    //Future<SalidaResolucion> resolverApelacion(String sancionId, EntradaResolucion entradaResolucion) async
    test('test resolverApelacion', () async {
      // TODO
    });

    // Aplicar una sancion
    //
    // Sin motivo escrito no hay sancion: «bajo desempeno» a secas no se puede apelar porque no dice contra que defenderse. El plazo para apelar se calcula al sancionar y no se recalcula despues.
    //
    //Future<SalidaSancion> sancionarOrganizador(String organizadorId, EntradaSancion entradaSancion) async
    test('test sancionarOrganizador', () async {
      // TODO
    });

  });
}
