import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/kyc_del_titular.dart';

/// La verificación de identidad como **cabecera del perfil** (ADR-053): un escudo en un
/// círculo de marca, el estado en un chip con texto y, si falta evidencia, la acción para
/// completarla. Antes era una tarjeta con un título y una oración suelta.
class SeccionVerificacionKyc extends ConsumerWidget {
  const SeccionVerificacionKyc({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = Tokens.of(context);
    final estado = ref.watch(kycDeLaSesionProvider);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              DecoratedBox(
                decoration: BoxDecoration(
                  color: t.brandBg,
                  shape: BoxShape.circle,
                ),
                child: SizedBox.square(
                  dimension: Tactil.minimo + Espacio.s2,
                  child: Icon(
                    IconosDeMarca.verificada,
                    size: Espacio.s5 + Espacio.s1,
                    color: t.brandTexto,
                  ),
                ),
              ),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Text(
                  'Verificación de identidad (KYC)',
                  style: Tipo.titulo3.copyWith(color: t.text),
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),
          estado.when(
            loading: () => Text(
              'Consultando tu verificación…',
              style: Tipo.cuerpo.copyWith(color: t.text2),
            ),
            error: (_, _) => Boton(
              texto: 'No pudimos consultar el estado. Reintentar',
              variante: BotonVariante.secundario,
              expandido: true,
              onPressed: () => ref.invalidate(kycDeLaSesionProvider),
            ),
            data: (expediente) {
              if (expediente == null) {
                return Text(
                  'Iniciá sesión para consultar tu verificación.',
                  style: Tipo.cuerpo.copyWith(color: t.text2),
                );
              }
              final faltantes = pendientesDeKyc(expediente);
              final cuerpo = Tipo.cuerpo.copyWith(color: t.text2);
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: ChipEstado(
                      texto: etiquetaEstadoKyc(expediente.estado),
                      tono: _tonoDe(expediente.estado),
                    ),
                  ),
                  if (faltantes.isNotEmpty &&
                      kycAdmiteCorreccion(expediente)) ...[
                    const SizedBox(height: Espacio.s3),
                    Text(
                      'Para completar tu expediente falta: ${faltantes.join(', ')}.',
                      style: cuerpo,
                    ),
                    const SizedBox(height: Espacio.s3),
                    Boton(
                      texto: 'Completar expediente',
                      variante: BotonVariante.primario,
                      expandido: true,
                      onPressed: () =>
                          context.push('/identidad/perfil/verificacion'),
                    ),
                  ] else if (kycAdmiteCorreccion(expediente)) ...[
                    const SizedBox(height: Espacio.s3),
                    Text(
                      'Tu expediente está completo. La aprobación sigue siendo manual.',
                      style: cuerpo,
                    ),
                  ],
                  if (expediente.estado ==
                      EstadoDeVerificacionEstadoEnum.RECHAZADA) ...[
                    const SizedBox(height: Espacio.s3),
                    Text(
                      'Revisá el motivo informado y contactá soporte para conocer los próximos pasos.',
                      style: cuerpo,
                    ),
                  ],
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  /// El estado se dice con texto Y con tono: nunca solo con el color.
  Tono _tonoDe(EstadoDeVerificacionEstadoEnum e) => switch (e) {
    EstadoDeVerificacionEstadoEnum.APROBADA => Tono.ok,
    EstadoDeVerificacionEstadoEnum.RECHAZADA => Tono.error,
    EstadoDeVerificacionEstadoEnum.EN_REVISION => Tono.info,
    EstadoDeVerificacionEstadoEnum.PENDIENTE => Tono.aviso,
  };
}
