import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// La foto (o las iniciales) con un aro claro y una insignia de cámara. Tocarla abre lo
/// mismo que el botón; para el lector de pantalla la acción es solo el botón, así no
/// anuncia dos veces lo mismo.
class AvatarConAro extends StatelessWidget {
  const AvatarConAro({
    super.key,
    required this.nombre,
    required this.imagen,
    required this.alTocar,
  });

  final String nombre;
  final ImageProvider? imagen;
  final VoidCallback? alTocar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    const tamano = Espacio.s7 + Espacio.s5;
    return ExcludeSemantics(
      child: GestureDetector(
        onTap: alTocar,
        child: Stack(
          clipBehavior: Clip.none,
          children: [
            DecoratedBox(
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: t.heroTexto, width: Borde.foco),
              ),
              child: Padding(
                padding: const EdgeInsets.all(Borde.desfase),
                child: Avatar(
                  key: const ValueKey('perfil-avatar'),
                  nombre: nombre.isEmpty ? 'Tu foto de perfil' : nombre,
                  tamano: tamano,
                  imagen: imagen,
                ),
              ),
            ),
            Positioned(
              right: -Espacio.s1,
              bottom: -Espacio.s1,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: t.surface,
                  shape: BoxShape.circle,
                  border: Border.all(color: t.heroFin, width: Borde.desfase),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(Espacio.s1 + Borde.desfase),
                  child: Icon(
                    Icons.photo_camera_rounded,
                    size: Espacio.s4,
                    color: t.brandTexto,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
