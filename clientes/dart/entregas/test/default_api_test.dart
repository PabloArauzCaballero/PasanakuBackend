import 'package:test/test.dart';
import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteEntregas().getDefaultApi();

  group(DefaultApi, () {
    // Registrar lo que contesto el proveedor
    //
    // La llamada al proveedor la hace quien orquesta, FUERA de esta transaccion (invariante 6). Un error definitivo NO se reintenta: insistir contra una cuenta que no existe solo demora el momento en que alguien mira el caso, mientras la plata del beneficiario sigue retenida.
    //
    //Future<SalidaIntentoDesembolso> anotarRespuestaDesembolso(String ordenId, EntradaRespuestaDesembolso entradaRespuestaDesembolso) async
    test('test anotarRespuestaDesembolso', () async {
      // TODO
    });

    // Autorizar la entrega
    //
    // Una validacion bloqueante sin resolver la frena. Saltearlas para «no demorar la entrega» es como se entrega mal.
    //
    //Future<SalidaAutorizacion> autorizarEntrega(String entregaId) async
    test('test autorizarEntrega', () async {
      // TODO
    });

    // Si la cuenta puede recibir plata ahora, y si no, cuanto falta
    //
    // Se dice CUANTO falta, no solo que no se puede: negar sin explicar convierte una medida de seguridad en una falla del sistema.
    //
    //Future<Disponibilidad> consultarDisponibilidad(String cuentaId) async
    test('test consultarDisponibilidad', () async {
      // TODO
    });

    // Designar la cuenta principal
    //
    // Una sola principal por usuario (R-BIL-17). Dos harian que el destino de un retiro dependiera del orden en que la base devuelve las filas.
    //
    //Future<DesignarCuentaPrincipal200Response> designarCuentaPrincipal(String cuentaId) async
    test('test designarCuentaPrincipal', () async {
      // TODO
    });

    // Ejecutar la entrega
    //
    // Quien autoriza NO ejecuta (R-SEG-04). Una sola persona que hace las dos cosas puede sacar el fondo entero de un grupo sin que nadie mas lo vea pasar.
    //
    //Future<SalidaEjecucionEntrega> ejecutarEntrega(String entregaId, EjecutarEntregaRequest ejecutarEntregaRequest) async
    test('test ejecutarEntrega', () async {
      // TODO
    });

    // Emitir la orden de desembolso
    //
    // Una orden VIVA por entrega (R-DES-01) y ninguna a cuenta sin verificar (R-DES-02). Dos ordenes de la misma entrega es pagar dos veces, y no se descubre hasta que alguien concilia el extracto.
    //
    //Future<SalidaOrdenDesembolso> emitirOrdenDesembolso(String idempotencyKey, EntradaOrdenDesembolso entradaOrdenDesembolso) async
    test('test emitirOrdenDesembolso', () async {
      // TODO
    });

    // Liquidar la entrega del turno
    //
    // Una entrega por turno (R-GRP-01) y el neto NUNCA negativo: que las deducciones superen la bolsa significa que el beneficiario terminaria debiendo por cobrar su turno. Cada deduccion viaja con su tipo y su origen.
    //
    //Future<SalidaLiquidacion> liquidarEntrega(String idempotencyKey, EntradaLiquidacion entradaLiquidacion) async
    test('test liquidarEntrega', () async {
      // TODO
    });

    // Registrar una cuenta bancaria de destino
    //
    // El numero NUNCA se guarda en claro (R-SEG-01): la fila lleva el cifrado, un hash con pimienta para buscar, y una version enmascarada. Y la cuenta esta a nombre de quien la registra (R-SEG-02): una cuenta de destino ajena es la forma mas simple de sacar plata de un grupo hacia afuera.
    //
    //Future<SalidaRegistroCuenta> registrarCuentaDestino(EntradaRegistroCuenta entradaRegistroCuenta) async
    test('test registrarCuentaDestino', () async {
      // TODO
    });

    // Verificar la titularidad de la cuenta
    //
    // Verificada no es utilizable: empieza la ventana de enfriamiento y su fin se GUARDA. Si alguien toma una cuenta ajena, lo primero que hace es cambiar el destino y retirar; la ventana le da al titular real el tiempo de enterarse.
    //
    //Future<SalidaVerificacionCuenta> verificarCuentaDestino(String cuentaId, VerificarCuentaDestinoRequest verificarCuentaDestinoRequest) async
    test('test verificarCuentaDestino', () async {
      // TODO
    });

  });
}
