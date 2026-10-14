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
    _iniciarBiometria(_episodio);
  }

  void _iniciarBiometria(int episodio) {
    if (_biometriaEnCurso) {
      _reintentoBiometricoPendiente = true;
      return;
    }
    _biometriaEnCurso = true;
    unawaited(
      _intentarDesbloqueoBiometrico(episodio).whenComplete(() {
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

  void _solicitarBiometriaManual() {
    if (_biometriaEnCurso) return;
    _episodio++;
    _pidioBiometriaEnEsteEpisodio = true;
    _iniciarBiometria(_episodio);
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
