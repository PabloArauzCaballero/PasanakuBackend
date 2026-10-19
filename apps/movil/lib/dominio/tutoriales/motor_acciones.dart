part of 'motor.dart';

/// Acciones públicas del motor, separadas para mantener el archivo de estado pequeño.
extension AccionesMotorDeTutoriales on MotorDeTutoriales {
  /// Empieza de cero. Si ya se había hecho, las repeticiones viajan.
  Future<void> iniciar(
    String tutorialId, {
    int repeticiones = 0,
    bool requiereSesionActiva = false,
  }) async {
    final tutorial = _registro.buscar(tutorialId);
    if (tutorial == null || tutorial.pasos.isEmpty) return;
    final generacion = ++_generacion;
    _estado = EstadoDelMotor(tutorial: tutorial);
    await _escribano.guardar(
      progresoInicial(tutorial, DateTime.now(), repeticiones: repeticiones),
    );
    if (generacion != _generacion ||
        (requiereSesionActiva && !_sesion.activa)) {
      cancelar();
      return;
    }
    await _ir(0, generacion: generacion);
  }

  /// Retoma donde quedó. Un índice fuera de rango vuelve al último paso posible.
  Future<void> continuar(String tutorialId, ProgresoDeTutorial progreso) async {
    final tutorial = _registro.buscar(tutorialId);
    if (tutorial == null || tutorial.pasos.isEmpty) return;
    final generacion = ++_generacion;
    _estado = EstadoDelMotor(tutorial: tutorial);
    await _ir(
      progreso.indice.clamp(0, tutorial.pasos.length - 1).toInt(),
      generacion: generacion,
    );
  }

  Future<void> avanzar() async {
    if (!_estado.puedeAvanzar) {
      _estado = _estado.copiaCon(
        problema:
            _estado.paso?.ayudaSiFalla ??
            'Hacé lo que pide el paso para seguir.',
      );
      return;
    }
    if (_estado.esUltimo) return _terminar();
    await _ir(_estado.indice + 1, generacion: _generacion);
  }

  Future<void> retroceder() async {
    if (_estado.indice == 0) return;
    await _ir(_estado.indice - 1, generacion: _generacion);
  }

  /// Reintenta el paso actual: vuelve a buscar el objetivo desde cero.
  Future<void> reintentar() => _ir(_estado.indice, generacion: _generacion);

  /// La acción que el paso pedía ya está hecha.
  void marcarAccionCumplida() {
    if (_estado.activo) _estado = _estado.copiaCon(accionCumplida: true);
  }

  /// Pide confirmación si va por la mitad; si recién empezó, cierra sin preguntar.
  void pedirSalida() {
    if (_estado.indice == 0 || _estado.esUltimo) {
      omitir();
      return;
    }
    _estado = _estado.copiaCon(confirmandoSalida: true);
  }

  void seguirEnElTutorial() {
    _estado = _estado.copiaCon(confirmandoSalida: false);
  }

  /// Abandona: queda omitido, con el paso donde se fue. Se puede repetir después.
  void omitir() {
    final tutorial = _estado.tutorial;
    if (tutorial != null) {
      final ahora = DateTime.now();
      _escribano.sinEsperar(
        cerrado(
          enPaso(
            _escribano.de(tutorial, ahora),
            _estado.indice,
            _estado.paso?.id,
            ahora,
          ),
          EstadoDeProgreso.omitido,
          ahora,
        ),
      );
    }
    _estado = EstadoDelMotor.apagado;
  }

  /// Cierra sin marcar omitido cuando desaparece el contexto autorizado.
  void cancelar() {
    _generacion++;
    _temporizadorDeEspera?.cancel();
    _temporizadorDeEspera = null;
    final espera = _esperaEnCurso;
    _esperaEnCurso = null;
    if (espera != null && !espera.isCompleted) espera.complete();
    _estado = EstadoDelMotor.apagado;
  }

  /// Vuelve a medir el elemento actual si la pantalla se desplazó o cambió de tamaño.
  void medirDeNuevo() {
    final ancla = _estado.paso?.ancla;
    if (!_estado.activo || ancla == null) return;
    final recuadro = _anclas?.recuadroDe(ancla);
    if (recuadro != null && recuadro != _estado.recuadro) {
      _estado = _estado.copiaCon(recuadro: recuadro);
    }
  }
}
