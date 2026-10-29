import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'aviso_en_linea.dart';
import 'errores_del_traspaso.dart';
import 'textos_del_traspaso.dart';

/// Lo que el traspaso NO mueve, dicho antes de confirmar: el turno, la deuda vencida,
/// tu permanencia en el grupo y el dinero (no hay precio).
class LoQueNoSeMueve extends StatelessWidget {
  const LoQueNoSeMueve({super.key, required this.turno});
  final int? turno;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            TextosDelTraspaso.noSeMueveTitulo,
            style: Tipo.titulo3.copyWith(color: t.text),
          ),
          _Fila(
            icono: IconosDeMarca.periodo,
            titulo: TextosDelTraspaso.turnoQueda,
            detalle: TextosDelTraspaso.turnoQuedaDetalle(turno),
          ),
          const _Fila(
            icono: IconosDeMarca.vencimiento,
            titulo: TextosDelTraspaso.deudaQueda,
            detalle: TextosDelTraspaso.deudaQuedaDetalle,
          ),
          const _Fila(
            icono: IconosDeMarca.darDeBaja,
            titulo: TextosDelTraspaso.salisDelGrupo,
            detalle: TextosDelTraspaso.salisDelGrupoDetalle,
          ),
          const _Fila(
            icono: IconosDeMarca.custodia,
            titulo: TextosDelTraspaso.sinPrecio,
            detalle: TextosDelTraspaso.sinPrecioDetalle,
          ),
        ],
      ),
    );
  }
}

/// Las cuatro condiciones que revisa el servidor. La app no las da por cumplidas: no
/// tiene cómo saberlo. Cada rechazo aparece debajo de la condición que falló.
class CondicionesDelTraspaso extends StatelessWidget {
  const CondicionesDelTraspaso({
    super.key,
    required this.hayAcuerdo,
    this.rechazo,
  });
  final bool hayAcuerdo;
  final RechazoDelTraspaso? rechazo;

  String? _en(LugarDelRechazo lugar) =>
      rechazo?.lugar == lugar ? rechazo!.mensaje : null;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            TextosDelTraspaso.condicionesTitulo,
            style: Tipo.titulo3.copyWith(color: t.text),
          ),
          _Fila(
            icono: IconosDeMarca.aportar,
            neutra: true,
            titulo: TextosDelTraspaso.condicionAlDia,
            rechazo: _en(LugarDelRechazo.deuda),
          ),
          _Fila(
            icono: IconosDeMarca.perfil,
            neutra: true,
            titulo: TextosDelTraspaso.condicionVerificacion,
            rechazo: _en(LugarDelRechazo.verificacion),
          ),
          _Fila(
            icono: IconosDeMarca.reputacion,
            neutra: true,
            titulo: TextosDelTraspaso.condicionReputacion,
            rechazo: _en(LugarDelRechazo.reputacion),
          ),
          _Fila(
            icono: IconosDeMarca.grupos,
            neutra: true,
            titulo: TextosDelTraspaso.condicionAcuerdo,
            detalle: hayAcuerdo
                ? TextosDelTraspaso.acuerdoAdjunto
                : TextosDelTraspaso.acuerdoFalta,
            rechazo: _en(LugarDelRechazo.acuerdo),
          ),
        ],
      ),
    );
  }
}

class _Fila extends StatelessWidget {
  const _Fila({
    required this.icono,
    required this.titulo,
    this.detalle,
    this.rechazo,
    this.neutra = false,
  });

  /// Una condición que la app no puede saber si se cumple: ícono gris, nunca un check.
  final bool neutra;
  final IconData icono;
  final String titulo;
  final String? detalle;
  final String? rechazo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final fallo = rechazo != null;
    return Padding(
      padding: const EdgeInsets.only(top: Espacio.s3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ExcludeSemantics(
            child: Icon(
              fallo ? IconosDeMarca.tramiteFallido : icono,
              size: Espacio.s5,
              color: fallo ? t.errTexto : (neutra ? t.text3 : t.brandTexto),
            ),
          ),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(titulo, style: Tipo.cuerpoFuerte.copyWith(color: t.text)),
                if (detalle != null)
                  Text(
                    detalle!,
                    style: Tipo.cuerpoChico.copyWith(color: t.text2),
                  ),
                if (fallo) AvisoEnLinea(rechazo!),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
