import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/tutoriales.dart';

/// Envuelve el inicio y, tras el primer cuadro, le pide al motor que ofrezca la guía de
/// bienvenida si esta persona nunca la vio. No dibuja nada: la capa del tutorial es la
/// que pinta el velo, y trae su propia salida ("Saltar") para no bloquear nada.
class AutolanzadorDeGuia extends ConsumerStatefulWidget {
  const AutolanzadorDeGuia({super.key, required this.hijo});

  final Widget hijo;

  @override
  ConsumerState<AutolanzadorDeGuia> createState() => _AutolanzadorDeGuiaState();
}

class _AutolanzadorDeGuiaState extends ConsumerState<AutolanzadorDeGuia> {
  @override
  void initState() {
    super.initState();
    // Después del primer cuadro: las anclas del inicio ya están montadas y medibles.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) ref.read(autolanzarGuiaDeInicioProvider)().ignore();
    });
  }

  @override
  Widget build(BuildContext context) => widget.hijo;
}
