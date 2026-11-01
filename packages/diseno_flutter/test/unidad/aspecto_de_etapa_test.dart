import 'package:aportaya_diseno/atomos/aspecto_de_etapa.dart';
import 'package:flutter/painting.dart';
import 'package:flutter_test/flutter_test.dart';

double _contraste(Color a, Color b) {
  final la = a.computeLuminance();
  final lb = b.computeLuminance();
  final (claro, oscuro) = la > lb ? (la, lb) : (lb, la);
  return (claro + 0.05) / (oscuro + 0.05);
}

void main() {
  test('el ícono de cada etapa contrasta al menos 3:1 con su relleno', () {
    for (var orden = 0; orden < AspectoDeEtapa.cuantas; orden++) {
      final c = _contraste(
        AspectoDeEtapa.relleno(orden),
        AspectoDeEtapa.tintaSobre(orden),
      );
      expect(c, greaterThanOrEqualTo(3), reason: 'etapa $orden: $c');
    }
  });

  test('dos etapas vecinas nunca comparten color', () {
    for (var orden = 1; orden < AspectoDeEtapa.cuantas; orden++) {
      expect(
        AspectoDeEtapa.relleno(orden),
        isNot(AspectoDeEtapa.relleno(orden - 1)),
        reason: 'etapas ${orden - 1} y $orden',
      );
    }
  });

  test('un orden fuera del catálogo no rompe: toma el extremo', () {
    expect(AspectoDeEtapa.relleno(-1), AspectoDeEtapa.relleno(0));
    expect(
      AspectoDeEtapa.relleno(99),
      AspectoDeEtapa.relleno(AspectoDeEtapa.cuantas - 1),
    );
  });
}
