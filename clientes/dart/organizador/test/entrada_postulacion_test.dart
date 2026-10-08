import 'package:test/test.dart';
import 'package:aportaya_cliente_organizador/aportaya_cliente_organizador.dart';

// tests for EntradaPostulacion
void main() {
  final EntradaPostulacion? instance = /* EntradaPostulacion(...) */ null;
  // TODO add properties to the entity

  group(EntradaPostulacion, () {
    // String motivacion
    test('to test the property `motivacion`', () async {
      // TODO
    });

    // String experienciaDeclarada
    test('to test the property `experienciaDeclarada`', () async {
      // TODO
    });

    // sin diligencia reforzada no se administra plata ajena (R-UIF-09)
    // String kycReforzadoId
    test('to test the property `kycReforzadoId`', () async {
      // TODO
    });

    // String reputacion
    test('to test the property `reputacion`', () async {
      // TODO
    });

    // lo que se sabe del postulante por codigo de requisito. Un requisito obligatorio SIN dato no se da por cumplido (invariante 9)
    // Map<String, String> medidos
    test('to test the property `medidos`', () async {
      // TODO
    });

  });
}
