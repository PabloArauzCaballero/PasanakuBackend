import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/numero_que_sube.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
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
                  vacio: (p) => !p.tieneHistorial,
                  reintentar: () => ref.invalidate(puntajeProvider(usuarioId)),
                  exito: (p) => _Puntaje(puntaje: p, t: t),
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
  const _Puntaje({required this.puntaje, required this.t});

  final PuntajeDelUsuario puntaje;
  final Tokens t;

  @override
  Widget build(BuildContext context) {
    // El contrato da el puntaje como cadena. Si un día trae algo que no es un entero,
    // se muestra tal cual en vez de romper la pantalla: el número es información de la
    // persona, la animación es un adorno, y el adorno nunca puede tapar el dato.
    final entero = int.tryParse(puntaje.puntaje);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        const SizedBox(height: Espacio.s4),
        if (entero == null)
          Text(puntaje.puntaje, style: Tipo.cifraGrande.copyWith(color: t.text))
        else
          NumeroQueSube(
            valor: entero,
            etiqueta: TextosPasanaku.tituloMiPuntaje,
            estilo: Tipo.cifraGrande.copyWith(color: t.text),
          ),
        const SizedBox(height: Espacio.s2),
        Text(
          puntaje.nivelDeConfianza,
          style: Tipo.cuerpo.copyWith(color: t.text2),
        ),
      ],
    );
  }
}
