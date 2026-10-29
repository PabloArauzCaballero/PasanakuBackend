import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../tokens/tokens.dart';
import 'campo_monto.dart';
import 'chip_elegible.dart';

/// El importe como **protagonista de la pantalla** (ADR-053): una cifra grande y centrada que
/// se escribe en el lugar, como en las apps bancarias, con montos sugeridos debajo.
///
/// Mismo contrato que [CampoMonto]: devuelve la cadena del contrato (`1240.00`), nunca un
/// `double`, o `null` si lo escrito no es un importe. Tiene nombre accesible ([etiqueta]) y la
/// etiqueta también se ve, encima de la cifra: el «0,00» de fondo no es una etiqueta.
class CampoMontoGrande extends StatefulWidget {
  const CampoMontoGrande({
    super.key,
    required this.etiqueta,
    required this.onChanged,
    this.moneda = 'BOB',
    this.ayuda,
    this.error,
    this.sugeridos = const [],
  });

  final String etiqueta;
  final ValueChanged<String?> onChanged;
  final String moneda;
  final String? ayuda;
  final String? error;

  /// Atajos de importe, en enteros («50», «100»). Rellenan el campo; no cambian nada más.
  final List<int> sugeridos;

  @override
  State<CampoMontoGrande> createState() => _CampoMontoGrandeState();
}

class _CampoMontoGrandeState extends State<CampoMontoGrande> {
  final _controlador = TextEditingController();

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  void _escribir(String valor) {
    _controlador.text = valor;
    _controlador.selection = TextSelection.collapsed(offset: valor.length);
    widget.onChanged(CampoMonto.aCadenaDelContrato(valor));
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final simbolo = widget.moneda == 'BOB' ? 'Bs' : widget.moneda;
    final cifra = Tipo.cifraGrande.copyWith(color: t.text);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          widget.etiqueta,
          textAlign: TextAlign.center,
          style: Tipo.cuerpo.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s2),
        Semantics(
          label: widget.etiqueta,
          textField: true,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              ExcludeSemantics(
                child: Text(simbolo, style: cifra.copyWith(color: t.text3)),
              ),
              const SizedBox(width: Espacio.s2),
              Flexible(
                child: IntrinsicWidth(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      minWidth: Espacio.s7 + Espacio.s6,
                    ),
                    child: TextField(
                      controller: _controlador,
                      textAlign: TextAlign.center,
                      style: cifra,
                      cursorColor: t.brandTexto,
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
                      ],
                      onChanged: (v) {
                        widget.onChanged(CampoMonto.aCadenaDelContrato(v));
                        setState(() {});
                      },
                      decoration: InputDecoration(
                        isDense: true,
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        errorBorder: InputBorder.none,
                        hintText: '0,00',
                        hintStyle: cifra.copyWith(color: t.text3),
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        // La línea que dice «acá se escribe»: sin ella la cifra parece un título.
        Center(
          child: SizedBox(
            width: Espacio.s7 * 3,
            child: Divider(
              height: Espacio.s3,
              thickness: widget.error == null
                  ? Borde.fino
                  : Borde.foco - Borde.fino,
              color: widget.error == null ? t.border : t.err,
            ),
          ),
        ),
        const SizedBox(height: Espacio.s1),
        Text(
          widget.error ??
              widget.ayuda ??
              'Con coma para los centavos: 1.240,50',
          textAlign: TextAlign.center,
          style: Tipo.cuerpoChico.copyWith(
            color: widget.error == null ? t.text3 : t.errTexto,
          ),
        ),
        if (widget.sugeridos.isNotEmpty) ...[
          const SizedBox(height: Espacio.s4),
          Wrap(
            alignment: WrapAlignment.center,
            spacing: Espacio.s2,
            runSpacing: Espacio.s2,
            children: [
              for (final monto in widget.sugeridos)
                ChipElegible(
                  texto: '$simbolo $monto',
                  seleccionado:
                      CampoMonto.aCadenaDelContrato(_controlador.text) ==
                      '$monto.00',
                  onTap: () => _escribir('$monto'),
                ),
            ],
          ),
        ],
      ],
    );
  }
}
