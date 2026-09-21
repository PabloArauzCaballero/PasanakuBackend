import 'package:flutter/material.dart';

import '../atomos/progreso.dart';
import '../tokens/tokens.dart';
import 'acciones_del_globo.dart';

/// **El globo del tutorial**: una idea, un elemento, una acción.
///
/// Es presentación pura: no sabe qué tutorial corre ni cómo se guarda el avance.
/// Recibe textos y avisa qué tocó la persona.
///
/// En el teléfono no flota al lado del elemento como en el escritorio: se apoya abajo,
/// ancho completo, donde llega el pulgar y donde no tapa lo que está señalando.
class GloboDeTutorial extends StatelessWidget {
  const GloboDeTutorial({
    super.key,
    required this.titulo,
    required this.descripcion,
    required this.indice,
    required this.total,
    required this.alAvanzar,
    required this.alRetroceder,
    required this.alSalir,
    this.problema,
    this.alReintentar,
    this.textoAvanzar = 'Siguiente',
    this.textoSalir = 'Omitir el tutorial',
    this.arriba = false,
  });

  final String titulo;
  final String descripcion;
  final int indice;
  final int total;

  /// Qué salió mal, si algo salió mal. Aparece como aviso, sin tapar el paso.
  final String? problema;
  final VoidCallback alAvanzar;
  final VoidCallback alRetroceder;
  final VoidCallback alSalir;
  final VoidCallback? alReintentar;
  final String textoAvanzar;
  final String textoSalir;

  /// El globo está apoyado **arriba** de la pantalla, no abajo: redondea del otro lado
  /// y deja el aire de la barra de estado en vez del de la barra de gestos.
  final bool arriba;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final cuenta = 'Paso ${indice + 1} de $total';
    return Semantics(
      container: true,
      liveRegion: true,
      label: '$cuenta. $titulo',
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(Espacio.s4),
        decoration: BoxDecoration(
          color: t.surface,
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(arriba ? 0 : Radios.xl),
            bottom: Radius.circular(arriba ? Radios.xl : 0),
          ),
          border: Border.all(color: t.border, width: Borde.fino),
          boxShadow: [t.sombra3],
        ),
        child: SafeArea(
          top: arriba,
          bottom: !arriba,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // La cabecera se acota a un área táctil de alto: sin eso el `Row` hereda
              // la altura sin límite de la hoja y desborda. La cuenta se recorta con
              // puntos suspensivos antes que empujar al botón de cerrar fuera.
              SizedBox(
                height: Tactil.minimo,
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        cuenta,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Tipo.etiqueta.copyWith(color: t.text3),
                      ),
                    ),
                    // La etiqueta va en el ícono, que el `IconButton` fusiona en su
                    // propio nodo. Ni un `Semantics` de afuera —no alcanza al nodo del
                    // botón, y quedaba un tocable de 48 dp sin nombre— ni un `tooltip`:
                    // el globo se dibuja POR ENCIMA del `Navigator`, ahí no hay
                    // `Overlay` donde mostrarlo y el botón ni siquiera construía.
                    IconButton(
                      onPressed: alSalir,
                      icon: const Icon(
                        Icons.close,
                        semanticLabel: 'Cerrar el tutorial',
                      ),
                      color: t.text3,
                      constraints: const BoxConstraints(
                        minWidth: Tactil.minimo,
                        minHeight: Tactil.minimo,
                      ),
                    ),
                  ],
                ),
              ),
              Text(titulo, style: Tipo.titulo3.copyWith(color: t.text)),
              const SizedBox(height: Espacio.s2),
              Text(descripcion, style: Tipo.cuerpo.copyWith(color: t.text2)),
              if (problema != null) ...[
                const SizedBox(height: Espacio.s3),
                _Aviso(texto: problema!),
              ],
              const SizedBox(height: Espacio.s4),
              Progreso(
                valor: total == 0 ? 0 : (indice + 1) / total,
                etiqueta: 'Avance del tutorial',
              ),
              const SizedBox(height: Espacio.s4),
              AccionesDelGlobo(
                indice: indice,
                textoAvanzar: textoAvanzar,
                textoSalir: textoSalir,
                alAvanzar: alAvanzar,
                alRetroceder: alRetroceder,
                alSalir: alSalir,
                alReintentar: problema == null ? null : alReintentar,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Aviso extends StatelessWidget {
  const _Aviso({required this.texto});

  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: Espacio.s3,
        vertical: Espacio.s2,
      ),
      decoration: BoxDecoration(
        color: t.warnBg,
        borderRadius: BorderRadius.circular(Radios.md),
      ),
      child: Text(texto, style: Tipo.cuerpoChico.copyWith(color: t.avisoTexto)),
    );
  }
}
