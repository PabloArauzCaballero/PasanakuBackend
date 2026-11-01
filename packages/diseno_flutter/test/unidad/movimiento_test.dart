import 'dart:math' as math;

import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:flutter/physics.dart';
import 'package:flutter_test/flutter_test.dart';

/// ζ = c / (2·√(m·k)) y respuesta = 2π / √(k/m): los dos parámetros con que Apple
/// describe un resorte (ADR-053). Los valores de `Movimiento` están escritos ya calculados
/// para que sean `const`; esta prueba impide que se desvíen de lo que dice su comentario.
double _razon(SpringDescription r) =>
    r.damping / (2 * math.sqrt(r.mass * r.stiffness));
double _respuesta(SpringDescription r) =>
    2 * math.pi / math.sqrt(r.stiffness / r.mass);

void main() {
  test(
    'el resorte por omisión es crítico (sin rebote) y responde en 0,35 s',
    () {
      expect(_razon(Movimiento.resorteCritico), closeTo(1.0, 0.01));
      expect(_respuesta(Movimiento.resorteCritico), closeTo(0.35, 0.005));
    },
  );

  test('el resorte con impulso rebota apenas (ζ 0,8) y responde en 0,3 s', () {
    expect(_razon(Movimiento.resorteConImpulso), closeTo(0.8, 0.01));
    expect(_respuesta(Movimiento.resorteConImpulso), closeTo(0.3, 0.005));
  });

  test('nada disparado por la persona supera 320 ms', () {
    for (final d in [
      Movimiento.pasoEscalonado,
      Movimiento.ocultarSaldo,
      Movimiento.entrada,
      Movimiento.pagina,
    ]) {
      expect(d.inMilliseconds, lessThanOrEqualTo(320));
    }
  });
}
