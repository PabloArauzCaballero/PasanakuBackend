import 'package:test/test.dart';
import 'package:aportaya_cliente_publicidad/aportaya_cliente_publicidad.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClientePublicidad().getDefaultApi();

  group(DefaultApi, () {
    // CU-111 · Operaciones aprueba y la campana empieza a entregar
    //
    // Exige PUBLICIDAD_APROBAR_CAMPANA.
    //
    //Future<SalidaCampana> aprobarCampana(String campanaId, String idempotencyKey) async
    test('test aprobarCampana', () async {
      // TODO
    });

    // CU-114 · Lo consumido en un mes, sin emitir nada
    //
    // Exige PUBLICIDAD_ANUNCIANTES. Sin efecto, asi que no exige clave: es lo que mira Contabilidad antes de correr la liquidacion.
    //
    //Future<SalidaConsumo> consumoDelPeriodo(String cuentaId, String periodo) async
    test('test consumoDelPeriodo', () async {
      // TODO
    });

    // CU-111 · Crea la campana con sus conjuntos y la manda a revision
    //
    // Exige PUBLICIDAD_APROBAR_CAMPANA para el anunciante autenticado.
    //
    //Future<SalidaCampana> crearCampana(String idempotencyKey, EntradaCampana entradaCampana) async
    test('test crearCampana', () async {
      // TODO
    });

    // CU-110 · Da de alta el anunciante y abre su cuenta publicitaria
    //
    // Exige PUBLICIDAD_ANUNCIANTES. El anunciante es un organizador o un socio comercial, nunca los dos (R-PUB-01).
    //
    //Future<SalidaAnunciante> darDeAltaAnunciante(String idempotencyKey, EntradaAnunciante entradaAnunciante) async
    test('test darDeAltaAnunciante', () async {
      // TODO
    });

    // CU-113 · Elige que anuncio mostrar y registra su impresion
    //
    // Interno del motor de entrega. Si ningun anuncio tiene presupuesto para hoy, responde 200 con `anuncioId` nulo y el motivo: que el espacio no muestre nada es el comportamiento correcto, no un error.
    //
    //Future<SalidaEntrega> entregarAnuncio(String espacioId, String idempotencyKey, { EntradaEntrega entradaEntrega }) async
    test('test entregarAnuncio', () async {
      // TODO
    });

    // CU-114 · Factura el gasto publicitario del mes
    //
    // Interno (worker mensual). El comprobante fiscal y la cuenta por cobrar viven en otros esquemas: llegan como dato, ya emitidos. La factura es append-only y nace con ellos o no los tiene nunca.
    //
    //Future<SalidaLiquidacion> liquidarPeriodo(String cuentaId, String idempotencyKey, EntradaLiquidacion entradaLiquidacion) async
    test('test liquidarPeriodo', () async {
      // TODO
    });

    // CU-112 · Un moderador aprueba o rechaza la pieza
    //
    // Exige PUBLICIDAD_MODERAR. Quien subio la pieza no la modera (R-PUB-05), y un rechazo exige motivo.
    //
    //Future<SalidaRevision> moderarPiezaCreativa(String piezaId, String idempotencyKey, EntradaRevision entradaRevision) async
    test('test moderarPiezaCreativa', () async {
      // TODO
    });

    // CU-110 · Postula un negocio externo como socio comercial
    //
    // Exige PUBLICIDAD_ANUNCIANTES. Reintentar con el mismo documento devuelve el socio que ya hay.
    //
    //Future<SalidaSocio> postularSocioComercial(String idempotencyKey, EntradaSocio entradaSocio) async
    test('test postularSocioComercial', () async {
      // TODO
    });

    // CU-113 · Programa un anuncio con una pieza aprobada
    //
    // Exige PUBLICIDAD_ANUNCIANTES. Una pieza que no esta APROBADA no entra (R-PUB-04).
    //
    //Future<SalidaAnuncio> programarAnuncio(String idempotencyKey, EntradaAnuncio entradaAnuncio) async
    test('test programarAnuncio', () async {
      // TODO
    });

    // CU-111 · Operaciones rechaza la campana, con motivo
    //
    // Exige PUBLICIDAD_APROBAR_CAMPANA. El motivo viaja en el evento: la tabla no tiene columna donde guardarlo.
    //
    //Future<SalidaCampana> rechazarCampana(String campanaId, String idempotencyKey, EntradaRechazo entradaRechazo) async
    test('test rechazarCampana', () async {
      // TODO
    });

    // CU-113 · Registra el clic sobre una impresion
    //
    // Interno del motor de entrega. En CPC, es el clic el que cuesta.
    //
    //Future<SalidaClic> registrarClic(String impresionId, String idempotencyKey) async
    test('test registrarClic', () async {
      // TODO
    });

    // CU-113 · Registra el hecho de negocio al que llevo el anuncio
    //
    // Interno. Una postulacion a un grupo, un registro o una descarga.
    //
    //Future<SalidaConversion> registrarConversion(String idempotencyKey, EntradaConversion entradaConversion) async
    test('test registrarConversion', () async {
      // TODO
    });

    // CU-112 · El anunciante sube una pieza, que nace PENDIENTE
    //
    // Exige PUBLICIDAD_ANUNCIANTES para el anunciante autenticado.
    //
    //Future<SalidaPieza> subirPiezaCreativa(String idempotencyKey, EntradaPieza entradaPieza) async
    test('test subirPiezaCreativa', () async {
      // TODO
    });

    // CU-110 · Operaciones verifica al socio y lo deja ACTIVO
    //
    // Exige PUBLICIDAD_ANUNCIANTES.
    //
    //Future<SalidaSocio> verificarSocioComercial(String socioId, String idempotencyKey) async
    test('test verificarSocioComercial', () async {
      // TODO
    });

  });
}
