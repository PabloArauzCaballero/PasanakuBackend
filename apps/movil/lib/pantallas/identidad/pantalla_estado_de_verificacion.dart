import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/estado_de_verificacion.dart';
import 'acciones_de_verificacion.dart';
import 'textos_de_verificacion.dart';

/// El estado de la verificación, igual que Atlas: PENDIENTE/EN_REVISION siguen
/// sondeando solas, APROBADA y RECHAZADA son finales. Siempre "Caso `&lt;id&gt;`" y
/// "Actualizar estado".
class PantallaDeEstadoDeVerificacion extends ConsumerStatefulWidget {
  const PantallaDeEstadoDeVerificacion({
    super.key,
    required this.usuarioId,
    this.sesionActiva = false,
  });
  final String usuarioId;
  final bool sesionActiva;

  @override
  ConsumerState<PantallaDeEstadoDeVerificacion> createState() =>
      _PantallaDeEstadoDeVerificacionState();
}

class _PantallaDeEstadoDeVerificacionState
    extends ConsumerState<PantallaDeEstadoDeVerificacion> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => ref.read(verificacionProvider.notifier).iniciar(widget.usuarioId),
    );
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(verificacionProvider);
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: EstadoDePantalla<EstadoDeVerificacion>(
            valor: estado,
            mensajeVacio: '',
            reintentar: () =>
                ref.read(verificacionProvider.notifier).actualizar(),
            exito: (expediente) => _Contenido(
              expediente: expediente,
              sesionActiva: widget.sesionActiva,
              onActualizar: () =>
                  ref.read(verificacionProvider.notifier).actualizar(),
            ),
          ),
        ),
      ),
    );
  }
}

class _Contenido extends ConsumerWidget {
  const _Contenido({
    required this.expediente,
    required this.sesionActiva,
    required this.onActualizar,
  });
  final EstadoDeVerificacion expediente;
  final bool sesionActiva;
  final VoidCallback onActualizar;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final (titulo, detalle) = switch (expediente.estado) {
      // PENDIENTE y EN_REVISION se dicen igual: la revisión es humana y puede tardar,
      // así que nunca «No cierres la app». Recién creada la cuenta, decir eso dejaba
      // a la persona mirando esta pantalla como si tuviera que esperar (2026-10-19).
      EstadoDeVerificacionEstadoEnum.PENDIENTE ||
      EstadoDeVerificacionEstadoEnum.EN_REVISION => (
        sesionActiva
            ? TextosDeVerificacion.cuentaEnVerificacionTitulo
            : TextosDeVerificacion.enRevisionTitulo,
        sesionActiva
            ? TextosDeVerificacion.cuentaEnVerificacionDetalle
            : TextosDeVerificacion.enRevisionDetalle,
      ),
      EstadoDeVerificacionEstadoEnum.APROBADA => (
        TextosDeVerificacion.aprobadaTitulo,
        sesionActiva ? TextosDeVerificacion.accesoAunNoHabilitado : '',
      ),
      EstadoDeVerificacionEstadoEnum.RECHAZADA => (
        TextosDeVerificacion.rechazadaTitulo,
        motivoDeRechazo(expediente.motivoRechazo),
      ),
    };
    final aprobada =
        expediente.estado == EstadoDeVerificacionEstadoEnum.APROBADA;
    final rechazada =
        expediente.estado == EstadoDeVerificacionEstadoEnum.RECHAZADA;
    // Las etapas dicen dónde está el trámite sin inventar plazos: recibido, revisándose y
    // listo; si se rechazó, la última etapa es la que no se pudo completar.
    final pasos = [
      TextosDeVerificacion.pasoRecibido,
      TextosDeVerificacion.pasoEnRevision,
      rechazada
          ? TextosDeVerificacion.pasoRechazado
          : TextosDeVerificacion.pasoListo,
    ];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: Center(
            child: SingleChildScrollView(
              child: EstadoDeTramite(
                icono: aprobada
                    ? IconosDeMarca.tramiteListo
                    : rechazada
                    ? IconosDeMarca.tramiteFallido
                    : IconosDeMarca.enEspera,
                tono: aprobada
                    ? Tono.ok
                    : rechazada
                    ? Tono.error
                    : Tono.info,
                titulo: titulo,
                detalle: detalle,
                pasos: pasos,
                pasoActual: aprobada ? pasos.length : (rechazada ? 2 : 1),
                fallo: rechazada,
                caso: TextosDeVerificacion.caso(expediente.verificacionId),
              ),
            ),
          ),
        ),
        const SizedBox(height: Espacio.s4),
        AccionesDeVerificacion(
          sesionActiva: sesionActiva,
          aprobada: aprobada,
          rechazada: rechazada,
          onActualizar: onActualizar,
        ),
      ],
    );
  }
}
