import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'textos.dart';

const _anchoMaximoPerfil = 560.0;

/// Solo muestra acciones que realmente tienen efecto. Los cambios de cuenta
/// quedan explícitamente pausados hasta tener contrato y servicio verificables.
class PantallaDePerfil extends ConsumerWidget {
  const PantallaDePerfil({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
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
                      maxWidth: _anchoMaximoPerfil,
                    ),
                    child: const Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        MarcaDeTutorial(
                          id: 'identidad.perfil',
                          hijo: CabeceraDeInicio(
                            titulo: TextosIdentidad.tituloPerfil,
                          ),
                        ),
                        SizedBox(height: Espacio.s4),
                        Alerta(
                          titulo: TextosIdentidad.perfilNoDisponible,
                          detalle: TextosIdentidad.detallePerfilNoDisponible,
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
                constraints: const BoxConstraints(maxWidth: _anchoMaximoPerfil),
                child: Boton(
                  texto: TextosIdentidad.cerrarSesion,
                  variante: BotonVariante.secundario,
                  expandido: true,
                  onPressed: () async {
                    await ref
                        .read(sesionIdentidadProvider.notifier)
                        .cerrarSesion();
                    if (context.mounted) context.go('/portada');
                  },
                ),
              ),
            ),
          ),
        ],
      ),
    ),
  );
}
