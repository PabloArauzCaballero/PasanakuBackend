part of 'encabezado_de_saldo.dart';

class _BotonDeOcultar extends StatelessWidget {
  const _BotonDeOcultar({required this.oculto, required this.alTocar});

  final bool oculto;
  final VoidCallback alTocar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return IconButton(
      onPressed: alTocar,
      tooltip: oculto ? 'Mostrar saldo' : 'Ocultar saldo',
      icon: Icon(
        oculto ? IconosDeMarca.ocultarSaldo : IconosDeMarca.mostrarSaldo,
        size: Espacio.s4 + Espacio.s1 / 2,
      ),
      color: t.heroTexto,
      style: IconButton.styleFrom(
        backgroundColor: t.heroTexto.withValues(alpha: 0.12),
      ),
      constraints: const BoxConstraints.tightFor(
        width: Tactil.minimo,
        height: Tactil.minimo,
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({required this.hijo});

  final Widget hijo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return MergeSemantics(
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: t.heroTexto.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(Radios.pill),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: Espacio.s3,
            vertical: Espacio.s1 + Espacio.s1 / 2,
          ),
          child: DefaultTextStyle(
            style: Tipo.cuerpoChico.copyWith(color: t.heroTexto),
            child: hijo,
          ),
        ),
      ),
    );
  }
}

/// El escudo y la frase de custodia, en el tono secundario de la tarjeta: se lee, pero
/// no compite con el saldo. Es su propio nodo para el lector: pegada al saldo, se
/// anunciaría como parte de la cifra.
class _Garantia extends StatelessWidget {
  const _Garantia({required this.texto});

  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return MergeSemantics(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ExcludeSemantics(
            child: Icon(
              IconosDeMarca.custodia,
              size: Espacio.s4,
              color: t.heroTexto2,
            ),
          ),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Text(
              texto,
              style: Tipo.cuerpoChico.copyWith(color: t.heroTexto2),
            ),
          ),
        ],
      ),
    );
  }
}
