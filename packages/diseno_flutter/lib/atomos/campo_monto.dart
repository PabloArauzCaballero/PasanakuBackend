import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../atomos/campo.dart';

/// Un importe se escribe con addon `Bs`, teclado numérico y coma decimal; el valor que
/// sale es **la cadena del contrato** (`1240.00`), nunca un `double`.
class CampoMonto extends StatefulWidget {
  const CampoMonto({
    super.key,
    required this.etiqueta,
    required this.onChanged,
    this.moneda = 'BOB',
    this.error,
    this.ayuda,
    this.valorInicial,
  });

  final String etiqueta;

  /// Recibe la cadena del contrato (`^\d+\.\d{2}$`) o `null` si lo escrito no es un importe.
  final ValueChanged<String?> onChanged;
  final String moneda;
  final String? error;
  final String? ayuda;
  final String? valorInicial;

  /// `"1.240,50"` → `"1240.50"`; `"12"` → `"12.00"`; `"abc"` → `null`.
  static String? aCadenaDelContrato(String escrito) {
    final limpio = escrito.replaceAll('.', '').replaceAll(' ', '').trim();
    if (limpio.isEmpty) {
      return null;
    }
    final partes = limpio.split(',');
    if (partes.length > 2) {
      return null;
    }
    final enteros = partes[0];
    final centavos = partes.length == 2 ? partes[1] : '';
    if (!RegExp(r'^\d+$').hasMatch(enteros)) {
      return null;
    }
    if (centavos.isNotEmpty && !RegExp(r'^\d{1,2}$').hasMatch(centavos)) {
      return null;
    }
    return '$enteros.${centavos.padRight(2, '0')}';
  }

  @override
  State<CampoMonto> createState() => _CampoMontoState();
}

class _CampoMontoState extends State<CampoMonto> {
  late final TextEditingController _controlador = TextEditingController(
    text: widget.valorInicial,
  );

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Campo(
      etiqueta: widget.etiqueta,
      controlador: _controlador,
      prefijo: widget.moneda == 'BOB' ? 'Bs' : widget.moneda,
      tipoDeTeclado: const TextInputType.numberWithOptions(decimal: true),
      formateadores: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
      error: widget.error,
      ayuda: widget.ayuda ?? 'Con coma para los centavos: 1.240,50',
      onChanged: (v) => widget.onChanged(CampoMonto.aCadenaDelContrato(v)),
    );
  }
}
