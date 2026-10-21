part of 'tour_en_stages.dart';

/// La página de una etapa: ícono, textos y destacados.
Widget _paginaDeEtapa(EtapaDelTour etapa, Tokens t) {
  return Padding(
    padding: const EdgeInsets.symmetric(horizontal: Espacio.s5),
    child: Column(
      children: [
        const Spacer(),
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
        const Spacer(),
      ],
    ),
  );
}

/// Los puntos de progreso y los botones de navegación del tour.
Widget _navegacionDelTour({
  required List<EtapaDelTour> etapas,
  required int paginaActual,
  required Tokens t,
  required bool esUltima,
  required VoidCallback irAAtras,
  required VoidCallback irASiguiente,
}) {
  return Padding(
    padding: const EdgeInsets.fromLTRB(
      Espacio.s5,
      Espacio.s2,
      Espacio.s5,
      Espacio.s5,
    ),
    child: Column(
      children: [
        // Dots con expansión animada
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(etapas.length, (i) {
            final activa = i == paginaActual;
            return AnimatedContainer(
              duration: const Duration(milliseconds: 250),
              margin: const EdgeInsets.symmetric(horizontal: 4),
              height: 6,
              width: activa ? 24 : 6,
              decoration: BoxDecoration(
                color: activa ? etapas[paginaActual].colorAcento : t.border,
                borderRadius: BorderRadius.circular(Radios.pill),
              ),
            );
          }),
        ),
        const SizedBox(height: Espacio.s4),

        // Botones de acción
        Row(
          children: [
            if (paginaActual > 0) ...[
              Expanded(
                child: Boton(
                  texto: 'Atrás',
                  variante: BotonVariante.secundario,
                  onPressed: irAAtras,
                ),
              ),
              const SizedBox(width: Espacio.s3),
            ],
            Expanded(
              flex: 2,
              child: Boton(
                texto: esUltima ? '¡Entendido, a jugar!' : 'Siguiente etapa',
                variante: BotonVariante.primario,
                onPressed: irASiguiente,
              ),
            ),
          ],
        ),
      ],
    ),
  );
}
