import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'abrir_mi_grupo.dart';
import 'dominio/mis_grupos.dart';
import 'textos.dart';

/// Un grupo como tarjeta de billetera (ADR-053): arriba quién es y cómo está, abajo la plata
/// que pide por período como cifra protagonista. Antes era una fila con una «G», el nombre y
/// un chip: no decía cuánto ni invitaba a entrar.
///
/// Solo con lo que el servidor manda (`GrupoMio`): sin turno ni avance inventados.
class TarjetaDeMiGrupo extends ConsumerWidget {
  const TarjetaDeMiGrupo({super.key, required this.grupo});
  final GrupoMio grupo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = Tokens.of(context);
    final nombre = grupo.nombre ?? TextosPasanaku.grupoSinNombre;
    final (etiqueta, tono) = estadoDeGrupo(grupo.estado);
    final monto = grupo.montoAporte;
    return Tarjeta(
      relleno: false,
      etiqueta: 'Grupo $nombre, $etiqueta',
      onTap: () => abrirMiGrupo(context, ref, grupo, nombre: nombre),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.all(Espacio.s4),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Avatar(nombre: nombre),
                const SizedBox(width: Espacio.s3),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        nombre,
                        style: Tipo.titulo3.copyWith(color: t.text),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: Espacio.s1),
                      Text(
                        grupo.esOrganizador
                            ? TextosPasanaku.organizasEsteGrupo
                            : TextosPasanaku.participasEnEsteGrupo,
                        style: Tipo.cuerpoChico.copyWith(color: t.text2),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: Espacio.s2),
                ChipEstado(texto: etiqueta, tono: tono),
              ],
            ),
          ),
          // La franja de abajo: la cifra del grupo y la flecha de entrar.
          DecoratedBox(
            decoration: BoxDecoration(
              color: t.surface2,
              borderRadius: const BorderRadius.vertical(
                bottom: Radius.circular(Radios.lg),
              ),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: Espacio.s4,
                vertical: Espacio.s3,
              ),
              child: Row(
                children: [
                  Expanded(
                    child: monto == null
                        ? Text(
                            TextosPasanaku.verEstadoDelGrupo,
                            style: Tipo.cuerpo.copyWith(color: t.text2),
                          )
                        : Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                TextosPasanaku.etiquetaAportePorPeriodo,
                                style: Tipo.cuerpoChico.copyWith(
                                  color: t.text2,
                                ),
                              ),
                              Monto(
                                monto: monto,
                                moneda: grupo.moneda ?? 'BOB',
                                etiqueta:
                                    TextosPasanaku.etiquetaAportePorPeriodo,
                                estilo: Tipo.cifra.copyWith(color: t.text),
                              ),
                            ],
                          ),
                  ),
                  Icon(IconosDeMarca.seguir, color: t.text2),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

(String, Tono) estadoDeGrupo(String estado) => switch (estado) {
  'ACTIVO' => ('Activo', Tono.ok),
  'ACEPTADO_PENDIENTE_FIRMA' => ('Falta firmar', Tono.aviso),
  'EN_MORA' => ('Con deuda', Tono.aviso),
  'SUSPENDIDO' => ('Suspendido', Tono.aviso),
  _ => ('En proceso', Tono.info),
};
