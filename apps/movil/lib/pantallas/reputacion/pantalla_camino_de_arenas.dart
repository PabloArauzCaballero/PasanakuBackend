import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/camino_de_arenas.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/camino_de_arenas.dart';
import 'tarjeta_de_arena.dart';
import 'textos_de_arenas.dart';

/// El camino entero de arenas: la propia arriba, con su avance, y debajo todas las
/// arenas del catálogo —las alcanzadas, la actual y las bloqueadas con su umbral—.
///
/// Sin historial no es un vacío: el servidor da la arena del puntaje base (Semilla en
/// el modelo v1) y se muestra como cualquier otra, con una línea que lo explica.
/// Vacío es solo que no haya catálogo publicado.
class PantallaCaminoDeArenas extends ConsumerWidget {
  const PantallaCaminoDeArenas({super.key, required this.usuarioId});
  final String usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final camino = ref.watch(caminoDeArenasProvider(usuarioId));
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosDeArenas.titulo),
              Expanded(
                child: EstadoDePantalla<CaminoDelUsuario>(
                  valor: camino,
                  etiquetaDeCarga: TextosDeArenas.cargando,
                  tituloVacio: TextosDeArenas.tituloSinArenas,
                  mensajeVacio: TextosDeArenas.sinArenas,
                  iconoVacio: IconosDeMarca.reputacion,
                  vacio: (c) => c.vacio,
                  reintentar: () =>
                      ref.invalidate(caminoDeArenasProvider(usuarioId)),
                  exito: (c) => _Camino(camino: c),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Camino extends StatelessWidget {
  const _Camino({required this.camino});
  final CaminoDelUsuario camino;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final arena = camino.puntaje.arena!;
    return ListView(
      padding: const EdgeInsets.only(top: Espacio.s4, bottom: Espacio.s6),
      children: [
        TarjetaDeArena(
          arena: arena,
          puntaje: camino.puntaje.puntaje,
          sinHistorial: !camino.puntaje.tieneHistorial,
        ),
        const SizedBox(height: Espacio.s4),
        Text(
          TextosDeArenas.comoSeSube,
          style: Tipo.cuerpoChico.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s4),
        CaminoDeArenas(peldanos: peldanosDe(camino.arenas, arena)),
      ],
    );
  }
}
