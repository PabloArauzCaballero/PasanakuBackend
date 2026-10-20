import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/solicitudes_del_grupo.dart';
import 'textos_enlace_del_grupo.dart';

/// «Quién pidió unirse» (CU-68): el organizador ve cada pedido —con el nombre visible de
/// la persona— y lo acepta o lo rechaza. Deslizar hacia abajo vuelve a preguntar.
class PantallaSolicitudesDelGrupo extends ConsumerWidget {
  const PantallaSolicitudesDelGrupo({super.key, required this.grupoId});
  final String grupoId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final solicitudes = ref.watch(solicitudesDelGrupoProvider(grupoId));
    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () =>
              ref.refresh(solicitudesDelGrupoProvider(grupoId).future),
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
            children: [
              const CabeceraDeSeccion(
                titulo: TextosEnlaceDelGrupo.tituloSolicitudes,
              ),
              EstadoDePantalla<List<SolicitudDeIngreso>>(
                valor: solicitudes,
                mensajeVacio: TextosEnlaceDelGrupo.sinSolicitudes,
                vacio: (l) => l.isEmpty,
                reintentar: () =>
                    ref.invalidate(solicitudesDelGrupoProvider(grupoId)),
                exito: (lista) => Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    for (final s in lista) ...[
                      _TarjetaDeSolicitud(grupoId: grupoId, solicitud: s),
                      const SizedBox(height: Espacio.s3),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TarjetaDeSolicitud extends ConsumerStatefulWidget {
  const _TarjetaDeSolicitud({required this.grupoId, required this.solicitud});
  final String grupoId;
  final SolicitudDeIngreso solicitud;

  @override
  ConsumerState<_TarjetaDeSolicitud> createState() =>
      _TarjetaDeSolicitudState();
}

class _TarjetaDeSolicitudState extends ConsumerState<_TarjetaDeSolicitud> {
  bool? _decidiendo;

  Future<void> _decidir(bool aceptar) async {
    if (_decidiendo != null) return;
    setState(() => _decidiendo = aceptar);
    try {
      await ref.read(decidirSolicitudProvider)(
        widget.solicitud.solicitudId,
        aceptar: aceptar,
      );
      if (!mounted) return;
      mostrarSnackbar(
        context,
        aceptar
            ? TextosEnlaceDelGrupo.aceptada
            : TextosEnlaceDelGrupo.rechazada,
      );
      ref.invalidate(solicitudesDelGrupoProvider(widget.grupoId));
    } on Object catch (e) {
      if (mounted) {
        mostrarSnackbar(
          context,
          e is ErrorPresentable
              ? e.mensaje
              : 'No pudimos guardar tu decisión. Probá de nuevo.',
        );
      }
    } finally {
      if (mounted) setState(() => _decidiendo = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final s = widget.solicitud;
    final nombre = s.nombreVisible ?? TextosEnlaceDelGrupo.sinNombre;
    final cuando = Fecha.formatear(
      s.fechaSolicitud.toUtc().toIso8601String(),
      conHora: false,
    );
    return Tarjeta(
      etiqueta: 'Solicitud de $nombre',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Avatar(nombre: nombre),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(nombre, style: Tipo.titulo3.copyWith(color: t.text)),
                    Text(
                      s.porEnlace == true
                          ? '${TextosEnlaceDelGrupo.porEnlace} · $cuando'
                          : TextosEnlaceDelGrupo.pidioEl(cuando),
                      style: Tipo.cuerpoChico.copyWith(color: t.text3),
                    ),
                    // Para que el organizador decida si conoce a la persona (2026-10-20).
                    if (s.telefono case final telefono?)
                      Text(
                        '${TextosEnlaceDelGrupo.telefono}: $telefono',
                        style: Tipo.cuerpoChico.copyWith(color: t.text2),
                      ),
                    if (s.correo case final correo?)
                      Text(
                        '${TextosEnlaceDelGrupo.correo}: $correo',
                        style: Tipo.cuerpoChico.copyWith(color: t.text2),
                      ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s3),
          Row(
            children: [
              Expanded(
                child: Boton(
                  texto: TextosEnlaceDelGrupo.rechazar,
                  variante: BotonVariante.fantasma,
                  cargando: _decidiendo == false,
                  onPressed: _decidiendo == null ? () => _decidir(false) : null,
                ),
              ),
              const SizedBox(width: Espacio.s2),
              Expanded(
                child: Boton(
                  texto: TextosEnlaceDelGrupo.aceptar,
                  variante: BotonVariante.primario,
                  cargando: _decidiendo == true,
                  onPressed: _decidiendo == null ? () => _decidir(true) : null,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
