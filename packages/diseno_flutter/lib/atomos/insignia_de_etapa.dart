import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'aspecto_de_etapa.dart';

/// Dónde está la persona respecto de una etapa del camino.
enum EstadoDeEtapa {
  /// La etapa en la que está hoy.
  actual,

  /// Una etapa de abajo: ya la pasó.
  alcanzada,

  /// Una etapa de arriba: se abre con más reputación.
  bloqueada,
}

/// Tamaño de la insignia: en una fila de lista o como protagonista.
enum TamanoDeInsignia {
  chico(Espacio.s6),
  grande(Espacio.s7 + Espacio.s4);

  const TamanoDeInsignia(this.diametro);
  final double diametro;
}

/// La insignia redonda de una etapa: su color y su ícono, o un candado si todavía no
/// se alcanzó. La actual lleva un anillo de su propio color separado por un hueco, para
/// que se distinga de las alcanzadas también sin ver el color.
///
/// Nunca va sola: el nombre de la etapa se escribe al lado. Por eso su semántica es
/// opcional —con [etiqueta] se anuncia; sin ella, se oculta y la lee el texto vecino—.
class InsigniaDeEtapa extends StatelessWidget {
  const InsigniaDeEtapa({
    super.key,
    required this.orden,
    this.estado = EstadoDeEtapa.actual,
    this.tamano = TamanoDeInsignia.chico,
    this.etiqueta,
  });

  /// La posición de la etapa en el camino (0 es la de abajo).
  final int orden;
  final EstadoDeEtapa estado;
  final TamanoDeInsignia tamano;

  /// Nombre accesible, por ejemplo «Etapa 2, En marcha, la tuya».
  final String? etiqueta;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final bloqueada = estado == EstadoDeEtapa.bloqueada;
    final relleno = bloqueada ? t.surface2 : AspectoDeEtapa.relleno(orden);
    final disco = Container(
      width: tamano.diametro,
      height: tamano.diametro,
      decoration: BoxDecoration(
        color: relleno,
        shape: BoxShape.circle,
        border: bloqueada
            ? Border.all(color: t.border, width: Borde.fino)
            : null,
      ),
      alignment: Alignment.center,
      child: Icon(
        bloqueada ? AspectoDeEtapa.bloqueada : AspectoDeEtapa.icono(orden),
        size: tamano.diametro / 2,
        color: bloqueada ? t.text3 : AspectoDeEtapa.tintaSobre(orden),
      ),
    );
    final insignia = estado == EstadoDeEtapa.actual
        ? Container(
            padding: const EdgeInsets.all(Borde.desfase),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(
                color: AspectoDeEtapa.relleno(orden),
                width: Borde.desfase,
              ),
            ),
            child: disco,
          )
        : disco;
    if (etiqueta == null) return ExcludeSemantics(child: insignia);
    return Semantics(
      container: true,
      label: etiqueta,
      image: true,
      excludeSemantics: true,
      child: insignia,
    );
  }
}
