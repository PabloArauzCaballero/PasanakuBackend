import 'package:aportaya_movil/pantallas/pasanaku/dominio/detalle_visible_de_invitacion.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final valido = <String, dynamic>{
    'nombre': 'Grupo de prueba',
    'montoAporte': '25.00',
    'moneda': 'BOB',
    'periodicidad': 'MENSUAL',
    'reglamento': 'Reglamento de prueba',
    'hashReglamento': 'a' * 64,
  };

  test('formatea monto de contrato y traduce periodicidad', () {
    final detalle = DetalleVisibleDeInvitacion.desde(valido);
    expect(detalle.aporteFormateado, 'Bs 25,00');
    expect(detalle.frecuencia, 'Mensual');
    expect(detalle.hashReglamento, 'a' * 64);
  });

  for (final (nombre, cambio) in [
    ('importe JSON numérico', <String, dynamic>{'montoAporte': 25}),
    ('importe sin centavos', <String, dynamic>{'montoAporte': '25'}),
    ('importe negativo', <String, dynamic>{'montoAporte': '-1.00'}),
    ('periodicidad desconocida', <String, dynamic>{'periodicidad': 'DIARIA'}),
    ('hash ausente', <String, dynamic>{'hashReglamento': null}),
  ]) {
    test('rechaza $nombre antes de mostrar compromiso', () {
      expect(
        () => DetalleVisibleDeInvitacion.desde({...valido, ...cambio}),
        throwsFormatException,
      );
    });
  }
}
