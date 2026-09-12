import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/consultar_puntaje.dart';
import 'textos.dart';

/// "Tu nivel" (D-20): el puntaje propio y el nivel de confianza, tal como
/// `transparencia` los calcula. `SIN_HISTORIAL` se muestra igual que cualquier otro
/// nivel — nunca como un error ni como un castigo (CU-71).
class PantallaMiPuntaje extends ConsumerWidget {
  const PantallaMiPuntaje({super.key, required this.usuarioId});
  final String usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final puntaje = ref.watch(puntajeProvider(usuarioId));
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloMiPuntaje)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: EstadoDePantalla<PuntajeDelUsuario>(
            valor: puntaje,
            etiquetaDeCarga: TextosPasanaku.cargando,
            mensajeVacio: TextosPasanaku.sinHistorial,
            vacio: (p) => !p.tieneHistorial,
            reintentar: () => ref.invalidate(puntajeProvider(usuarioId)),
            exito: (p) => Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  p.puntaje,
                  style: Theme.of(context).textTheme.displaySmall,
                ),
                const SizedBox(height: Espacio.s2),
                Text(p.nivelDeConfianza),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
