import 'package:test/test.dart';
import 'package:aportaya_cliente_organizador/aportaya_cliente_organizador.dart';

// tests for EntradaRegla
void main() {
  final EntradaRegla? instance = /* EntradaRegla(...) */ null;
  // TODO add properties to the entity

  group(EntradaRegla, () {
    // String codigo
    test('to test the property `codigo`', () async {
      // TODO
    });

    // String descripcion
    test('to test the property `descripcion`', () async {
      // TODO
    });

    // String disparador
    test('to test the property `disparador`', () async {
      // TODO
    });

    // String expresionDisparo
    test('to test the property `expresionDisparo`', () async {
      // TODO
    });

    // obligatoria. Una regla sin condicion se dispara siempre
    // String condicion
    test('to test the property `condicion`', () async {
      // TODO
    });

    // String accion
    test('to test the property `accion`', () async {
      // TODO
    });

    // obligatorio en true para APLICAR_MORA, EJECUTAR_ENTREGA, ESCALAR_COBRANZA y LIQUIDAR_PERIODO. La regla se rechaza si viene sin el, en vez de forzarlo callado: forzarlo deja a quien la escribio creyendo que definio otra cosa
    // bool requiereConfirmacionHumana (default value: false)
    test('to test the property `requiereConfirmacionHumana`', () async {
      // TODO
    });

    // unica entre las activas del mismo disparador
    // int prioridad
    test('to test the property `prioridad`', () async {
      // TODO
    });

  });
}
