import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/progreso.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/iniciar_grupo.dart';
import 'errores_de_etapa_e_inicio.dart';
import 'textos_de_etapa_e_inicio.dart';

/// Cuánto falta para poder iniciar el grupo (75 % de los cupos ocupados) y, para quien
/// organiza, el botón Iniciar. Si se puede o no lo dice el servidor (`puedoIniciar`);
/// la app solo explica el porqué con los números que vienen en el detalle.
class InicioDelGrupo extends ConsumerStatefulWidget {
  const InicioDelGrupo({super.key, required this.detalle});
  final DetalleDelGrupo detalle;

  /// Solo en `ABIERTO_A_INSCRIPCION` y con un servidor que mande el mínimo.
  static bool aplica(DetalleDelGrupo d) =>
      d.estado == 'ABIERTO_A_INSCRIPCION' && d.minimoParaIniciar != null;

  @override
  ConsumerState<InicioDelGrupo> createState() => _InicioDelGrupoState();
}

class _InicioDelGrupoState extends ConsumerState<InicioDelGrupo> {
  bool _trabajando = false;
  Object? _error;

  int get _faltan =>
      widget.detalle.minimoParaIniciar! - widget.detalle.cuposOcupados;

  String? _motivo() {
    final d = widget.detalle;
    if (d.puedoIniciar == true) return null;
    if (_faltan > 0) return TextosDeEtapaEInicio.motivoFaltan(_faltan);
    final sinFirmar = d.integrantes
        .where((i) => i.estado == IntegranteDelGrupoEstadoEnum.PENDIENTE_FIRMA)
        .length;
    if (sinFirmar > 0) return TextosDeEtapaEInicio.motivoFirmas(sinFirmar);
    return TextosDeEtapaEInicio.motivoGeneral;
  }

  Future<void> _iniciar() async {
    final d = widget.detalle;
    if (!await _confirmar(d) || !mounted) return;
    setState(() {
      _trabajando = true;
      _error = null;
    });
    try {
      final r = await ref.read(iniciarGrupoProvider)(d.grupoId);
      if (mounted) {
        mostrarSnackbar(context, TextosDeEtapaEInicio.iniciado(r.cuposTotales));
      }
    } on Object catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _trabajando = false);
    }
  }

  /// `true` solo si la persona tocó «Sí, iniciar»; un segundo toque mientras la hoja se
  /// cierra no hace nada.
  Future<bool> _confirmar(DetalleDelGrupo d) async {
    var cerrada = false;
    final r = await HojaDeConfirmacion.mostrar<bool>(
      context,
      hoja: Builder(
        builder: (hoja) => HojaDeConfirmacion(
          titulo: TextosDeEtapaEInicio.confirmarTitulo(d.cuposOcupados),
          detalle: Alerta(
            titulo: TextosDeEtapaEInicio.confirmarDetalle(
              d.cuposOcupados,
              d.cuposTotales,
            ),
            tono: Tono.aviso,
          ),
          textoConfirmar: TextosDeEtapaEInicio.confirmar,
          onConfirmar: () {
            if (cerrada) return;
            cerrada = true;
            Navigator.of(hoja).pop(true);
          },
        ),
      ),
    );
    return r ?? false;
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final d = widget.detalle;
    final minimo = d.minimoParaIniciar!;
    final motivo = _motivo();
    final rechazo = _error == null ? null : rechazoAlIniciar(_error!);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Semantics(
            header: true,
            child: Text(
              TextosDeEtapaEInicio.inicioTitulo,
              style: Tipo.titulo3.copyWith(color: t.text),
            ),
          ),
          const SizedBox(height: Espacio.s3),
          Progreso(
            valor: minimo == 0 ? 1 : d.cuposOcupados / minimo,
            tono: _faltan <= 0 ? Tono.ok : Tono.marca,
            etiqueta: TextosDeEtapaEInicio.inicioEtiquetaProgreso,
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosDeEtapaEInicio.inicioProgreso(
              d.cuposOcupados,
              d.cuposTotales,
              _faltan,
            ),
            style: Tipo.cuerpo.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s1),
          Text(
            TextosDeEtapaEInicio.inicioRegla(minimo, d.cuposTotales),
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
          if (d.soyOrganizador) ...[
            const SizedBox(height: Espacio.s4),
            Boton(
              key: const ValueKey('iniciar-grupo'),
              texto: TextosDeEtapaEInicio.iniciarBoton,
              variante: BotonVariante.primario,
              expandido: true,
              cargando: _trabajando,
              motivoDeBloqueo: motivo,
              onPressed: motivo == null ? _iniciar : null,
            ),
          ],
          if (rechazo != null) ...[
            const SizedBox(height: Espacio.s3),
            Alerta(
              titulo: rechazo.mensaje,
              detalle: rechazo.traza == null
                  ? null
                  : 'Código de seguimiento: ${rechazo.traza}',
              tono: Tono.error,
            ),
          ],
        ],
      ),
    );
  }
}
