import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/monto.dart';
import '../atomos/tono.dart';
import '../moleculas/tipo_de_movimiento.dart';
import '../tokens/tokens.dart';

/// Un movimiento del extracto: tipado por lo que pasó, con concepto y saldo corrido opcional.
class FilaDeMovimiento extends StatelessWidget {
  const FilaDeMovimiento({
    super.key,
    required this.tipo,
    required this.concepto,
    required this.monto,
    required this.moneda,
    this.detalle,
    this.saldoCorrido,
    this.pendiente = false,
    this.onTap,
  });

  final TipoDeMovimiento tipo;
  final String concepto;

  /// Cadena del contrato, con signo: `-250.00` es un débito.
  final String monto;
  final String moneda;
  final String? detalle;
  final String? saldoCorrido;

  /// Un `202`: aceptado, no confirmado. Se muestra así, nunca como éxito.
  final bool pendiente;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final debito = monto.startsWith('-');
    return Semantics(
      button: onTap != null,
      label: '${tipo.nombre}: $concepto${pendiente ? ', pendiente' : ''}',
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: Espacio.s3),
          child: Row(
            children: [
              Container(
                width: Tactil.minimo - Espacio.s2,
                height: Tactil.minimo - Espacio.s2,
                decoration: BoxDecoration(
                  color: t.surface2,
                  borderRadius: BorderRadius.circular(Radios.md),
                ),
                child: Icon(
                  tipo.icono,
                  color: pendiente ? t.text3 : t.brandTexto,
                ),
              ),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      concepto,
                      style: texto.bodyLarge?.copyWith(color: t.text),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    Text(
                      detalle ?? tipo.nombre,
                      style: texto.bodySmall?.copyWith(color: t.text3),
                    ),
                    if (pendiente)
                      const Padding(
                        padding: EdgeInsets.only(top: Espacio.s1),
                        child: ChipEstado(texto: 'Pendiente', tono: Tono.aviso),
                      ),
                  ],
                ),
              ),
              const SizedBox(width: Espacio.s3),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Monto(
                    monto: monto,
                    moneda: moneda,
                    estilo: texto.bodyLarge?.copyWith(
                      color: debito ? t.text : t.okTexto,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  if (saldoCorrido != null)
                    Monto(
                      monto: saldoCorrido!,
                      moneda: moneda,
                      etiqueta: 'Saldo después',
                      estilo: texto.bodySmall?.copyWith(color: t.text3),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
