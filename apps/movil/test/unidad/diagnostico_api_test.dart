import 'package:flutter/foundation.dart' show debugPrint;
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/dominio/diagnostico_api.dart';

void main() {
  late void Function(String? message, {int? wrapWidth}) debugPrintAnterior;
  late List<String?> mensajes;

  setUp(() {
    debugPrintAnterior = debugPrint;
    mensajes = [];
    debugPrint = (mensaje, {wrapWidth}) => mensajes.add(mensaje);
  });

  tearDown(() => debugPrint = debugPrintAnterior);

  test('canoniza el ID de cuenta y conserva el contexto de soporte', () {
    const cuenta = '550e8400-e29b-41d4-a716-446655440000';

    registrarRechazoApi(
      metodo: 'GET',
      ruta: '/billetera/$cuenta/saldo',
      estado: 403,
      codigo: 'AP-SEG-01',
      traza: 'traza-de-prueba',
    );

    expect(mensajes, hasLength(1));
    expect(
      mensajes.single,
      'API rechazo GET /billetera/{cuentaId}/saldo HTTP 403 '
      'codigo=AP-SEG-01 traza=traza-de-prueba',
    );
    expect(mensajes.single, isNot(contains(cuenta)));
  });

  test('canoniza rutas de grupo y omite por completo las desconocidas', () {
    const grupo = '550e8400-e29b-41d4-a716-446655440001';

    registrarRechazoApi(
      metodo: 'POST',
      ruta: '/grupos/$grupo/participantes',
      estado: 403,
      codigo: 'AP-SEG-01',
      traza: '',
    );
    registrarRechazoApi(
      metodo: 'GET',
      ruta: '/personas/$grupo?documento=1234567',
      estado: 404,
      codigo: 'AP-DESCONOCIDO',
      traza: '',
    );

    expect(mensajes, hasLength(2));
    expect(mensajes.first, contains('POST /grupos/{ruta} HTTP 403'));
    expect(mensajes.first, contains('traza=sin-traza'));
    expect(mensajes.first, isNot(contains(grupo)));
    expect(mensajes.last, contains('GET <ruta-omitida> HTTP 404'));
    expect(mensajes.last, isNot(contains('1234567')));
    expect(mensajes.last, isNot(contains(grupo)));
  });

  test('avisa de un refresh ausente sin incluir datos de la sesión', () {
    registrarAvisoRefrescoAusente();

    expect(mensajes, [
      'El servidor no entregó tokenRefresco para la sesión móvil.',
    ]);
  });
}
