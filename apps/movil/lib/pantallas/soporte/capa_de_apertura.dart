import 'dart:async';

import 'package:aportaya_diseno/moviles/apertura_de_marca.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../navegacion/abridor_de_avisos.dart';
import '../../proveedores/avisos_push.dart';

/// Apertura de marca y listener de avisos push por encima del router.
class CapaDeApertura extends ConsumerStatefulWidget {
  const CapaDeApertura({
    super.key,
    required this.child,
    required this.enrutador,
  });

  final Widget child;
  final GoRouter enrutador;

  @override
  ConsumerState<CapaDeApertura> createState() => _CapaDeAperturaState();
}

class _CapaDeAperturaState extends ConsumerState<CapaDeApertura> {
  bool _mostrando = true;
  Timer? _respaldo;
  AbridorDeAvisos? _avisos;

  @override
  void initState() {
    super.initState();
    _avisos = AbridorDeAvisos(
      toques: ref.read(avisosPushProvider).toques,
      abrir: (ruta) => widget.enrutador.push(ruta),
    )..iniciar();
    _respaldo = Timer(const Duration(seconds: 2), _ocultar);
  }

  void _ocultar() {
    _respaldo?.cancel();
    if (!mounted || !_mostrando) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !_mostrando) return;
      setState(() => _mostrando = false);
    });
  }

  @override
  void dispose() {
    _respaldo?.cancel();
    _avisos?.detener();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Stack(
    children: [
      widget.child,
      if (_mostrando)
        Positioned.fill(child: AperturaDeMarca(alTerminar: _ocultar)),
    ],
  );
}
