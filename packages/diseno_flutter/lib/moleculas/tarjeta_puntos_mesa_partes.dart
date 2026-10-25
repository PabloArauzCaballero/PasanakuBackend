part of 'tarjeta_puntos_mesa.dart';

/// Cabecera: ícono y título del juego a la izquierda, rango y nivel a la derecha.
class _CabeceraDePuntos extends StatelessWidget {
  const _CabeceraDePuntos({
    required this.rango,
    required this.nivel,
    required this.colorOro,
  });

  final String rango;
  final int nivel;
  final Color colorOro;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Row(
            children: [
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: colorOro.withValues(alpha: 0.18),
                ),
                child: Icon(Icons.casino_outlined, size: 18, color: colorOro),
              ),
              const SizedBox(width: Espacio.s2),
              Expanded(
                child: Text(
                  'PUNTOS EN JUEGO',
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
            gradient: LinearGradient(
              colors: [colorOro, colorOro.withValues(alpha: 0.8)],
            ),
            borderRadius: BorderRadius.circular(Radios.pill),
            boxShadow: [
              BoxShadow(
                color: colorOro.withValues(alpha: 0.35),
                blurRadius: 8,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.stars_rounded, size: 14, color: Paleta.white),
              const SizedBox(width: 4),
              Text(
                '$rango · Nivel $nivel',
                style: Tipo.etiqueta.copyWith(
                  color: Paleta.white,
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
