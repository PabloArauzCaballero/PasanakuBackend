part of 'tarjeta_riesgo_confianza.dart';

/// La cabecera: ícono, título y la insignia con la calificación.
Widget _cabeceraDeRiesgo(Tokens t, Color colorAcento, String calificacion) {
  return Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    crossAxisAlignment: CrossAxisAlignment.center,
    children: [
      Row(
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: colorAcento.withValues(alpha: 0.15),
            ),
            child: Icon(Icons.shield_outlined, size: 18, color: colorAcento),
          ),
          const SizedBox(width: Espacio.s2),
          Text(
            'CALIFICACIÓN DE RIESGO',
            style: Tipo.etiqueta.copyWith(
              color: t.text2,
              fontWeight: FontWeight.w700,
              letterSpacing: 1.1,
            ),
          ),
        ],
      ),
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
          style: Tipo.etiqueta.copyWith(
            color: t.text3,
            fontSize: Tipo.eyebrow.fontSize,
          ),
        ),
      ],
    );
  }
}
