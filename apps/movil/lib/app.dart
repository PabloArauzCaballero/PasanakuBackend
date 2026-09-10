import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'navegacion/rutas.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

class AppAportaYa extends StatelessWidget {
  AppAportaYa({super.key, String inicial = '/billetera/inicio'})
    : _enrutador = crearEnrutador(inicial: inicial);

  final GoRouter _enrutador;

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'AportaYa',
    theme: temaDesde(Tokens.claro, Brightness.light),
    darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
    themeMode: ThemeMode.system,
    routerConfig: _enrutador,
    debugShowCheckedModeBanner: false,
  );
}
