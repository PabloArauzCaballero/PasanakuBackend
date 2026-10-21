part of 'insignia_tile.dart';

/// El círculo con el ícono (o el candado si está bloqueada).
Widget _circuloDelIcono(
  Tokens t,
  InsigniaTier tier,
  bool conseguida,
  IconData icono,
  double? progreso,
) {
  return Container(
    width: 48,
    height: 48,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      color: conseguida
          ? tier.colorPrimario.withValues(alpha: 0.18)
          : t.surface2,
      border: Border.all(
        color: conseguida ? tier.colorPrimario : t.border,
        width: 1.5,
      ),
    ),
    child: Center(
      child: Icon(
        conseguida ? icono : (progreso != null ? icono : Icons.lock_outline),
        color: conseguida ? tier.colorPrimario : t.text3,
        size: 24,
      ),
    ),
  );
}

/// La insignia con los puntos ganados de una insignia conseguida.
Widget _insigniaDePuntos(InsigniaTier tier, int puntos) {
  return Container(
    padding: const EdgeInsets.symmetric(horizontal: Espacio.s2, vertical: 2),
    decoration: BoxDecoration(
      color: tier.colorPrimario.withValues(alpha: 0.15),
      borderRadius: BorderRadius.circular(Radios.sm),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(Icons.auto_awesome, size: 13, color: tier.colorPrimario),
        const SizedBox(width: 4),
        Text(
          '+$puntos pts',
          style: Tipo.etiqueta.copyWith(
            color: tier.colorPrimario,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    ),
  );
}
