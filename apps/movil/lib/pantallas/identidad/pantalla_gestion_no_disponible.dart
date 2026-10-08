import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';

const _anchoMaximoGestion = 560.0;

/// Contención honesta para gestiones de cuenta sin operación contractual.
/// No recoge datos ni simula que una solicitud llegó al servidor.
class PantallaGestionNoDisponible extends StatelessWidget {
  const PantallaGestionNoDisponible({
    super.key,
    required this.titulo,
    required this.aviso,
    required this.detalle,
  });

  final String titulo;
  final String aviso;
  final String detalle;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      bottom: false,
      child: Column(
        children: [
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(Espacio.s4),
              children: [
                Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      maxWidth: _anchoMaximoGestion,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        CabeceraDeSeccion(titulo: titulo),
                        const SizedBox(height: Espacio.s4),
                        Alerta(
                          titulo: aviso,
                          detalle: detalle,
                          tono: Tono.info,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          BarraDeAccion(
            hijo: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(
                  maxWidth: _anchoMaximoGestion,
                ),
                child: Boton(
                  texto: TextosIdentidad.volverAlPerfil,
                  variante: BotonVariante.primario,
                  expandido: true,
                  onPressed: () => context.go('/identidad/perfil'),
                ),
              ),
            ),
          ),
        ],
      ),
    ),
  );
}
