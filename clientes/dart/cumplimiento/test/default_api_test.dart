import 'package:test/test.dart';
import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';


/// tests for DefaultApi
void main() {
  final instance = AportayaClienteCumplimiento().getDefaultApi();

  group(DefaultApi, () {
    // Si un destino pidio no recibir mas
    //
    // La pregunta CU-69 de `grupos` antes de invitar: a quien se dio de baja no se le escribe, aunque quien invita no lo sepa. La lista de supresion es de este servicio y nadie mas la puede leer (invariante 11). Se mira en los tres canales encendidos y alcanza con que uno este suprimido: quien pidio no recibir por el canal que usariamos, pidio no recibir.
    //
    //Future<Supresion> consultarSupresion(String identificador, String categoria) async
    test('test consultarSupresion', () async {
      // TODO
    });

    // Emite un aviso por los canales habilitados del destinatario
    //
    // Los canales por defecto son IN_APP, PUSH y CORREO (ADR-035). WhatsApp, SMS y voz estan APAGADOS: no se usan en un flujo nuevo. El servicio decide el canal segun consentimiento y supresion; el emisor no lo elige.
    //
    //Future<SalidaNotificacion> emitirNotificacion(String idempotencyKey, EntradaNotificacion entradaNotificacion) async
    test('test emitirNotificacion', () async {
      // TODO
    });

    // Procesar una respuesta entrante
    //
    // La unica puerta que abre alguien de afuera. Verifica la firma ANTES de tocar la base, y no revela nada: un remitente desconocido recibe la misma respuesta que un mensaje duplicado. Contestar «ese numero no existe» convertiria este webhook en un directorio de clientes.
    //
    //Future<SalidaRespuesta> procesarRespuestaEntrante(String proveedor, EntradaRespuesta entradaRespuesta) async
    test('test procesarRespuestaEntrante', () async {
      // TODO
    });

  });
}
