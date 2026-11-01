import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../identidad/dominio/foto_de_perfil.dart';

/// El avatar de un integrante del grupo (2026-10-30).
///
/// Si es uno mismo, lleva la foto de perfil que subió: antes la fila «Tú» mostraba
/// las iniciales aunque la foto existiera. Para los demás siguen las iniciales: el
/// servidor solo sirve la foto propia (`GET /usuarios/yo/foto-de-perfil`) y mostrar
/// la de otra persona necesita contrato y consentimiento que todavía no existen.
class AvatarDelIntegrante extends ConsumerWidget {
  const AvatarDelIntegrante({
    super.key,
    required this.nombre,
    required this.soyYo,
  });

  final String nombre;
  final bool soyYo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final bytes = soyYo ? ref.watch(fotoDePerfilProvider).asData?.value : null;
    return Avatar(
      nombre: nombre,
      imagen: bytes == null ? null : MemoryImage(bytes),
    );
  }
}
