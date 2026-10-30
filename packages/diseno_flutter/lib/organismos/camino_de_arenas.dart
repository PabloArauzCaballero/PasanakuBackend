import 'package:flutter/material.dart';

import '../atomos/aspecto_de_arena.dart';
import '../atomos/insignia_de_arena.dart';
import '../tokens/tokens.dart';

/// Una arena del camino, ya traducida a lo que se ve. Los textos los pone quien la
/// usa: el sistema de diseño no sabe de puntajes ni de catálogos.
class PeldanoDeArena {
  const PeldanoDeArena({
    required this.nombre,
    required this.orden,
    required this.estado,
    required this.detalle,
    required this.etiqueta,
    this.marca,
    this.beneficio,
  });

  final String nombre;
  final int orden;
  final EstadoDeArena estado;

  /// El umbral o la situación: «Desde 600 puntos», «Se abre con 600 puntos».
  final String detalle;

  /// Lo que anuncia el lector de pantalla para todo el peldaño.
  final String etiqueta;

  /// Rótulo corto junto al nombre de la actual: «Tu arena».
  final String? marca;

  /// Lo que la arena permite, si está vigente: «Grupos de hasta 12 cupos».
  final String? beneficio;
}

/// El camino entero de arenas, **de arriba hacia abajo como una montaña**: la más alta
/// arriba, la actual destacada, las de abajo con su color (alcanzadas) y las de arriba
/// con candado y su umbral (bloqueadas). Cada tramo del camino se pinta con el color de
/// la arena de la que sale si ya se recorrió.
///
/// Recibe los peldaños en orden ascendente. No se desplaza solo: la arena actual se
/// pone a la vista aparte, arriba de la pantalla (una [BarraDeArena]), y el camino lo
/// recorre la persona con el dedo (regla 95.5.2).
class CaminoDeArenas extends StatelessWidget {
  const CaminoDeArenas({super.key, required this.peldanos});

  final List<PeldanoDeArena> peldanos;

  @override
  Widget build(BuildContext context) {
    final deArriba = peldanos.reversed.toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (var i = 0; i < deArriba.length; i++)
          _Peldano(
            peldano: deArriba[i],
            tramoDeArriba: i == 0 ? null : deArriba[i - 1],
            tramoDeAbajo: i == deArriba.length - 1 ? null : deArriba[i + 1],
          ),
      ],
    );
  }
}

class _Peldano extends StatelessWidget {
  const _Peldano({
    required this.peldano,
    required this.tramoDeArriba,
    required this.tramoDeAbajo,
  });

  final PeldanoDeArena peldano;
  final PeldanoDeArena? tramoDeArriba;
  final PeldanoDeArena? tramoDeAbajo;

  bool get _actual => peldano.estado == EstadoDeArena.actual;

  /// Un tramo está recorrido si la arena de arriba ya no está bloqueada; toma el color
  /// de la arena de abajo, que es de la que se sale.
  Color? _colorDelTramo(
    PeldanoDeArena? arriba,
    PeldanoDeArena? abajo,
    Tokens t,
  ) {
    if (arriba == null || abajo == null) return null;
    if (arriba.estado == EstadoDeArena.bloqueada) return t.border;
    return AspectoDeArena.relleno(abajo.orden);
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final arriba = _colorDelTramo(tramoDeArriba, peldano, t);
    final abajo = _colorDelTramo(peldano, tramoDeAbajo, t);
    return Semantics(
      container: true,
      label: peldano.etiqueta,
      excludeSemantics: true,
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SizedBox(
              width: TamanoDeInsignia.grande.diametro,
              child: Column(
                children: [
                  Expanded(child: _Tramo(color: arriba)),
                  InsigniaDeArena(
                    orden: peldano.orden,
                    estado: peldano.estado,
                    tamano: _actual
                        ? TamanoDeInsignia.grande
                        : TamanoDeInsignia.chico,
                  ),
                  Expanded(child: _Tramo(color: abajo)),
                ],
              ),
            ),
            const SizedBox(width: Espacio.s3),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: Espacio.s3),
                child: Container(
                  padding: const EdgeInsets.all(Espacio.s3),
                  decoration: _actual
                      ? BoxDecoration(
                          color: t.surface,
                          borderRadius: BorderRadius.circular(Radios.lg),
                          border: Border.all(
                            color: t.brand,
                            width: Borde.desfase,
                          ),
                        )
                      : null,
                  child: _Textos(peldano: peldano),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Textos extends StatelessWidget {
  const _Textos({required this.peldano});
  final PeldanoDeArena peldano;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final bloqueada = peldano.estado == EstadoDeArena.bloqueada;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Wrap(
          spacing: Espacio.s2,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Text(
              peldano.nombre,
              style: Tipo.titulo3.copyWith(color: bloqueada ? t.text2 : t.text),
            ),
            if (peldano.marca != null)
              Text(
                peldano.marca!,
                style: Tipo.etiqueta.copyWith(color: t.brandTexto),
              ),
          ],
        ),
        Text(peldano.detalle, style: Tipo.cuerpoChico.copyWith(color: t.text2)),
        if (peldano.beneficio != null)
          Text(
            peldano.beneficio!,
            style: Tipo.cuerpoChico.copyWith(color: t.text3),
          ),
      ],
    );
  }
}

/// Un segmento del camino entre dos insignias. Sin color, no se dibuja (los extremos).
class _Tramo extends StatelessWidget {
  const _Tramo({required this.color});
  final Color? color;

  @override
  Widget build(BuildContext context) => Center(
    child: Container(width: Borde.foco, color: color),
  );
}
