import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/hundido_al_tocar.dart';
import '../atomos/iconos_de_marca.dart';
import '../atomos/monto.dart';
import '../atomos/tono.dart';
import '../dinero/formatear.dart';
import '../moleculas/tipo_de_movimiento.dart';
import '../tokens/tokens.dart';

/// Un movimiento del extracto, como en las apps bancarias (ADR-053): avatar redondo con lo que
/// pasó, concepto y fecha, y el importe con su signo a la derecha.
///
/// El importe va en tinta, no en verde ni rojo: el signo y el ícono ya dicen si entra o sale,
/// y el color queda para los estados (regla 1 de la maqueta). Con texto grande, el importe baja
/// debajo del concepto en vez de dejarlo en «Movi…».
class FilaDeMovimiento extends StatelessWidget {
  const FilaDeMovimiento({
    super.key,
    this.tipo,
    required this.concepto,
    required this.monto,
    required this.moneda,
    this.detalle,
    this.saldoCorrido,
    this.pendiente = false,
    this.onTap,
  });

  /// Qué pasó. `null` cuando el contrato solo informa el sentido: el ícono sale del signo de
  /// [monto] y no se adivina el tipo por la glosa.
  final TipoDeMovimiento? tipo;
  final String concepto;

  /// Cadena del contrato, con signo: `-250.00` es un débito.
  final String monto;
  final String moneda;
  final String? detalle;
  final String? saldoCorrido;

  /// Un `202`: aceptado, no confirmado. Se muestra así, nunca como éxito.
  final bool pendiente;
  final VoidCallback? onTap;

  bool get _entra => !monto.startsWith('-');
  String get _absoluto => _entra ? monto : monto.substring(1);
  String get _signo => _entra ? '+' : '\u2212';

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final nombre = tipo?.nombre ?? (_entra ? 'Crédito' : 'Débito');
    // Cifras tabulares: en una columna de importes, los dígitos se alinean (fintech-ux).
    final estiloMonto = Tipo.cuerpoFuerte.copyWith(
      color: t.text,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
    final importe = Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Un movimiento es una diferencia: lleva su signo siempre, «+» o el menos tipográfico
        // (U+2212), que se lee mejor que el guion. El lector ya oye «Crédito»/«Débito».
        ExcludeSemantics(child: Text(_signo, style: estiloMonto)),
        Monto(monto: _absoluto, moneda: moneda, estilo: estiloMonto),
      ],
    );
    final corrido = saldoCorrido == null
        ? null
        : Monto(
            monto: saldoCorrido!,
            moneda: moneda,
            etiqueta: 'Saldo después',
            estilo: Tipo.cuerpoChico.copyWith(color: t.text3),
          );

    // Una fila, un anuncio: «Débito: Recarga, 15/01 · 10:30, menos Bs 4,44».
    return MergeSemantics(
      child: Semantics(
        button: onTap != null,
        label: '$nombre: $concepto${pendiente ? ', pendiente' : ''}',
        child: HundidoAlTocar(
          activo: onTap != null,
          child: InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(Radios.md),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: Espacio.s3),
              child: LayoutBuilder(
                builder: (context, limites) {
                  final apilar =
                      _anchoDelImporte(context, estiloMonto) >
                      limites.maxWidth * 0.4;
                  final textos = Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      ExcludeSemantics(
                        child: Text(
                          concepto,
                          style: Tipo.cuerpo.copyWith(
                            color: t.text,
                            fontWeight: FontWeight.w500,
                          ),
                          maxLines: apilar ? 2 : 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Text(
                        detalle ?? nombre,
                        style: Tipo.cuerpoChico.copyWith(color: t.text3),
                      ),
                      if (apilar) ...[
                        const SizedBox(height: Espacio.s1),
                        // Un importe no se parte: si ni solo entra, se achica.
                        FittedBox(
                          fit: BoxFit.scaleDown,
                          alignment: Alignment.centerLeft,
                          child: importe,
                        ),
                        ?corrido,
                      ],
                      if (pendiente)
                        const Padding(
                          padding: EdgeInsets.only(top: Espacio.s1),
                          child: ChipEstado(
                            texto: 'Pendiente',
                            tono: Tono.aviso,
                          ),
                        ),
                    ],
                  );
                  return Row(
                    crossAxisAlignment: apilar
                        ? CrossAxisAlignment.start
                        : CrossAxisAlignment.center,
                    children: [
                      _Avatar(icono: tipo?.icono ?? _iconoDelSentido),
                      const SizedBox(width: Espacio.s3),
                      Expanded(child: textos),
                      if (!apilar) ...[
                        const SizedBox(width: Espacio.s3),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [importe, ?corrido],
                        ),
                      ],
                    ],
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }

  IconData get _iconoDelSentido =>
      _entra ? IconosDeMarca.entra : IconosDeMarca.sale;

  double _anchoDelImporte(BuildContext context, TextStyle estilo) {
    final pintor = TextPainter(
      text: TextSpan(
        text: '$_signo${formatearMonto(monto: _absoluto, moneda: moneda)}',
        style: estilo,
      ),
      textScaler: MediaQuery.textScalerOf(context),
      textDirection: TextDirection.ltr,
      maxLines: 1,
    )..layout();
    final ancho = pintor.width;
    pintor.dispose();
    return ancho;
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({required this.icono});

  final IconData icono;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(color: t.surface2, shape: BoxShape.circle),
      child: SizedBox.square(
        dimension: Tactil.minimo - Espacio.s2,
        child: Icon(icono, size: Espacio.s5, color: t.text2),
      ),
    );
  }
}
