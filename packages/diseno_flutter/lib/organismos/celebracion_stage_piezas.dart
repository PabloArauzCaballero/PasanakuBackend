part of 'celebracion_stage.dart';

/// Ícono monumental con halo de color del nivel.
class _IconoConHalo extends StatelessWidget {
  const _IconoConHalo(this.celebracion);

  final CelebracionStage celebracion;

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: Alignment.center,
      children: [
        Container(
          width: 104,
          height: 104,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: celebracion.tier.colorPrimario.withValues(alpha: 0.14),
            border: Border.all(
              color: celebracion.tier.colorPrimario.withValues(alpha: 0.35),
              width: 2,
            ),
          ),
        ),
        Container(
          width: 82,
          height: 82,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                celebracion.tier.colorPrimario,
                celebracion.tier.colorPrimario.withValues(alpha: 0.75),
              ],
            ),
            boxShadow: [
              BoxShadow(
                color: celebracion.tier.colorPrimario.withValues(alpha: 0.45),
                blurRadius: 18,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Center(
            child: Icon(celebracion.icono, color: Paleta.white, size: 42),
          ),
        ),
      ],
    );
  }
}

/// Contador de puntos ganados, con el número que sube.
class _ContadorDePuntos extends StatelessWidget {
  const _ContadorDePuntos(this.celebracion);

  final CelebracionStage celebracion;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: Espacio.s4,
        vertical: Espacio.s3,
      ),
      decoration: BoxDecoration(
        color: celebracion.tier.colorPrimario.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(Radios.lg),
        border: Border.all(
          color: celebracion.tier.colorPrimario.withValues(alpha: 0.3),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.auto_awesome,
            color: celebracion.tier.colorPrimario,
            size: 18,
          ),
          const SizedBox(width: Espacio.s1),
          Text(
            '+',
            style: Tipo.cifra.copyWith(
              color: celebracion.tier.colorPrimario,
              fontWeight: FontWeight.w700,
            ),
          ),
          NumeroQueSube(
            valor: celebracion.puntos,
            etiqueta: 'Puntos ganados',
            estilo: Tipo.cifra.copyWith(
              color: celebracion.tier.colorPrimario,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(width: 4),
          Flexible(
            child: Text(
              'pts en mesa',
              style: Tipo.etiqueta.copyWith(
                color: celebracion.tier.colorPrimario,
                fontWeight: FontWeight.w700,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
