import 'package:flutter/material.dart';

import 'hundido_al_tocar.dart';
import 'movimiento.dart';
import '../tokens/tokens.dart';

/// Chip de elección o filtro, con o sin cierre. (`ChipElegible`: `Chip` es de Flutter.)
class ChipElegible extends StatelessWidget {
  const ChipElegible({
    super.key,
    required this.texto,
    this.seleccionado = false,
    this.icono,
    this.onTap,
    this.onCerrar,
  });

  final String texto;
  final bool seleccionado;
  final IconData? icono;
  final VoidCallback? onTap;
  final VoidCallback? onCerrar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final frente = seleccionado ? t.sobreVerdeSolido : t.text2;
    return HundidoAlTocar(
      activo: onTap != null,
      child: Semantics(
        button: onTap != null,
        selected: seleccionado,
        label: texto,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(Radios.pill),
          child: AnimatedContainer(
            duration: duracion(context, Movimiento.cambioDeEstado),
            curve: Movimiento.llega,
            constraints: const BoxConstraints(minHeight: Tactil.minimo),
            padding: const EdgeInsets.symmetric(
              horizontal: Espacio.s3,
              vertical: Espacio.s2,
            ),
            decoration: BoxDecoration(
              color: seleccionado ? t.verdeSolido : t.surface,
              border: Border.all(
                color: seleccionado ? t.verdeSolido : t.border,
                width: Borde.fino,
              ),
              borderRadius: BorderRadius.circular(Radios.pill),
              boxShadow: seleccionado ? [t.sombra1] : null,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (icono != null) ...[
                  Icon(icono, size: Espacio.s4, color: frente),
                  const SizedBox(width: Espacio.s2),
                ],
                Flexible(
                  child: AnimatedDefaultTextStyle(
                    duration: duracion(context, Movimiento.cambioDeEstado),
                    curve: Movimiento.llega,
                    style: (Theme.of(context).textTheme.labelLarge ?? Tipo.boton).copyWith(
                      color: frente,
                      fontWeight: seleccionado ? FontWeight.w600 : FontWeight.w500,
                    ),
                    child: Text(
                      texto,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ),
                if (onCerrar != null) ...[
                  const SizedBox(width: Espacio.s1),
                  IconButton(
                    onPressed: onCerrar,
                    tooltip: 'Quitar $texto',
                    icon: Icon(Icons.close, size: Espacio.s4, color: frente),
                    constraints: const BoxConstraints.tightFor(
                      width: Espacio.s6,
                      height: Espacio.s6,
                    ),
                    padding: EdgeInsets.zero,
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
