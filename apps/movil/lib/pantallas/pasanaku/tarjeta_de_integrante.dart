import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'pantalla_detalle_miembro.dart';
import 'textos_detalle_del_grupo.dart';

/// Un cupo del grupo como tarjeta (2026-10-20): quién lo ocupa, si firmó y su turno.
/// Un cupo libre se ve libre, no se esconde: así se entiende cuánto falta.
/// Tocarla abre la pantalla completa del detalle del participante con sus métricas
/// fácticas de reputación, riesgo, puntos y trofeos sin romper su privacidad.
class TarjetaDeIntegrante extends StatelessWidget {
  const TarjetaDeIntegrante({
    super.key,
    required this.integrante,
    this.grupoId,
    this.nombreGrupo,
  });

  final IntegranteDelGrupo integrante;
  final String? grupoId;
  final String? nombreGrupo;

  String get _nombre => integrante.nombre ?? TextosDetalleDelGrupo.sinNombre;

  String get _estado => switch (integrante.estado) {
    IntegranteDelGrupoEstadoEnum.FIRMADO => TextosDetalleDelGrupo.firmo,
    IntegranteDelGrupoEstadoEnum.PENDIENTE_FIRMA =>
      TextosDetalleDelGrupo.faltaFirmar,
    _ => TextosDetalleDelGrupo.libre,
  };

  bool get _libre => integrante.estado == IntegranteDelGrupoEstadoEnum.LIBRE;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final turno = integrante.turno;
    return Tarjeta(
      etiqueta: _libre
          ? '${TextosDetalleDelGrupo.cupo(integrante.numero)}, ${TextosDetalleDelGrupo.cupoLibre}'
          : TextosDetalleDelGrupo.etiquetaDeTarjeta(_nombre, _estado, turno),
      onTap: _libre ? null : () => _abrirPerfil(context),
      child: Row(
        children: [
          if (_libre)
            CircleAvatar(
              radius: (Espacio.s7 - Espacio.s2) / 2,
              backgroundColor: t.border,
              child: Icon(Icons.person_add_alt_1_outlined, color: t.text3),
            )
          else
            Avatar(nombre: _nombre),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _libre ? TextosDetalleDelGrupo.cupoLibre : _nombre,
                  style: Tipo.titulo3.copyWith(
                    color: _libre ? t.text3 : t.text,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  [
                    TextosDetalleDelGrupo.cupo(integrante.numero),
                    if (integrante.esOrganizador)
                      TextosDetalleDelGrupo.organiza,
                    if (integrante.soyYo) TextosDetalleDelGrupo.vos,
                  ].join(' · '),
                  style: Tipo.cuerpoChico.copyWith(color: t.text3),
                ),
              ],
            ),
          ),
          if (turno != null)
            _Turno(numero: turno)
          else if (!_libre)
            ChipEstado(
              texto: _estado,
              tono: integrante.estado == IntegranteDelGrupoEstadoEnum.FIRMADO
                  ? Tono.ok
                  : Tono.aviso,
            ),
        ],
      ),
    );
  }

  void _abrirPerfil(BuildContext context) {
    final cupoId = integrante.cupoId ?? integrante.numero.toString();
    final gid = grupoId ?? '';
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => PantallaDetalleMiembro(
          grupoId: gid,
          cupoId: cupoId,
          nombre: _nombre,
          numero: integrante.numero,
          turno: integrante.turno,
          esOrganizador: integrante.esOrganizador,
          soyYo: integrante.soyYo,
          estado: integrante.estado.name,
          nombreGrupo: nombreGrupo,
          usuarioId: integrante.usuarioId,
        ),
      ),
    );
  }
}

/// El turno con el color de esa posición en la rueda: el mismo que ve en la ruleta.
class _Turno extends StatelessWidget {
  const _Turno({required this.numero});
  final int numero;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(
      horizontal: Espacio.s3,
      vertical: Espacio.s1,
    ),
    decoration: BoxDecoration(
      color: ColoresDeTurno.de(numero - 1),
      borderRadius: BorderRadius.circular(Radios.lg),
    ),
    child: Text(
      TextosDetalleDelGrupo.turno(numero),
      style: Tipo.etiqueta.copyWith(color: Paleta.ink),
    ),
  );
}
