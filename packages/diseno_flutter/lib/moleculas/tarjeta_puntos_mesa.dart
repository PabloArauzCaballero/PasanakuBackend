import 'package:flutter/material.dart';

import '../atomos/numero_que_sube.dart';
import '../atomos/progreso.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Tarjeta monumental inspirada en el RankHero de GymSheet que destaca
/// los puntos acumulados por cada boliviano puesto en la mesa de juego
/// y el avance hacia el siguiente rango de honor.
class TarjetaPuntosMesa extends StatelessWidget {
  const TarjetaPuntosMesa({
    super.key,
    required this.puntos,
    this.rango = 'Rango Oro',
    this.nivel = 4,
    this.siguienteRango = 'Rango Diamante',
    this.puntosParaSiguiente = 1500,
    this.progresoRango = 0.82,
    this.onVerReglas,
  });

  final int puntos;
  final String rango;
  final int nivel;
  final String siguienteRango;
  final int puntosParaSiguiente;
  final double progresoRango;
  final VoidCallback? onVerReglas;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    const colorOro = Color(0xFFF59E0B);

    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.xl),
        border: Border.all(color: colorOro.withValues(alpha: 0.35), width: 1.5),
        boxShadow: [
          BoxShadow(
            color: colorOro.withValues(alpha: 0.1),
            blurRadius: 22,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Cabecera de rango y estrellas
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: colorOro.withValues(alpha: 0.18),
                      ),
                      child: const Icon(
                        Icons.casino_outlined,
                        size: 18,
                        color: colorOro,
                      ),
                    ),
                    const SizedBox(width: Espacio.s2),
                    Expanded(
                      child: Text(
                        'PUNTOS EN JUEGO',
                        style: Tipo.etiqueta.copyWith(
                          color: t.text2,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1.1,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: Espacio.s2),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: Espacio.s3,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [colorOro, colorOro.withValues(alpha: 0.8)],
                  ),
                  borderRadius: BorderRadius.circular(Radios.pill),
                  boxShadow: [
                    BoxShadow(
                      color: colorOro.withValues(alpha: 0.35),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.stars_rounded,
                      size: 14,
                      color: Colors.white,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '$rango · Nivel $nivel',
                      style: Tipo.etiqueta.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s3),

          // Puntos con efecto CountUp
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              NumeroQueSube(
                valor: puntos,
                etiqueta: 'Puntos en la mesa',
                estilo: Tipo.cifraGrande.copyWith(
                  color: colorOro,
                  fontWeight: FontWeight.w800,
                  fontSize: 44,
                ),
              ),
              const SizedBox(width: Espacio.s2),
              Text(
                'pts',
                style: Tipo.titulo3.copyWith(
                  color: t.text3,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s1),

          // Regla clave del sistema de juego
          Row(
            children: [
              Icon(Icons.check_circle_outline, size: 14, color: t.brand),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  '1 punto por cada Bs puesto en juego en la mesa',
                  style: Tipo.etiqueta.copyWith(
                    color: t.brand,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),

          // Barra de progreso hacia el siguiente nivel
          Progreso(
            valor: progresoRango.clamp(0.0, 1.0),
            etiqueta: 'Progreso hacia $siguienteRango',
            tono: Tono.marca,
          ),
          const SizedBox(height: Espacio.s2),

          // Texto de avance hacia el siguiente hito
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  'Te faltan $puntosParaSiguiente pts para $siguienteRango',
                  style: Tipo.etiqueta.copyWith(
                    color: t.text2,
                    fontWeight: FontWeight.w500,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: Espacio.s2),
              Text(
                '${(progresoRango * 100).toInt()}%',
                style: Tipo.etiqueta.copyWith(
                  color: colorOro,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
