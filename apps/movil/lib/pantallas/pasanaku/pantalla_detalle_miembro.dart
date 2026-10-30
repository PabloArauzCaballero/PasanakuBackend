import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/organismos/motivo_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'avatar_del_integrante.dart';
import 'dominio/perfil_del_integrante.dart';
import 'seccion_de_confianza.dart';
import 'seccion_de_contacto.dart';
import 'textos_detalle_del_grupo.dart';
import 'textos_perfil_del_integrante.dart';

/// El perfil de un integrante del grupo, a pantalla completa y con «volver»
/// (2026-10-21). Muestra solo lo que el servidor devuelve: el teléfono
/// (`identidad`) y la reputación (`transparencia`), los dos únicamente para quien
/// comparte el grupo. Ninguna cifra se calcula ni se completa en la app.
class PantallaDetalleMiembro extends ConsumerWidget {
  const PantallaDetalleMiembro({
    super.key,
    required this.grupoId,
    required this.cupoId,
    required this.nombre,
    required this.numero,
    this.turno,
    this.esOrganizador = false,
    this.soyYo = false,
    this.estado = 'FIRMADO',
    this.nombreGrupo,
    this.usuarioId,
  });

  final String grupoId;
  final String cupoId;
  final String nombre;
  final int numero;
  final int? turno;
  final bool esOrganizador;
  final bool soyYo;
  final String estado;
  final String? nombreGrupo;
  final String? usuarioId;

  String get _nombre =>
      nombre.isNotEmpty ? nombre : TextosDetalleDelGrupo.sinNombre;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final id = usuarioId;
    final conDatos = id != null && id.isNotEmpty && grupoId.isNotEmpty;
    final IntegranteDelGrupo? clave = conDatos
        ? (grupoId: grupoId, usuarioId: id)
        : null;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(
            Espacio.s4,
            0,
            Espacio.s4,
            Espacio.s6,
          ),
          children: [
            const CabeceraDeSeccion(titulo: TextosPerfilDelIntegrante.titulo),
            const SizedBox(height: Espacio.s3),
            _Encabezado(
              nombre: _nombre,
              numero: numero,
              turno: turno,
              esOrganizador: esOrganizador,
              soyYo: soyYo,
              nombreGrupo: nombreGrupo,
            ),
            const SizedBox(height: Espacio.s5),
            if (clave == null)
              const EstadoVacio(
                mensaje: TextosPerfilDelIntegrante.soloMismoGrupo,
                motivo: MotivoVacio.porPermiso,
              )
            else ...[
              SeccionDeContacto(
                valor: ref.watch(contactoDelIntegranteProvider(clave)),
                reintentar: () =>
                    ref.invalidate(contactoDelIntegranteProvider(clave)),
              ),
              const SizedBox(height: Espacio.s5),
              SeccionDeConfianza(
                valor: ref.watch(reputacionDelIntegranteProvider(clave)),
                reintentar: () =>
                    ref.invalidate(reputacionDelIntegranteProvider(clave)),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Quién es, en el grupo: nombre, cupo, si organiza, si es uno mismo y su turno.
class _Encabezado extends StatelessWidget {
  const _Encabezado({
    required this.nombre,
    required this.numero,
    required this.turno,
    required this.esOrganizador,
    required this.soyYo,
    required this.nombreGrupo,
  });

  final String nombre;
  final int numero;
  final int? turno;
  final bool esOrganizador;
  final bool soyYo;
  final String? nombreGrupo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final turnoNum = turno;
    return Row(
      children: [
        AvatarDelIntegrante(nombre: nombre, soyYo: soyYo),
        const SizedBox(width: Espacio.s3),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Semantics(
                header: true,
                child: Text(
                  nombre,
                  style: Tipo.titulo2.copyWith(color: t.text),
                ),
              ),
              if (nombreGrupo != null && nombreGrupo!.isNotEmpty)
                Text(
                  nombreGrupo!,
                  style: Tipo.cuerpoChico.copyWith(color: t.text3),
                ),
              const SizedBox(height: Espacio.s2),
              Wrap(
                spacing: Espacio.s2,
                runSpacing: Espacio.s2,
                children: [
                  ChipEstado(
                    texto: TextosDetalleDelGrupo.cupo(numero),
                    tono: Tono.neutro,
                  ),
                  if (esOrganizador)
                    const ChipEstado(
                      texto: TextosDetalleDelGrupo.organiza,
                      tono: Tono.marca,
                    ),
                  if (soyYo)
                    const ChipEstado(
                      texto: TextosDetalleDelGrupo.vos,
                      tono: Tono.info,
                    ),
                  ChipEstado(
                    texto: turnoNum == null
                        ? TextosDetalleDelGrupo.sinTurno
                        : TextosDetalleDelGrupo.turno(turnoNum),
                    tono: turnoNum == null ? Tono.neutro : Tono.ok,
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}
