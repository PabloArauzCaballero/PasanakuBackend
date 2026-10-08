import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';
import 'textos_accion_personal.dart';

/// Un enlace puede nombrar un recurso, pero no demostrar que quien lo abre sea
/// su organizador o participante. Estas acciones esperan un origen autenticado.
class PantallaAccionPersonalNoDisponible extends StatelessWidget {
  const PantallaAccionPersonalNoDisponible({super.key, required this.titulo});

  final String titulo;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(titulo)),
    body: SafeArea(
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 560),
          child: SingleChildScrollView(
            child: EstadoVacio(
              mensaje: TextosAccionPersonal.sinTitular,
              accion: Boton(
                texto: TextosPasanaku.irAAyuda,
                variante: BotonVariante.secundario,
                onPressed: () => context.goNamed('soporte.ayuda'),
              ),
            ),
          ),
        ),
      ),
    ),
  );
}
