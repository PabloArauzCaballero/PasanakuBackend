import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../atomos/campo.dart';
import '../dinero/formatear.dart';
import '../dinero/interpretar.dart';

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
    this.ejemplo,
    this.valorInicial,
  });

  final String etiqueta;

  /// Recibe la cadena del contrato (`^\d+\.\d{2}$`) o `null` si lo escrito no es un importe.
  final ValueChanged<String?> onChanged;
  final String moneda;
  final String? error;
  final String? ayuda;
  final String? ejemplo;
  final String? valorInicial;

  /// `"1.240,50"` → `"1240.50"`; `"12.50"` → `"12.50"`; `"12"` → `"12.00"`;
  /// `"abc"` → `null`. La regla es [interpretarMontoEscrito] (F1.S4.M1).
  static String? aCadenaDelContrato(String escrito) =>
      interpretarMontoEscrito(escrito);

  /// Lo que se muestra debajo del campo según lo escrito: el importe que se va a enviar,
  /// o el error de formato asociado al campo (regla 95.3.1). `null` si no hay nada escrito.
  static ({String? ayuda, String? error}) lecturaDe(
    String escrito,
    String moneda,
  ) {
    if (escrito.trim().isEmpty) {
      return (ayuda: null, error: null);
    }
    final contrato = aCadenaDelContrato(escrito);
    if (contrato == null) {
      return (ayuda: null, error: 'Escribí un monto como 1.240,50');
    }
    return (
      ayuda: 'Vas a enviar ${formatearMonto(monto: contrato, moneda: moneda)}',
      error: null,
    );
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
    final lectura = CampoMonto.lecturaDe(_controlador.text, widget.moneda);
    return Campo(
      etiqueta: widget.etiqueta,
      controlador: _controlador,
      prefijo: widget.moneda == 'BOB' ? 'Bs' : widget.moneda,
      tipoDeTeclado: const TextInputType.numberWithOptions(decimal: true),
      formateadores: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
      error: widget.error ?? lectura.error,
      ayuda:
          lectura.ayuda ??
          widget.ayuda ??
          'Con coma para los centavos: 1.240,50',
      ejemplo: widget.ejemplo,
      onChanged: (v) {
        widget.onChanged(CampoMonto.aCadenaDelContrato(v));
        setState(() {});
      },
    );
  }
}
