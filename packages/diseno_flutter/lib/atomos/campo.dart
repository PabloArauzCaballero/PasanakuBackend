import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../tokens/tokens.dart';

/// Los estados de un campo: normal, foco, error, éxito, deshabilitado. El error va
/// **en el campo**, diciendo cómo corregir, y se anuncia.
class Campo extends StatelessWidget {
  const Campo({
    super.key,
    required this.etiqueta,
    this.controlador,
    this.valorInicial,
    this.ayuda,
    this.error,
    this.exito = false,
    this.habilitado = true,
    this.tipoDeTeclado,
    this.oculto = false,
    this.prefijo,
    this.icono,
    this.sufijo,
    this.lineas = 1,
    this.formateadores,
    this.onChanged,
    this.onSubmitted,
    this.autofocus = false,
  });

  final String etiqueta;
  final TextEditingController? controlador;
  final String? valorInicial;
  final String? ayuda;

  /// El texto de error, en voz de marca: «Revisá el número, le falta un dígito».
  final String? error;
  final bool exito;
  final bool habilitado;
  final TextInputType? tipoDeTeclado;
  final bool oculto;

  /// Un addon fijo antes del valor: `Bs` en un campo de monto.
  final String? prefijo;
  final IconData? icono;
  final Widget? sufijo;
  final int lineas;
  final List<TextInputFormatter>? formateadores;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final colorBorde = error != null
        ? t.err
        : exito
        ? t.ok
        : t.fieldBorder;
    OutlineInputBorder borde(Color color, double ancho) => OutlineInputBorder(
      borderRadius: BorderRadius.circular(Radios.md),
      borderSide: BorderSide(color: color, width: ancho),
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Una etiqueta vacía significa que quien compone ya la puso afuera —el bloque
        // `Pregunta` de un formulario, por ejemplo—. Sin esto, el `Text` vacío deja
        // igual su renglón y abre un hueco entre la pregunta y su campo.
        if (etiqueta.isNotEmpty) ...[
          Text(etiqueta, style: texto.labelLarge?.copyWith(color: t.text2)),
          const SizedBox(height: Espacio.s1),
        ],
        TextField(
          controller: controlador,
          enabled: habilitado,
          obscureText: oculto,
          keyboardType: tipoDeTeclado,
          maxLines: oculto ? 1 : lineas,
          inputFormatters: formateadores,
          onChanged: onChanged,
          onSubmitted: onSubmitted,
          autofocus: autofocus,
          style: texto.bodyLarge?.copyWith(color: t.text),
          decoration: InputDecoration(
            filled: true,
            fillColor: habilitado ? t.field : t.surface2,
            prefixIcon: icono == null ? null : Icon(icono, color: t.text3),
            prefixText: prefijo == null ? null : '$prefijo ',
            prefixStyle: texto.bodyLarge?.copyWith(
              color: t.text2,
              fontFamily: Fuente.display,
            ),
            suffixIcon: sufijo,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: Espacio.s4,
              vertical: Espacio.s3,
            ),
            enabledBorder: borde(colorBorde, Borde.fino),
            focusedBorder: borde(
              error != null ? t.err : t.brand,
              Borde.foco - Borde.fino,
            ),
            disabledBorder: borde(t.border, Borde.fino),
            errorText: error,
            errorStyle: texto.bodySmall?.copyWith(color: t.errTexto),
            helperText: error == null ? ayuda : null,
            helperStyle: texto.bodySmall?.copyWith(color: t.text3),
            errorMaxLines: 3,
          ),
        ),
      ],
    );
  }
}
