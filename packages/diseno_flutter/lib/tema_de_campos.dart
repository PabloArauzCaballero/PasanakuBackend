import 'package:flutter/material.dart';

import 'atomos/borde_con_halo.dart';
import 'tokens/tokens.dart';

/// Cómo se ve un campo en toda la app: relleno suave, y el foco con borde grueso **y**
/// halo (`BordeConHalo`): quien navega con teclado o con el ojo cansado tiene que saber
/// dónde está parado sin depender solo del color.
InputDecorationTheme campoDesde(Tokens t) {
  OutlineInputBorder borde(Color color, double ancho) => OutlineInputBorder(
    borderRadius: BorderRadius.circular(Radios.md),
    borderSide: BorderSide(color: color, width: ancho),
  );
  BordeConHalo halo(Color color, Color anillo) => BordeConHalo(
    borderRadius: BorderRadius.circular(Radios.md),
    borderSide: BorderSide(color: color, width: Borde.desfase),
    halo: anillo,
    anchoDeHalo: Borde.foco,
  );
  return InputDecorationTheme(
    filled: true,
    fillColor: t.surface2,
    isDense: false,
    contentPadding: const EdgeInsets.symmetric(
      horizontal: Espacio.s3,
      vertical: Espacio.s3,
    ),
    border: borde(t.fieldBorder, Borde.fino),
    enabledBorder: borde(t.fieldBorder, Borde.fino),
    focusedBorder: halo(t.brand, t.halo),
    errorBorder: borde(t.err, Borde.fino),
    focusedErrorBorder: halo(t.err, t.errBg),
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
