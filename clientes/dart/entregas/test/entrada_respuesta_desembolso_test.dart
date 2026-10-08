import 'package:test/test.dart';
import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';

// tests for EntradaRespuestaDesembolso
void main() {
  final EntradaRespuestaDesembolso? instance = /* EntradaRespuestaDesembolso(...) */ null;
  // TODO add properties to the entity

  group(EntradaRespuestaDesembolso, () {
    // DateTime iniciado
    test('to test the property `iniciado`', () async {
      // TODO
    });

    // bool exitoso
    test('to test the property `exitoso`', () async {
      // TODO
    });

    // obligatoria si fue exitoso — es contra lo que cruza el extracto
    // String referenciaProveedor
    test('to test the property `referenciaProveedor`', () async {
      // TODO
    });

    // CUENTA_INEXISTENTE, CUENTA_CERRADA, TITULAR_NO_COINCIDE y MONEDA_INCOMPATIBLE son definitivos: no se reintentan
    // String codigoError
    test('to test the property `codigoError`', () async {
      // TODO
    });

    // String mensajeProveedor
    test('to test the property `mensajeProveedor`', () async {
      // TODO
    });

  });
}
