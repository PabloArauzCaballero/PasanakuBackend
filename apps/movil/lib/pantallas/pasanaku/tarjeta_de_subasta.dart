import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_de_la_subasta.dart';

/// Un importe del contrato, para leer: «Bs 1.500,00».
String bs(Dinero d) => formatearMonto(monto: d.monto, moneda: d.moneda.value);

Tono tonoDe(VistaDeSubastaEstadoEnum e) => switch (e) {
  VistaDeSubastaEstadoEnum.ABIERTA => Tono.info,
  VistaDeSubastaEstadoEnum.VENDIDA => Tono.ok,
  VistaDeSubastaEstadoEnum.FALLIDA => Tono.error,
  VistaDeSubastaEstadoEnum.ADJUDICADA ||
  VistaDeSubastaEstadoEnum.LIQUIDANDO => Tono.aviso,
  _ => Tono.neutro,
};

/// Una subasta, como se lee en la lista y arriba del detalle: el grupo, la oferta más
/// alta (o la base), cuántas ofertas hay y cuándo cierra. Nunca quién sale ni quién
/// ofrece. Solo pinta: lo que trae los datos es el provider.
class TarjetaDeSubasta extends StatelessWidget {
  const TarjetaDeSubasta({super.key, required this.subasta, this.alTocar});
  final VistaDeSubasta subasta;
  final VoidCallback? alTocar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final s = subasta;
    final masAlta = s.montoMasAlto;
    final estado = TextosDeLaSubasta.estados[s.estado.value] ?? s.estado.value;
    final cifra = masAlta == null ? bs(s.montoBase) : bs(masAlta);
    final rotulo = masAlta == null
        ? TextosDeLaSubasta.sinOfertas
        : TextosDeLaSubasta.ofertaMasAlta;
    return Tarjeta(
      key: ValueKey('subasta-${s.subastaId}'),
      onTap: alTocar,
      etiqueta: '${s.nombreDelGrupo}. $rotulo $cifra. $estado.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  s.nombreDelGrupo,
                  style: Tipo.cuerpoFuerte.copyWith(color: t.text),
                ),
              ),
              ChipEstado(texto: estado, tono: tonoDe(s.estado)),
            ],
          ),
          const SizedBox(height: Espacio.s1),
          Text(
            '${TextosDeLaSubasta.aporte} ${bs(s.montoAporte)} · ${s.periodicidad.toLowerCase()}',
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s3),
          Text(rotulo, style: Tipo.etiqueta.copyWith(color: t.text2)),
          Text(cifra, style: Tipo.cifra.copyWith(color: t.text)),
          const SizedBox(height: Espacio.s2),
          Text(
            '${masAlta == null ? '${TextosDeLaSubasta.base} ${bs(s.montoBase)}' : TextosDeLaSubasta.ofertas(s.cantidadDePujas)}'
            ' · ${TextosDeLaSubasta.cierra} ${Fecha.formatear(s.cierraEn.toLocal().toIso8601String())}',
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
        ],
      ),
    );
  }
}

/// Una hoja de confirmación que se abre una vez por toque y devuelve si se confirmó.
Future<bool> confirmarEnHoja(
  BuildContext context, {
  required String titulo,
  required String detalle,
  required String textoConfirmar,
}) async {
  var cerrada = false;
  final r = await HojaDeConfirmacion.mostrar<bool>(
    context,
    hoja: Builder(
      builder: (hoja) => HojaDeConfirmacion(
        titulo: titulo,
        detalle: Text(detalle),
        textoConfirmar: textoConfirmar,
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
