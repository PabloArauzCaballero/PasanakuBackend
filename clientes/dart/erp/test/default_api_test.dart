import 'package:test/test.dart';
import 'package:aportaya_cliente_erp/aportaya_cliente_erp.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteErp().getDefaultApi();

  group(DefaultApi, () {
    // CU-104 · Abre la cuenta por cobrar de un tercero
    //
    // Exige CONTABILIDAD_ERP_COBRAR.
    //
    //Future<SalidaCuentaPorCobrar> abrirCuentaPorCobrar(String idempotencyKey, EntradaCuentaPorCobrar entradaCuentaPorCobrar) async
    test('test abrirCuentaPorCobrar', () async {
      // TODO
    });

    // CU-100 · Abre el ejercicio y sus doce periodos
    //
    // Exige CONTABILIDAD_ERP_CERRAR.
    //
    //Future<SalidaEjercicio> abrirEjercicioFiscal(String idempotencyKey, EntradaEjercicio entradaEjercicio) async
    test('test abrirEjercicioFiscal', () async {
      // TODO
    });

    // CU-102 · Aprueba la orden y habilita facturarla
    //
    // Exige CONTABILIDAD_ERP_COMPRAS.
    //
    //Future<SalidaOrden> aprobarOrdenDeCompra(String ordenId, String idempotencyKey) async
    test('test aprobarOrdenDeCompra', () async {
      // TODO
    });

    // CU-101 · Aprueba el presupuesto y lo deja vigente
    //
    // Exige CONTABILIDAD_ERP_PRESUPUESTO.
    //
    //Future<SalidaPresupuesto> aprobarPresupuesto(String presupuestoId, String idempotencyKey) async
    test('test aprobarPresupuesto', () async {
      // TODO
    });

    // CU-100 · Cierra el mes contra su cuadre
    //
    // Exige CONTABILIDAD_ERP_CERRAR. El cierre es irreversible.
    //
    //Future<SalidaCierre> cerrarPeriodoContable(String periodoId, String idempotencyKey, EntradaCierre entradaCierre) async
    test('test cerrarPeriodoContable', () async {
      // TODO
    });

    // CU-105 · Corre la depreciacion de todos los activos del mes
    //
    // Exige CONTABILIDAD_ERP_ACTIVOS_FIJOS. Es idempotente: los activos ya corridos se informan en `yaCorridos` y no se vuelven a depreciar.
    //
    //Future<SalidaCorrida> correrDepreciacionDelPeriodo(String periodoId, String idempotencyKey) async
    test('test correrDepreciacionDelPeriodo', () async {
      // TODO
    });

    // CU-102 · Crea la orden de compra en BORRADOR
    //
    // Exige CONTABILIDAD_ERP_COMPRAS.
    //
    //Future<SalidaOrden> crearOrdenDeCompra(String idempotencyKey, EntradaOrden entradaOrden) async
    test('test crearOrdenDeCompra', () async {
      // TODO
    });

    // CU-101 · Presupuesta un centro de costo para el ejercicio
    //
    // Exige CONTABILIDAD_ERP_PRESUPUESTO.
    //
    //Future<SalidaPresupuesto> crearPresupuesto(String idempotencyKey, EntradaPresupuesto entradaPresupuesto) async
    test('test crearPresupuesto', () async {
      // TODO
    });

    // CU-102 · Da de alta un tercero comercial
    //
    // Exige CONTABILIDAD_ERP_COMPRAS. Reintentar con el mismo documento devuelve el tercero que ya hay; con otro tipo devuelve AP-CU102-02.
    //
    //Future<SalidaTercero> darDeAltaTercero(String idempotencyKey, EntradaTercero entradaTercero) async
    test('test darDeAltaTercero', () async {
      // TODO
    });

    // CU-105 · Calcula la cuota del activo en el periodo
    //
    // Exige CONTABILIDAD_ERP_ACTIVOS_FIJOS.
    //
    //Future<SalidaDepreciacion> depreciarActivo(String activoId, String idempotencyKey, EntradaDepreciacion entradaDepreciacion) async
    test('test depreciarActivo', () async {
      // TODO
    });

    // CU-106 · Genera el balance o el estado de resultados del mes
    //
    // Exige CONTABILIDAD_ERP_REPORTES.
    //
    //Future<SalidaEstadoFinanciero> generarEstadoFinanciero(String periodoId, String idempotencyKey, EntradaEstadoFinanciero entradaEstadoFinanciero) async
    test('test generarEstadoFinanciero', () async {
      // TODO
    });

    // CU-103 · Paga, total o parcialmente, una factura aprobada
    //
    // Exige CONTABILIDAD_ERP_PAGAR. Quien aprobo la factura no puede autorizar su pago (R-CTB-05).
    //
    //Future<SalidaFactura> pagarFacturaDeProveedor(String facturaId, String idempotencyKey, EntradaPago entradaPago) async
    test('test pagarFacturaDeProveedor', () async {
      // TODO
    });

    // CU-104 · Registra un cobro contra la cuenta
    //
    // Exige CONTABILIDAD_ERP_COBRAR.
    //
    //Future<SalidaCobro> registrarCobro(String cuentaId, String idempotencyKey, EntradaCobro entradaCobro) async
    test('test registrarCobro', () async {
      // TODO
    });

    // CU-103 · Registra la factura del proveedor
    //
    // Exige CONTABILIDAD_ERP_CUENTAS_POR_PAGAR. La aprobacion es un dato del alta: `factura_proveedor` es append-only y no admite aprobarse despues.
    //
    //Future<SalidaFactura> registrarFacturaDeProveedor(String idempotencyKey, EntradaFactura entradaFactura) async
    test('test registrarFacturaDeProveedor', () async {
      // TODO
    });

    // CU-106 · Comprueba que la plantilla no apunte a sumarizadoras
    //
    // Exige CONTABILIDAD_ERP_REPORTES. Sin efecto, asi que no exige clave.
    //
    //Future<SalidaPlantilla> validarPlantillaDeAsiento(String plantillaId) async
    test('test validarPlantillaDeAsiento', () async {
      // TODO
    });

  });
}
