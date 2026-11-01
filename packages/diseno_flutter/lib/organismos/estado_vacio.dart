import 'package:flutter/material.dart';

import '../atomos/entrada_de_estado.dart';
import '../atomos/iconos_de_marca.dart';
import '../organismos/motivo_vacio.dart';
import '../tokens/tokens.dart';

/// Dice por qué no hay nada y qué hacer. Nunca «no hay datos».
///
/// Un vacío es la oportunidad de vender la primera acción (ADR-053, `mobile-app-ui-design`):
/// el ícono va en un círculo de marca, el [titulo] nombra lo que falta y la [accion] lo
/// resuelve. Antes era una bandeja gris con una oración.
class EstadoVacio extends StatelessWidget {
  const EstadoVacio({
    super.key,
    required this.mensaje,
    this.motivo = MotivoVacio.sinDatos,
    this.titulo,
    this.icono,
    this.accion,
  });

  final String mensaje;
  final MotivoVacio motivo;
  final String? titulo;

  /// El ícono propio del vacío («grupos» para «todavía no tenés grupos»); sin él, el del
  /// [motivo].
  final IconData? icono;
  final Widget? accion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final simbolo =
        icono ??
        switch (motivo) {
          MotivoVacio.sinDatos => IconosDeMarca.sinDatos,
          MotivoVacio.porFiltro => IconosDeMarca.porFiltro,
          MotivoVacio.porPermiso => IconosDeMarca.porPermiso,
        };
    return EntradaDeEstado(
      child: Padding(
        padding: const EdgeInsets.all(Espacio.s5),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ExcludeSemantics(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: t.brandBg,
                  shape: BoxShape.circle,
                ),
                child: SizedBox.square(
                  dimension: Tactil.minimo + Espacio.s5,
                  child: Icon(simbolo, size: Espacio.s6, color: t.brandTexto),
                ),
              ),
            ),
            const SizedBox(height: Espacio.s4),
            if (titulo != null) ...[
              Text(
                titulo!,
                textAlign: TextAlign.center,
                style: Tipo.titulo3.copyWith(color: t.text),
              ),
              const SizedBox(height: Espacio.s1),
            ],
            Text(
              mensaje,
              textAlign: TextAlign.center,
              style: Tipo.cuerpo.copyWith(color: t.text2),
            ),
            if (accion != null) ...[
              const SizedBox(height: Espacio.s4),
              accion!,
            ],
          ],
        ),
      ),
    );
  }
}
