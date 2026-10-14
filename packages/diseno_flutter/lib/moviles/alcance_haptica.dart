import 'package:flutter/widgets.dart';

/// Acciones táctiles semánticas que puede emitir la aplicación.
abstract interface class HapticaDeInterfaz {
  Future<void> seleccion();
  Future<void> botonPrimario();
  Future<void> avisoDeCredencialesIncorrectas();
  Future<void> exito();
  Future<void> errorQueBloqueaDinero();
}

/// Lleva la política de hápticos de la app a los componentes compartidos.
class AlcanceHaptica extends InheritedWidget {
  const AlcanceHaptica({
    super.key,
    required this.haptica,
    required super.child,
  });

  final HapticaDeInterfaz haptica;

  static HapticaDeInterfaz? maybeOf(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<AlcanceHaptica>()?.haptica;

  @override
  bool updateShouldNotify(AlcanceHaptica oldWidget) =>
      !identical(haptica, oldWidget.haptica);
}
