import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/progreso.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/detalle_del_grupo.dart';
import 'tarjeta_de_integrante.dart';
import 'textos_detalle_del_grupo.dart';
import 'textos_invitacion.dart';

/// El grupo por dentro (2026-10-20): resumen, lo que me toca hacer (firmar, sortear,
/// invitar) y una tarjeta por cupo. Lo que se puede hacer lo dice el servidor.
class SeccionDelGrupo extends ConsumerWidget {
  const SeccionDelGrupo({super.key, required this.grupoId});
  final String grupoId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detalle = ref.watch(detalleDelGrupoProvider(grupoId));
    return EstadoDePantalla<DetalleDelGrupo>(
      valor: detalle,
      mensajeVacio: '',
      reintentar: () => ref.invalidate(detalleDelGrupoProvider(grupoId)),
      exito: (d) => VistaDelGrupo(detalle: d),
    );
  }
}

class VistaDelGrupo extends StatelessWidget {
  const VistaDelGrupo({super.key, required this.detalle});
  final DetalleDelGrupo detalle;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final d = detalle;
    final id = d.grupoId;
    final completo = d.cuposOcupados >= d.cuposTotales;
    final monto = formatearMonto(monto: d.montoAporte, moneda: d.moneda);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ChipEstado(
                texto: TextosDetalleDelGrupo.estado(d.estado),
                tono: d.estado == 'CONFORMADO' ? Tono.ok : Tono.info,
              ),
              const SizedBox(height: Espacio.s3),
              Text(
                TextosDetalleDelGrupo.aporte(monto, d.periodicidad),
                style: Tipo.titulo3.copyWith(color: t.text),
              ),
              const SizedBox(height: Espacio.s3),
              Progreso(
                valor: d.cuposTotales == 0
                    ? 0
                    : d.cuposOcupados / d.cuposTotales,
                etiqueta: TextosDetalleDelGrupo.cupos(
                  d.cuposOcupados,
                  d.cuposTotales,
                ),
              ),
              const SizedBox(height: Espacio.s1),
              Text(
                TextosDetalleDelGrupo.cupos(d.cuposOcupados, d.cuposTotales),
                style: Tipo.cuerpoChico.copyWith(color: t.text2),
              ),
            ],
          ),
        ),
        const SizedBox(height: Espacio.s3),
        if (d.debeFirmar) ...[
          const Alerta(
            titulo: TextosDetalleDelGrupo.debesFirmar,
            detalle: TextosDetalleDelGrupo.debesFirmarDetalle,
            tono: Tono.aviso,
          ),
          const SizedBox(height: Espacio.s2),
          Boton(
            key: const ValueKey('firmar-reglamento'),
            texto: TextosDetalleDelGrupo.firmarReglamento,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.push('/pasanaku/grupos/$id/reglamento'),
          ),
          const SizedBox(height: Espacio.s3),
        ],
        if (d.puedeSortear || d.sorteo != null) ...[
          Boton(
            key: const ValueKey('iniciar-sorteo'),
            texto: d.puedeSortear
                ? TextosDetalleDelGrupo.iniciarSorteo
                : TextosDetalleDelGrupo.verSorteo,
            variante: d.puedeSortear
                ? BotonVariante.primario
                : BotonVariante.secundario,
            expandido: true,
            onPressed: () => context.push('/pasanaku/grupos/$id/sorteo'),
          ),
          const SizedBox(height: Espacio.s3),
        ] else if (d.soyOrganizador && completo) ...[
          Text(
            TextosDetalleDelGrupo.esperandoFirmas,
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s3),
        ],
        if (d.soyOrganizador && !completo) ...[
          Boton(
            texto: TextosInvitacion.accionInvitar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.push(
              '/pasanaku/grupos/$id/invitar?nombre=${Uri.encodeQueryComponent(d.nombre)}',
            ),
          ),
          const SizedBox(height: Espacio.s3),
        ],
        Text(
          TextosDetalleDelGrupo.integrantes,
          style: Tipo.titulo3.copyWith(color: t.text),
        ),
        const SizedBox(height: Espacio.s1),
        Text(
          TextosDetalleDelGrupo.queEs,
          style: Tipo.cuerpoChico.copyWith(color: t.text3),
        ),
        const SizedBox(height: Espacio.s3),
        for (final integrante in d.integrantes) ...[
          TarjetaDeIntegrante(
            integrante: integrante,
            grupoId: d.grupoId,
            nombreGrupo: d.nombre,
          ),
          const SizedBox(height: Espacio.s2),
        ],
      ],
    );
  }
}
