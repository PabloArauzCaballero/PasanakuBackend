import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

part 'tarjeta_metrica_miembro.dart';

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
        colorAcento: t.ok,
        subtitulo: 'En juego ahora',
      ),
      _DatoMetrica(
        titulo: 'Grupos Pasados',
        valor: '$gruposHistoricos',
        icono: Icons.history_edu_outlined,
        colorAcento: t.info,
        subtitulo: 'Completados',
      ),
      _DatoMetrica(
        titulo: 'Dinero Depositado',
        valor: dineroDepositado,
        icono: Icons.account_balance_wallet_outlined,
        colorAcento: t.warn,
        subtitulo: 'Total en la mesa',
        anchoCompleto: true,
      ),
      _DatoMetrica(
        titulo: 'Ciclos Completos',
        valor: '$ciclosCompletos',
        icono: Icons.replay_circle_filled_outlined,
        colorAcento: t.ok,
        subtitulo: 'De inicio a fin',
      ),
      _DatoMetrica(
        titulo: 'Turnos Jugados',
        valor: '$turnosJugados',
        icono: Icons.view_carousel_outlined,
        colorAcento: t.brand,
        subtitulo: 'Cobrados y al día',
      ),
      _DatoMetrica(
        titulo: 'Días de Retraso',
        valor: diasRetraso == 0 ? '0 días' : '$diasRetraso d',
        icono: Icons.schedule_outlined,
        colorAcento: diasRetraso == 0 ? t.ok : t.err,
        subtitulo: diasRetraso == 0 ? 'Puntualidad 100%' : 'Atraso promedio',
      ),
      _DatoMetrica(
        titulo: 'Impagos',
        valor: impagos == 0 ? '0' : '$impagos',
        icono: Icons.verified_outlined,
        colorAcento: impagos == 0 ? t.ok : t.err,
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
