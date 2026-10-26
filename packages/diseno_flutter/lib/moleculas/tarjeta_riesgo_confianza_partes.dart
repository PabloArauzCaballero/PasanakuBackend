part of 'tarjeta_riesgo_confianza.dart';

/// Cabecera con la etiqueta y el distintivo de la calificación de riesgo.
class _CabeceraDeRiesgo extends StatelessWidget {
  const _CabeceraDeRiesgo({
    required this.calificacion,
    required this.colorAcento,
  });

  final String calificacion;
  final Color colorAcento;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Row(
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
    );
  }
}

/// Las tres métricas medidas: puntualidad, uso del fondo e impagos.
class _MetricasFacticas extends StatelessWidget {
  const _MetricasFacticas({
    required this.puntualidadPorcentaje,
    required this.coberturasUsadas,
    required this.impagos,
    required this.colorAcento,
  });

  final double puntualidadPorcentaje;
  final int coberturasUsadas;
  final int impagos;
  final Color colorAcento;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
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
              valor: formatearPorcentaje(puntualidadPorcentaje),
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
              tono: impagos == 0 ? colorAcento : t.err,
            ),
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
        Text(etiqueta, style: Tipo.ayuda.copyWith(color: t.text3)),
      ],
    );
  }
}
