import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';

/// Contención hasta que aportes publique un GET autenticado del importe vigente.
/// No deja iniciar un cobro con un monto recibido por enlace.
class PantallaAporteNoDisponible extends StatelessWidget {
  const PantallaAporteNoDisponible({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text(TextosPasanaku.tituloAportar)),
    body: SafeArea(
      child: Center(
        child: SingleChildScrollView(
          child: EstadoVacio(
            mensaje: TextosPasanaku.aporteSinMontoVerificado,
            accion: Boton(
              texto: TextosPasanaku.irAAyuda,
              variante: BotonVariante.secundario,
              onPressed: () => context.goNamed('soporte.ayuda'),
            ),
          ),
        ),
      ),
    ),
  );
}
