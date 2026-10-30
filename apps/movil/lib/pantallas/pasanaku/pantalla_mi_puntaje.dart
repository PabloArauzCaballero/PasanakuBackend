import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/numero_que_sube.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../reputacion/pantalla_camino_de_arenas.dart';
import '../reputacion/tarjeta_de_arena.dart';
import 'dominio/consultar_puntaje.dart';
import 'textos.dart';

/// "Tu nivel" (D-20): el puntaje propio y el nivel de confianza, tal como
/// `transparencia` los calcula. `SIN_HISTORIAL` se muestra igual que cualquier otro
/// nivel — nunca como un error ni como un castigo (CU-71).
///
/// Debajo, la arena (2026-10-30): la que el servidor asigna al puntaje, con su avance
/// hacia la siguiente y la entrada al camino entero. Sin historial también hay arena
/// —la del puntaje base—, así que solo es vacío si el servidor no manda ninguna.
class PantallaMiPuntaje extends ConsumerWidget {
  const PantallaMiPuntaje({super.key, required this.usuarioId});
  final String usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final puntaje = ref.watch(puntajeProvider(usuarioId));
    final t = Tokens.of(context);
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosPasanaku.tituloMiPuntaje),
              Expanded(
                child: EstadoDePantalla<PuntajeDelUsuario>(
                  valor: puntaje,
                  etiquetaDeCarga: TextosPasanaku.cargando,
                  mensajeVacio: TextosPasanaku.sinHistorial,
                  tituloVacio: TextosPasanaku.tituloSinHistorial,
                  iconoVacio: IconosDeMarca.recienEmpiezas,
                  vacio: (p) => !p.tieneHistorial && p.arena == null,
                  reintentar: () => ref.invalidate(puntajeProvider(usuarioId)),
                  exito: (p) =>
                      _Puntaje(puntaje: p, t: t, usuarioId: usuarioId),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Puntaje extends StatelessWidget {
  const _Puntaje({
    required this.puntaje,
    required this.t,
    required this.usuarioId,
  });

  final PuntajeDelUsuario puntaje;
  final Tokens t;
  final String usuarioId;

  @override
  Widget build(BuildContext context) {
    // El contrato da el puntaje como cadena. Si un día trae algo que no es un entero,
    // se muestra tal cual en vez de romper la pantalla: el número es información de la
    // persona, la animación es un adorno, y el adorno nunca puede tapar el dato.
    final entero = int.tryParse(puntaje.puntaje);
    return ListView(
      padding: const EdgeInsets.only(top: Espacio.s4, bottom: Espacio.s6),
      children: [
        Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    IconosDeMarca.reputacion,
                    size: Espacio.s6,
                    color: t.brandTexto,
                  ),
                  const SizedBox(width: Espacio.s2),
                  Expanded(
                    child: Text(
                      TextosPasanaku.etiquetaPuntaje,
                      style: Tipo.cuerpoChico.copyWith(color: t.text2),
                    ),
                  ),
                  ChipEstado(
                    texto: _legible(puntaje.nivelDeConfianza),
                    tono: Tono.info,
                  ),
                ],
              ),
              const SizedBox(height: Espacio.s3),
              if (!puntaje.tieneHistorial) ...[
                Text(
                  TextosPasanaku.tituloSinHistorial,
                  style: Tipo.titulo2.copyWith(color: t.text),
                ),
                Text(
                  TextosPasanaku.sinHistorial,
                  style: Tipo.cuerpoChico.copyWith(color: t.text2),
                ),
              ] else if (entero == null)
                Text(
                  puntaje.puntaje,
                  style: Tipo.cifraGrande.copyWith(color: t.text),
                )
              else
                NumeroQueSube(
                  valor: entero,
                  etiqueta: TextosPasanaku.tituloMiPuntaje,
                  estilo: Tipo.cifraGrande.copyWith(color: t.text),
                ),
              const SizedBox(height: Espacio.s3),
              Divider(height: 1, color: t.border),
              const SizedBox(height: Espacio.s3),
              Text(
                TextosPasanaku.origenDelPuntaje,
                style: Tipo.cuerpoChico.copyWith(color: t.text2),
              ),
            ],
          ),
        ),
        if (puntaje.arena != null) ...[
          const SizedBox(height: Espacio.s4),
          TarjetaDeArena(
            arena: puntaje.arena!,
            puntaje: puntaje.puntaje,
            sinHistorial: !puntaje.tieneHistorial,
            verCamino: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) => PantallaCaminoDeArenas(usuarioId: usuarioId),
              ),
            ),
          ),
        ],
      ],
    );
  }

  /// `EN_CONSTRUCCION` → «En construcción»: solo formato, sin traducir valores del servidor.
  static String _legible(String nivel) {
    final plano = nivel.replaceAll('_', ' ').toLowerCase().trim();
    return plano.isEmpty ? nivel : plano[0].toUpperCase() + plano.substring(1);
  }
}
