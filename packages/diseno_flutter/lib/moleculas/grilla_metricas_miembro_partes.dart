part of 'grilla_metricas_miembro.dart';

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
            color: Paleta.ink.withValues(alpha: 0.03),
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
          Text(metrica.subtitulo, style: Tipo.ayuda.copyWith(color: t.text3)),
        ],
      ),
    );
  }
}
