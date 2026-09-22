import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_cuenta.dart';
import 'dominio/estado_sesion.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';

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
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: ListView(
            children: [
              const MarcaDeTutorial(
                id: 'identidad.perfil',
                hijo: CabeceraDeInicio(titulo: TextosIdentidad.tituloPerfil),
              ),
              const SizedBox(height: Espacio.s4),
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
              // Cerrar sesión no estaba en ningún lado: se podía entrar y no salir.
              // Va antes de «dar de baja» y con otra variante, porque son cosas muy
              // distintas —una se deshace volviendo a ingresar, la otra no— y en una
              // app de dinero no pueden parecerse.
              Boton(
                texto: TextosIdentidad.cerrarSesion,
                variante: BotonVariante.fantasma,
                expandido: true,
                onPressed: () async {
                  await ref
                      .read(sesionIdentidadProvider.notifier)
                      .cerrarSesion();
                  if (context.mounted) context.go('/portada');
                },
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
