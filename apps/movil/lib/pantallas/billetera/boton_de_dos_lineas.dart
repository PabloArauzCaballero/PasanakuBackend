import 'package:flutter/material.dart';

import 'package:aportaya_diseno/tokens/tokens.dart';

/// Mismo tamaño de toque, forma y colores que el átomo `Boton` de
/// `packages/diseno_flutter`, pero sin el límite de una línea.
///
/// La maqueta deja que «Ver aportes pendientes» pase a dos líneas dentro del botón
/// (`docs/Views/AportaYa-Maqueta.html`, `.saldo__b .btn--acc`). El átomo `Boton` del
/// sistema de diseño corta a una línea con puntos suspensivos — es una limitación
/// real del átomo, no algo para reescribir desde una pantalla
/// (`packages/diseno_flutter` está congelado). Se pide como micro-PR (un parámetro
/// `maxLines`) y, mientras tanto, este widget usa los mismos tokens de color y forma
/// que `Boton` directamente, sin su límite de una línea.
class BotonDeDosLineas extends StatelessWidget {
  const BotonDeDosLineas({
    super.key,
    required this.texto,
    required this.fondo,
    required this.frente,
    required this.onTap,
    this.borde,
  });

  final String texto;
  final Color fondo;
  final Color frente;
  final Color? borde;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: texto,
      child: Material(
        color: fondo,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radios.md),
          side: borde == null
              ? BorderSide.none
              : BorderSide(color: borde!, width: Borde.fino),
        ),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(Radios.md),
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: Tactil.minimo),
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: Espacio.s3,
                vertical: Espacio.s2,
              ),
              child: Center(
                child: Text(
                  texto,
                  textAlign: TextAlign.center,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    fontFamily: Fuente.display,
                    fontWeight: FontWeight.w600,
                    color: frente,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
