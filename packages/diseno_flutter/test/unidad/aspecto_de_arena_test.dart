import 'package:aportaya_diseno/atomos/aspecto_de_arena.dart';
import 'package:flutter/painting.dart';
import 'package:flutter_test/flutter_test.dart';

double _contraste(Color a, Color b) {
  final la = a.computeLuminance();
  final lb = b.computeLuminance();
  final (claro, oscuro) = la > lb ? (la, lb) : (lb, la);
  return (claro + 0.05) / (oscuro + 0.05);
}

void main() {
  test('el ícono de cada arena contrasta al menos 3:1 con su relleno', () {
    for (var orden = 0; orden < AspectoDeArena.cuantas; orden++) {
      final c = _contraste(
        AspectoDeArena.relleno(orden),
        AspectoDeArena.tintaSobre(orden),
      );
      expect(c, greaterThanOrEqualTo(3), reason: 'arena $orden: $c');
    }
  });

  test('dos arenas vecinas nunca comparten color', () {
    for (var orden = 1; orden < AspectoDeArena.cuantas; orden++) {
      expect(
        AspectoDeArena.relleno(orden),
        isNot(AspectoDeArena.relleno(orden - 1)),
        reason: 'arenas ${orden - 1} y $orden',
      );
    }
  });

  test('un orden fuera del catálogo no rompe: toma el extremo', () {
    expect(AspectoDeArena.relleno(-1), AspectoDeArena.relleno(0));
    expect(
      AspectoDeArena.relleno(99),
      AspectoDeArena.relleno(AspectoDeArena.cuantas - 1),
    );
  });
}
