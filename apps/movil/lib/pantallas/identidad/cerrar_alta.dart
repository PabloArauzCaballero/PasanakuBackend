import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../navegacion/retorno_de_invitacion.dart';
import 'dominio/estado_contrato.dart';
import 'dominio/notifier_alta.dart';
import 'textos.dart';

/// Cierra el alta una sola vez y conserva el resultado visible al ir al ingreso.
Future<void> cerrarAlta(BuildContext context, WidgetRef ref) async {
  final aceptado = await context.push<bool>('/identidad/contrato');
  if (aceptado != true || !context.mounted) return;

  final notifier = ref.read(altaProvider.notifier);
  await notifier.enviarAlServidor();
  if (!context.mounted) return;
  final estado = ref.read(altaProvider);
  if (estado.enviando || estado.error != null) return;

  final retorno = retornoDeInvitacion(
    GoRouterState.of(context).uri.queryParameters['volver'],
  );
  final parametros = <String, String>{
    'alta': 'lista',
    if (estado.fotosPendientes) 'fotos': 'pendientes',
  };
  if (retorno != null) parametros['volver'] = retorno;
  final destino = Uri(path: '/ingreso', queryParameters: parametros);
  final falloAlBorrarBorrador = estado.errorBorrador == 'borrar';
  notifier.reiniciar();
  ref.read(contratoProvider.notifier).reiniciar();
  context.go(destino.toString());
  if (falloAlBorrarBorrador && context.mounted) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text(TextosIdentidad.registroBorradorNoBorrado)),
    );
  }
}
