import 'package:flutter/material.dart';

import 'tokens/tokens.dart';

/// Lo que Material dibuja como superficie —tarjeta, hoja, diálogo, aviso, cabecera—,
/// con el lenguaje del rediseño bancario (ADR-053). Separado de `tema.dart` por el
/// barrido de 200 líneas.

/// Tarjeta de Material: sin borde en claro (la separa la sombra de `Tarjeta`); en
/// oscuro la sombra no se ve, así que la separa un borde apenas más claro que el fondo.
CardThemeData tarjetaDesde(Tokens t, Brightness brillo) => CardThemeData(
  color: t.surface,
  surfaceTintColor: Colors.transparent,
  elevation: 0,
  margin: EdgeInsets.zero,
  shape: RoundedRectangleBorder(
    borderRadius: BorderRadius.circular(Radios.xl),
    side: brillo == Brightness.dark
        ? BorderSide(color: t.border, width: Borde.fino)
        : BorderSide.none,
  ),
);

/// Cabecera de fábrica: transparente sobre el fondo y con el título en Bricolage. Las
/// pantallas nuevas usan `Cabecera`; esto cubre las que todavía piden `AppBar`.
AppBarTheme cabeceraDesde(Tokens t) => AppBarTheme(
  backgroundColor: t.bg,
  surfaceTintColor: Colors.transparent,
  foregroundColor: t.text,
  elevation: 0,
  scrolledUnderElevation: 0,
  centerTitle: false,
  toolbarHeight: 64,
  titleTextStyle: Tipo.titulo2.copyWith(color: t.text),
);

/// Hoja inferior: radio grande, agarradera visible y sombra de superpuesta.
BottomSheetThemeData hojaDesde(Tokens t) => BottomSheetThemeData(
  backgroundColor: t.surface,
  surfaceTintColor: Colors.transparent,
  showDragHandle: true,
  dragHandleColor: t.fieldBorder,
  dragHandleSize: const Size(40, 5),
  shape: const RoundedRectangleBorder(
    borderRadius: BorderRadius.vertical(top: Radius.circular(Radios.xxl)),
  ),
);

DialogThemeData dialogoDesde(Tokens t) => DialogThemeData(
  backgroundColor: t.surface,
  surfaceTintColor: Colors.transparent,
  titleTextStyle: Tipo.titulo2.copyWith(color: t.text),
  contentTextStyle: Tipo.cuerpo.copyWith(color: t.text2),
  shape: RoundedRectangleBorder(
    borderRadius: BorderRadius.circular(Radios.xxl),
  ),
);

/// Aviso flotante: tinta sobre fondo, como en la bóveda, con radio de tarjeta.
SnackBarThemeData avisoDesde(Tokens t) => SnackBarThemeData(
  behavior: SnackBarBehavior.floating,
  backgroundColor: t.text,
  contentTextStyle: Tipo.cuerpo.copyWith(color: t.bg),
  actionTextColor: t.bg,
  insetPadding: const EdgeInsets.all(Espacio.s4),
  elevation: 0,
  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Radios.lg)),
);
