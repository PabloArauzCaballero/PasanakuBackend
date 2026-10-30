import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/opcion_seleccionable.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'condiciones_del_traspaso.dart';
import 'errores_del_traspaso.dart';
import 'tarjeta_de_cesion.dart';
import 'textos_del_traspaso.dart';

/// Paso 1 · elegir quién entra, entre los integrantes del grupo que no soy yo. Es la
/// única fuente honesta del `usuarioEntranteId`: el servidor exige que quien entra ya
/// participe del grupo (`consultarDetalleDelGrupo`).
class PasoElegir extends StatelessWidget {
  const PasoElegir({
    super.key,
    required this.candidatos,
    required this.elegido,
    required this.alElegir,
  });
  final List<IntegranteDelGrupo> candidatos;
  final String? elegido;
  final ValueChanged<String> alElegir;

  @override
  Widget build(BuildContext context) {
    if (candidatos.isEmpty) {
      return const EstadoVacio(
        titulo: TextosDelTraspaso.nadieTitulo,
        mensaje: TextosDelTraspaso.nadieDetalle,
        icono: IconosDeMarca.solicitudes,
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const IntroDePantalla(
          icono: IconosDeMarca.solicitudes,
          titulo: TextosDelTraspaso.elegirTitulo,
          explicacion: TextosDelTraspaso.elegirExplicacion,
        ),
        for (final c in candidatos) ...[
          OpcionSeleccionable(
            key: ValueKey('candidato-${c.usuarioId}'),
            titulo: c.nombre ?? TextosDelTraspaso.sinNombre,
            detalle: _detalle(c),
            elegida: c.usuarioId == elegido,
            onTap: () => alElegir(c.usuarioId!),
          ),
          const SizedBox(height: Espacio.s2),
        ],
      ],
    );
  }

  static String _detalle(IntegranteDelGrupo c) {
    final turno = c.turno;
    return [
      TextosDelTraspaso.cupoN(c.numero),
      if (turno != null) TextosDelTraspaso.turno(turno),
      c.estado == IntegranteDelGrupoEstadoEnum.FIRMADO
          ? TextosDelTraspaso.firmoReglamento
          : TextosDelTraspaso.faltaFirmar,
    ].join(' · ');
  }
}

/// Paso 2 · el motivo (10 a 300 caracteres, como pide el contrato).
class PasoMotivo extends StatelessWidget {
  const PasoMotivo({
    super.key,
    required this.controlador,
    required this.alCambiar,
  });
  final TextEditingController controlador;
  final ValueChanged<String> alCambiar;

  @override
  Widget build(BuildContext context) {
    final largo = controlador.text.trim().length;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const IntroDePantalla(
          icono: IconosDeMarca.guia,
          titulo: TextosDelTraspaso.motivoTitulo,
          explicacion: TextosDelTraspaso.motivoExplicacion,
        ),
        Campo(
          etiqueta: TextosDelTraspaso.motivoEtiqueta,
          controlador: controlador,
          lineas: 4,
          ayuda:
              '${TextosDelTraspaso.motivoAyuda} '
              '${TextosDelTraspaso.contador(largo)}',
          error: largo > 300 ? TextosDelTraspaso.motivoAyuda : null,
          onChanged: alCambiar,
        ),
      ],
    );
  }
}

/// Paso 3 · la revisión: la orden (entregás / recibe), lo que no se mueve y lo que se
/// revisa al confirmar. Cada rechazo cae en la tarjeta que lo explica.
class PasoRevisar extends StatelessWidget {
  const PasoRevisar({
    super.key,
    required this.mio,
    required this.entrante,
    required this.nombreDelGrupo,
    required this.hayAcuerdo,
    this.detalleAcuerdo,
    this.rechazo,
  });
  final IntegranteDelGrupo mio;
  final IntegranteDelGrupo entrante;
  final String nombreDelGrupo;
  final bool hayAcuerdo;
  final String? detalleAcuerdo;
  final RechazoDelTraspaso? rechazo;

  @override
  Widget build(BuildContext context) {
    final r = rechazo;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TarjetaDeCesion(
          mio: mio,
          nombreDelGrupo: nombreDelGrupo,
          entrante: entrante,
          rechazo: r?.lugar == LugarDelRechazo.cupo ? r!.mensaje : null,
        ),
        const SizedBox(height: Espacio.s3),
        LoQueNoSeMueve(turno: mio.turno),
        const SizedBox(height: Espacio.s3),
        CondicionesDelTraspaso(
          hayAcuerdo: hayAcuerdo,
          detalleAcuerdo: detalleAcuerdo,
          rechazo: r,
        ),
        if (r != null && r.lugar == LugarDelRechazo.general) ...[
          const SizedBox(height: Espacio.s3),
          Alerta(
            titulo: r.mensaje,
            detalle: r.traza == null
                ? null
                : TextosDelTraspaso.codigoDeSeguimiento(r.traza!),
            tono: Tono.error,
          ),
        ],
      ],
    );
  }
}
