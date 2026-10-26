part of 'tour_en_stages.dart';

/// Una etapa del recorrido: ícono con capas, textos y puntos destacados.
class _PaginaDeEtapa extends StatelessWidget {
  const _PaginaDeEtapa(this.etapa);

  final EtapaDelTour etapa;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: Espacio.s5),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: Espacio.s2),
          // Icono con capas concéntricas
          Stack(
            alignment: Alignment.center,
            children: [
              Container(
                width: 100,
                height: 100,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: etapa.colorAcento.withValues(alpha: 0.12),
                ),
              ),
              Container(
                width: 76,
                height: 76,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: etapa.colorAcento.withValues(alpha: 0.22),
                  border: Border.all(
                    color: etapa.colorAcento.withValues(alpha: 0.4),
                    width: 2,
                  ),
                ),
                child: Center(
                  child: Icon(etapa.icono, size: 36, color: etapa.colorAcento),
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),

          // Etiqueta superior
          Text(
            etapa.etiqueta,
            style: Tipo.etiqueta.copyWith(
              color: etapa.colorAcento,
              fontWeight: FontWeight.w800,
              letterSpacing: 1.5,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: Espacio.s2),

          // Título
          Text(
            etapa.titulo,
            style: Tipo.titulo1.copyWith(
              color: t.text,
              fontWeight: FontWeight.w800,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: Espacio.s1),

          // Subtítulo
          Text(
            etapa.subtitulo,
            style: Tipo.cuerpo.copyWith(
              color: t.brand,
              fontWeight: FontWeight.w600,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: Espacio.s3),

          // Descripción
          Text(
            etapa.descripcion,
            style: Tipo.cuerpoChico.copyWith(color: t.text2, height: 1.4),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: Espacio.s4),

          // Puntos destacados con checks
          Container(
            padding: const EdgeInsets.all(Espacio.s3),
            decoration: BoxDecoration(
              color: t.surface2,
              borderRadius: BorderRadius.circular(Radios.md),
              border: Border.all(color: t.border),
            ),
            child: Column(
              children: [
                for (final d in etapa.destacados) ...[
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(
                        Icons.check_circle_rounded,
                        size: 16,
                        color: etapa.colorAcento,
                      ),
                      const SizedBox(width: Espacio.s2),
                      Expanded(
                        child: Text(
                          d,
                          style: Tipo.etiqueta.copyWith(
                            color: t.text,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                    ],
                  ),
                  if (d != etapa.destacados.last)
                    const SizedBox(height: Espacio.s2),
                ],
              ],
            ),
          ),
          const SizedBox(height: Espacio.s4),
        ],
      ),
    );
  }
}
