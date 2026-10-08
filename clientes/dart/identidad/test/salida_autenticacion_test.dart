import 'package:test/test.dart';
import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';

// tests for SalidaAutenticacion
void main() {
  final SalidaAutenticacion? instance = /* SalidaAutenticacion(...) */ null;
  // TODO add properties to the entity

  group(SalidaAutenticacion, () {
    // String sesionId
    test('to test the property `sesionId`', () async {
      // TODO
    });

    // DateTime expiraEn
    test('to test the property `expiraEn`', () async {
      // TODO
    });

    // bool requiereFactorAdicional
    test('to test the property `requiereFactorAdicional`', () async {
      // TODO
    });

    // bool dispositivoConfiable
    test('to test the property `dispositivoConfiable`', () async {
      // TODO
    });

    // El JWT firmado con RS256 que los otros trece servicios validan contra el JWKS de identidad (ADR-024). Ausente cuando la respuesta pide un segundo factor: recien se emite con la sesion abierta.
    // String tokenAcceso
    test('to test the property `tokenAcceso`', () async {
      // TODO
    });

  });
}
