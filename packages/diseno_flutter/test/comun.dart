import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Monta una pieza con el tema real del sistema, en claro u oscuro.
Widget conTema(
  Widget hijo, {
  Brightness brillo = Brightness.light,
  double escala = 1,
  bool pantallaEntera = false,
}) => MaterialApp(
  theme: temaDesde(Tokens.claro, Brightness.light),
  darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
  themeMode: brillo == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
  debugShowCheckedModeBanner: false,
  builder: (context, child) => MediaQuery(
    data: MediaQuery.of(
      context,
    ).copyWith(textScaler: TextScaler.linear(escala)),
    child: child!,
  ),
  home: pantallaEntera
      ? hijo
      : Scaffold(
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(Espacio.s4),
            child: hijo,
          ),
        ),
);

/// Pantalla de gama baja para goldens deterministas.
void pantallaChica(WidgetTester tester) {
  tester.view.physicalSize = const Size(360, 760);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
}
