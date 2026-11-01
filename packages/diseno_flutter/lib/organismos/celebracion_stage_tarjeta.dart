part of 'celebracion_stage.dart';

/// Tarjeta central de la celebración: ícono, textos, puntos y botón.
class _TarjetaCentral extends StatelessWidget {
  const _TarjetaCentral({required this.celebracion});

  final CelebracionStage celebracion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
      constraints: const BoxConstraints(maxWidth: 380),
      padding: const EdgeInsets.fromLTRB(
        Espacio.s5,
        Espacio.s7,
        Espacio.s5,
        Espacio.s5,
      ),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.xl),
        border: Border.all(
          color: celebracion.tier.colorPrimario.withValues(alpha: 0.6),
          width: 2,
        ),
        boxShadow: [
          BoxShadow(
            color: celebracion.tier.colorPrimario.withValues(alpha: 0.35),
            blurRadius: 36,
            spreadRadius: 4,
            offset: const Offset(0, 8),
          ),
          BoxShadow(
            color: Paleta.ink.withValues(alpha: 0.3),
            blurRadius: 24,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Ícono monumental con halo
          _IconoConHalo(celebracion),
          const SizedBox(height: Espacio.s4),

          // Eyebrow
          Text(
            celebracion.eyebrow.toUpperCase(),
            style: Tipo.etiqueta.copyWith(
              color: celebracion.tier.colorPrimario,
              letterSpacing: 2.2,
              fontWeight: FontWeight.w800,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: Espacio.s2),

          // Título principal
          Text(
            celebracion.nombre,
            style: Tipo.titulo1.copyWith(
              color: t.text,
              fontWeight: FontWeight.w800,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: Espacio.s2),

          // Descripción o historia del logro
          Text(
            celebracion.flavorText ?? celebracion.descripcion,
            style: Tipo.cuerpo.copyWith(color: t.text2, height: 1.45),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: Espacio.s5),

          // Contador de puntos con animación
          _ContadorDePuntos(celebracion),
          const SizedBox(height: Espacio.s5),

          // Botones de acción
          SizedBox(
            width: double.infinity,
            child: Boton(
              texto: 'Continuar en el Pasanaku',
              variante: BotonVariante.primario,
              onPressed:
                  celebracion.onCerrar ?? () => Navigator.of(context).pop(),
            ),
          ),
        ],
      ),
    );
  }
}
