import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El cartel de cada resultado: el puesto grande y a quién le tocó. Entra con un resorte
/// corto cada vez que sale un puesto nuevo; si le tocó a quien mira, lo dice con su
/// nombre propio («¡Te tocó!») y el borde del color de su gajo.
class AnuncioDelSorteo extends StatelessWidget {
  const AnuncioDelSorteo({
    super.key,
    required this.puesto,
    required this.nombre,
    required this.soyYo,
    required this.gajo,
  });

  /// Puesto que acaba de salir (1 = primero).
  final int puesto;
  final String nombre;
  final bool soyYo;

  /// Gajo de la ruleta, para el color.
  final int gajo;

  static String titulo(int puesto, {required bool soyYo}) =>
      soyYo ? '¡Te tocó el $puesto.º turno!' : 'Turno $puesto.º';

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final color = ColoresDeTurno.de(gajo);
    return Semantics(
      liveRegion: true,
      child: TweenAnimationBuilder<double>(
        key: ValueKey(puesto),
        tween: Tween(begin: 0, end: 1),
        duration: duracion(context, const Duration(milliseconds: 520)),
        curve: Curves.easeOutBack,
        builder: (context, v, hijo) => Opacity(
          opacity: v.clamp(0.0, 1.0),
          child: Transform.scale(scale: 0.86 + 0.14 * v, child: hijo),
        ),
        child: Container(
          padding: const EdgeInsets.symmetric(
            horizontal: Espacio.s4,
            vertical: Espacio.s3,
          ),
          decoration: BoxDecoration(
            color: soyYo ? t.brandBg : t.surface,
            borderRadius: BorderRadius.circular(Radios.lg),
            border: Border.all(
              color: soyYo ? color : t.border,
              width: soyYo ? Borde.foco : Borde.fino,
            ),
            boxShadow: [t.sombra2],
          ),
          child: Row(
            children: [
              Container(
                width: Espacio.s7,
                height: Espacio.s7,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: color, shape: BoxShape.circle),
                child: Text(
                  '$puesto.º',
                  style: Tipo.titulo3.copyWith(color: Paleta.white),
                ),
              ),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      titulo(puesto, soyYo: soyYo),
                      style: Tipo.titulo3.copyWith(color: t.text),
                    ),
                    Text(
                      nombre,
                      style: Tipo.cuerpoChico.copyWith(color: t.text2),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
