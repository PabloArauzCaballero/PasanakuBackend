import 'package:test/test.dart';
import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';

// tests for ContratoVigente
void main() {
  final ContratoVigente? instance = /* ContratoVigente(...) */ null;
  // TODO add properties to the entity

  group(ContratoVigente, () {
    // String id
    test('to test the property `id`', () async {
      // TODO
    });

    // String codigo
    test('to test the property `codigo`', () async {
      // TODO
    });

    // int version
    test('to test the property `version`', () async {
      // TODO
    });

    // String tipo
    test('to test the property `tipo`', () async {
      // TODO
    });

    // String urlDocumento
    test('to test the property `urlDocumento`', () async {
      // TODO
    });

    // SHA-256 del documento publicado. Es lo que permite comprobar que el texto que se leyo en la app es el mismo que quedo registrado en la aceptacion.
    // String hashDocumento
    test('to test the property `hashDocumento`', () async {
      // TODO
    });

    // DateTime vigenteDesde
    test('to test the property `vigenteDesde`', () async {
      // TODO
    });

    // El numero con el que ASFI registro el contrato de adhesion. Ausente mientras el registro no se haya obtenido: no se inventa.
    // String numeroRegistro
    test('to test the property `numeroRegistro`', () async {
      // TODO
    });

    // DateTime fechaRegistro
    test('to test the property `fechaRegistro`', () async {
      // TODO
    });

  });
}
