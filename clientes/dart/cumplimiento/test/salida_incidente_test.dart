import 'package:test/test.dart';
import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';

// tests for SalidaIncidente
void main() {
  final SalidaIncidente? instance = /* SalidaIncidente(...) */ null;
  // TODO add properties to the entity

  group(SalidaIncidente, () {
    // String incidenteId
    test('to test the property `incidenteId`', () async {
      // TODO
    });

    // Derivado del hecho. Dos avisos del mismo incidente, el del monitoreo y el de la persona que lo vio, producen el mismo codigo y el segundo choca contra el UNIQUE, en vez de abrir dos expedientes con dos relojes para el mismo hecho.
    // String codigo
    test('to test the property `codigo`', () async {
      // TODO
    });

    // Guardado, no recalculado. Es el plazo que regia el dia del incidente.
    // DateTime plazoReporte
    test('to test the property `plazoReporte`', () async {
      // TODO
    });

    // bool requiereNotificarTitulares
    test('to test the property `requiereNotificarTitulares`', () async {
      // TODO
    });

    // Ausente cuando no hay datos personales afectados.
    // DateTime plazoNotificacion
    test('to test the property `plazoNotificacion`', () async {
      // TODO
    });

    // El contrato del proveedor, resuelto por el activo afectado. Presente cuando el incidente ocurrio en un tercero: la responsabilidad frente al cliente no se terceriza, pero el contrato hay que poder mirarlo.
    // String contratoTerceroId
    test('to test the property `contratoTerceroId`', () async {
      // TODO
    });

    // DateTime registradoEn
    test('to test the property `registradoEn`', () async {
      // TODO
    });

  });
}
