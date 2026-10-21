part of 'celebracion_stage.dart';

/// La tarjeta central de [CelebracionStage].
Widget _tarjetaDeCelebracion(
  BuildContext context,
  CelebracionStage widget,
  Tokens t,
) {
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
        color: widget.tier.colorPrimario.withValues(alpha: 0.6),
        width: 2,
      ),
      boxShadow: [
        BoxShadow(
          color: widget.tier.colorPrimario.withValues(alpha: 0.35),
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
        _iconoConHalo(widget),
        const SizedBox(height: Espacio.s4),

        // Eyebrow
        Text(
          widget.eyebrow.toUpperCase(),
          style: Tipo.etiqueta.copyWith(
            color: widget.tier.colorPrimario,
            letterSpacing: 2.2,
            fontWeight: FontWeight.w800,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Espacio.s2),

        // Título principal
        Text(
          widget.nombre,
          style: Tipo.titulo1.copyWith(
            color: t.text,
            fontWeight: FontWeight.w800,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Espacio.s2),

        // Descripción o historia del logro
        Text(
          widget.flavorText ?? widget.descripcion,
          style: Tipo.cuerpo.copyWith(color: t.text2, height: 1.45),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Espacio.s5),

        // Contador de puntos con animación
        _contadorDePuntos(widget),
        const SizedBox(height: Espacio.s5),

        // Botones de acción
        SizedBox(
          width: double.infinity,
          child: Boton(
            texto: 'Continuar en el Pasanaku',
            variante: BotonVariante.primario,
            onPressed: widget.onCerrar ?? () => Navigator.of(context).pop(),
          ),
        ),
      ],
    ),
  );
}

/// La estampilla inclinada sobre la tarjeta.
Widget _estampillaDeLogro(CelebracionStage widget) {
  return Positioned(
    top: -14,
    child: Transform.rotate(
      angle: -0.1,
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: Espacio.s4,
          vertical: Espacio.s2,
        ),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [
              widget.tier.colorPrimario,
              widget.tier.colorPrimario.withValues(alpha: 0.85),
            ],
          ),
          borderRadius: BorderRadius.circular(Radios.pill),
          boxShadow: [
            BoxShadow(
              color: widget.tier.colorPrimario.withValues(alpha: 0.5),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Text(
          '¡NUEVO LOGRO!',
          style: Tipo.etiqueta.copyWith(
            color: Paleta.white,
            fontWeight: FontWeight.w900,
            letterSpacing: 1.5,
          ),
        ),
      ),
    ),
  );
}
