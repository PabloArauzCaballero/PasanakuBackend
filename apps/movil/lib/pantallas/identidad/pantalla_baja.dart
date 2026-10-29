import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:flutter/material.dart';

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
      appBar: AppBar(title: const Text(TextosIdentidad.tituloBaja)),
      body: SafeArea(
        child: Padding(
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
              // La consecuencia es el título; la condición, el detalle (antes, una sola
              // oración larga en naranja).
              const Alerta(
                titulo: TextosDeTramites.bajaConsecuencia,
                detalle: TextosDeTramites.bajaCondicion,
                tono: Tono.aviso,
              ),
              const SizedBox(height: Espacio.s5),
              Boton(
                texto: TextosDeTramites.bajaBoton,
                variante: BotonVariante.peligro,
                expandido: true,
                onPressed: () => HojaDeConfirmacion.mostrar<void>(
                  context,
                  hoja: HojaDeConfirmacion(
                    titulo: TextosIdentidad.tituloBaja,
                    detalle: const Alerta(
                      titulo: TextosDeTramites.bajaConsecuencia,
                      detalle: TextosDeTramites.bajaCondicion,
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
    );
  }
}
