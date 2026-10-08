import 'package:test/test.dart';
import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';

// tests for SalidaPuntaje
void main() {
  final SalidaPuntaje? instance = /* SalidaPuntaje(...) */ null;
  // TODO add properties to the entity

  group(SalidaPuntaje, () {
    // String puntajeId
    test('to test the property `puntajeId`', () async {
      // TODO
    });

    // Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza.
    // String puntaje
    test('to test the property `puntaje`', () async {
      // TODO
    });

    // String nivelDeConfianza
    test('to test the property `nivelDeConfianza`', () async {
      // TODO
    });

    // Su suma es el total (R-REP-03). Es lo que hace que el numero se pueda apelar.
    // List<Componente> componentes
    test('to test the property `componentes`', () async {
      // TODO
    });

    // String puntajeAnterior
    test('to test the property `puntajeAnterior`', () async {
      // TODO
    });

    // int eventosConsiderados
    test('to test the property `eventosConsiderados`', () async {
      // TODO
    });

  });
}
