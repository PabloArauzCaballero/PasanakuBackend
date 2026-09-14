import 'package:flutter/material.dart';

import '../atomos/campo.dart';
import '../atomos/fecha.dart';
import '../tokens/tokens.dart';

/// Un campo de fecha que **se ve como los demás campos** y abre un calendario.
///
/// Antes esto era un `OutlinedButton` suelto en medio del formulario: no parecía un
/// campo, no tenía etiqueta, no podía mostrar un error y rompía la columna de cajas
/// iguales. Un dato que se pide es un campo, aunque se elija en vez de escribirse.
///
/// El calendario abre en la **grilla de años**, no en el mes de hoy: para una fecha de
/// nacimiento, empezar en el mes actual obliga a retroceder trescientas veces.
class CampoDeFecha extends StatefulWidget {
  const CampoDeFecha({
    super.key,
    required this.etiqueta,
    required this.valor,
    required this.onElegida,
    this.primera,
    this.ultima,
    this.inicial,
    this.ayuda,
    this.error,
    this.icono = Icons.cake_outlined,
    this.foco,
  });

  final String etiqueta;
  final DateTime? valor;
  final ValueChanged<DateTime> onElegida;

  final DateTime? primera;
  final DateTime? ultima;

  /// Dónde se para el calendario cuando todavía no hay valor.
  final DateTime? inicial;

  final String? ayuda;
  final String? error;
  final IconData icono;
  final FocusNode? foco;

  @override
  State<CampoDeFecha> createState() => _CampoDeFechaState();
}

class _CampoDeFechaState extends State<CampoDeFecha> {
  final _controlador = TextEditingController();

  static String _texto(DateTime f) =>
      Fecha.formatear(f.toIso8601String(), conHora: false);

  @override
  void initState() {
    super.initState();
    _escribir();
  }

  @override
  void didUpdateWidget(CampoDeFecha anterior) {
    super.didUpdateWidget(anterior);
    if (anterior.valor != widget.valor) _escribir();
  }

  void _escribir() =>
      _controlador.text = widget.valor == null ? '' : _texto(widget.valor!);

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  Future<void> _abrir() async {
    final hoy = DateTime.now();
    final ultima = widget.ultima ?? hoy;
    final elegida = await showDatePicker(
      context: context,
      initialDate: widget.valor ?? widget.inicial ?? ultima,
      firstDate: widget.primera ?? DateTime(hoy.year - 120),
      lastDate: ultima,
      initialDatePickerMode: DatePickerMode.year,
      helpText: widget.etiqueta,
      cancelText: 'Cancelar',
      confirmText: 'Listo',
      fieldLabelText: widget.etiqueta,
    );
    if (elegida != null) widget.onElegida(elegida);
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Campo(
      etiqueta: widget.etiqueta,
      soloLectura: true,
      foco: widget.foco,
      onTap: _abrir,
      controlador: _controlador,
      icono: widget.icono,
      ayuda: widget.ayuda,
      error: widget.error,
      exito: widget.valor != null && widget.error == null,
      sufijo: IconButton(
        onPressed: _abrir,
        icon: Icon(Icons.calendar_month_outlined, color: t.brandTexto),
        tooltip: widget.etiqueta,
      ),
    );
  }
}
