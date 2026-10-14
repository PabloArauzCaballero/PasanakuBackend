import 'package:flutter/widgets.dart' show AppLifecycleState;

/// Tiempo máximo que la app puede quedar fuera antes de pedir desbloqueo.
const salidaCorta = Duration(seconds: 60);

/// Inactividad máxima con la app abierta.
const inactividadMaxima = Duration(minutes: 5);

/// Frecuencia de revisión de inactividad en primer plano.
const revisionDeInactividad = Duration(seconds: 15);

enum MotivoDeBloqueo { arranque, segundoPlano, inactividad }

/// Regla pura del bloqueo local; las dependencias externas se inyectan.
class Vigilante {
  Vigilante({
    required this.ahora,
    required this.bloquear,
    required this.bloqueada,
  }) : _ultimaInteraccion = ahora();

  final DateTime Function() ahora;
  final void Function(MotivoDeBloqueo) bloquear;
  final bool Function() bloqueada;

  DateTime? _salidaEn;
  DateTime _ultimaInteraccion;

  bool _inactivaDemasiado(DateTime instante) {
    final inactividad = instante.difference(_ultimaInteraccion);
    // Si el reloj retrocede, no es seguro asumir que la sesión siga desbloqueada.
    return inactividad.isNegative || inactividad >= inactividadMaxima;
  }

  void cambioDeEstado(AppLifecycleState estado) {
    final instante = ahora();
    switch (estado) {
      case AppLifecycleState.hidden:
      case AppLifecycleState.paused:
        // Avisos repetidos al salir no deben reiniciar la ventana corta.
        _salidaEn ??= instante;
        return;
      case AppLifecycleState.inactive:
      case AppLifecycleState.detached:
        // Inactive también se emite por biometría/centro de control; detached no equivale a salida.
        return;
      case AppLifecycleState.resumed:
        final salida = _salidaEn;
        _salidaEn = null;
        if (bloqueada()) return;
        if (salida != null) {
          final fuera = instante.difference(salida);
          if (fuera.isNegative || fuera > salidaCorta) {
            bloquear(MotivoDeBloqueo.segundoPlano);
            return;
          }
        }
        if (_inactivaDemasiado(instante)) bloquear(MotivoDeBloqueo.inactividad);
    }
  }

  void interaccion() => _ultimaInteraccion = ahora();

  void revisar() {
    if (_salidaEn != null || bloqueada()) return;
    if (_inactivaDemasiado(ahora())) bloquear(MotivoDeBloqueo.inactividad);
  }

  void desbloqueado() => _ultimaInteraccion = ahora();
}
