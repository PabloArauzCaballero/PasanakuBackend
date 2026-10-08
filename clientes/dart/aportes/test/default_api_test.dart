import 'package:test/test.dart';
import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteAportes().getDefaultApi();

  group(DefaultApi, () {
    // Aprobar y ejecutar un reembolso solicitado
    //
    // Quien aprueba no puede ser quien solicito. Al ejecutar, la obligacion vuelve a estar pendiente por ese importe: si quedara dada por pagada, el faltante lo absorben los demas del grupo sin enterarse.
    //
    //Future<SalidaEjecucionReembolso> aprobarReembolso(String idempotencyKey, String reembolsoId) async
    test('test aprobarReembolso', () async {
      // TODO
    });

    // Acreditar el pago de una obligacion de aporte
    //
    // El dinero lo mueve nucleo-financiero (invariante 12): esta operacion aplica el pago a la obligacion y pide el movimiento por evento. La clave de idempotencia se valida ANTES de escribir.
    //
    //Future<SalidaCobro> cobrarAporte(String idempotencyKey, String obligacionId, EntradaCobro entradaCobro) async
    test('test cobrarAporte', () async {
      // TODO
    });

    // Si esta al dia, cuanto aporto y cuanto debe
    //
    // La consulta que hace posible que otros servicios decidan sin leer este esquema (invariante 11). `grupos` la usa antes de permitir una permuta o un traspaso; `nucleo-financiero`, antes de cerrar una billetera. Sin ella, el dato lo tendria que afirmar quien pide — y el que pide seria juez de si esta al dia. Quien no tiene ninguna obligacion esta al dia: no deber nada porque todavia no se le pidio nada no es estar en mora.
    //
    //Future<EstadoDelParticipante> consultarEstadoDelParticipante(String participanteId) async
    test('test consultarEstadoDelParticipante', () async {
      // TODO
    });

    // Cuantos participantes del grupo estan en mora
    //
    // La usa CU-68 para medir la concentracion de riesgo antes de aceptar a alguien nuevo: un grupo con la mitad en mora no admite mas gente, la ayuda.
    //
    //Future<Morosos> contarMorosos(String grupoId) async
    test('test contarMorosos', () async {
      // TODO
    });

    // Dar de alta un proveedor de pago
    //
    // La columna referencia_credenciales guarda una REFERENCIA al almacen de secretos, nunca el secreto. Se comprueba antes de tocar la base: una clave que llega a la fila ya se filtro aunque la transaccion se revierta.
    //
    //Future<SalidaProveedor> darDeAltaProveedor(String idempotencyKey, EntradaProveedor entradaProveedor) async
    test('test darDeAltaProveedor', () async {
      // TODO
    });

    // Elegir por que proveedor cobrar una orden
    //
    // No escribe. Al conmutar de proveedor se conserva la MISMA clave de idempotencia: si el primero cobro y no alcanzo a responder, cambiarla es exactamente como se cobra dos veces.
    //
    //Future<SalidaEnrutamiento> enrutarCobro(EntradaEnrutamiento entradaEnrutamiento) async
    test('test enrutarCobro', () async {
      // TODO
    });

    // Registrar una disputa recibida del proveedor
    //
    // El plazo de respuesta se calcula al ingresar y queda GUARDADO (R-CON-01, invariante 8): recalcularlo al consultar es un argumento que el proveedor no acepta.
    //
    //Future<SalidaDisputa> registrarDisputa(String pagoId, EntradaDisputa entradaDisputa) async
    test('test registrarDisputa', () async {
      // TODO
    });

    // Solicitar el reembolso de un pago
    //
    // Solo SOLICITA. Ejecutar exige una segunda persona (R-SEG-04): un reembolso es la via mas corta para sacar dinero con una excusa verosimil.
    //
    //Future<SalidaReembolso> solicitarReembolso(String idempotencyKey, String pagoId, EntradaReembolso entradaReembolso) async
    test('test solicitarReembolso', () async {
      // TODO
    });

  });
}
