import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../proveedores/sesion.dart';
import 'activar_cuenta.dart';
import 'dominio/estado_alta.dart';
import 'textos_de_verificacion.dart';

/// Lo que se puede hacer desde el estado de la verificación: una acción principal según el
/// estado y si hay sesión, y «Actualizar estado» siempre.
class AccionesDeVerificacion extends ConsumerWidget {
  const AccionesDeVerificacion({
    super.key,
    required this.sesionActiva,
    required this.aprobada,
    required this.rechazada,
    required this.onActualizar,
  });
  final bool sesionActiva;
  final bool aprobada;
  final bool rechazada;
  final VoidCallback onActualizar;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (sesionActiva && aprobada)
          Boton(
            texto: TextosDeVerificacion.activarCuenta,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => activarCuentaAprobada(context, ref),
          )
        else if (sesionActiva && rechazada)
          Boton(
            texto: TextosDeVerificacion.salirDeCuenta,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () async {
              await ref.read(sesionProvider).cerrar();
              if (context.mounted) context.go('/ingreso');
            },
          )
        else if (sesionActiva)
          Boton(
            texto: TextosDeVerificacion.irALaApp,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.go('/billetera/inicio'),
          )
        else if (rechazada)
          Boton(
            texto: TextosDeVerificacion.intentarDeNuevo,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () {
              ref.read(altaProvider.notifier).reiniciar();
              context.go('/registro');
            },
          )
        else if (aprobada)
          Boton(
            texto: TextosDeVerificacion.entrar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () {
              ref.read(altaProvider.notifier).reiniciar();
              context.go('/ingreso?alta=lista');
            },
          )
        else
          Boton(
            texto: TextosDeVerificacion.seguirConElRegistro,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.go('/ingreso?alta=lista'),
          ),
        const SizedBox(height: Espacio.s2),
        TextButton(
          onPressed: onActualizar,
          child: const Text(TextosDeVerificacion.actualizarEstado),
        ),
      ],
    );
  }
}
