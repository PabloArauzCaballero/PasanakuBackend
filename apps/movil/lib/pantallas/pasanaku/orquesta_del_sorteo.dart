import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/scheduler.dart';
import 'package:flutter/widgets.dart';

import 'ruleta_del_sorteo.dart';

/// La puesta en escena del sorteo: **no decide nada**. Recibe el gajo que el servidor ya
/// sorteó y lo lleva hasta la aguja con amague, giro que frena de a poco, golpes de la
/// aguja en cada clavija, focos que corren y el brillo del gajo ganador.
///
/// Un solo [Ticker] mueve todo y se apaga cuando no hay nada que mover: quieta, la
/// pantalla no pide cuadros (y las pruebas pueden asentarse).
class OrquestaDelSorteo extends ChangeNotifier {
  OrquestaDelSorteo(TickerProvider vsync, {this.alTic}) {
    _ticker = vsync.createTicker(_cuadro);
  }

  /// Se llama al pasar cada clavija (como mucho cada 45 ms): para el háptico.
  final VoidCallback? alTic;

  late final Ticker _ticker;
  double angulo = 0;
  double aleta = 0;
  double luces = 0;
  double resalte = 0;
  int? ganador;
  bool todas = false;

  _Tramo? _tramo;
  Duration _antes = Duration.zero;
  Duration _ultimoTic = Duration.zero;
  int _segmentos = 1;

  /// Frenado de ruleta: arranca fuerte y se arrastra al final.
  static const _frenado = Cubic(0.12, 0.62, 0.16, 1);

  Future<void> girarHasta(
    int indice,
    int segmentos, {
    required bool primera,
  }) async {
    _segmentos = segmentos;
    ganador = null;
    resalte = 0;
    final desde = angulo;
    // Amague: retrocede apenas antes de arrancar, como cuando se toma envión.
    await _correr(const Duration(milliseconds: 260), Curves.easeOut, (t) {
      angulo = desde - 0.16 * t;
    });
    final arranque = angulo;
    final hasta = RuletaDelSorteo.anguloPara(
      indice,
      segmentos,
      desde: desde,
      vueltas: primera ? 5 : 2,
    );
    await _correr(
      Duration(milliseconds: primera ? 4800 : 1900),
      _frenado,
      (t) => angulo = arranque + (hasta - arranque) * t,
    );
    angulo = hasta;
    ganador = indice;
    await _correr(const Duration(milliseconds: 420), Curves.easeOutBack, (t) {
      resalte = t.clamp(0.0, 1.0);
    });
  }

  /// Un respiro entre puestos, medido con el mismo reloj (no con un temporizador aparte).
  Future<void> pausa(Duration d) => _correr(d, Curves.linear, (_) {});

  /// El cierre: los focos parpadean juntos tres veces.
  Future<void> celebrar() async {
    for (var k = 0; k < 3; k++) {
      todas = true;
      await _correr(const Duration(milliseconds: 200), Curves.linear, (_) {});
      todas = false;
      await _correr(const Duration(milliseconds: 160), Curves.linear, (_) {});
    }
    todas = true;
    notifyListeners();
  }

  Future<void> _correr(Duration d, Curve curva, void Function(double t) paso) {
    final listo = Completer<void>();
    _tramo = _Tramo(d, curva, paso, listo);
    if (!_ticker.isActive) {
      _antes = Duration.zero;
      _ticker.start();
    }
    return listo.future;
  }

  void _cuadro(Duration ahora) {
    final dt = (ahora - _antes).inMicroseconds / 1e6;
    _antes = ahora;
    final previo = angulo;
    final tramo = _tramo;
    if (tramo != null) {
      tramo.transcurrido += dt;
      final t = (tramo.transcurrido / tramo.segundos).clamp(0.0, 1.0);
      tramo.paso(tramo.curva.transform(t));
      if (t >= 1) {
        _tramo = null;
        tramo.listo.complete();
      }
    }
    final pasoGajo = 2 * math.pi / _segmentos;
    final velocidad = dt > 0 ? (angulo - previo).abs() / dt : 0.0;
    if ((angulo / pasoGajo).floor() != (previo / pasoGajo).floor()) {
      // La clavija empuja la aguja hacia atrás; más fuerte cuanto más rápido gira.
      aleta = -0.5 * math.min(1, 0.25 + velocidad / 14);
      if (ahora - _ultimoTic > const Duration(milliseconds: 45)) {
        _ultimoTic = ahora;
        alTic?.call();
      }
    }
    aleta *= math.exp(-dt / 0.07);
    luces += dt * (1.5 + velocidad * 0.9);
    notifyListeners();
    if (_tramo == null && aleta.abs() < 0.002) {
      aleta = 0;
      _ticker.stop();
    }
  }

  @override
  void dispose() {
    _ticker.dispose();
    final tramo = _tramo;
    if (tramo != null && !tramo.listo.isCompleted) tramo.listo.complete();
    super.dispose();
  }
}

class _Tramo {
  _Tramo(Duration d, this.curva, this.paso, this.listo)
    : segundos = math.max(d.inMicroseconds / 1e6, 1e-6);
  final double segundos;
  final Curve curva;
  final void Function(double t) paso;
  final Completer<void> listo;
  double transcurrido = 0;
}
