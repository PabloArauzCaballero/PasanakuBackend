import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/camino_de_etapas.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/camino_de_etapas.dart';
import 'tarjeta_de_etapa.dart';
import 'textos_de_etapas.dart';

/// El camino entero de etapas: la propia arriba, con su avance, y debajo todas las
/// etapas del catálogo —las alcanzadas, la actual y las bloqueadas con su umbral—.
///
/// Sin historial no es un vacío: el servidor da la etapa del puntaje base (Inicial en
/// el modelo v1) y se muestra como cualquier otra, con una línea que lo explica.
/// Vacío es solo que no haya catálogo publicado.
class PantallaCaminoDeEtapas extends ConsumerWidget {
  const PantallaCaminoDeEtapas({super.key, required this.usuarioId});
  final String usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final camino = ref.watch(caminoDeEtapasProvider(usuarioId));
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosDeEtapas.titulo),
              Expanded(
                child: EstadoDePantalla<CaminoDelUsuario>(
                  valor: camino,
                  etiquetaDeCarga: TextosDeEtapas.cargando,
                  tituloVacio: TextosDeEtapas.tituloSinEtapas,
                  mensajeVacio: TextosDeEtapas.sinEtapas,
                  iconoVacio: IconosDeMarca.reputacion,
                  vacio: (c) => c.vacio,
                  reintentar: () =>
                      ref.invalidate(caminoDeEtapasProvider(usuarioId)),
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
    final etapa = camino.puntaje.etapa!;
    return ListView(
      padding: const EdgeInsets.only(top: Espacio.s4, bottom: Espacio.s6),
      children: [
        TarjetaDeEtapa(
          etapa: etapa,
          puntaje: camino.puntaje.puntaje,
          sinHistorial: !camino.puntaje.tieneHistorial,
        ),
        const SizedBox(height: Espacio.s4),
        Text(
          TextosDeEtapas.comoSeSube,
          style: Tipo.cuerpoChico.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s4),
        CaminoDeEtapas(peldanos: peldanosDe(camino.etapas, etapa)),
      ],
    );
  }
}
