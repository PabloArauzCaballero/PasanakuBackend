import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/esqueleto.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/arena_minima.dart';
import 'textos_de_arena_e_inicio.dart';

/// La arena mínima del grupo nuevo (2026-10-30): por omisión la de quien organiza, y
/// nunca una más alta. Es opcional: si el catálogo no llega, el formulario sigue y el
/// servidor usa la arena actual de quien organiza.
class SelectorDeArenaMinima extends ConsumerWidget {
  const SelectorDeArenaMinima({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final opciones = ref.watch(opcionesDeArenaProvider);
    final elegida = ref.watch(arenaMinimaElegidaProvider);
    return switch (opciones) {
      AsyncData(:final value) when value.elegibles.isNotEmpty => _Campo(
        opciones: value,
        elegida: elegida ?? value.propia!.codigo,
        onElegida: ref.read(arenaMinimaElegidaProvider.notifier).elegir,
      ),
      AsyncData() => const _Nota(texto: TextosDeArenaEInicio.arenaSinCatalogo),
      AsyncError() => _Nota(
        texto: TextosDeArenaEInicio.arenaError,
        reintentar: () => ref.invalidate(opcionesDeArenaProvider),
      ),
      _ => Semantics(
        label: TextosDeArenaEInicio.arenaCargando,
        child: const Esqueleto(filas: 1, alto: Tactil.minimo),
      ),
    };
  }
}

class _Campo extends StatelessWidget {
  const _Campo({
    required this.opciones,
    required this.elegida,
    required this.onElegida,
  });
  final OpcionesDeArena opciones;
  final String elegida;
  final ValueChanged<String> onElegida;

  @override
  Widget build(BuildContext context) {
    final nombre = opciones.elegibles
        .firstWhere((a) => a.codigo == elegida, orElse: () => opciones.propia!)
        .nombre;
    return CampoDeSeleccion<String>(
      etiqueta: TextosDeArenaEInicio.arenaEtiqueta,
      valor: elegida,
      ayuda: TextosDeArenaEInicio.arenaAyuda(nombre),
      opciones: [
        for (final a in opciones.elegibles.reversed)
          (
            valor: a.codigo,
            texto: a.nombre,
            detalle: TextosDeArenaEInicio.arenaDesde(
              _puntos(a),
              esLaTuya: a.codigo == opciones.propia!.codigo,
            ),
          ),
      ],
      onElegida: onElegida,
    );
  }

  /// «500.00» → «500»: los tramos son enteros; si no lo fueran, se muestran tal cual.
  static String _puntos(ArenaDelCatalogo a) => a.desde.endsWith('.00')
      ? a.desde.substring(0, a.desde.length - 3)
      : a.desde;
}

/// Lo que se ve cuando no hay nada para elegir: dice por qué y que se puede seguir.
class _Nota extends StatelessWidget {
  const _Nota({required this.texto, this.reintentar});
  final String texto;
  final VoidCallback? reintentar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          TextosDeArenaEInicio.arenaEtiqueta,
          style: Tipo.campoEtiqueta.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s2),
        Text(texto, style: Tipo.ayuda.copyWith(color: t.text3)),
        if (reintentar != null)
          Boton(
            texto: TextosDeArenaEInicio.reintentar,
            variante: BotonVariante.enlace,
            onPressed: reintentar,
          ),
      ],
    );
  }
}
