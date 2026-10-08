import 'package:test/test.dart';
import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';

// tests for SalidaEventoReputacion
void main() {
  final SalidaEventoReputacion? instance = /* SalidaEventoReputacion(...) */ null;
  // TODO add properties to the entity

  group(SalidaEventoReputacion, () {
    // String eventoId
    test('to test the property `eventoId`', () async {
      // TODO
    });

    // Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza.
    // String impacto
    test('to test the property `impacto`', () async {
      // TODO
    });

    // String factorAfectado
    test('to test the property `factorAfectado`', () async {
      // TODO
    });

    // false cuando el hecho ya estaba puntuado (R-REP-01). El reintento es inocuo.
    // bool esNuevo
    test('to test the property `esNuevo`', () async {
      // TODO
    });

  });
}
