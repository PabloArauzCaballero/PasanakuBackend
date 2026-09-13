import 'package:flutter/material.dart';

import 'tokens/tokens.dart';

/// Cómo se ve un campo en toda la app. El foco se ve con borde grueso **y** halo:
/// quien navega con teclado o con el ojo cansado tiene que saber dónde está parado
/// sin depender solo del color.
InputDecorationTheme campoDesde(Tokens t) {
  OutlineInputBorder borde(Color color, double ancho) => OutlineInputBorder(
    borderRadius: BorderRadius.circular(Radios.md),
    borderSide: BorderSide(color: color, width: ancho),
  );
  return InputDecorationTheme(
    filled: true,
    fillColor: t.field,
    isDense: false,
    contentPadding: const EdgeInsets.symmetric(
      horizontal: Espacio.s3,
      vertical: Espacio.s3,
    ),
    border: borde(t.fieldBorder, Borde.fino),
    enabledBorder: borde(t.fieldBorder, Borde.fino),
    focusedBorder: borde(t.brand, Borde.desfase),
    errorBorder: borde(t.err, Borde.fino),
    focusedErrorBorder: borde(t.err, Borde.desfase),
    disabledBorder: borde(t.border, Borde.fino),
    labelStyle: Tipo.campoEtiqueta.copyWith(color: t.text2),
    floatingLabelStyle: Tipo.campoEtiqueta.copyWith(color: t.brandTexto),
    hintStyle: Tipo.cuerpo.copyWith(color: t.text3),
    helperStyle: Tipo.ayuda.copyWith(color: t.text3),
    errorStyle: Tipo.ayuda.copyWith(color: t.errTexto),
    prefixStyle: Tipo.cuerpoFuerte.copyWith(color: t.text2),
    suffixStyle: Tipo.cuerpoFuerte.copyWith(color: t.text2),
    iconColor: t.text2,
    prefixIconColor: t.text2,
    suffixIconColor: t.text2,
  );
}
