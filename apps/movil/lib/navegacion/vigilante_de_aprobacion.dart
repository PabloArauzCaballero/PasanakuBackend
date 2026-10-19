import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../pantallas/identidad/dominio/kyc_del_titular.dart';
import '../pantallas/identidad/dominio/verificacion_diferida.dart';
import '../pantallas/identidad/textos_de_verificacion.dart';
import '../proveedores/renovar_acceso.dart';
import '../proveedores/sesion.dart';

/// Se entera solo de que aprobaron la identidad (2026-10-18). Antes, la persona veía
/// «Estamos verificando tu identidad» hasta salir de la app y volver a entrar: el token
/// de la sesión seguía sin los permisos de billetera y grupos.
///
/// Mientras la sesión no tenga esos permisos, consulta el estado de la cuenta cada
/// [intervalo] y cada vez que la app vuelve al frente. Cuando la cuenta ya está
/// verificada, renueva el acceso (el servidor recalcula los permisos al renovar) y
/// avisa. Sin sesión, o con la cuenta ya habilitada, no consulta nada.
class VigilanteDeAprobacion extends ConsumerStatefulWidget {
  const VigilanteDeAprobacion({
    super.key,
    required this.hijo,
    this.intervalo = const Duration(seconds: 30),
  });

  final Widget hijo;
  final Duration intervalo;

  @override
  ConsumerState<VigilanteDeAprobacion> createState() =>
      _VigilanteDeAprobacionState();
}

class _VigilanteDeAprobacionState extends ConsumerState<VigilanteDeAprobacion> {
  late final Sesion _sesion;
  late final AppLifecycleListener _oyente;
  Timer? _reloj;
  bool _consultando = false;

  bool get _esperandoAprobacion =>
      _sesion.requiereVerificacion && _sesion.usuarioIdActual != null;

  @override
  void initState() {
    super.initState();
    _sesion = ref.read(sesionProvider);
    _sesion.addListener(_ajustar);
    _oyente = AppLifecycleListener(onResume: _consultar);
    _ajustar();
  }

  @override
  void dispose() {
    _sesion.removeListener(_ajustar);
    _oyente.dispose();
    _reloj?.cancel();
    super.dispose();
  }

  void _ajustar() {
    if (_esperandoAprobacion) {
      _reloj ??= Timer.periodic(widget.intervalo, (_) => _consultar());
    } else {
      _reloj?.cancel();
      _reloj = null;
    }
  }

  Future<void> _consultar() async {
    if (!_esperandoAprobacion || _consultando || !mounted) return;
    _consultando = true;
    try {
      ref.invalidate(estadoDeCuentaProvider);
      final estado = await ref.read(estadoDeCuentaProvider.future);
      if (estado != EstadoDeCuenta.verificada || !mounted) return;
      final listo = await ref.read(renovarAccesoProvider)();
      if (!listo || !mounted) return;
      final usuarioId = _sesion.usuarioIdActual;
      if (usuarioId != null) ref.invalidate(kycDelTitularProvider(usuarioId));
      ScaffoldMessenger.maybeOf(context)?.showSnackBar(
        const SnackBar(content: Text(TextosDeVerificacion.cuentaActivada)),
      );
    } on Object {
      // Sin red o con el servidor caído: se vuelve a intentar en la próxima vuelta.
    } finally {
      _consultando = false;
    }
  }

  @override
  Widget build(BuildContext context) => widget.hijo;
}
