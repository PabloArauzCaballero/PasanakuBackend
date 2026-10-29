import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/fila_de_dato.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:flutter/material.dart';

import 'textos_del_traspaso.dart';

/// El resumen que se repite en la hoja de confirmación y en el comprobante.
class ResumenDelTraspaso extends StatelessWidget {
  const ResumenDelTraspaso({
    super.key,
    required this.mio,
    required this.entrante,
    required this.motivo,
  });
  final IntegranteDelGrupo mio;
  final IntegranteDelGrupo entrante;
  final String motivo;

  @override
  Widget build(BuildContext context) {
    final turno = mio.turno;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        FilaDeDato(
          etiqueta: TextosDelTraspaso.cupo,
          valor: [
            TextosDelTraspaso.tuCupo(mio.numero),
            turno == null
                ? TextosDelTraspaso.sinTurno
                : TextosDelTraspaso.turno(turno),
          ].join(' · '),
        ),
        FilaDeDato(
          etiqueta: TextosDelTraspaso.pasaA,
          valor: entrante.nombre ?? TextosDelTraspaso.sinNombre,
        ),
        FilaDeDato(etiqueta: TextosDelTraspaso.motivo, valor: motivo),
      ],
    );
  }
}

/// Abre la hoja que confirma el traspaso. Devuelve `true` solo si la persona tocó
/// «Sí, traspasar»; un segundo toque mientras la hoja se cierra no hace nada.
Future<bool> confirmarTraspaso(BuildContext context, Widget resumen) async {
  var cerrada = false;
  final r = await HojaDeConfirmacion.mostrar<bool>(
    context,
    hoja: Builder(
      builder: (hoja) => HojaDeConfirmacion(
        titulo: TextosDelTraspaso.confirmarTitulo,
        detalle: resumen,
        textoConfirmar: TextosDelTraspaso.confirmar,
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

/// El comprobante: el servidor aceptó el traspaso y devolvió su número. Se muestra
/// desde arriba —la cabecera abierta—, no a la altura donde quedó la revisión.
class ComprobanteDelTraspaso extends StatefulWidget {
  const ComprobanteDelTraspaso({
    super.key,
    required this.salida,
    required this.resumen,
    required this.nombreEntrante,
  });
  final SalidaTraspaso salida;
  final Widget resumen;
  final String nombreEntrante;

  @override
  State<ComprobanteDelTraspaso> createState() => _ComprobanteState();
}

class _ComprobanteState extends State<ComprobanteDelTraspaso> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final anidado = mounted
          ? context.findAncestorStateOfType<NestedScrollViewState>()
          : null;
      for (final c in [anidado?.innerController, anidado?.outerController]) {
        if (c != null && c.hasClients) c.jumpTo(0);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final salida = widget.salida;
    return EstadoDeTramite(
      icono: IconosDeMarca.tramiteListo,
      tono: Tono.ok,
      titulo: TextosDelTraspaso.listoTitulo,
      detalle: TextosDelTraspaso.listoDetalle(widget.nombreEntrante),
      caso: TextosDelTraspaso.comprobante(salida.traspasoId),
      extra: Tarjeta(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            widget.resumen,
            FilaDeDato(
              etiqueta: TextosDelTraspaso.numeroDeTraspaso,
              valor: salida.traspasoId,
            ),
          ],
        ),
      ),
    );
  }
}
