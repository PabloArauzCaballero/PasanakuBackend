import 'package:test/test.dart';
import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';

// tests for EntradaIncidente
void main() {
  final EntradaIncidente? instance = /* EntradaIncidente(...) */ null;
  // TODO add properties to the entity

  group(EntradaIncidente, () {
    // String tipo
    test('to test the property `tipo`', () async {
      // TODO
    });

    // String severidad
    test('to test the property `severidad`', () async {
      // TODO
    });

    // El activo del inventario que se vio afectado. De el salen la clasificacion de datos y el contrato del proveedor, si lo hay.
    // String activoInformacionId
    test('to test the property `activoInformacionId`', () async {
      // TODO
    });

    // Lo que declara quien reporta. El activo lo puede sobrescribir: la clasificacion del inventario sabe mas que quien esta apagando el incendio.
    // bool datosPersonalesAfectados
    test('to test the property `datosPersonalesAfectados`', () async {
      // TODO
    });

    // Aproximado esta bien. Esperar al numero final para notificar es como se pierde el plazo de notificacion.
    // int usuariosAfectados
    test('to test the property `usuariosAfectados`', () async {
      // TODO
    });

    // Desde aca corren los plazos, y no desde la ocurrencia: no se le puede exigir a nadie reportar algo que todavia no sabia.
    // DateTime detectadoEn
    test('to test the property `detectadoEn`', () async {
      // TODO
    });

    // La perdida asociada, si ya se registro (CU-54).
    // String eventoRiesgoId
    test('to test the property `eventoRiesgoId`', () async {
      // TODO
    });

  });
}
