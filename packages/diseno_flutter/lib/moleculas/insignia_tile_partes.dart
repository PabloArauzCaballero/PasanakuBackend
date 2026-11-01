part of 'insignia_tile.dart';

/// Icono del logro a la izquierda; nombre, puntos o estado y descripción a la derecha.
class _EncabezadoDeInsignia extends StatelessWidget {
  const _EncabezadoDeInsignia(this.insignia);

  final InsigniaTile insignia;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final InsigniaTile(
      :nombre,
      :descripcion,
      :flavorText,
      :icono,
      :puntos,
      :tier,
      :conseguida,
      :progreso,
    ) = insignia;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
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
              conseguida
                  ? icono
                  : (progreso != null ? icono : Icons.lock_outline),
              color: conseguida ? tier.colorPrimario : t.text3,
              size: 24,
            ),
          ),
        ),
        const SizedBox(width: Espacio.s3),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      nombre,
                      style: Tipo.titulo3.copyWith(
                        color: conseguida ? t.text : t.text2,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  if (conseguida)
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: Espacio.s2,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: tier.colorPrimario.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(Radios.sm),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.auto_awesome,
                            size: 13,
                            color: tier.colorPrimario,
                          ),
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
                    )
                  else
                    ChipEstado(
                      texto: progreso != null ? 'En curso' : 'Bloqueada',
                      tono: progreso != null ? Tono.aviso : Tono.neutro,
                    ),
                ],
              ),
              const SizedBox(height: Espacio.s1),
              Text(
                conseguida && flavorText != null ? flavorText : descripcion,
                style: Tipo.cuerpoChico.copyWith(
                  color: conseguida ? t.text2 : t.text3,
                  height: 1.35,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
