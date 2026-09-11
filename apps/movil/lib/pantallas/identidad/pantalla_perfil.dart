import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_cuenta.dart';
import 'textos.dart';

/// CU-07 — ejercer derechos sobre datos personales: ver y corregir el correo de
/// contacto. La identidad verificada (nombre, documento) no se edita acá; para eso
/// hace falta un trámite con nuevo KYC, fuera del alcance de esta pantalla.
class PantallaDePerfil extends ConsumerStatefulWidget {
  const PantallaDePerfil({super.key});

  @override
  ConsumerState<PantallaDePerfil> createState() => _PantallaDePerfilState();
}

class _PantallaDePerfilState extends ConsumerState<PantallaDePerfil> {
  final _correo = TextEditingController();

  @override
  void dispose() {
    _correo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(TextosIdentidad.tituloPerfil)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Campo(
                etiqueta: 'Correo',
                controlador: _correo,
                tipoDeTeclado: TextInputType.emailAddress,
              ),
              const SizedBox(height: Espacio.s5),
              Boton(
                texto: TextosIdentidad.guardar,
                variante: BotonVariante.primario,
                expandido: true,
                onPressed: () => ref
                    .read(perfilProvider.notifier)
                    .actualizarCorreo(_correo.text),
              ),
              const SizedBox(height: Espacio.s3),
              Boton(
                texto: TextosIdentidad.tituloContrasenaNueva,
                variante: BotonVariante.fantasma,
                expandido: true,
                onPressed: () => context.push('/identidad/contrasena'),
              ),
              const SizedBox(height: Espacio.s3),
              Boton(
                texto: TextosIdentidad.tituloBaja,
                variante: BotonVariante.peligro,
                expandido: true,
                onPressed: () => context.push('/identidad/baja'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
