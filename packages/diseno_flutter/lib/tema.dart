import 'package:flutter/material.dart';

import 'tokens/tokens.dart';

/// Los dos temas se construyen desde `tokens.dart` (generado). Ningún `Colors.*`.
/// La app los usa tal cual: `theme: temaDesde(Tokens.claro, Brightness.light)`.
ThemeData temaDesde(Tokens t, Brightness brillo) => ThemeData(
  useMaterial3: true,
  brightness: brillo,
  scaffoldBackgroundColor: t.bg,
  colorScheme: ColorScheme(
    brightness: brillo,
    primary: t.brand,
    onPrimary: t.sobreVerdeSolido,
    secondary: t.accent,
    onSecondary: t.accentInk,
    error: t.err,
    onError: t.sobreRojoSolido,
    surface: t.surface,
    onSurface: t.text,
  ),
  fontFamily: Fuente.cuerpo,
  appBarTheme: AppBarTheme(
    backgroundColor: t.bg,
    foregroundColor: t.text,
    elevation: 0,
  ),
  dividerColor: t.border,
  extensions: [t],
);
