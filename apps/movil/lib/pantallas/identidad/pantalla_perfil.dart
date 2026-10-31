import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_cuenta.dart';
import 'dominio/estado_sesion.dart';
import 'textos.dart';
import 'textos_del_perfil.dart';
import 'fila_de_accion_de_perfil.dart';
import 'foto_de_perfil_editable.dart';
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
                hijo: CabeceraDeInicio(
                  saludo: TextosPerfil.saludo,
                  titulo: TextosIdentidad.tituloPerfil,
                ),
              ),
              const SizedBox(height: Espacio.s4),
              const FotoDePerfilEditable(),
              const SizedBox(height: Espacio.s6),
              const TituloDeSeccion(titulo: TextosPerfil.seccionCuenta),
              const SeccionVerificacionKyc(),
              const SizedBox(height: Espacio.s4),
              // El correo identifica la cuenta: se muestra, no se edita. Cambiarlo
              // desde acá rompía la relación con la verificación hecha; lo único
              // editable del perfil es la contraseña (2026-10-19).
              Campo(
                etiqueta: TextosPerfil.correo,
                controlador: _correo,
                habilitado: false,
                ayuda: TextosIdentidad.correoNoEditable,
              ),
              const SizedBox(height: Espacio.s6),
              const TituloDeSeccion(titulo: TextosPerfil.seccionSeguridad),
              GrupoDeFilas(
                filas: [
                  FilaDeAccionDePerfil(
                    claveSemantica: const ValueKey('perfil-fila-contrasena'),
                    icono: IconosDeMarca.contrasena,
                    texto: TextosIdentidad.tituloContrasenaNueva,
                    pista: TextosPerfil.pistaContrasena,
                    onPressed: () => context.push('/identidad/contrasena'),
                  ),
                  FilaDeAccionDePerfil(
                    claveSemantica: const ValueKey('perfil-fila-guia'),
                    icono: IconosDeMarca.guia,
                    texto: TextosIdentidad.verGuiaOtraVez,
                    pista: TextosPerfil.pistaGuia,
                    onPressed: () =>
                        ref.read(verGuiaDeInicioOtraVezProvider)().ignore(),
                  ),
                  // Ayuda dejó la barra de pestañas (ADR-053, A6) y cuelga de acá.
                  FilaDeAccionDePerfil(
                    claveSemantica: const ValueKey('perfil-fila-ayuda'),
                    icono: IconosDeMarca.ayuda,
                    texto: TextosPerfil.ayuda,
                    pista: TextosPerfil.pistaAyuda,
                    onPressed: () => context.go('/soporte/ayuda'),
                  ),
                ],
              ),
              const SizedBox(height: Espacio.s5),
              // Cerrar sesión no estaba en ningún lado: se podía entrar y no salir.
              // Y «dar de baja» no se parece: una se deshace volviendo a ingresar, la otra
              // no, y en una app de dinero no pueden verse iguales.
              Boton(
                texto: _cerrandoSesion
                    ? TextosPerfil.cerrandoSesion
                    : TextosIdentidad.cerrarSesion,
                variante: BotonVariante.secundario,
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
              const SizedBox(height: Espacio.s5),
              // Lo irreversible, al final y sin competir: una fila roja, no el segundo botón
              // más grande de la pantalla. La baja pide confirmación en su propia pantalla.
              GrupoDeFilas(
                filas: [
                  FilaDeAccionDePerfil(
                    claveSemantica: const ValueKey('perfil-fila-baja'),
                    icono: IconosDeMarca.darDeBaja,
                    texto: TextosIdentidad.tituloBaja,
                    pista: TextosPerfil.pistaBaja,
                    destructiva: true,
                    onPressed: () => context.push('/identidad/baja'),
                  ),
                ],
              ),
              const SizedBox(height: Espacio.s6),
            ],
          ),
        ),
      ),
    );
  }
}
