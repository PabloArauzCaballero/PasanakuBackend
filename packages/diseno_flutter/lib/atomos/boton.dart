import 'package:flutter/material.dart';

import '../atomos/boton_tamano.dart';
import '../atomos/boton_variante.dart';
import '../tokens/tokens.dart';

/// El botón, con sus siete estados: normal, hover, presionado, foco, deshabilitado,
/// cargando y —para efectos— bloqueado mientras envía. `cargando` deshabilita y
/// muestra progreso: el doble toque no puede duplicar un efecto.
class Boton extends StatelessWidget {
  const Boton({
    super.key,
    required this.texto,
    required this.onPressed,
    this.variante = BotonVariante.secundario,
    this.tamano = BotonTamano.base,
    this.cargando = false,
    this.icono,
    this.expandido = false,
  });

  final String texto;
  final VoidCallback? onPressed;
  final BotonVariante variante;
  final BotonTamano tamano;
  final bool cargando;
  final IconData? icono;
  final bool expandido;

  bool get _habilitado => onPressed != null && !cargando;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final (fondo, frente, borde) = switch (variante) {
      BotonVariante.primario => (t.accent, t.accentInk, null),
      BotonVariante.secundario => (t.verdeSolido, t.sobreVerdeSolido, null),
      BotonVariante.fantasma => (Colors.transparent, t.brandTexto, t.brand),
      BotonVariante.peligro => (t.rojoSolido, t.sobreRojoSolido, null),
      BotonVariante.enlace => (Colors.transparent, t.brandTexto, null),
      BotonVariante.sobreVerde => (
        Colors.transparent,
        t.sobreVerdeSolido,
        t.sobreVerdeSolido,
      ),
    };
    final alto = switch (tamano) {
      BotonTamano.sm => Tactil.minimo - Espacio.s2,
      BotonTamano.base => Tactil.minimo,
      BotonTamano.lg => Tactil.minimo + Espacio.s2,
    };
    final estilo = ButtonStyle(
      backgroundColor: WidgetStatePropertyAll(fondo),
      foregroundColor: WidgetStatePropertyAll(frente),
      overlayColor: WidgetStatePropertyAll(frente.withValues(alpha: 0.08)),
      minimumSize: WidgetStatePropertyAll(Size(Tactil.minimo, alto)),
      padding: const WidgetStatePropertyAll(
        EdgeInsets.symmetric(horizontal: Espacio.s5),
      ),
      shape: WidgetStatePropertyAll(
        RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radios.md),
          side: borde == null
              ? BorderSide.none
              : BorderSide(color: borde, width: Borde.fino),
        ),
      ),
      textStyle: WidgetStatePropertyAll(
        Theme.of(context).textTheme.labelLarge?.copyWith(
          fontFamily: Fuente.display,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
    final hijo = Row(
      mainAxisSize: expandido ? MainAxisSize.max : MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (cargando) ...[
          SizedBox(
            width: Espacio.s4,
            height: Espacio.s4,
            child: CircularProgressIndicator(
              strokeWidth: Borde.foco - Borde.fino,
              color: frente,
            ),
          ),
          const SizedBox(width: Espacio.s2),
        ] else if (icono != null) ...[
          Icon(icono, size: Espacio.s5),
          const SizedBox(width: Espacio.s2),
        ],
        Flexible(
          child: Text(texto, overflow: TextOverflow.ellipsis, maxLines: 1),
        ),
      ],
    );
    return Semantics(
      button: true,
      enabled: _habilitado,
      label: cargando ? '$texto, enviando' : null,
      child: Opacity(
        opacity: _habilitado || cargando ? 1 : 0.5,
        child: FilledButton(
          onPressed: _habilitado ? onPressed : null,
          style: estilo,
          child: hijo,
        ),
      ),
    );
  }
}
