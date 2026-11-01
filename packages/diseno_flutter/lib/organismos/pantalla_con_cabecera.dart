import 'package:flutter/material.dart';

import '../moleculas/cabecera.dart';
import '../tokens/tokens.dart';

/// Una pantalla interior con la cabecera grande que se compacta al desplazar (ADR-053,
/// H4.S2.M6). Reemplaza al `AppBar` de fábrica.
///
/// Arriba, el título grande; al subir el contenido, el título se achica hasta quedar al
/// lado del botón de volver y aparece una línea fina debajo. El cambio sigue al dedo,
/// no es una animación con tiempo propio: no hay nada que apagar con movimiento
/// reducido. El [cuerpo] trae su propio desplazamiento (`SingleChildScrollView`,
/// `ListView`) y se conecta solo a la cabecera.
class PantallaConCabecera extends StatelessWidget {
  const PantallaConCabecera({
    super.key,
    required this.titulo,
    required this.cuerpo,
    this.accion,
    this.alVolver,
  });

  final String titulo;
  final Widget cuerpo;
  final Widget? accion;
  final VoidCallback? alVolver;

  @override
  Widget build(BuildContext context) {
    final arriba = MediaQuery.paddingOf(context).top;
    final escala = MediaQuery.textScalerOf(context);
    return LayoutBuilder(
      builder: (context, limites) {
        // El alto del título grande depende del ancho y del tamaño de letra del
        // sistema: se mide para que un título largo baje de renglón sin recortarse.
        final medida = TextPainter(
          text: TextSpan(text: titulo, style: Tipo.tituloPantalla),
          textScaler: escala,
          textDirection: Directionality.of(context),
          maxLines: 3,
        )..layout(maxWidth: limites.maxWidth - Espacio.s4 * 2);
        final altoDelTitulo = medida.height + Espacio.s3;
        medida.dispose();
        return NestedScrollView(
          headerSliverBuilder: (context, _) => [
            SliverPersistentHeader(
              pinned: true,
              delegate: _Cabecera(
                titulo: titulo,
                accion: accion,
                alVolver: alVolver,
                arriba: arriba,
                altoDelTitulo: altoDelTitulo,
              ),
            ),
          ],
          body: MediaQuery.removePadding(
            context: context,
            removeTop: true,
            child: cuerpo,
          ),
        );
      },
    );
  }
}

class _Cabecera extends SliverPersistentHeaderDelegate {
  _Cabecera({
    required this.titulo,
    required this.accion,
    required this.alVolver,
    required this.arriba,
    required this.altoDelTitulo,
  });

  final String titulo;
  final Widget? accion;
  final VoidCallback? alVolver;
  final double arriba;
  final double altoDelTitulo;

  static const _fila = kToolbarHeight;

  /// Desde qué parte del recorrido aparece el título chico.
  static const _entraElChico = 0.6;

  @override
  double get minExtent => arriba + _fila;

  @override
  double get maxExtent => arriba + _fila + altoDelTitulo;

  @override
  Widget build(BuildContext context, double desplazado, bool encima) {
    final t = Tokens.of(context);
    final avance = (desplazado / (maxExtent - minExtent)).clamp(0.0, 1.0);
    final puedeVolver = alVolver != null || Navigator.of(context).canPop();
    return DecoratedBox(
      decoration: BoxDecoration(
        color: t.bg,
        border: Border(
          bottom: BorderSide(
            color: t.border.withValues(alpha: avance),
            width: Borde.fino,
          ),
        ),
      ),
      child: Padding(
        padding: EdgeInsets.only(top: arriba),
        child: Stack(
          fit: StackFit.expand,
          children: [
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              height: _fila,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
                child: Row(
                  children: [
                    if (puedeVolver)
                      Padding(
                        padding: const EdgeInsets.only(right: Espacio.s3),
                        child: BotonDeCabecera(
                          icono: Icons.arrow_back,
                          etiqueta: 'Volver',
                          onTap:
                              alVolver ??
                              () => Navigator.of(context).maybePop(),
                        ),
                      ),
                    // El título chico es el mismo que el grande: el lector lo lee una vez.
                    Expanded(
                      child: ExcludeSemantics(
                        child: Opacity(
                          // Entra recién cuando el grande ya se fue debajo de la fila.
                          opacity:
                              ((avance - _entraElChico) / (1 - _entraElChico))
                                  .clamp(0.0, 1.0),
                          child: Text(
                            titulo,
                            style: Tipo.titulo3.copyWith(color: t.text),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ),
                    ),
                    ?accion,
                  ],
                ),
              ),
            ),
            // El título grande vive en su franja, debajo de la fila: al compactarse la
            // franja, sube con el contenido y se recorta en el borde de la fila. Nunca
            // se superpone al título chico.
            Positioned(
              top: _fila,
              left: Espacio.s4,
              right: Espacio.s4,
              bottom: 0,
              child: ClipRect(
                child: OverflowBox(
                  alignment: Alignment.bottomLeft,
                  minHeight: 0,
                  maxHeight: altoDelTitulo,
                  child: Align(
                    alignment: Alignment.topLeft,
                    child: Semantics(
                      header: true,
                      child: Opacity(
                        opacity: 1 - avance,
                        child: Text(
                          titulo,
                          style: Tipo.tituloPantalla.copyWith(color: t.text),
                          maxLines: 3,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  bool shouldRebuild(_Cabecera viejo) =>
      viejo.titulo != titulo ||
      viejo.accion != accion ||
      viejo.arriba != arriba ||
      viejo.altoDelTitulo != altoDelTitulo;
}
