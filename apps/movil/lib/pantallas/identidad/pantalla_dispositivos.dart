import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'dominio/estado_sesion.dart';
import 'textos.dart';
import 'textos_de_tramites.dart';

/// CU-04, paso 3: ofrecer confiar en el dispositivo por 30 días (evita pedir MFA
/// de nuevo). El listado de dispositivos ya registrados (`GET
/// /sesion/dispositivos`) queda pendiente del cliente Dart de identidad — hueco.
class PantallaDeDispositivos extends ConsumerWidget {
  const PantallaDeDispositivos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(sesionIdentidadProvider);
    final notifier = ref.read(sesionIdentidadProvider.notifier);
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosIdentidad.tusDispositivos,
        cuerpo: SafeArea(
          top: false,
          // Sin desplazamiento propio, la cabecera no se compactaría y el botón
          // quedaría bajo el pliegue en una pantalla chica.
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(Espacio.s4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const IntroDePantalla(
                  icono: IconosDeMarca.custodia,
                  titulo: TextosDeTramites.dispositivosTitulo,
                  explicacion: TextosDeTramites.dispositivosExplicacion,
                ),
                Tarjeta(
                  child: Casilla(
                    etiqueta: TextosIdentidad.dispositivoDeConfianza,
                    valor: estado.confiarEnDispositivo,
                    onChanged: notifier.elegirConfianza,
                  ),
                ),
                const SizedBox(height: Espacio.s5),
                Boton(
                  texto: TextosIdentidad.continuar,
                  variante: BotonVariante.primario,
                  expandido: true,
                  onPressed: () => context.go('/billetera/inicio'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
