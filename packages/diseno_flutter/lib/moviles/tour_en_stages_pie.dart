part of 'tour_en_stages.dart';

/// Puntos indicadores y botones de navegación.
class _PieDelTour extends StatelessWidget {
  const _PieDelTour({
    required this.etapas,
    required this.paginaActual,
    required this.esUltima,
    required this.onAtras,
    required this.onSiguiente,
  });

  final List<EtapaDelTour> etapas;
  final int paginaActual;
  final bool esUltima;
  final VoidCallback onAtras;
  final VoidCallback onSiguiente;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
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
                    onPressed: onAtras,
                  ),
                ),
                const SizedBox(width: Espacio.s3),
              ],
              Expanded(
                flex: 2,
                child: Boton(
                  texto: esUltima ? '¡Entendido, a jugar!' : 'Siguiente etapa',
                  variante: BotonVariante.primario,
                  onPressed: onSiguiente,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
