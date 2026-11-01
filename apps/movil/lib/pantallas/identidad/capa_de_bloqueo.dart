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

import '../../dominio/puertos/biometria.dart';
import '../../dominio/vigilante_de_sesion.dart';
import '../../proveedores/biometria.dart';
import '../../proveedores/bloqueo.dart';

part 'capa_de_bloqueo_vista.dart';
part 'capa_de_bloqueo_animacion.dart';
part 'capa_de_bloqueo_biometria.dart';

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

  /// Lo que contestó el sistema si no pudo abrir el diálogo; se muestra en el candado.
  String? _falloBiometrico;

  void _mostrarFallo(String? fallo) {
    if (mounted && fallo != _falloBiometrico) {
      setState(() => _falloBiometrico = fallo);
    }
  }

  void _alCambiarEstadoDeAnimacion(AnimationStatus estado) {
    if (estado != AnimationStatus.dismissed || !_conservarCapa) return;
    _conservarCapa = false;
    if (mounted) setState(() {});
  }

  /// Si «reducir movimiento» se activa con la capa animándose, la capa salta a su
  /// estado final: el candado nunca depende de una animación para verse.
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_bloqueadaAnteriormente != null) {
      _sincronizarAnimacion(ref.read(bloqueoProvider).bloqueada);
    }
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
        falloBiometrico: _falloBiometrico,
        onDesbloquear: widget.onDesbloquear ?? _solicitarBiometriaManual,
        onIngresarDeNuevo: widget.onIngresarDeNuevo,
      ),
    );
  }
}

/// Recorta la respuesta física al periodo de entrada de marca (220 ms) y la
/// normaliza para que termine exactamente en 1, aun si el resorte aún se asienta.
