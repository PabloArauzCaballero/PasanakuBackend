import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// El estado del aporte como **objeto protagonista** (ADR-053, crítica `roast-pantallas-hoy`):
/// el chip dice cómo estás, la cifra grande dice cuánto y cada número lleva su nombre a la
/// vista. Antes la deuda iba dentro de una oración y «lo que falta aportar» era un número
/// suelto que solo el lector de pantalla sabía qué era.
///
/// Sin color de alarma: una deuda es información, no una sirena (fintech-ux). El tono lo da
/// el chip, con texto.
class TarjetaDeAporte extends StatelessWidget {
  const TarjetaDeAporte({super.key, required this.e});
  final EstadoDelParticipante e;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final moneda = e.moneda.value;
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ChipEstado(
            texto: e.alDia ? 'Al día' : 'Con deuda vigente',
            tono: e.alDia ? Tono.ok : Tono.aviso,
          ),
          const SizedBox(height: Espacio.s4),
          if (e.alDia)
            Text(
              TextosPasanaku.alDiaTitulo,
              style: Tipo.titulo1.copyWith(color: t.text),
            )
          else ...[
            Text(
              TextosPasanaku.etiquetaDeudaVigente,
              style: Tipo.cuerpoChico.copyWith(color: t.text2),
            ),
            const SizedBox(height: Espacio.s1),
            FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerLeft,
              child: Monto(
                monto: e.deudaVigente,
                moneda: moneda,
                etiqueta: TextosPasanaku.etiquetaDeudaVigente,
                estilo: Tipo.cifraGrande.copyWith(color: t.text),
              ),
            ),
          ],
          const SizedBox(height: Espacio.s4),
          Divider(height: 1, color: t.border),
          const SizedBox(height: Espacio.s4),
          Wrap(
            spacing: Espacio.s5,
            runSpacing: Espacio.s3,
            children: [
              _Dato(
                etiqueta: TextosPasanaku.etiquetaPorAportar,
                valor: Monto(
                  monto: e.porAportar,
                  moneda: moneda,
                  etiqueta: TextosPasanaku.etiquetaPorAportar,
                  estilo: Tipo.cifra.copyWith(color: t.text),
                ),
              ),
              _Dato(
                etiqueta: TextosPasanaku.etiquetaObligacionesAbiertas,
                valor: Text(
                  '${e.obligacionesAbiertas}',
                  style: Tipo.cifra.copyWith(color: t.text),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Dato extends StatelessWidget {
  const _Dato({required this.etiqueta, required this.valor});
  final String etiqueta;
  final Widget valor;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return MergeSemantics(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(etiqueta, style: Tipo.cuerpoChico.copyWith(color: t.text2)),
          const SizedBox(height: Espacio.s1),
          valor,
        ],
      ),
    );
  }
}

/// La restricción como aviso propio, con el monto que la levanta; sin restricción, una línea
/// tranquila con escudo (lo bueno también se dice, pero sin gritar).
class AvisoDeRestriccion extends StatelessWidget {
  const AvisoDeRestriccion({
    super.key,
    required this.vigente,
    required this.montoQueLaLevanta,
  });
  final bool vigente;

  /// Ya formateado, con espacio sin corte entre moneda y cifra.
  final String montoQueLaLevanta;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    if (vigente) {
      return Alerta(
        titulo: TextosPasanaku.tituloRestriccion,
        detalle: TextosPasanaku.levantaRestriccion(montoQueLaLevanta),
        tono: Tono.aviso,
      );
    }
    return MergeSemantics(
      child: Row(
        children: [
          Icon(IconosDeMarca.custodia, size: Espacio.s5, color: t.okTexto),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Text(
              TextosPasanaku.sinRestriccion,
              style: Tipo.cuerpo.copyWith(color: t.text2),
            ),
          ),
        ],
      ),
    );
  }
}
