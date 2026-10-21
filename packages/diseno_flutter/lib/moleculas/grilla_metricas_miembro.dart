import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Grilla ultra estética de tarjetas métricas (KPIs) de participación y
/// comportamiento financiero de un miembro en la mesa de juego:
/// - Grupos activos actuales
/// - Grupos históricos participados
/// - Total dinero depositado / puesto en juego
/// - Ciclos completos completados
/// - Turnos jugados / cobrados
/// - Días de retraso histórico
/// - Impagos registrados
class GrillaMetricasMiembro extends StatelessWidget {
  const GrillaMetricasMiembro({
    super.key,
    this.gruposActivos = 2,
    this.gruposHistoricos = 5,
    this.dineroDepositado = 'Bs 24.800',
    this.ciclosCompletos = 5,
    this.turnosJugados = 14,
    this.diasRetraso = 0,
    this.impagos = 0,
  });

  final int gruposActivos;
  final int gruposHistoricos;
  final String dineroDepositado;
  final int ciclosCompletos;
  final int turnosJugados;
  final int diasRetraso;
  final int impagos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);

    final metricas = [
      _DatoMetrica(
        titulo: 'Grupos Activos',
        valor: '$gruposActivos',
        icono: Icons.group_work_outlined,
        colorAcento: const Color(0xFF10B981),
        subtitulo: 'En juego ahora',
      ),
      _DatoMetrica(
        titulo: 'Grupos Pasados',
        valor: '$gruposHistoricos',
        icono: Icons.history_edu_outlined,
        colorAcento: const Color(0xFF0284C7),
        subtitulo: 'Completados',
      ),
      _DatoMetrica(
        titulo: 'Dinero Depositado',
        valor: dineroDepositado,
        icono: Icons.account_balance_wallet_outlined,
        colorAcento: const Color(0xFFF59E0B),
        subtitulo: 'Total en la mesa',
        anchoCompleto: true,
      ),
      _DatoMetrica(
        titulo: 'Ciclos Completos',
        valor: '$ciclosCompletos',
        icono: Icons.replay_circle_filled_outlined,
        colorAcento: const Color(0xFF10B981),
        subtitulo: 'De inicio a fin',
      ),
      _DatoMetrica(
        titulo: 'Turnos Jugados',
        valor: '$turnosJugados',
        icono: Icons.view_carousel_outlined,
        colorAcento: const Color(0xFF8B5CF6),
        subtitulo: 'Cobrados y al día',
      ),
      _DatoMetrica(
        titulo: 'Días de Retraso',
        valor: diasRetraso == 0 ? '0 días' : '$diasRetraso d',
        icono: Icons.schedule_outlined,
        colorAcento: diasRetraso == 0 ? const Color(0xFF10B981) : const Color(0xFFEF4444),
        subtitulo: diasRetraso == 0 ? 'Puntualidad 100%' : 'Atraso promedio',
      ),
      _DatoMetrica(
        titulo: 'Impagos',
        valor: impagos == 0 ? '0' : '$impagos',
        icono: Icons.verified_outlined,
        colorAcento: impagos == 0 ? const Color(0xFF10B981) : const Color(0xFFEF4444),
        subtitulo: impagos == 0 ? 'Récord limpio' : 'Incumplimientos',
      ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'HISTORIAL Y MÉTRICAS DE JUEGO',
          style: Tipo.etiqueta.copyWith(
            color: t.text2,
            fontWeight: FontWeight.w700,
            letterSpacing: 1.1,
          ),
        ),
        const SizedBox(height: Espacio.s3),
        LayoutBuilder(
          builder: (context, constraints) {
            final anchoTarjeta = (constraints.maxWidth - Espacio.s3) / 2;
            return Wrap(
              spacing: Espacio.s3,
              runSpacing: Espacio.s3,
              children: metricas.map((m) {
                return SizedBox(
                  width: m.anchoCompleto ? constraints.maxWidth : anchoTarjeta,
                  child: _TarjetaMetricaIndividual(metrica: m),
                );
              }).toList(),
            );
          },
        ),
      ],
    );
  }
}

class _DatoMetrica {
  const _DatoMetrica({
    required this.titulo,
    required this.valor,
    required this.icono,
    required this.colorAcento,
    required this.subtitulo,
    this.anchoCompleto = false,
  });

  final String titulo;
  final String valor;
  final IconData icono;
  final Color colorAcento;
  final String subtitulo;
  final bool anchoCompleto;
}

class _TarjetaMetricaIndividual extends StatelessWidget {
  const _TarjetaMetricaIndividual({required this.metrica});
  final _DatoMetrica metrica;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);

    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.lg),
        border: Border.all(color: t.border),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  metrica.titulo,
                  style: Tipo.etiqueta.copyWith(
                    color: t.text2,
                    fontWeight: FontWeight.w600,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: Espacio.s1),
              Container(
                width: 28,
                height: 28,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: metrica.colorAcento.withValues(alpha: 0.12),
                ),
                child: Center(
                  child: Icon(
                    metrica.icono,
                    size: 16,
                    color: metrica.colorAcento,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            metrica.valor,
            style: Tipo.cifra.copyWith(
              color: t.text,
              fontSize: metrica.anchoCompleto ? 28 : 22,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            metrica.subtitulo,
            style: Tipo.etiqueta.copyWith(
              color: t.text3,
              fontSize: 11,
            ),
          ),
        ],
      ),
    );
  }
}
