import 'package:test/test.dart';
import 'package:aportaya_cliente_auditoria/aportaya_cliente_auditoria.dart';

// tests for SalidaReporte
void main() {
  final SalidaReporte? instance = /* SalidaReporte(...) */ null;
  // TODO add properties to the entity

  group(SalidaReporte, () {
    // String ejecucionId
    test('to test the property `ejecucionId`', () async {
      // TODO
    });

    // String estado
    test('to test the property `estado`', () async {
      // TODO
    });

    // Cero es un resultado valido, no un error.
    // int filasGeneradas
    test('to test the property `filasGeneradas`', () async {
      // TODO
    });

    // SHA-256 de la forma canonica del resultado. Es lo que permite probar que el archivo que alguien presenta es el que salio de aca.
    // String hashResultado
    test('to test the property `hashResultado`', () async {
      // TODO
    });

    // Presente solo si se pidio formato. El archivo se cifra y caduca cuando la definicion tiene datos sensibles; no lo elige quien exporta.
    // String exportacionId
    test('to test the property `exportacionId`', () async {
      // TODO
    });

  });
}
