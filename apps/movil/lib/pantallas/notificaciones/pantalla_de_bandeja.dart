import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/item_de_notificacion.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'proveedor_bandeja.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

/// `/notificaciones/bandeja`. Compone `ItemDeNotificacion` por cada aviso; la lista
/// vive en Riverpod, no en la pantalla (regla del organismo/molécula: sin lógica).
class PantallaDeBandeja extends ConsumerWidget {
  const PantallaDeBandeja({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final avisos = ref.watch(bandejaProvider);
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosNotificaciones.titulo,
        cuerpo: avisos.isEmpty
            ? const EstadoVacio(
                icono: IconosDeMarca.avisos,
                titulo: TextosNotificaciones.tituloVacio,
                mensaje: TextosNotificaciones.sinAvisos,
              )
            : MarcaDeTutorial(
                id: 'notificaciones.bandeja',
                hijo: ListView.separated(
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
              ),
      ),
    );
  }

  Tono _tonoDesde(String valor) => switch (valor) {
    'ok' => Tono.ok,
    'aviso' => Tono.aviso,
    'error' => Tono.error,
    // Una novedad de producto no es un estado de éxito; evita convertir marca en
    // verde semántico dentro de la bandeja.
    'marca' => Tono.neutro,
    _ => Tono.info,
  };

  /// Sin `intl` (no está en `pubspec.yaml`, congelado para este carril): una fecha
  /// relativa simple alcanza para la bandeja y no agrega dependencia.
  String _cuandoLegible(DateTime cuando) {
    final diferencia = DateTime.now().difference(cuando);
    if (diferencia.inMinutes < 1) return TextosNotificaciones.ahora;
    if (diferencia.inMinutes < 60) {
      return TextosNotificaciones.haceMinutos(diferencia.inMinutes);
    }
    if (diferencia.inHours < 24) {
      return TextosNotificaciones.haceHoras(diferencia.inHours);
    }
    final d = cuando;
    return '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}';
  }
}
