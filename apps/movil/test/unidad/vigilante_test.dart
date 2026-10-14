import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:flutter/widgets.dart' show AppLifecycleState;
import 'package:flutter_test/flutter_test.dart';

class _Reloj {
  DateTime instante = DateTime.utc(2026, 10, 12);
  DateTime ahora() => instante;
  void avanzar(Duration duracion) => instante = instante.add(duracion);
}

class _Escenario {
  _Escenario() {
    vigilante = Vigilante(
      ahora: reloj.ahora,
      bloquear: (motivo) => bloqueos.add(motivo),
      bloqueada: () => bloqueos.isNotEmpty,
    );
  }

  final _Reloj reloj = _Reloj();
  final List<MotivoDeBloqueo> bloqueos = [];
  late final Vigilante vigilante;
}

void main() {
  test('no bloquea antes de cinco minutos sin interacción', () {
    final caso = _Escenario();
    caso.reloj.avanzar(const Duration(minutes: 4, seconds: 59));

    caso.vigilante.revisar();

    expect(caso.bloqueos, isEmpty);
  });

  test('bloquea por inactividad al cumplir cinco minutos', () {
    final caso = _Escenario();
    caso.reloj.avanzar(const Duration(minutes: 5));

    caso.vigilante.revisar();

    expect(caso.bloqueos, [MotivoDeBloqueo.inactividad]);
  });

  test('reinicia el límite con una interacción y bloquea al cumplirlo', () {
    final caso = _Escenario();
    caso.reloj.avanzar(const Duration(minutes: 4));
    caso.vigilante.interaccion();
    caso.reloj.avanzar(const Duration(minutes: 4, seconds: 59));
    caso.vigilante.revisar();
    expect(caso.bloqueos, isEmpty);

    caso.reloj.avanzar(const Duration(seconds: 1));
    caso.vigilante.revisar();

    expect(caso.bloqueos, [MotivoDeBloqueo.inactividad]);
  });

  test('volver antes de 60 segundos no bloquea', () {
    final caso = _Escenario();
    caso.vigilante.cambioDeEstado(AppLifecycleState.paused);
    caso.reloj.avanzar(const Duration(seconds: 59));

    caso.vigilante.cambioDeEstado(AppLifecycleState.resumed);

    expect(caso.bloqueos, isEmpty);
  });

  test('volver después de 60 segundos bloquea por segundo plano', () {
    final caso = _Escenario();
    caso.vigilante.cambioDeEstado(AppLifecycleState.paused);
    caso.reloj.avanzar(const Duration(seconds: 61));

    caso.vigilante.cambioDeEstado(AppLifecycleState.resumed);

    expect(caso.bloqueos, [MotivoDeBloqueo.segundoPlano]);
  });

  test(
    'inactive y resumed no cuenta como salida pero sí revisa inactividad',
    () {
      final caso = _Escenario();
      caso.reloj.avanzar(const Duration(minutes: 10));
      caso.vigilante.cambioDeEstado(AppLifecycleState.inactive);

      caso.vigilante.cambioDeEstado(AppLifecycleState.resumed);

      expect(caso.bloqueos, [MotivoDeBloqueo.inactividad]);
    },
  );

  test('dos paused seguidos conservan la primera hora de salida', () {
    final caso = _Escenario();
    caso.vigilante.cambioDeEstado(AppLifecycleState.paused);
    caso.reloj.avanzar(const Duration(seconds: 40));
    caso.vigilante.cambioDeEstado(AppLifecycleState.paused);
    caso.reloj.avanzar(const Duration(seconds: 21));

    caso.vigilante.cambioDeEstado(AppLifecycleState.resumed);

    expect(caso.bloqueos, [MotivoDeBloqueo.segundoPlano]);
  });

  test('un reloj hacia atrás al revisar bloquea por seguridad', () {
    final caso = _Escenario();
    caso.reloj.avanzar(const Duration(seconds: -1));

    caso.vigilante.revisar();

    expect(caso.bloqueos, [MotivoDeBloqueo.inactividad]);
  });

  test('un reloj hacia atrás al volver bloquea por segundo plano', () {
    final caso = _Escenario();
    caso.vigilante.cambioDeEstado(AppLifecycleState.paused);
    caso.reloj.avanzar(const Duration(seconds: -1));

    caso.vigilante.cambioDeEstado(AppLifecycleState.resumed);

    expect(caso.bloqueos, [MotivoDeBloqueo.segundoPlano]);
  });

  test('no vuelve a solicitar bloqueo cuando ya está bloqueada', () {
    final caso = _Escenario();
    caso.reloj.avanzar(const Duration(minutes: 5));
    caso.vigilante.revisar();
    caso.reloj.avanzar(const Duration(minutes: 1));

    caso.vigilante.revisar();

    expect(caso.bloqueos, [MotivoDeBloqueo.inactividad]);
  });

  test('no revisa inactividad mientras la app está fuera', () {
    final caso = _Escenario();
    caso.vigilante.cambioDeEstado(AppLifecycleState.paused);
    caso.reloj.avanzar(const Duration(minutes: 10));

    caso.vigilante.revisar();

    expect(caso.bloqueos, isEmpty);
  });

  test('desbloqueado reinicia el reloj de inactividad', () {
    final caso = _Escenario();
    caso.reloj.avanzar(const Duration(minutes: 4));
    caso.vigilante.desbloqueado();
    caso.reloj.avanzar(const Duration(minutes: 4, seconds: 59));

    caso.vigilante.revisar();

    expect(caso.bloqueos, isEmpty);
  });

  test('hidden se trata como salida a segundo plano', () {
    final caso = _Escenario();
    caso.vigilante.cambioDeEstado(AppLifecycleState.hidden);
    caso.reloj.avanzar(const Duration(seconds: 61));

    caso.vigilante.cambioDeEstado(AppLifecycleState.resumed);

    expect(caso.bloqueos, [MotivoDeBloqueo.segundoPlano]);
  });

  test('detached no cuenta como salida', () {
    final caso = _Escenario();
    caso.vigilante.cambioDeEstado(AppLifecycleState.detached);
    caso.reloj.avanzar(const Duration(seconds: 30));

    caso.vigilante.cambioDeEstado(AppLifecycleState.resumed);

    expect(caso.bloqueos, isEmpty);
  });
}
