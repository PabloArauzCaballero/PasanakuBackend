import 'package:test/test.dart';
import 'package:aportaya_cliente_auditoria/aportaya_cliente_auditoria.dart';

// tests for EntradaDerechos
void main() {
  final EntradaDerechos? instance = /* EntradaDerechos(...) */ null;
  // TODO add properties to the entity

  group(EntradaDerechos, () {
    // String usuarioId
    test('to test the property `usuarioId`', () async {
      // TODO
    });

    // Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
    // String tipo
    test('to test the property `tipo`', () async {
      // TODO
    });

    // String descripcion
    test('to test the property `descripcion`', () async {
      // TODO
    });

    // Desde cuando corre la retencion. Lo aporta quien atiende porque la ultima operacion del titular vive en `nucleo-financiero` y este servicio no la puede consultar. Hueco declarado: deja de ser parametro cuando el contrato de la billetera exponga la fecha.
    // DateTime ultimaActividad
    test('to test the property `ultimaActividad`', () async {
      // TODO
    });

  });
}
