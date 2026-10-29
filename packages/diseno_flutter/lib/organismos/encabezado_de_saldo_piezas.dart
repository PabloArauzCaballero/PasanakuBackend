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

/// Lo que entra y sale al ocultar o mostrar el saldo (H5.S2.M7): la cifra que se va se
/// desenfoca mientras se funde, así el número deja de leerse antes de desaparecer. Con
/// movimiento reducido el cambio es inmediato (la duración llega en cero).
Widget _desenfoqueYFundido(
  Widget hijo,
  Animation<double> avance,
) => AnimatedBuilder(
  animation: avance,
  child: FadeTransition(opacity: avance, child: hijo),
  builder: (context, conFundido) {
    final sigma = Movimiento.desenfoqueDeSaldo * (1 - avance.value);
    // El filtro está siempre en el árbol y se apaga con `enabled`: ponerlo y
    // sacarlo cambiaba la forma del árbol, recreaba la cifra que se iba y su
    // rodillo volvía a arrancar desde cero — mostraba importes que no eran.
    return ImageFiltered(
      enabled: sigma >= 0.01,
      imageFilter: ImageFilter.blur(sigmaX: sigma, sigmaY: sigma),
      child: conFundido,
    );
  },
);

/// Mientras conviven la cifra que se va y la que llega, las dos quedan alineadas a la
/// izquierda, donde vive el saldo: centradas, la que llega saltaba al final.
Widget _alineadasALaIzquierda(Widget? actual, List<Widget> previas) =>
    Stack(alignment: Alignment.centerLeft, children: [...previas, ?actual]);
