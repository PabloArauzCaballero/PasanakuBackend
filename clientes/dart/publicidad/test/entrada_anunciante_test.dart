import 'package:test/test.dart';
import 'package:aportaya_cliente_publicidad/aportaya_cliente_publicidad.dart';

// tests for EntradaAnunciante
void main() {
  final EntradaAnunciante? instance = /* EntradaAnunciante(...) */ null;
  // TODO add properties to the entity

  group(EntradaAnunciante, () {
    // String tipo
    test('to test the property `tipo`', () async {
      // TODO
    });

    // Obligatorio si tipo es ORGANIZADOR, y prohibido si no (R-PUB-01).
    // String organizadorId
    test('to test the property `organizadorId`', () async {
      // TODO
    });

    // Obligatorio si tipo es SOCIO_COMERCIAL, y prohibido si no.
    // String socioComercialId
    test('to test the property `socioComercialId`', () async {
      // TODO
    });

    // String razonSocialFacturacion
    test('to test the property `razonSocialFacturacion`', () async {
      // TODO
    });

    // Importe como CADENA decimal. Un number JSON es un doble.
    // String limiteGastoMensual
    test('to test the property `limiteGastoMensual`', () async {
      // TODO
    });

    // String moneda
    test('to test the property `moneda`', () async {
      // TODO
    });

  });
}
