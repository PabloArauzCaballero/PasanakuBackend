import 'dart:math' as math;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'marco_de_ruleta.dart';
import 'pintor_de_ruleta.dart';
import 'textos_detalle_del_grupo.dart';

/// La ruleta del sorteo: un gajo por integrante, en el orden de los cupos, con el color
/// de la rueda del grupo. **No decide nada**: el orden lo devolvió el servidor; la
/// ruleta solo lo muestra, persona por persona (`OrquestaDelSorteo` la mueve).
///
/// Capas, de atrás hacia adelante: sombra, disco que gira ([angulo]), aro con focos y
/// vidrio (quietos), centro con el puesto que se está sorteando y la aguja.
class RuletaDelSorteo extends StatelessWidget {
  const RuletaDelSorteo({
    super.key,
    required this.nombres,
    required this.angulo,
    this.diametro = 300,
    this.puestos = const {},
    this.ganador,
    this.resalte = 0,
    this.luces = 0,
    this.todas = false,
    this.aleta = 0,
    this.centro,
  });

  final List<String> nombres;
  final double angulo;
  final double diametro;

  /// Gajo → puesto que ya le salió (1 = primero).
  final Map<int, int> puestos;
  final int? ganador;
  final double resalte;
  final double luces;
  final bool todas;
  final double aleta;

  /// Lo que dice el centro: el puesto que se sortea («2.º») o «TURNO».
  final String? centro;

  /// El giro que deja el segmento [indice] bajo la aguja, en la vuelta que sigue a
  /// [desde] más [vueltas] vueltas completas.
  static double anguloPara(
    int indice,
    int segmentos, {
    required double desde,
    int vueltas = 3,
  }) {
    final paso = 2 * math.pi / segmentos;
    final destino = -(indice + 0.5) * paso;
    var delta = (destino - desde) % (2 * math.pi);
    if (delta < 0) delta += 2 * math.pi;
    return desde + vueltas * 2 * math.pi + delta;
  }

  @override
  Widget build(BuildContext context) {
    final disco = diametro * 0.86;
    final aguja = diametro * 0.13;
    return Semantics(
      label: TextosDetalleDelGrupo.ruletaEtiqueta,
      image: true,
      child: SizedBox(
        width: diametro,
        height: diametro + aguja * 0.35,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Positioned(
              top: aguja * 0.35,
              child: SizedBox.square(
                dimension: diametro,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    DecoratedBox(
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                            color: Paleta.ink.withValues(alpha: 0.18),
                            blurRadius: diametro * 0.12,
                            offset: Offset(0, diametro * 0.02),
                          ),
                        ],
                      ),
                      child: SizedBox.square(dimension: diametro),
                    ),
                    Transform.rotate(
                      angle: angulo,
                      child: CustomPaint(
                        size: Size.square(disco),
                        painter: PintorDeRuleta(
                          nombres: nombres,
                          puestos: puestos,
                          ganador: ganador,
                          resalte: resalte,
                        ),
                      ),
                    ),
                    CustomPaint(
                      size: Size.square(diametro),
                      painter: PintorDeMarco(luces: luces, todas: todas),
                    ),
                    _Centro(
                      diametro: diametro * 0.27,
                      texto: centro ?? 'TURNO',
                    ),
                  ],
                ),
              ),
            ),
            Positioned(
              top: 0,
              child: CustomPaint(
                size: Size(aguja * 0.78, aguja),
                painter: PintorDeAguja(aleta: aleta),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Centro extends StatelessWidget {
  const _Centro({required this.diametro, required this.texto});

  final double diametro;
  final String texto;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: diametro,
      height: diametro,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: const RadialGradient(
          center: Alignment(-0.3, -0.4),
          colors: [Paleta.g500, Paleta.g800],
        ),
        border: Border.all(color: Paleta.g300, width: Borde.desfase),
        boxShadow: [
          BoxShadow(
            color: Paleta.ink.withValues(alpha: 0.4),
            blurRadius: diametro * 0.12,
          ),
        ],
      ),
      child: Padding(
        padding: EdgeInsets.all(diametro * 0.12),
        child: FittedBox(
          child: Text(
            texto,
            style: Tipo.cifraGrande.copyWith(color: Paleta.crema),
          ),
        ),
      ),
    );
  }
}
