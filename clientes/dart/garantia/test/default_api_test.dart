import 'package:test/test.dart';
import 'package:aportaya_cliente_garantia/aportaya_cliente_garantia.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteGarantia().getDefaultApi();

  group(DefaultApi, () {
    // Aprobar el reemplazo — lo decide el grupo
    //
    //Future<AprobarReemplazo200Response> aprobarReemplazo(String reemplazoId) async
    test('test aprobarReemplazo', () async {
      // TODO
    });

    // Cerrar la disolucion
    //
    // La cuenta del grupo cierra EN CERO (R-GRP-13). Un grupo disuelto con saldo es plata de alguien que quedo sin dueno.
    //
    //Future<SalidaCierreDisolucion> cerrarDisolucion(String disolucionId) async
    test('test cerrarDisolucion', () async {
      // TODO
    });

    // Si alguien esta en la lista de restriccion interna
    //
    // La pregunta CU-68 de `grupos` antes de aceptar a alguien. La lista es de este servicio y nadie mas la puede leer (invariante 11). Devuelve tambien cuanto tendria que pagar para salir: una restriccion sin salida es una condena, y quien consulta necesita poder decirle a la persona que hacer, no solo que no puede entrar.
    //
    //Future<RestriccionVigente> consultarRestriccion(String usuarioId) async
    test('test consultarRestriccion', () async {
      // TODO
    });

    // Cubrir el incumplimiento con el fondo
    //
    // El fondo existe para que el incumplimiento de uno no se lleve puesto al grupo entero. Pero cubrir todo siempre lo VACIA: gana el mas chico de cuatro limites —porcentaje, tope por participante, tope del periodo y saldo—. Y cubrir NO perdona: deja una deuda contra quien incumplio.
    //
    //Future<SalidaCobertura> cubrirIncumplimiento(String expedienteId, EntradaCobertura entradaCobertura) async
    test('test cubrirIncumplimiento', () async {
      // TODO
    });

    // Declarar un incumplimiento con su evidencia
    //
    // Declarar que alguien incumplio tiene consecuencias reales: le bloquea grupos, lo pone en una lista, le genera una deuda. El expediente nace NOTIFICADO y con su plazo de descargo GUARDADO (R-GAR-01) — enterarse despues de que el plazo empezo a correr es no tener plazo. La evidencia es inmutable (R-GAR-02).
    //
    //Future<SalidaDeclaracion> declararIncumplimiento(String idempotencyKey, EntradaDeclaracion entradaDeclaracion) async
    test('test declararIncumplimiento', () async {
      // TODO
    });

    // Devolver los aportes del fondo al cerrar el grupo
    //
    // No se devuelve mas de lo aportado ni un importe negativo (R-GAR-06), y lo que el fondo gasto en cubrir lo pierden TODOS en proporcion: esa es la idea de un fondo mutual. El remanente se ajusta en la ultima devolucion para que la suma cierre exacta contra el saldo.
    //
    //Future<SalidaDevolucionFondo> devolverFondo(String grupoId, EntradaDevolucionFondo entradaDevolucionFondo) async
    test('test devolverFondo', () async {
      // TODO
    });

    // Ejecutar el aval y subrogar la deuda
    //
    // Un avalista acepto responder por una cantidad CONCRETA. Cobrarle mas es cobrarle algo que nunca acepto (R-GAR-04), y el tope se mide contra lo YA ejecutado. Quien paga se subroga: la deuda no desaparece, cambia de acreedor.
    //
    //Future<SalidaEjecucionAval> ejecutarAval(String expedienteId) async
    test('test ejecutarAval', () async {
      // TODO
    });

    // Ejecutar el reemplazo aprobado
    //
    //Future<SalidaReemplazo> ejecutarReemplazo(String reemplazoId) async
    test('test ejecutarReemplazo', () async {
      // TODO
    });

    // Calcular la disolucion anticipada del grupo
    //
    // Es el peor momento posible: unos ya cobraron y otros no, y todos pusieron. La posicion de cada uno es lo aportado menos lo recibido. Repartir sin mirar eso le devolveria a quien ya cobro lo mismo que a quien nunca cobro.
    //
    //Future<SalidaDisolucion> iniciarDisolucion(String grupoId, EntradaDisolucion entradaDisolucion) async
    test('test iniciarDisolucion', () async {
      // TODO
    });

    // Levantar la restriccion, con motivo escrito
    //
    // El levantamiento SE MOTIVA (R-GAR-05): sin motivo escrito se convierte en un favor que nadie puede auditar.
    //
    //Future<LevantarRestriccion200Response> levantarRestriccion(String restriccionId, LevantarRestriccionRequest levantarRestriccionRequest) async
    test('test levantarRestriccion', () async {
      // TODO
    });

    // Presentar el descargo, dentro del plazo
    //
    //Future<SalidaDescargo> presentarDescargo(String expedienteId, EntradaDescargo entradaDescargo) async
    test('test presentarDescargo', () async {
      // TODO
    });

    // Proponer el reemplazo de un participante moroso
    //
    // La deuda del saliente NO se le traslada al entrante salvo que este la asuma expresamente: nadie entra a un grupo a pagar lo que otro dejo sin decirselo. Lo que no asume queda como deuda del saliente — perdonarla se la cobraria a los demas.
    //
    //Future<SalidaReemplazo> proponerReemplazo(String expedienteId, EntradaReemplazo entradaReemplazo) async
    test('test proponerReemplazo', () async {
      // TODO
    });

    // Resolver el descargo
    //
    // Quien resuelve no puede ser el imputado. Aceptarlo cierra el expediente como SUBSANADO y NO habilita la cobertura del fondo.
    //
    //Future<SalidaResolucionDescargo> resolverDescargo(String expedienteId, EntradaResolucionDescargo entradaResolucionDescargo) async
    test('test resolverDescargo', () async {
      // TODO
    });

    // Restringir al deudor
    //
    // Una restriccion vigente por usuario (R-GAR-05). **No le cierra la puerta a pagar ni a ver su estado**: bloquear al deudor de todo, incluido el camino para regularizarse, garantiza que no se regularice.
    //
    //Future<SalidaRestriccion> restringirDeudor(String expedienteId, EntradaRestriccion entradaRestriccion) async
    test('test restringirDeudor', () async {
      // TODO
    });

  });
}
