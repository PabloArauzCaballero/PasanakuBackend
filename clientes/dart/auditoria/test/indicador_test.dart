import 'package:test/test.dart';
import 'package:aportaya_cliente_auditoria/aportaya_cliente_auditoria.dart';

// tests for Indicador
void main() {
  final Indicador? instance = /* Indicador(...) */ null;
  // TODO add properties to the entity

  group(Indicador, () {
    // String codigo
    test('to test the property `codigo`', () async {
      // TODO
    });

    // String nombre
    test('to test the property `nombre`', () async {
      // TODO
    });

    // Cadena decimal, con la escala que guardo el calculo. No se redondea al publicar: el numero del tablero tiene que ser el numero de la tabla. AUSENTE cuando se suprimio por privacidad. La serie tambien viene vacia en ese caso: publicarla seria dar el numero por la puerta de atras.
    // String valor
    test('to test the property `valor`', () async {
      // TODO
    });

    // String unidad
    test('to test the property `unidad`', () async {
      // TODO
    });

    // Ausente cuando no hay meta fijada para el periodo. Sin meta no hay semaforo, y ponerla despues de ver el resultado no es medir.
    // String meta
    test('to test the property `meta`', () async {
      // TODO
    });

    // Ausente cuando no hay meta.
    // bool cumpleMeta
    test('to test the property `cumpleMeta`', () async {
      // TODO
    });

    // Ausente en un indicador sin historia. Sin periodo anterior no hay comparacion, y un cero fingido afirmaria que se mantuvo igual.
    // String variacionPeriodoAnterior
    test('to test the property `variacionPeriodoAnterior`', () async {
      // TODO
    });

    // Los periodos anteriores, del mas viejo al mas nuevo.
    // List<IndicadorSerieInner> serie
    test('to test the property `serie`', () async {
      // TODO
    });

    // Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
    // String familia
    test('to test the property `familia`', () async {
      // TODO
    });

    // Quien responde cuando el indicador esta en rojo.
    // String duenoFamilia
    test('to test the property `duenoFamilia`', () async {
      // TODO
    });

    // Con que version de la definicion se calculo. Es lo que permite que un numero de hace un ano vuelva a salir igual.
    // String definicionVersion
    test('to test the property `definicionVersion`', () async {
      // TODO
    });

    // Verdadero cuando la muestra no alcanza el minimo de la definicion. Un promedio de tres personas identifica a las tres (R-SEG-03).
    // bool suprimidoPorPrivacidad
    test('to test the property `suprimidoPorPrivacidad`', () async {
      // TODO
    });

    // Tamano de la muestra, para poder explicar la supresion.
    // int casos
    test('to test the property `casos`', () async {
      // TODO
    });

    // El piso que fija la definicion del indicador.
    // int minimoCasos
    test('to test the property `minimoCasos`', () async {
      // TODO
    });

    // DateTime calculadoEn
    test('to test the property `calculadoEn`', () async {
      // TODO
    });

  });
}
