import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'parte_de_fecha.dart';

/// Una fecha en **tres selects: Día, Mes y Año**, en ese orden, como se dice.
///
/// Antes abría el calendario de Material, cuyo lápiz pedía escribir `mm/dd/yyyy`
/// —mes primero, barras—, un formato que nadie usa acá. El calendario queda como
/// atajo, **solo calendario**. Día y año se eligen con buscador (más de doce
/// opciones, la disciplina de `CampoDeSeleccion`): se escribe «1987» y listo.
///
/// Emite la fecha recién cuando las tres partes forman un día que existe; si falta
/// una, o se armó un 31 de febrero, emite `null` y dice qué pasa.
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
  final ValueChanged<DateTime?> onElegida;

  final DateTime? primera;
  final DateTime? ultima;

  /// Dónde se para el calendario cuando todavía no hay valor.
  final DateTime? inicial;

  final String? ayuda, error;
  final IconData icono;

  /// Va al select del día, el primero que se completa.
  final FocusNode? foco;

  @override
  State<CampoDeFecha> createState() => _CampoDeFechaState();
}

class _CampoDeFechaState extends State<CampoDeFecha> {
  int? _dia;
  int? _mes;
  int? _anio;

  @override
  void initState() {
    super.initState();
    _copiar(widget.valor);
  }

  @override
  void didUpdateWidget(CampoDeFecha anterior) {
    super.didUpdateWidget(anterior);
    if (widget.valor != null && anterior.valor != widget.valor) {
      _copiar(widget.valor);
    }
  }

  void _copiar(DateTime? f) {
    if (f == null) return;
    _dia = f.day;
    _mes = f.month;
    _anio = f.year;
  }

  DateTime get _primera =>
      widget.primera ?? DateTime(DateTime.now().year - 120);
  DateTime get _ultima => widget.ultima ?? DateTime.now();

  /// El problema de la combinación elegida, o `null` si no hay ninguno (o falta algo).
  String? get _problema {
    final d = _dia, m = _mes, a = _anio;
    if (d == null || m == null || a == null) return null;
    final dias = diasDelMes(a, m);
    if (d > dias) {
      return 'Esa fecha no existe: ${mesesDelAnio[m - 1].toLowerCase()} '
          'de $a tiene $dias días.';
    }
    return null;
  }

  void _cambiar(void Function() asignar) {
    setState(asignar);
    final d = _dia, m = _mes, a = _anio;
    final completa = d != null && m != null && a != null && _problema == null;
    widget.onElegida(completa ? DateTime(a, m, d) : null);
  }

  Future<void> _abrirCalendario() async {
    final elegida = await showDatePicker(
      context: context,
      initialDate: widget.valor ?? widget.inicial ?? _ultima,
      firstDate: _primera,
      lastDate: _ultima,
      initialDatePickerMode: DatePickerMode.year,
      // Sin el lápiz: el modo de texto pedía `mm/dd/yyyy`, que nadie entiende.
      initialEntryMode: DatePickerEntryMode.calendarOnly,
      helpText: widget.etiqueta,
      cancelText: 'Cancelar',
      confirmText: 'Listo',
    );
    if (elegida != null) _cambiar(() => _copiar(elegida));
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final problema = _problema;
    final mensaje = problema ?? widget.error;
    // El borde rojo en las tres cajas; el mensaje, una sola vez debajo.
    final marca = mensaje == null ? null : '';
    final completa = widget.valor != null && mensaje == null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(widget.icono, size: 18, color: t.text3),
            const SizedBox(width: Espacio.s1),
            Expanded(
              child: Text(
                widget.etiqueta,
                style: texto.labelLarge?.copyWith(color: t.text2),
              ),
            ),
            IconButton(
              onPressed: _abrirCalendario,
              icon: Icon(Icons.calendar_month_outlined, color: t.brandTexto),
              tooltip: 'Elegir en el calendario',
            ),
          ],
        ),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ParteDeFecha(
              vacio: 'Día',
              flex: 5,
              valor: _dia,
              marca: marca,
              completa: completa,
              foco: widget.foco,
              opciones: [for (var d = 1; d <= 31; d++) (valor: d, texto: '$d')],
              onElegida: (d) => _cambiar(() => _dia = d),
            ),
            const SizedBox(width: Espacio.s2),
            ParteDeFecha(
              vacio: 'Mes',
              flex: 8,
              valor: _mes,
              marca: marca,
              completa: completa,
              numerico: false,
              opciones: [
                for (var m = 1; m <= 12; m++)
                  (valor: m, texto: mesesDelAnio[m - 1]),
              ],
              onElegida: (m) => _cambiar(() => _mes = m),
            ),
            const SizedBox(width: Espacio.s2),
            // Del más cercano al más lejano: el que se busca suele estar arriba.
            ParteDeFecha(
              vacio: 'Año',
              flex: 6,
              valor: _anio,
              marca: marca,
              completa: completa,
              opciones: [
                for (var a = _ultima.year; a >= _primera.year; a--)
                  (valor: a, texto: '$a'),
              ],
              onElegida: (a) => _cambiar(() => _anio = a),
            ),
          ],
        ),
        if (mensaje != null || widget.ayuda != null)
          Padding(
            padding: const EdgeInsets.only(top: Espacio.s1, left: Espacio.s3),
            child: Text(
              mensaje ?? widget.ayuda!,
              style: Tipo.ayuda.copyWith(
                color: mensaje != null ? t.errTexto : t.text3,
              ),
            ),
          ),
      ],
    );
  }
}
