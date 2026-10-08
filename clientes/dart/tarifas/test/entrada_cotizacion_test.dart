import 'package:test/test.dart';
import 'package:aportaya_cliente_tarifas/aportaya_cliente_tarifas.dart';

// tests for EntradaCotizacion
void main() {
  final EntradaCotizacion? instance = /* EntradaCotizacion(...) */ null;
  // TODO add properties to the entity

  group(EntradaCotizacion, () {
    // String codigoTarifario
    test('to test the property `codigoTarifario`', () async {
      // TODO
    });

    // String hechoGenerador
    test('to test the property `hechoGenerador`', () async {
      // TODO
    });

    // String referenciaTipo
    test('to test the property `referenciaTipo`', () async {
      // TODO
    });

    // String referenciaId
    test('to test the property `referenciaId`', () async {
      // TODO
    });

    // Dinero montoBase
    test('to test the property `montoBase`', () async {
      // TODO
    });

    // si viene y el grupo tiene tarifa congelada, manda el snapshot (R-TAR-07)
    // String grupoId
    test('to test the property `grupoId`', () async {
      // TODO
    });

    // Dinero descuento
    test('to test the property `descuento`', () async {
      // TODO
    });

    // el segmento ya resuelto por CU-36. Queda en el desglose: cotizacion_comision no tiene columna para el y no se invento una
    // String segmentoCodigo
    test('to test the property `segmentoCodigo`', () async {
      // TODO
    });

  });
}
