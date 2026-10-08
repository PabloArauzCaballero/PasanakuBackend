import 'package:test/test.dart';
import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';

// tests for EntradaRiesgoOperativo
void main() {
  final EntradaRiesgoOperativo? instance = /* EntradaRiesgoOperativo(...) */ null;
  // TODO add properties to the entity

  group(EntradaRiesgoOperativo, () {
    // Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
    // String categoriaEvento
    test('to test the property `categoriaEvento`', () async {
      // TODO
    });

    // String factorRiesgo
    test('to test the property `factorRiesgo`', () async {
      // TODO
    });

    // String lineaNegocio
    test('to test the property `lineaNegocio`', () async {
      // TODO
    });

    // String descripcion
    test('to test the property `descripcion`', () async {
      // TODO
    });

    // DateTime fechaOcurrencia
    test('to test the property `fechaOcurrencia`', () async {
      // TODO
    });

    // Nunca anterior a la ocurrencia. Es el error de carga mas comun, y una fecha invertida rompe el unico indicador que de verdad importa: cuanto tardamos en darnos cuenta.
    // DateTime fechaDeteccion
    test('to test the property `fechaDeteccion`', () async {
      // TODO
    });

    // CADENA decimal. Cero es valido: la casi-perdida se registra igual, porque la frecuencia tambien es informacion.
    // String perdidaBruta
    test('to test the property `perdidaBruta`', () async {
      // TODO
    });

    // Lo ya recuperado al registrar. La recuperacion POSTERIOR no edita el evento: la tabla es append-only y la perdida bruta no se toca.
    // String recuperacion
    test('to test the property `recuperacion`', () async {
      // TODO
    });

    // String moneda
    test('to test the property `moneda`', () async {
      // TODO
    });

    // Sin responsable no hay plan de accion, y el evento se registra igual. Es peor perder el registro que registrar sin plan.
    // String responsableId
    test('to test the property `responsableId`', () async {
      // TODO
    });

    // String accionComprometida
    test('to test the property `accionComprometida`', () async {
      // TODO
    });

    // DateTime fechaCompromiso
    test('to test the property `fechaCompromiso`', () async {
      // TODO
    });

  });
}
