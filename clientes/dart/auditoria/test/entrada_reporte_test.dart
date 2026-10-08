import 'package:test/test.dart';
import 'package:aportaya_cliente_auditoria/aportaya_cliente_auditoria.dart';

// tests for EntradaReporte
void main() {
  final EntradaReporte? instance = /* EntradaReporte(...) */ null;
  // TODO add properties to the entity

  group(EntradaReporte, () {
    // Se validan contra `parametros_esperados` de la definicion. Viajan como cadena a proposito: el tipo lo declara el reporte, no el llamador, y asi no hay dos opiniones sobre si «2026-01» es una fecha o un texto.
    // Map<String, String> parametros
    test('to test the property `parametros`', () async {
      // TODO
    });

    // Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`.
    // String formato
    test('to test the property `formato`', () async {
      // TODO
    });

    // Obligatoria cuando la definicion tiene `contiene_datos_sensibles`. El minimo de diez no es capricho: lo exige `ck_acceso_justificacion`, porque una justificacion de tres letras no se puede leer dentro de un ano.
    // String justificacion
    test('to test the property `justificacion`', () async {
      // TODO
    });

  });
}
