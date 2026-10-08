import 'package:test/test.dart';
import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';

// tests for Documento
void main() {
  final Documento? instance = /* Documento(...) */ null;
  // TODO add properties to the entity

  group(Documento, () {
    // String tipo
    test('to test the property `tipo`', () async {
      // TODO
    });

    // String numero
    test('to test the property `numero`', () async {
      // TODO
    });

    // La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
    // String lugarExpedicion
    test('to test the property `lugarExpedicion`', () async {
      // TODO
    });

  });
}
