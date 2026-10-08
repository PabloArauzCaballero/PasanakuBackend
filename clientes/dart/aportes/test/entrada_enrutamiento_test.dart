import 'package:test/test.dart';
import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';

// tests for EntradaEnrutamiento
void main() {
  final EntradaEnrutamiento? instance = /* EntradaEnrutamiento(...) */ null;
  // TODO add properties to the entity

  group(EntradaEnrutamiento, () {
    // String claveIdempotencia
    test('to test the property `claveIdempotencia`', () async {
      // TODO
    });

    // Ausente equivale a lista vacía. Sin `default` a propósito — el generador Dart (dart-dio) emite el valor por omisión sin `const`, y una lista no-const no es una expresión constante válida (rompe la compilación del cliente).
    // List<String> yaIntentados
    test('to test the property `yaIntentados`', () async {
      // TODO
    });

    // Salud por codigo de proveedor, de quien la mide. No hay tabla de salud en aportes; un proveedor sin medicion se trata como sano porque acaba de pasar contrato y pruebas. Hueco declarado en planes/informes/carril-3A.md.
    // Map<String, int> saludObservada
    test('to test the property `saludObservada`', () async {
      // TODO
    });

  });
}
