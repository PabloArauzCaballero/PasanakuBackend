import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../atomos/boton_tamano.dart';
import '../atomos/hundido_al_tocar.dart';
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
    this.maxLineas = 2,
  });

  final String texto;
  final VoidCallback? onPressed;
  final BotonVariante variante;
  final BotonTamano tamano;
  final bool cargando;
  final IconData? icono;
  final bool expandido;

  /// Cuántas líneas puede ocupar el texto antes de cortarse. Dos por defecto: una
  /// etiqueta honesta como «Ver aportes pendientes» no entra en una sola dentro de
  /// media pantalla, y cortarla con puntos suspensivos esconde justamente el verbo.
  final int maxLineas;

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
    // Deshabilitado no es «el mismo botón, más pálido»: es otro color. Bajarle la
    // opacidad al naranja da un naranja claro que sigue leyéndose como acción y deja a
    // la persona tocando algo que no responde. Apagado se ve apagado.
    final apagado = !_habilitado && !cargando;
    final estilo = ButtonStyle(
      backgroundColor: WidgetStatePropertyAll(
        apagado ? (fondo == Colors.transparent ? fondo : t.surface2) : fondo,
      ),
      foregroundColor: WidgetStatePropertyAll(apagado ? t.text3 : frente),
      overlayColor: WidgetStatePropertyAll(frente.withValues(alpha: 0.08)),
      minimumSize: WidgetStatePropertyAll(Size(Tactil.minimo, alto)),
      // La bóveda le da al botón 18px de aire a los costados (`.btn`); s4 es el token
      // más cercano. Pero a un botón `expandido` el ancho se lo da el padre, así que
      // ese relleno deja de ser aire y pasa a ser espacio que le falta a su propia
      // palabra: tres botones con ícono en una fila de teléfono cortaban «Recargar»
      // en «Recar…». Expandido, el relleno es apenas un mínimo de cortesía.
      padding: WidgetStatePropertyAll(
        EdgeInsets.symmetric(horizontal: expandido ? Espacio.s2 : Espacio.s4),
      ),
      shape: WidgetStatePropertyAll(
        RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radios.md),
          side: borde == null
              ? BorderSide.none
              : BorderSide(
                  color: apagado ? t.border : borde,
                  width: Borde.fino,
                ),
        ),
      ),
      textStyle: const WidgetStatePropertyAll(Tipo.boton),
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
          // Un ícono de 20 y un respiro de s1: con s5/s2 —lo que había— un botón
          // expandido de una fila de tres se quedaba sin ancho para su palabra.
          Icon(icono, size: Espacio.s5 - Espacio.s1),
          const SizedBox(width: Espacio.s1 + 2),
        ],
        Flexible(
          child: Text(
            texto,
            textAlign: TextAlign.center,
            overflow: TextOverflow.ellipsis,
            maxLines: maxLineas,
          ),
        ),
      ],
    );
    return Semantics(
      button: true,
      enabled: _habilitado,
      label: cargando ? '$texto, enviando' : null,
      child: HundidoAlTocar(
        activo: _habilitado,
        child: FilledButton(
          onPressed: _habilitado
              ? () {
                  // El golpecito llega en el momento del toque, no cuando la pantalla
                  // siguiente terminó de dibujarse: es lo que confirma que el botón
                  // entendió, sobre todo en un teléfono lento.
                  HapticFeedback.lightImpact();
                  onPressed!();
                }
              : null,
          style: estilo,
          child: hijo,
        ),
      ),
    );
  }
}
