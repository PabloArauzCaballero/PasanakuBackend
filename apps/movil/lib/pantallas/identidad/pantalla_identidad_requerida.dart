import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../navegacion/acceso_a_rutas.dart';
import '../../proveedores/renovar_acceso.dart';
import 'boton_consultar_estado.dart';
import 'dominio/verificacion_diferida.dart';
import 'textos_de_verificacion.dart';

/// Se interpone cuando una cuenta sin la identidad verificada intenta mover dinero o
/// entrar a un pasanaku (2026-10-17). Según el estado de la cuenta ofrece verificarse,
/// ver en qué va la revisión, o activar la cuenta si ya la aprobaron. [volver] es la
/// operación que la persona quería hacer.
class PantallaIdentidadRequerida extends ConsumerWidget {
  const PantallaIdentidadRequerida({super.key, this.volver});
  final String? volver;

  void _cerrar(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/billetera/inicio');
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(estadoDeCuentaProvider);
    return Scaffold(
      appBar: AppBar(leading: CloseButton(onPressed: () => _cerrar(context))),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: EstadoDePantalla<EstadoDeCuenta>(
            valor: estado,
            mensajeVacio: '',
            reintentar: () => ref.invalidate(estadoDeCuentaProvider),
            exito: (cuenta) => _Contenido(
              cuenta: cuenta,
              onCerrar: () => _cerrar(context),
              onVerificar: () =>
                  context.pushReplacement(rutaDeVerificarIdentidad),
              onVerEstado: () =>
                  context.pushReplacement(rutaDeVerificacionDeSesion),
              onContinuar: () => context.go(volver ?? '/billetera/inicio'),
              onActivar: () async {
                final listo = await ref.read(renovarAccesoProvider)();
                if (!context.mounted) return;
                if (listo) {
                  context.go(volver ?? '/billetera/inicio');
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text(TextosDeVerificacion.noSePudoActivar),
                    ),
                  );
                }
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _Contenido extends StatelessWidget {
  const _Contenido({
    required this.cuenta,
    required this.onCerrar,
    required this.onVerificar,
    required this.onVerEstado,
    required this.onActivar,
    required this.onContinuar,
  });
  final EstadoDeCuenta cuenta;
  final VoidCallback onCerrar;
  final VoidCallback onVerificar;
  final VoidCallback onVerEstado;
  final VoidCallback onActivar;
  final VoidCallback onContinuar;

  @override
  Widget build(BuildContext context) {
    final (icono, titulo, detalle, accion, alTocar) = switch (cuenta) {
      EstadoDeCuenta.sinVerificar => (
        Icons.verified_user_outlined,
        TextosDeVerificacion.requeridaTitulo,
        TextosDeVerificacion.requeridaDetalle,
        TextosDeVerificacion.requeridaVerificar,
        onVerificar,
      ),
      EstadoDeCuenta.enRevision => (
        Icons.hourglass_top,
        TextosDeVerificacion.cuentaEnVerificacionTitulo,
        TextosDeVerificacion.requeridaEnRevisionDetalle,
        TextosDeVerificacion.requeridaVerEstado,
        onVerEstado,
      ),
      EstadoDeCuenta.verificada => (
        Icons.check_circle,
        TextosDeVerificacion.requeridaVerificadaTitulo,
        TextosDeVerificacion.requeridaVerificadaDetalle,
        TextosDeVerificacion.activarCuenta,
        onActivar,
      ),
      EstadoDeCuenta.otro => (
        Icons.info_outline,
        TextosDeVerificacion.requeridaTitulo,
        TextosDeVerificacion.requeridaNoDisponible,
        null,
        null,
      ),
    };
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Icon(icono, size: 56, color: t.brand),
        const SizedBox(height: Espacio.s4),
        Text(titulo, style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: Espacio.s2),
        Text(detalle),
        const Spacer(),
        // En revisión, lo útil es preguntar si ya la aprobaron, ahí mismo: si la
        // aprobaron, la cuenta se activa y se sigue con la operación (2026-10-19).
        if (cuenta == EstadoDeCuenta.enRevision) ...[
          BotonConsultarEstado(expandido: true, alActivar: onContinuar),
          const SizedBox(height: Espacio.s2),
        ] else if (accion != null && alTocar != null) ...[
          Boton(
            key: const ValueKey('identidad-requerida-accion'),
            texto: accion,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: alTocar,
          ),
          const SizedBox(height: Espacio.s2),
        ],
        Boton(
          texto: cuenta == EstadoDeCuenta.sinVerificar
              ? TextosDeVerificacion.requeridaAhoraNo
              : TextosDeVerificacion.volver,
          variante: BotonVariante.fantasma,
          expandido: true,
          onPressed: onCerrar,
        ),
      ],
    );
  }
}
