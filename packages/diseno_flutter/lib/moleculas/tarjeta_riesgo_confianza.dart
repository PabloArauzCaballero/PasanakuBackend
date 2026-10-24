import 'package:flutter/material.dart';

import '../atomos/numero_que_sube.dart';
import '../atomos/progreso.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Tarjeta ultra estética que presenta la Calificación de Riesgo y el Nivel
/// de Confianza de un participante del grupo sin emitir juicios arbitrarios:
/// se basa en hechos medidos (puntualidad, mora, coberturas usadas e impagos).
class TarjetaRiesgoConfianza extends StatelessWidget {
  const TarjetaRiesgoConfianza({
    super.key,
    this.score = 98,
    this.calificacion = 'Riesgo Bajo · A1',
    this.puntualidadPorcentaje = 99.8,
    this.coberturasUsadas = 0,
    this.impagos = 0,
    this.antiguedadMeses = 14,
    this.onVerDetalle,
  });

  final int score;
  final String calificacion;
  final double puntualidadPorcentaje;
  final int coberturasUsadas;
  final int impagos;
  final int antiguedadMeses;
  final VoidCallback? onVerDetalle;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final esVerde = score >= 80;
    final colorAcento = esVerde
        ? const Color(0xFF10B981)
        : const Color(0xFFF59E0B);

    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.xl),
        border: Border.all(
          color: colorAcento.withValues(alpha: 0.35),
          width: 1.5,
        ),
        boxShadow: [
          BoxShadow(
            color: colorAcento.withValues(alpha: 0.08),
            blurRadius: 20,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Cabecera con etiqueta y badge de riesgo
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Expanded(
                child: Row(
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: colorAcento.withValues(alpha: 0.15),
                      ),
                      child: Icon(
                        Icons.shield_outlined,
                        size: 18,
                        color: colorAcento,
                      ),
                    ),
                    const SizedBox(width: Espacio.s2),
                    Expanded(
                      child: Text(
                        'CALIFICACIÓN DE RIESGO',
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
                  color: colorAcento.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(Radios.pill),
                  border: Border.all(color: colorAcento.withValues(alpha: 0.4)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 7,
                      height: 7,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: colorAcento,
                      ),
                    ),
                    const SizedBox(width: 5),
                    Text(
                      calificacion,
                      style: Tipo.etiqueta.copyWith(
                        color: colorAcento,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),

          // Puntuación de confianza monumental
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              NumeroQueSube(
                valor: score,
                etiqueta: 'Puntos de confianza',
                estilo: Tipo.cifraGrande.copyWith(
                  color: t.text,
                  fontWeight: FontWeight.w800,
                  fontSize: 34,
                ),
              ),
              Text(
                ' / 100',
                style: Tipo.cifra.copyWith(
                  color: t.text3,
                  fontWeight: FontWeight.w600,
                  fontSize: 18,
                ),
              ),
              const Spacer(),
              Flexible(
                child: Text(
                  'Nivel Excelente',
                  style: Tipo.etiqueta.copyWith(
                    color: colorAcento,
                    fontWeight: FontWeight.w700,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s2),

          // Barra visual de confianza
          Progreso(
            valor: (score / 100).clamp(0.0, 1.0),
            etiqueta: 'Puntaje de confianza $score sobre 100',
            tono: score >= 90
                ? Tono.ok
                : (score >= 70 ? Tono.marca : Tono.aviso),
          ),
          const SizedBox(height: Espacio.s4),

          // Métricas fácticas auditadas
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: Espacio.s2,
              vertical: Espacio.s3,
            ),
            decoration: BoxDecoration(
              color: t.surface2,
              borderRadius: BorderRadius.circular(Radios.md),
              border: Border.all(color: t.border),
            ),
            child: Row(
              children: [
                Expanded(
                  child: _MetricaFactual(
                    etiqueta: 'Puntualidad',
                    valor: '${puntualidadPorcentaje.toStringAsFixed(1)}%',
                    tono: colorAcento,
                  ),
                ),
                Container(width: 1, height: 28, color: t.border),
                Expanded(
                  child: _MetricaFactual(
                    etiqueta: 'Fondo usado',
                    valor: '$coberturasUsadas veces',
                    tono: coberturasUsadas == 0 ? colorAcento : t.text2,
                  ),
                ),
                Container(width: 1, height: 28, color: t.border),
                Expanded(
                  child: _MetricaFactual(
                    etiqueta: 'Impagos',
                    valor: impagos == 0 ? '0 (Limpio)' : '$impagos',
                    tono: impagos == 0 ? colorAcento : Colors.red,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: Espacio.s2),

          // Pie de auditoría
          Text(
            'Auditoría fáctica: calculado a partir de asientos contables reales en AportaYa.',
            style: Tipo.etiqueta.copyWith(color: t.text3, fontSize: 11),
          ),
        ],
      ),
    );
  }
}

class _MetricaFactual extends StatelessWidget {
  const _MetricaFactual({
    required this.etiqueta,
    required this.valor,
    required this.tono,
  });

  final String etiqueta;
  final String valor;
  final Color tono;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      children: [
        Text(
          valor,
          style: Tipo.etiqueta.copyWith(
            color: tono,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          etiqueta,
          style: Tipo.etiqueta.copyWith(color: t.text3, fontSize: 11),
        ),
      ],
    );
  }
}
