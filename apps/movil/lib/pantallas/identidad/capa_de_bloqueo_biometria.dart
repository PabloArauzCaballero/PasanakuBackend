part of 'capa_de_bloqueo.dart';

/// El pedido de huella o PIN del candado: cuándo se abre el diálogo nativo y cómo un
/// toque nuevo reemplaza a un pedido colgado. Vive aparte para que ningún archivo pase
/// de 200 líneas; es parte de la misma clase.
extension _PedidoBiometrico on _CapaDeBloqueoState {
  /// El diálogo nativo se abre recién cuando la app está al frente. En el arranque en
  /// frío el candado se pone antes del primer cuadro; pedir la huella en ese momento
  /// puede caer en un error del sistema (la actividad todavía no está visible) y dejar
  /// la pantalla de bloqueo esperando un toque. Se espera por condición, no por tiempo.
  void _iniciarBiometriaCuandoSeVea(int episodio) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_sigueBloqueadoEn(episodio)) return;
      final estado = WidgetsBinding.instance.lifecycleState;
      // Sin dato del sistema (null) se pide como siempre; solo se espera cuando el
      // sistema dice explícitamente que la app no está al frente.
      if (estado == null || estado == AppLifecycleState.resumed) {
        _iniciarBiometria(episodio);
        return;
      }
      late final AppLifecycleListener oyente;
      oyente = AppLifecycleListener(
        onResume: () {
          oyente.dispose();
          if (_sigueBloqueadoEn(episodio)) _iniciarBiometria(episodio);
        },
      );
    });
    // Un callback de fin de cuadro no pide un cuadro por sí solo: sin esto, un pedido
    // que llega sin cambios visibles (al reanudar) quedaría esperando para siempre.
    WidgetsBinding.instance.scheduleFrame();
  }

  void _iniciarBiometria(int episodio, {bool reemplazar = false}) {
    if (_biometriaEnCurso && !reemplazar) {
      _reintentoBiometricoPendiente = true;
      return;
    }
    _biometriaEnCurso = true;
    final intento = ++_intentoBiometrico;
    unawaited(
      _intentarDesbloqueoBiometrico(episodio).whenComplete(() {
        // Un pedido más nuevo lo reemplazó: ese es el que decide.
        if (intento != _intentoBiometrico) return;
        _biometriaEnCurso = false;
        if (!_reintentoBiometricoPendiente || !mounted) return;
        _reintentoBiometricoPendiente = false;
        _pidioBiometriaEnEsteEpisodio = false;
        _observarBloqueo(ref.read(bloqueoProvider));
      }),
    );
  }

  Future<void> _intentarDesbloqueoBiometrico(int episodio) async {
    try {
      final disponible = await ref.read(biometriaDisponibleProvider.future);
      if (!disponible || !_sigueBloqueadoEn(episodio)) return;
      final confirmado = await ref
          .read(biometriaProvider)
          .confirmar('Desbloqueá AportaYa');
      if (confirmado && _sigueBloqueadoEn(episodio)) {
        // Confirma identidad local para quitar el candado; no autentica ni crea
        // una sesión ante el servidor.
        ref.read(bloqueoProvider).desbloquear();
      }
    } on Object {
      // Si el sistema cancela o falla, el candado queda puesto y no reintenta
      // hasta un nuevo episodio (otra salida/entrada o arranque).
    }
  }

  bool _sigueBloqueadoEn(int episodio) =>
      mounted && episodio == _episodio && ref.read(bloqueoProvider).bloqueada;

  /// El botón «Entrar con huella o PIN» nunca queda sin efecto: si un pedido anterior
  /// quedó colgado, este lo reemplaza (el lado nativo cancela el viejo y abre uno
  /// nuevo, con el PIN del teléfono si la huella no se puede usar).
  void _solicitarBiometriaManual() {
    _episodio++;
    _pidioBiometriaEnEsteEpisodio = true;
    _reintentoBiometricoPendiente = false;
    _iniciarBiometria(_episodio, reemplazar: true);
  }
}
