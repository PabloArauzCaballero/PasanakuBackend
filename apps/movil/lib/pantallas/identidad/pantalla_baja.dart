import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:flutter/material.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'textos.dart';
import 'textos_de_tramites.dart';

/// CU-09 — solicitar la baja. Confirmación explícita y sin ambigüedad: el botón de
/// peligro pide un segundo toque en un diálogo, y explica que hace falta saldo en
/// cero y sin cupos activos (supuesto: la bóveda no detalla la condición exacta en
/// la pantalla, así que se declara acá y se deja que el servidor la rechace con su
/// propio código si no se cumple — invariante 7).
class PantallaDeBaja extends StatelessWidget {
  const PantallaDeBaja({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosIdentidad.tituloBaja,
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
                  icono: IconosDeMarca.darDeBaja,
                  titulo: TextosDeTramites.bajaTitulo,
                  explicacion: TextosDeTramites.bajaExplicacion,
                  peligro: true,
                ),
                const Alerta(
                  titulo: TextosIdentidad.avisoBaja,
                  tono: Tono.aviso,
                ),
                const SizedBox(height: Espacio.s5),
                Boton(
                  texto: TextosIdentidad.confirmarBaja,
                  variante: BotonVariante.peligro,
                  expandido: true,
                  onPressed: () => HojaDeConfirmacion.mostrar<void>(
                    context,
                    hoja: HojaDeConfirmacion(
                      titulo: TextosIdentidad.tituloBaja,
                      detalle: const Alerta(
                        titulo: TextosIdentidad.avisoBaja,
                        tono: Tono.aviso,
                      ),
                      textoConfirmar: TextosIdentidad.confirmarBaja,
                      peligro: true,
                      onConfirmar: () => Navigator.of(context).pop(),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
