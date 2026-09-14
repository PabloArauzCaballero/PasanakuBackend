import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La caja de un campo: borde, relleno, adorno, ayuda y error.
///
/// Vive aparte porque la usan dos átomos —el campo de texto y el de selección— y dos
/// copias de la misma decoración divergen al primer retoque: un borde que cambia en
/// uno y no en el otro deja un formulario con dos cajas que no son la misma caja.
abstract final class DecoracionDeCampo {
  static InputDecoration de(
    BuildContext context, {
    String? ayuda,
    String? error,
    bool exito = false,
    bool habilitado = true,
    IconData? icono,
    String? prefijo,
    Widget? sufijo,
  }) {
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
    return InputDecoration(
      filled: true,
      fillColor: habilitado ? t.field : t.surface2,
      // El prefijo va DENTRO del adorno, no en `prefixText`: Flutter esconde
      // `prefixText` hasta que el campo tiene foco, y un `+591` que aparece recién al
      // tocar no le sirve a nadie — quien no lo ve lo escribe a mano y termina con
      // `+591+59171000090`.
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
                      prefijo,
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
      // Dos renglones de ayuda y tres de error: en una fila de dos campos, uno que
      // crece y el otro no desalinea las cajas.
      helperMaxLines: 2,
      errorMaxLines: 3,
    );
  }
}
