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
    this.soloLectura = false,
    this.onTap,
    this.foco,
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

  /// El valor no se escribe: se elige. Un campo de fecha se ve como cualquier otro
  /// —misma caja, mismo borde, mismo error— pero abre un calendario en vez del
  /// teclado. Deshabilitarlo lo pintaria de gris, que es mentir: el campo funciona.
  final bool soloLectura;
  final VoidCallback? onTap;

  /// Para que el formulario pueda llevar el foco al primer campo con error.
  final FocusNode? foco;

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
          focusNode: foco,
          readOnly: soloLectura,
          onTap: onTap,
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
            // El prefijo va DENTRO del adorno, no en `prefixText`: Flutter esconde
            // `prefixText` hasta que el campo tiene foco, y un `+591` que aparece
            // recien al tocar no le sirve a nadie — quien no lo ve lo escribe a mano
            // y termina con `+591+59171000090`.
            prefixIcon: (icono == null && prefijo == null)
                ? null
                : Padding(
                    padding: const EdgeInsets.only(
                      left: Espacio.s4,
                      right: Espacio.s2,
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (icono != null) Icon(icono, color: t.text3),
                        if (prefijo != null) ...[
                          if (icono != null) const SizedBox(width: Espacio.s2),
                          Text(
                            prefijo!,
                            style: texto.bodyLarge?.copyWith(
                              color: t.text2,
                              fontFamily: Fuente.display,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
            prefixIconConstraints: const BoxConstraints(minWidth: 0),
            suffixIcon: sufijo,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: Espacio.s4,
              vertical: Espacio.s4,
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
