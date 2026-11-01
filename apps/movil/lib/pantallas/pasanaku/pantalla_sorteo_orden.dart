part of 'pantalla_sorteo.dart';

/// El orden de cobro y, terminada la ruleta, el botón para volver.
class _OrdenConCierre extends StatelessWidget {
  const _OrdenConCierre({required this.puestos, required this.sorteando});

  final List<PuestoDeCobro> puestos;
  final bool sorteando;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        OrdenDeCobro(puestos: puestos),
        if (!sorteando) ...[
          const SizedBox(height: Espacio.s4),
          Boton(
            texto: TextosDetalleDelGrupo.listo,
            variante: BotonVariante.secundario,
            expandido: true,
            onPressed: () => context.canPop() ? context.pop() : null,
          ),
        ],
      ],
    );
  }
}
