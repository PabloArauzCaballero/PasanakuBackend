import 'package:test/test.dart';
import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteNucleofinanciero().getDefaultApi();

  group(DefaultApi, () {
    // Acredita una orden que el proveedor confirmo
    //
    // [CONTRATO] La confirmacion del proveedor. Es idempotente por la orden: si dos confirmaciones llegan juntas, una acredita y la otra encuentra la orden ya acreditada — la red duplica, y eso no puede duplicar el saldo.
    //
    //Future<SalidaAcreditacion> acreditarRecarga(String ordenId, String idempotencyKey) async
    test('test acreditarRecarga', () async {
      // TODO
    });

    // Registrar un bloqueo por orden de autoridad
    //
    // El unico caso en que una retencion no tiene fecha de fin: la levanta la misma autoridad que la puso, y ponerle vencimiento propio seria decidir por un juez cuando se acaba su orden. El documento de respaldo es obligatorio — inmovilizar la plata de alguien sin poder mostrar el papel es indefendible.
    //
    //Future<SalidaBloqueo> bloquearPorAutoridad(String idempotencyKey, EntradaBloqueo entradaBloqueo) async
    test('test bloquearPorAutoridad', () async {
      // TODO
    });

    // Libera o ejecuta una retencion
    //
    // [CONTRATO] Una retencion se cierra una sola vez. Si otra operacion la cerro primero, la segunda no la vuelve a cerrar: se responde `RETENCION_YA_EJECUTADA`, que no es una falla sino concurrencia sana.
    //
    //Future<SalidaCierreRetencion> cerrarRetencion(String retencionId, String idempotencyKey, EntradaCierreRetencion entradaCierreRetencion) async
    test('test cerrarRetencion', () async {
      // TODO
    });

    // Saldo de una cuenta de billetera
    //
    // El saldo NO se guarda: se deriva del libro. Esta operacion existe en el borrador porque los trece servicios necesitan saber que la forma del saldo es Dinero, no un number.
    //
    //Future<SaldoBilletera> consultarSaldo(String cuentaId) async
    test('test consultarSaldo', () async {
      // TODO
    });

    // Emitir el extracto de un periodo
    //
    // Si el extracto no cuadra con el cierre diario, NO se emite: uno con una cifra que no coincide es peor que no tenerlo — la persona lo usa para un tramite, se lo rechazan, y descubre el error cuando ya le costo algo. El descuadre queda registrado igual, que es lo que pide el criterio.
    //
    //Future<SalidaExtracto> emitirExtracto(String cuentaId, DateTime desde, DateTime hasta) async
    test('test emitirExtracto', () async {
      // TODO
    });

    // Aparta saldo sin sacarlo de la cuenta
    //
    // [CONTRATO] Retener no es debitar: el dinero sigue siendo del titular y por eso se ve como retenido y no desaparece. Lo que cambia es que deja de estar disponible mientras dure el motivo.
    //
    //Future<SalidaRetencion> retenerSaldo(String idempotencyKey, EntradaRetencion entradaRetencion) async
    test('test retenerSaldo', () async {
      // TODO
    });

    // Reversar una transaccion aplicada
    //
    // Reversar es escribir el espejo, no borrar el original. El libro es append-only y la transaccion original conserva sus movimientos sin un solo cambio: el extracto tiene que poder contar la historia completa —el cobro y su devolucion—, no un cobro que se desvanecio. Cuatro ojos: quien autoriza no puede ser quien solicita.
    //
    //Future<SalidaReverso> reversarTransaccion(String idempotencyKey, EntradaReverso entradaReverso) async
    test('test reversarTransaccion', () async {
      // TODO
    });

    // Solicitar el cierre de la billetera
    //
    // Cerrar NO borra: la cuenta queda CERRADA y sus movimientos siguen disponibles todo el plazo de conservacion. Cuatro puertas la frenan, y las cuatro protegen a terceros antes que a la empresa — un cierre que deje un aporte sin pagar traslada la perdida a los otros del pasanaku.
    //
    //Future<SalidaCierreBilletera> solicitarCierreBilletera(String idempotencyKey, EntradaCierre entradaCierre) async
    test('test solicitarCierreBilletera', () async {
      // TODO
    });

    // Abre una orden de recarga y devuelve cuanto se va a acreditar
    //
    // [CONTRATO] Derivada de la implementacion del carril 2A, que llego antes que su contrato. La orden NO acredita: reserva la intencion y vence. Acreditar es el paso siguiente y ocurre cuando el proveedor confirma, porque acreditar contra una promesa es acreditar plata que puede no llegar. `acreditara` viaja en la respuesta ya calculado: el cliente **no** resta el costo del proveedor por su cuenta. Un cliente que recalcula produce un numero que no es el del libro contable.
    //
    //Future<SalidaRecarga> solicitarRecarga(String idempotencyKey, EntradaRecarga entradaRecarga) async
    test('test solicitarRecarga', () async {
      // TODO
    });

    // Solicitar un retiro hacia una cuenta de destino
    //
    // Retencion primero, pago despues, nunca al reves. Si se instruyera el pago antes de retener, entre las dos cosas la persona podria gastar el mismo saldo en otra operacion y el retiro saldria contra un disponible que ya no existe. El proveedor se instruye FUERA de la transaccion (invariante 6).
    //
    //Future<SalidaRetiro> solicitarRetiro(String idempotencyKey, EntradaRetiro entradaRetiro) async
    test('test solicitarRetiro', () async {
      // TODO
    });

    // Transferir saldo entre billeteras
    //
    // La unica operacion donde el dinero no entra ni sale del sistema: cambia de bolsillo, y el saldo total permanece constante. Las dos cuentas se bloquean en orden de identificador — dos transferencias cruzadas que bloqueen cada una primero la suya se esperan para siempre.
    //
    //Future<SalidaTransferencia> transferirSaldo(String idempotencyKey, EntradaTransferencia entradaTransferencia) async
    test('test transferirSaldo', () async {
      // TODO
    });

  });
}
