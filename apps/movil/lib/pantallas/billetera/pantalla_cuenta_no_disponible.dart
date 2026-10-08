import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/organismos/motivo_vacio.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';

/// Contención hasta que identidad/núcleo financiero resuelvan la cuenta autenticada.
/// No consulta saldo ni permite operar con un ID de ejemplo o tomado del dispositivo.
class PantallaCuentaNoDisponible extends StatelessWidget {
  const PantallaCuentaNoDisponible({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text(TextosBilletera.titulo)),
    body: SafeArea(
      child: Center(
        child: SingleChildScrollView(
          child: EstadoVacio(
            mensaje: TextosBilletera.cuentaNoDisponible,
            motivo: MotivoVacio.porPermiso,
            accion: Boton(
              texto: TextosBilletera.irAAyuda,
              variante: BotonVariante.secundario,
              onPressed: () => context.goNamed('soporte.ayuda'),
            ),
          ),
        ),
      ),
    ),
  );
}
