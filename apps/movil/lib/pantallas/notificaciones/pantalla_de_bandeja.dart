import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/item_de_notificacion.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'proveedor_bandeja.dart';
import 'textos.dart';

/// `/notificaciones/bandeja`. Compone `ItemDeNotificacion` por cada aviso; la lista
/// vive en Riverpod, no en la pantalla (regla del organismo/molécula: sin lógica).
class PantallaDeBandeja extends ConsumerWidget {
  const PantallaDeBandeja({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final avisos = ref.watch(bandejaProvider);
    return Scaffold(
      appBar: AppBar(title: const Text(TextosNotificaciones.titulo)),
      body: avisos.isEmpty
          ? const EstadoVacio(mensaje: TextosNotificaciones.sinAvisos)
          : ListView.separated(
              itemCount: avisos.length,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: (context, i) {
                final a = avisos[i];
                return ItemDeNotificacion(
                  titulo: a.titulo,
                  detalle: a.detalle,
                  cuando: _cuandoLegible(a.cuando),
                  tono: _tonoDesde(a.tono),
                  leida: a.leida,
                  onTap: () {
                    ref.read(bandejaProvider.notifier).marcarLeida(a.id);
                    final ruta = a.ruta;
                    if (ruta != null) context.push(ruta);
                  },
                );
              },
            ),
    );
  }

  Tono _tonoDesde(String valor) => switch (valor) {
    'ok' => Tono.ok,
    'aviso' => Tono.aviso,
    'error' => Tono.error,
    'marca' => Tono.marca,
    _ => Tono.info,
  };

  /// Sin `intl` (no está en `pubspec.yaml`, congelado para este carril): una fecha
  /// relativa simple alcanza para la bandeja y no agrega dependencia.
  String _cuandoLegible(DateTime cuando) {
    final diferencia = DateTime.now().difference(cuando);
    if (diferencia.inMinutes < 1) return 'ahora';
    if (diferencia.inMinutes < 60) return 'hace ${diferencia.inMinutes} min';
    if (diferencia.inHours < 24) return 'hace ${diferencia.inHours} h';
    final d = cuando;
    return '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}';
  }
}
