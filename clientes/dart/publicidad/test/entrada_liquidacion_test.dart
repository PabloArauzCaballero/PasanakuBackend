import 'package:test/test.dart';
import 'package:aportaya_cliente_publicidad/aportaya_cliente_publicidad.dart';

// tests for EntradaLiquidacion
void main() {
  final EntradaLiquidacion? instance = /* EntradaLiquidacion(...) */ null;
  // TODO add properties to the entity

  group(EntradaLiquidacion, () {
    // String periodo
    test('to test the property `periodo`', () async {
      // TODO
    });

    // El comprobante fiscal, ya emitido por modulo 11. Sin el, la factura nace GENERADA y no puede avanzar: la tabla es append-only.
    // String facturaElectronicaId
    test('to test the property `facturaElectronicaId`', () async {
      // TODO
    });

    // La cuenta por cobrar, ya abierta por modulo 13 (CU-104).
    // String cuentaPorCobrarId
    test('to test the property `cuentaPorCobrarId`', () async {
      // TODO
    });

  });
}
