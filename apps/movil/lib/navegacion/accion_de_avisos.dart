import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../pantallas/notificaciones/proveedor_bandeja.dart';

/// La campana de avisos, con su punto cuando hay algo sin leer.
///
/// Vivía en el `AppBar` del shell; ahora viaja en la cabecera de cada pantalla de
/// nivel superior, que es lo que permitió que el shell dejara de dibujar una barra
/// propia encima del título de cada pantalla.
class AccionDeAvisos extends ConsumerWidget {
  const AccionDeAvisos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sinLeer = ref.watch(noLeidasProvider);
    return BotonDeCabecera(
      icono: Icons.notifications_outlined,
      etiqueta: sinLeer > 0
          ? 'Avisos, $sinLeer sin leer'
          : 'Avisos, ninguno sin leer',
      conAviso: sinLeer > 0,
      onTap: () => context.push('/notificaciones/bandeja'),
    );
  }
}
