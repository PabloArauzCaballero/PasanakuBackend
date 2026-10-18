import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/moviles/fondo_de_marca.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/vigilante_de_sesion.dart';
import '../../proveedores/biometria.dart';
import '../../proveedores/bloqueo.dart';

part 'capa_de_bloqueo_vista.dart';
part 'capa_de_bloqueo_animacion.dart';

/// Cubre el contenido completo mientras la sesión requiere confirmación local.
/// El cierre y la navegación de «Ingresar de nuevo» los decide quien integra
/// esta capa; así no se confunde el candado local con cerrar la sesión.
class CapaDeBloqueo extends ConsumerStatefulWidget {
  const CapaDeBloqueo({
    super.key,
    required this.hijo,
    required this.onIngresarDeNuevo,
    this.onDesbloquear,
  });

  final Widget hijo;
  final VoidCallback onIngresarDeNuevo;
  final VoidCallback? onDesbloquear;

  @override
  ConsumerState<CapaDeBloqueo> createState() => _CapaDeBloqueoState();
}

class _CapaDeBloqueoState extends ConsumerState<CapaDeBloqueo>
    with TickerProviderStateMixin {
  late final AnimationController _animacion = AnimationController(
    vsync: this,
    duration: Movimiento.entrada,
    reverseDuration: Movimiento.saleCandado,
  )..addStatusListener(_alCambiarEstadoDeAnimacion);
  late final Animation<double> _opacidad = CurvedAnimation(
    parent: _animacion,
    curve: Curves.easeOut,
    reverseCurve: Curves.easeIn,
  );
  late final Animation<double> _escala = Tween<double>(
    begin: 0.92,
    end: 1,
  ).animate(CurvedAnimation(parent: _animacion, curve: _CurvaResorte()));
  bool _conservarCapa = false;
  bool? _bloqueadaAnteriormente;
  bool _pidioBiometriaEnEsteEpisodio = false;
  bool _biometriaEnCurso = false;

  /// Cada pedido al diálogo nativo lleva su número: un pedido nuevo (el toque del
  /// botón) reemplaza al que hubiera quedado colgado, y solo el último libera la marca.
  int _intentoBiometrico = 0;
  bool _reintentoBiometricoPendiente = false;
  int _episodiosBiometricosVistos = 0;
  int _episodio = 0;

  void _alCambiarEstadoDeAnimacion(AnimationStatus estado) {
    if (estado != AnimationStatus.dismissed || !_conservarCapa) return;
    _conservarCapa = false;
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _animacion.dispose();
    super.dispose();
  }

  void _observarBloqueo(ControlDeBloqueo bloqueo) {
    _sincronizarAnimacion(bloqueo.bloqueada);
    var episodioNuevoSolicitado = false;
    if (_episodiosBiometricosVistos != bloqueo.episodioBiometrico) {
      _episodiosBiometricosVistos = bloqueo.episodioBiometrico;
      _episodio++;
      _pidioBiometriaEnEsteEpisodio = false;
      episodioNuevoSolicitado = true;
    }
    if (!bloqueo.bloqueada) {
      if (_bloqueadaAnteriormente != false) {
        _episodio++;
        _pidioBiometriaEnEsteEpisodio = false;
        _bloqueadaAnteriormente = false;
      }
      return;
    }

    if (_bloqueadaAnteriormente != true) {
      _episodio++;
      _bloqueadaAnteriormente = true;
      _pidioBiometriaEnEsteEpisodio = false;
    }

    final motivo = bloqueo.motivo;
    if (_pidioBiometriaEnEsteEpisodio ||
        (!episodioNuevoSolicitado &&
            motivo != MotivoDeBloqueo.arranque &&
            motivo != MotivoDeBloqueo.segundoPlano)) {
      return;
    }
    // Se marca antes del primer await: un rebuild o una cancelación no vuelve a
    // abrir el diálogo nativo durante el mismo episodio de bloqueo.
    _pidioBiometriaEnEsteEpisodio = true;
    _iniciarBiometriaCuandoSeVea(_episodio);
  }

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

  @override
  Widget build(BuildContext context) {
    ref.listen<ControlDeBloqueo>(bloqueoProvider, (_, siguiente) {
      _observarBloqueo(siguiente);
    });
    final bloqueo = ref.watch(bloqueoProvider);
    _observarBloqueo(bloqueo);
    if (!bloqueo.bloqueada && !_conservarCapa) return widget.hijo;

    final motivo = bloqueo.motivo ?? MotivoDeBloqueo.arranque;
    final biometriaDisponible =
        ref.watch(biometriaDisponibleProvider).asData?.value ?? false;

    return PopScope(
      canPop: false,
      child: _VistaDeBloqueo(
        hijo: widget.hijo,
        conservarCapa: _conservarCapa,
        motivo: motivo,
        opacidad: _opacidad,
        escala: _escala,
        biometriaDisponible: biometriaDisponible,
        onDesbloquear: widget.onDesbloquear ?? _solicitarBiometriaManual,
        onIngresarDeNuevo: widget.onIngresarDeNuevo,
      ),
    );
  }
}

/// Recorta la respuesta física al periodo de entrada de marca (220 ms) y la
/// normaliza para que termine exactamente en 1, aun si el resorte aún se asienta.
