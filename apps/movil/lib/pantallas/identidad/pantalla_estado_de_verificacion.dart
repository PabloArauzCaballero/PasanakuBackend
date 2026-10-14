import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_alta.dart';
import 'dominio/estado_de_verificacion.dart';
import 'textos_de_verificacion.dart';

/// El estado de la verificación, igual que Atlas: PENDIENTE/EN_REVISION siguen
/// sondeando solas, APROBADA y RECHAZADA son finales. Siempre "Caso `&lt;id&gt;`" y
/// "Actualizar estado".
class PantallaDeEstadoDeVerificacion extends ConsumerStatefulWidget {
  const PantallaDeEstadoDeVerificacion({super.key, required this.usuarioId});
  final String usuarioId;

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
      appBar: AppBar(automaticallyImplyLeading: false),
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
  const _Contenido({required this.expediente, required this.onActualizar});
  final EstadoDeVerificacion expediente;
  final VoidCallback onActualizar;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final (titulo, detalle) = switch (expediente.estado) {
      EstadoDeVerificacionEstadoEnum.PENDIENTE => (
        TextosDeVerificacion.pendienteTitulo,
        TextosDeVerificacion.pendienteDetalle,
      ),
      EstadoDeVerificacionEstadoEnum.EN_REVISION => (
        TextosDeVerificacion.enRevisionTitulo,
        TextosDeVerificacion.enRevisionDetalle,
      ),
      EstadoDeVerificacionEstadoEnum.APROBADA => (
        TextosDeVerificacion.aprobadaTitulo,
        '',
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Icon(
          aprobada
              ? Icons.check_circle
              : rechazada
              ? Icons.error_outline
              : Icons.hourglass_top,
          size: 56,
        ),
        const SizedBox(height: Espacio.s4),
        Text(titulo, style: Theme.of(context).textTheme.titleLarge),
        if (detalle.isNotEmpty) ...[
          const SizedBox(height: Espacio.s2),
          Text(detalle),
        ],
        const SizedBox(height: Espacio.s3),
        Text(
          TextosDeVerificacion.caso(expediente.verificacionId),
          style: Theme.of(context).textTheme.bodySmall,
        ),
        const Spacer(),
        if (rechazada)
          Boton(
            texto: TextosDeVerificacion.intentarDeNuevo,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () {
              ref.read(altaProvider.notifier).reiniciar();
              context.go('/registro');
            },
          )
        else if (aprobada)
          Boton(
            texto: TextosDeVerificacion.entrar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () {
              ref.read(altaProvider.notifier).reiniciar();
              context.go('/ingreso?alta=lista');
            },
          )
        else
          Boton(
            texto: TextosDeVerificacion.seguirConElRegistro,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.go('/ingreso?alta=lista'),
          ),
        const SizedBox(height: Espacio.s2),
        TextButton(
          onPressed: onActualizar,
          child: const Text(TextosDeVerificacion.actualizarEstado),
        ),
      ],
    );
  }
}
