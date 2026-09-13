import 'package:flutter/cupertino.dart' show CupertinoPageTransitionsBuilder;
import 'package:flutter/material.dart';

import 'tokens/tokens.dart';
import 'tema_de_campos.dart';
import 'tema_de_controles.dart';

/// Los dos temas se construyen desde `tokens.dart` (generado). Ningún `Colors.*`.
/// La app los usa tal cual: `theme: temaDesde(Tokens.claro, Brightness.light)`.
///
/// Acá se fija **todo** lo que Material dibujaría de fábrica —campos, radios, hojas,
/// avisos, transiciones—, porque una pantalla que tiene que decidir cómo se ve un
/// campo termina decidiendo distinto que la de al lado.
ThemeData temaDesde(Tokens t, Brightness brillo) {
  final base = ThemeData(
    useMaterial3: true,
    brightness: brillo,
    scaffoldBackgroundColor: t.bg,
    canvasColor: t.bg,
    colorScheme: ColorScheme(
      brightness: brillo,
      primary: t.brand,
      onPrimary: t.sobreVerdeSolido,
      primaryContainer: t.brandBg,
      onPrimaryContainer: t.brandInk,
      secondary: t.accent,
      onSecondary: t.accentInk,
      error: t.err,
      onError: t.sobreRojoSolido,
      errorContainer: t.errBg,
      onErrorContainer: t.errTexto,
      surface: t.surface,
      onSurface: t.text,
      surfaceContainerHighest: t.surface2,
      onSurfaceVariant: t.text2,
      outline: t.border,
      outlineVariant: t.fieldBorder,
    ),
    fontFamily: Fuente.cuerpo,
    textTheme: _textoDesde(t),
    dividerColor: t.border,
    dividerTheme: DividerThemeData(color: t.border, space: 1, thickness: 1),
    splashFactory: InkSparkle.splashFactory,
    extensions: [t],
  );
  return base.copyWith(
    appBarTheme: AppBarTheme(
      backgroundColor: t.bg,
      surfaceTintColor: Colors.transparent,
      foregroundColor: t.text,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      titleTextStyle: Tipo.titulo2.copyWith(color: t.text),
    ),
    pageTransitionsTheme: const PageTransitionsTheme(
      builders: {
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        TargetPlatform.macOS: CupertinoPageTransitionsBuilder(),
        TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
      },
    ),
    inputDecorationTheme: campoDesde(t),
    checkboxTheme: casillaDesde(t),
    radioTheme: radioDesde(t),
    switchTheme: interruptorDesde(t),
    sliderTheme: deslizadorDesde(t),
    elevatedButtonTheme: ElevatedButtonThemeData(style: botonRellenoDesde(t)),
    filledButtonTheme: FilledButtonThemeData(style: botonRellenoDesde(t)),
    outlinedButtonTheme: OutlinedButtonThemeData(style: botonBordeDesde(t)),
    textButtonTheme: TextButtonThemeData(style: botonTextoDesde(t)),
    cardTheme: CardThemeData(
      color: t.surface,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Radios.lg),
        side: BorderSide(color: t.border, width: Borde.fino),
      ),
    ),
    listTileTheme: ListTileThemeData(
      iconColor: t.text2,
      textColor: t.text,
      titleTextStyle: Tipo.cuerpoFuerte.copyWith(color: t.text),
      subtitleTextStyle: Tipo.cuerpoChico.copyWith(color: t.text2),
      minVerticalPadding: Espacio.s2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Radios.md),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: t.text,
      contentTextStyle: Tipo.cuerpo.copyWith(color: t.bg),
      actionTextColor: t.accent,
      insetPadding: const EdgeInsets.all(Espacio.s4),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Radios.md),
      ),
    ),
    bottomSheetTheme: BottomSheetThemeData(
      backgroundColor: t.surface,
      surfaceTintColor: Colors.transparent,
      showDragHandle: true,
      dragHandleColor: t.border,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(Radios.xl)),
      ),
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: t.surface,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: Tipo.titulo2.copyWith(color: t.text),
      contentTextStyle: Tipo.cuerpo.copyWith(color: t.text2),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Radios.xl),
      ),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: t.surface2,
      selectedColor: t.brandBg,
      labelStyle: Tipo.cuerpoChico.copyWith(color: t.text2),
      side: BorderSide(color: t.border, width: Borde.fino),
      shape: const StadiumBorder(),
      padding: const EdgeInsets.symmetric(
        horizontal: Espacio.s2,
        vertical: Espacio.s1,
      ),
    ),
    progressIndicatorTheme: ProgressIndicatorThemeData(
      color: t.brand,
      linearTrackColor: t.surface2,
      circularTrackColor: t.surface2,
    ),
    tooltipTheme: TooltipThemeData(
      textStyle: Tipo.cuerpoChico.copyWith(color: t.bg),
      decoration: BoxDecoration(
        color: t.text,
        borderRadius: BorderRadius.circular(Radios.sm),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: t.surface,
      surfaceTintColor: Colors.transparent,
      indicatorColor: t.brandBg,
      elevation: 0,
      height: 68,
      labelTextStyle: WidgetStateProperty.resolveWith(
        (estados) => Tipo.cuerpoChico.copyWith(
          color: estados.contains(WidgetState.selected) ? t.text : t.text3,
          fontWeight: estados.contains(WidgetState.selected)
              ? FontWeight.w600
              : FontWeight.w400,
        ),
      ),
      iconTheme: WidgetStateProperty.resolveWith(
        (estados) => IconThemeData(
          size: 24,
          color: estados.contains(WidgetState.selected) ? t.brand : t.text3,
        ),
      ),
    ),
  );
}

/// El `TextTheme` de Material mapeado a los roles de la bóveda, para que un widget
/// que ya pide `titleMedium` mejore sin tocarlo y uno nuevo pueda pedir `Tipo.cifra`.
TextTheme _textoDesde(Tokens t) {
  final tinta = t.text;
  return TextTheme(
    displayLarge: Tipo.cifraGrande.copyWith(color: tinta),
    displayMedium: Tipo.cifraGrande.copyWith(color: tinta),
    displaySmall: Tipo.cifra.copyWith(color: tinta),
    headlineLarge: Tipo.titulo1.copyWith(color: tinta),
    headlineMedium: Tipo.titulo1.copyWith(color: tinta),
    headlineSmall: Tipo.titulo2.copyWith(color: tinta),
    titleLarge: Tipo.titulo2.copyWith(color: tinta),
    titleMedium: Tipo.titulo3.copyWith(color: tinta),
    titleSmall: Tipo.cuerpoFuerte.copyWith(color: tinta),
    bodyLarge: Tipo.cuerpo.copyWith(color: tinta),
    bodyMedium: Tipo.cuerpo.copyWith(color: tinta),
    bodySmall: Tipo.cuerpoChico.copyWith(color: t.text2),
    labelLarge: Tipo.boton.copyWith(color: tinta),
    labelMedium: Tipo.campoEtiqueta.copyWith(color: t.text2),
    labelSmall: Tipo.etiqueta.copyWith(color: t.text2),
  );
}
