import 'dart:ui' show Rect;

import 'modelo.dart';

/// Lo que se ve de un recorrido en curso. Es inmutable: el motor publica estados
/// nuevos, nadie muta el de adentro.
class EstadoDelMotor {
  const EstadoDelMotor({
    this.tutorial,
    this.indice = 0,
    this.recuadro,
    this.problema,
    this.accionCumplida = false,
    this.confirmandoSalida = false,
  });

  /// `null` es «no hay ningún tutorial corriendo», que es el estado normal.
  final TutorialDefinicion? tutorial;
  final int indice;

  /// Dónde está el elemento resaltado. `null` es «todavía no» o «no está».
  final Rect? recuadro;

  /// Lo que impide seguir, en palabras. Se muestra en el globo, con reintento.
  final String? problema;
  final bool accionCumplida;
  final bool confirmandoSalida;

  bool get activo => tutorial != null;
  PasoDeTutorial? get paso =>
      tutorial == null || indice >= tutorial!.pasos.length
      ? null
      : tutorial!.pasos[indice];
  int get total => tutorial?.pasos.length ?? 0;
  bool get esUltimo => total > 0 && indice >= total - 1;

  /// Se puede avanzar si el paso no pedía nada o si ya se hizo lo que pedía.
  bool get puedeAvanzar =>
      paso == null ||
      paso!.accion.tipo == TipoDeAccion.ninguna ||
      accionCumplida;

  static const apagado = EstadoDelMotor();

  EstadoDelMotor copiaCon({
    TutorialDefinicion? tutorial,
    int? indice,
    Rect? recuadro,
    String? problema,
    bool? accionCumplida,
    bool? confirmandoSalida,
    bool limpiarRecuadro = false,
    bool limpiarProblema = false,
  }) => EstadoDelMotor(
    tutorial: tutorial ?? this.tutorial,
    indice: indice ?? this.indice,
    recuadro: limpiarRecuadro ? null : (recuadro ?? this.recuadro),
    problema: limpiarProblema ? null : (problema ?? this.problema),
    accionCumplida: accionCumplida ?? this.accionCumplida,
    confirmandoSalida: confirmandoSalida ?? this.confirmandoSalida,
  );
}
