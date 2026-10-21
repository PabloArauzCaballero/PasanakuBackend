import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../proveedores/sesion.dart';
import 'dominio/mis_grupos.dart';

/// Abre el grupo entero —integrantes, firma y sorteo— y desde ahí mis aportes
/// (2026-10-20). Lo usan la pestaña Grupos y el resumen del inicio (2026-10-21):
/// el mismo destino, se entre por donde se entre.
Future<void> abrirMiGrupo(
  BuildContext context,
  WidgetRef ref,
  GrupoMio grupo, {
  required String nombre,
}) async {
  final usuarioId = await ref.read(sesionProvider).usuarioId() ?? '';
  if (!context.mounted) return;
  await context.push(
    Uri(
      path: '/pasanaku/grupos/${grupo.grupoId}',
      queryParameters: {
        'participante': grupo.participanteId,
        'usuario': usuarioId,
        'nombre': nombre,
      },
    ).toString(),
  );
}
