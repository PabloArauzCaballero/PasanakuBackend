import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Claro / oscuro / sistema. `app.dart` ya arma los dos `ThemeData` desde
/// `Tokens.claro` y `Tokens.oscuro`; este proveedor solo guarda la preferencia de la
/// persona (Cuenta → Apariencia, fuera de alcance de F2) — hoy siempre `system`.
final temaProvider = NotifierProvider<TemaNotifier, ThemeMode>(
  TemaNotifier.new,
);

class TemaNotifier extends Notifier<ThemeMode> {
  @override
  ThemeMode build() => ThemeMode.system;

  void cambiar(ThemeMode modo) => state = modo;
}
