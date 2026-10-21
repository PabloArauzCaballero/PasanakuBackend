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
import 'fila_de_accion_de_perfil.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import '../../proveedores/tutoriales.dart';
import 'seccion_verificacion_kyc.dart';

/// El perfil: estado de la verificación, el correo de la cuenta (solo lectura) y las
/// acciones de la cuenta. Nada de la identidad se edita acá; para cambiar el correo o
/// los datos verificados hace falta un trámite con soporte.
class PantallaDePerfil extends ConsumerStatefulWidget {
  const PantallaDePerfil({super.key});

  @override
  ConsumerState<PantallaDePerfil> createState() => _PantallaDePerfilState();
}

class _PantallaDePerfilState extends ConsumerState<PantallaDePerfil> {
  final _correo = TextEditingController();
  bool _cerrandoSesion = false;

  @override
  void initState() {
    super.initState();
    // El correo de la cuenta llena el campo una vez; lo que la persona ya escribió no se pisa.
    ref.listenManual<AsyncValue<String?>>(correoDeLaCuentaProvider, (_, valor) {
      final correo = valor.asData?.value;
      if (correo != null && _correo.text.isEmpty) _correo.text = correo;
    }, fireImmediately: true);
  }

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
              const SeccionVerificacionKyc(),
              const SizedBox(height: Espacio.s3),
              // El correo identifica la cuenta: se muestra, no se edita. Cambiarlo
              // desde acá rompía la relación con la verificación hecha; lo único
              // editable del perfil es la contraseña (2026-10-19).
              Campo(
                etiqueta: 'Correo',
                controlador: _correo,
                habilitado: false,
                ayuda: TextosIdentidad.correoNoEditable,
              ),
              const SizedBox(height: Espacio.s5),
              FilaDeAccionDePerfil(
                claveSemantica: const ValueKey('perfil-fila-contrasena'),
                texto: TextosIdentidad.tituloContrasenaNueva,
                pista: 'Abrí el cambio de contraseña.',
                onPressed: () => context.push('/identidad/contrasena'),
              ),
              const SizedBox(height: Espacio.s2),
              FilaDeAccionDePerfil(
                claveSemantica: const ValueKey('perfil-fila-guia'),
                texto: TextosIdentidad.verGuiaOtraVez,
                pista: 'Reiniciá y abrí la guía de inicio.',
                onPressed: () =>
                    ref.read(verGuiaDeInicioOtraVezProvider)().ignore(),
              ),
              const SizedBox(height: Espacio.s2),
              // Ayuda dejó la barra de pestañas (ADR-053, A6) y cuelga de acá.
              FilaDeAccionDePerfil(
                claveSemantica: const ValueKey('perfil-fila-ayuda'),
                texto: 'Ayuda',
                pista: 'Abrí el centro de ayuda.',
                onPressed: () => context.go('/soporte/ayuda'),
              ),
              const SizedBox(height: Espacio.s3),
              // Cerrar sesión no estaba en ningún lado: se podía entrar y no salir.
              // Va antes de «dar de baja» y con otra variante, porque son cosas muy
              // distintas —una se deshace volviendo a ingresar, la otra no— y en una
              // app de dinero no pueden parecerse.
              Boton(
                texto: _cerrandoSesion
                    ? 'Cerrando sesión…'
                    : TextosIdentidad.cerrarSesion,
                variante: BotonVariante.fantasma,
                expandido: true,
                cargando: _cerrandoSesion,
                onPressed: () async {
                  if (_cerrandoSesion) return;
                  setState(() => _cerrandoSesion = true);
                  await ref
                      .read(sesionIdentidadProvider.notifier)
                      .cerrarSesion();
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
