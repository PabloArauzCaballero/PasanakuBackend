part of 'celebracion_stage.dart';

/// El ícono del logro con su halo.
Widget _iconoConHalo(CelebracionStage widget) {
  return Stack(
    alignment: Alignment.center,
    children: [
      Container(
        width: 104,
        height: 104,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: widget.tier.colorPrimario.withValues(alpha: 0.14),
          border: Border.all(
            color: widget.tier.colorPrimario.withValues(alpha: 0.35),
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
              widget.tier.colorPrimario,
              widget.tier.colorPrimario.withValues(alpha: 0.75),
            ],
          ),
          boxShadow: [
            BoxShadow(
              color: widget.tier.colorPrimario.withValues(alpha: 0.45),
              blurRadius: 18,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Center(child: Icon(widget.icono, color: Paleta.white, size: 42)),
      ),
    ],
  );
}

/// El contador de puntos que sube.
Widget _contadorDePuntos(CelebracionStage widget) {
  return Container(
    padding: const EdgeInsets.symmetric(
      horizontal: Espacio.s4,
      vertical: Espacio.s3,
    ),
    decoration: BoxDecoration(
      color: widget.tier.colorPrimario.withValues(alpha: 0.12),
      borderRadius: BorderRadius.circular(Radios.lg),
      border: Border.all(
        color: widget.tier.colorPrimario.withValues(alpha: 0.3),
      ),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(Icons.auto_awesome, color: widget.tier.colorPrimario, size: 20),
        const SizedBox(width: Espacio.s2),
        Text(
          '+',
          style: Tipo.cifra.copyWith(
            color: widget.tier.colorPrimario,
            fontWeight: FontWeight.w700,
          ),
        ),
        NumeroQueSube(
          valor: widget.puntos,
          etiqueta: 'Puntos ganados',
          estilo: Tipo.cifra.copyWith(
            color: widget.tier.colorPrimario,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(width: 6),
        Text(
          'puntos en la mesa',
          style: Tipo.etiqueta.copyWith(
            color: widget.tier.colorPrimario,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    ),
  );
}
