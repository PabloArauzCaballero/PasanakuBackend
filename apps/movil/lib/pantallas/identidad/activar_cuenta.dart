import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../proveedores/renovar_acceso.dart';
import 'textos_de_verificacion.dart';

/// «Activar mi cuenta» con la identidad ya aprobada: el token nuevo trae los permisos
/// de la identidad aprobada, así que no hace falta cerrar sesión ni volver a ingresar.
Future<void> activarCuentaAprobada(BuildContext context, WidgetRef ref) async {
  final listo = await ref.read(renovarAccesoProvider)();
  if (!context.mounted) return;
  if (listo) {
    context.go('/billetera/inicio');
  } else {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text(TextosDeVerificacion.noSePudoActivar)),
    );
  }
}
