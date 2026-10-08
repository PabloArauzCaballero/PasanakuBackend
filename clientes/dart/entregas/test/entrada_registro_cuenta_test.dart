import 'package:test/test.dart';
import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';

// tests for EntradaRegistroCuenta
void main() {
  final EntradaRegistroCuenta? instance = /* EntradaRegistroCuenta(...) */ null;
  // TODO add properties to the entity

  group(EntradaRegistroCuenta, () {
    // String tipoCuenta
    test('to test the property `tipoCuenta`', () async {
      // TODO
    });

    // String entidadFinanciera
    test('to test the property `entidadFinanciera`', () async {
      // TODO
    });

    // El numero en claro, y **nunca se persiste asi** (CU-18, R-SEG-01). Entra solo para derivar el enmascarado que se muestra y el hash con el que se detecta la cuenta repetida; las dos derivaciones ocurren ANTES de tocar la base, asi que el numero no llega a ninguna consulta.
    // String numeroCuenta
    test('to test the property `numeroCuenta`', () async {
      // TODO
    });

    // ya cifrado por el almacen de llaves. **El numero en claro nunca viaja en este campo ni se persiste** (R-SEG-01)
    // String numeroCifrado
    test('to test the property `numeroCifrado`', () async {
      // TODO
    });

    // String titularNombre
    test('to test the property `titularNombre`', () async {
      // TODO
    });

    // String titularDocumento
    test('to test the property `titularDocumento`', () async {
      // TODO
    });

    // String moneda
    test('to test the property `moneda`', () async {
      // TODO
    });

  });
}
