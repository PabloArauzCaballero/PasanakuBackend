import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/tutoriales.dart';
import 'avance.dart';
import 'modelo.dart';

/// **Quien escribe el avance, para que el motor no se ocupe de eso.**
///
/// Guardar no puede demorar el paso siguiente: si el almacén falla, el tutorial sigue y
/// lo único que se pierde es poder retomarlo después. Por eso hay dos formas de guardar
/// —esperando y sin esperar— y ninguna lanza hacia el recorrido.
class EscribanoDeProgreso {
  const EscribanoDeProgreso(this._ref);

  final Ref _ref;

  /// La fila de este tutorial, o una recién nacida si todavía no hay ninguna. Un
  /// guardado anterior que falló no puede hacer que terminar un tutorial no cuente.
  ProgresoDeTutorial de(TutorialDefinicion tutorial, DateTime ahora) =>
      _ref.read(progresoDeTutorialesProvider)[tutorial.id] ??
      progresoInicial(tutorial, ahora);

  Future<void> guardar(ProgresoDeTutorial progreso) =>
      _ref.read(progresoDeTutorialesProvider.notifier).guardar(progreso);

  void sinEsperar(ProgresoDeTutorial progreso) => guardar(progreso).ignore();
}
