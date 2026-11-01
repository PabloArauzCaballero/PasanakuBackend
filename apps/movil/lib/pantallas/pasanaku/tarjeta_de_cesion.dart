import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'aviso_en_linea.dart';
import 'textos_del_traspaso.dart';

/// La «orden» del traspaso: arriba lo que entregás (tu cupo y su turno), abajo quién lo
/// recibe. La forma de una orden P2P —dos bloques y una flecha— sin precio: acá no se
/// vende nada, solo cambia quién ocupa el cupo.
class TarjetaDeCesion extends StatelessWidget {
  const TarjetaDeCesion({
    super.key,
    required this.mio,
    required this.nombreDelGrupo,
    this.entrante,
    this.rechazo,
  });

  final IntegranteDelGrupo mio;
  final String nombreDelGrupo;
  final IntegranteDelGrupo? entrante;

  /// El rechazo del servidor que cae sobre el cupo (AP-CU64-01), debajo de lo que dice.
  final String? rechazo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final turno = mio.turno;
    final quien = entrante;
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _Rotulo(TextosDelTraspaso.entregas, t),
          const SizedBox(height: Espacio.s2),
          Row(
            children: [
              _Sello(icono: IconosDeMarca.grupos, t: t),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      TextosDelTraspaso.tuCupo(mio.numero),
                      style: Tipo.titulo3.copyWith(color: t.text),
                    ),
                    Text(
                      TextosDelTraspaso.delGrupo(nombreDelGrupo),
                      style: Tipo.cuerpoChico.copyWith(color: t.text2),
                    ),
                  ],
                ),
              ),
              ChipEstado(
                texto: turno == null
                    ? TextosDelTraspaso.sinTurno
                    : TextosDelTraspaso.turno(turno),
                tono: Tono.info,
              ),
            ],
          ),
          if (rechazo != null) AvisoEnLinea(rechazo!),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: Espacio.s3),
            child: Row(
              children: [
                Expanded(child: Divider(color: t.border)),
                ExcludeSemantics(
                  child: Icon(Icons.arrow_downward_rounded, color: t.text3),
                ),
                Expanded(child: Divider(color: t.border)),
              ],
            ),
          ),
          _Rotulo(TextosDelTraspaso.recibe, t),
          const SizedBox(height: Espacio.s2),
          if (quien != null) _Entrante(quien) else const SizedBox.shrink(),
        ],
      ),
    );
  }
}

class _Entrante extends StatelessWidget {
  const _Entrante(this.quien);
  final IntegranteDelGrupo quien;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final nombre = quien.nombre ?? TextosDelTraspaso.sinNombre;
    final firmo = quien.estado == IntegranteDelGrupoEstadoEnum.FIRMADO;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Avatar(nombre: nombre),
        const SizedBox(width: Espacio.s3),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(nombre, style: Tipo.titulo3.copyWith(color: t.text)),
              const SizedBox(height: Espacio.s2),
              Wrap(
                spacing: Espacio.s2,
                runSpacing: Espacio.s2,
                children: [
                  const ChipEstado(
                    texto: TextosDelTraspaso.integranteDelGrupo,
                    tono: Tono.ok,
                    icono: IconosDeMarca.verificada,
                  ),
                  ChipEstado(
                    texto: firmo
                        ? TextosDelTraspaso.firmoReglamento
                        : TextosDelTraspaso.faltaFirmar,
                    tono: firmo ? Tono.ok : Tono.aviso,
                  ),
                  if (quien.esOrganizador)
                    const ChipEstado(texto: TextosDelTraspaso.organiza),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Rotulo extends StatelessWidget {
  const _Rotulo(this.texto, this.t);
  final String texto;
  final Tokens t;

  @override
  Widget build(BuildContext context) =>
      Text(texto, style: Tipo.eyebrow.copyWith(color: t.text3));
}

class _Sello extends StatelessWidget {
  const _Sello({required this.icono, required this.t});
  final IconData icono;
  final Tokens t;

  @override
  Widget build(BuildContext context) => ExcludeSemantics(
    child: DecoratedBox(
      decoration: BoxDecoration(
        color: t.brandBg,
        borderRadius: BorderRadius.circular(Radios.md),
      ),
      child: SizedBox.square(
        dimension: Tactil.minimo,
        child: Icon(icono, color: t.brandTexto),
      ),
    ),
  );
}
