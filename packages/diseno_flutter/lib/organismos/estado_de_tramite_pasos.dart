part of 'estado_de_tramite.dart';

/// Las etapas del camino: un punto por etapa, unidos por una línea; el actual en tinta.
class _Pasos extends StatelessWidget {
  const _Pasos({
    required this.pasos,
    required this.actual,
    required this.fallo,
    required this.tokens,
    required this.frente,
  });

  final List<String> pasos;
  final int actual;
  final bool fallo;
  final Tokens tokens;
  final Color frente;

  @override
  Widget build(BuildContext context) {
    final t = tokens;
    const dimension = Espacio.s5;
    Widget punto(int i) {
      final hecho = i < actual || (actual >= pasos.length);
      final enCurso = i == actual && actual < pasos.length;
      final fallido = enCurso && fallo;
      final color = fallido
          ? t.errTexto
          : (hecho || enCurso)
          ? t.verdeSolido
          : t.border;
      return DecoratedBox(
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: hecho ? color : t.surface,
          border: Border.all(color: color, width: Borde.foco - Borde.fino),
        ),
        child: SizedBox.square(
          dimension: dimension,
          child: hecho
              ? Icon(
                  Icons.check,
                  size: Espacio.s4 - Espacio.s1,
                  color: t.sobreVerdeSolido,
                )
              : fallido
              ? Icon(Icons.close, size: Espacio.s4 - Espacio.s1, color: color)
              : null,
        ),
      );
    }

    final etiquetas = [
      for (final (i, p) in pasos.indexed)
        'Paso ${i + 1} de ${pasos.length}: $p, '
            '${i < actual || actual >= pasos.length
                ? 'hecho'
                : i == actual
                ? (fallo ? 'no se pudo completar' : 'en curso')
                : 'pendiente'}',
    ];
    return Semantics(
      container: true,
      label: etiquetas.join('. '),
      child: ExcludeSemantics(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final (i, p) in pasos.indexed) ...[
              if (i > 0)
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(top: dimension / 2 - 1),
                    child: Divider(
                      height: Borde.fino,
                      thickness: Borde.foco - Borde.fino,
                      color: i <= actual ? t.verdeSolido : t.border,
                    ),
                  ),
                ),
              SizedBox(
                // Ancho de una palabra de ocho letras: «Recibido» no se parte a la mitad.
                width: Tactil.minimo + Espacio.s6 + Espacio.s2,
                child: Column(
                  children: [
                    punto(i),
                    const SizedBox(height: Espacio.s1),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        p,
                        maxLines: 1,
                        style: Tipo.cuerpoChico.copyWith(
                          color: i == actual ? t.text : t.text2,
                          fontWeight: i == actual
                              ? FontWeight.w600
                              : FontWeight.w400,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
