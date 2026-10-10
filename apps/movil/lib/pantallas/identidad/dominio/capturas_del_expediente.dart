import 'package:flutter/foundation.dart';

/// Las cinco caras del expediente (CU-02), en el orden en que Atlas las pide y que
/// replicamos acá: primero el carnet, después la prueba de vida de frente y de
/// perfil. El valor de cada una es el que entiende `CaraDelExpediente` del contrato.
enum CaraDelCarril {
  anverso('ANVERSO', esDocumento: true),
  reverso('REVERSO', esDocumento: true),
  selfie('SELFIE', esDocumento: false),
  perfilIzquierdo('PERFIL_IZQUIERDO', esDocumento: false),
  perfilDerecho('PERFIL_DERECHO', esDocumento: false);

  const CaraDelCarril(this.valorApi, {required this.esDocumento});

  /// El valor exacto que el contrato espera. No es `name.toUpperCase()`: los
  /// nombres compuestos (`perfilIzquierdo`) perderían el guion bajo.
  final String valorApi;

  /// Las dos primeras usan la cámara trasera y el marco del carnet; las otras tres,
  /// la delantera y el marco de rostro.
  final bool esDocumento;
}

/// Una captura aceptada: la ruta local, su peso y un adelanto del hash para
/// mostrarlo en el carrusel («12 KB · SHA-256 9f2c1e4a3b7d…»), igual que Atlas.
@immutable
class Captura {
  const Captura({
    required this.ruta,
    required this.bytes,
    required this.ancho,
    required this.alto,
    required this.sha256Corto,
  });

  final String ruta;
  final int bytes;
  final int ancho;
  final int alto;
  final String sha256Corto;

  String get kilobytes => '${(bytes / 1024).round()} KB';
}

/// Las cinco capturas del expediente, inmutable como el resto del estado del alta.
@immutable
class CapturasDelExpediente {
  const CapturasDelExpediente({this.porCara = const {}});

  final Map<CaraDelCarril, Captura> porCara;

  static const orden = CaraDelCarril.values;

  bool tiene(CaraDelCarril cara) => porCara.containsKey(cara);

  bool get completo => orden.every(tiene);

  /// Para el carrusel: a qué cara saltar apenas se acepta una foto. `null` cuando
  /// ya están las cinco — ahí el carrusel se queda donde está.
  CaraDelCarril? get siguientePendiente {
    for (final cara in orden) {
      if (!tiene(cara)) return cara;
    }
    return null;
  }

  /// El motivo que se muestra bajo el botón «Enviar documento» deshabilitado —
  /// nombra la primera que falta, como hace Atlas («Falta la selfie de tu perfil
  /// izquierdo.»), no un genérico «faltan fotos».
  String? get primeraFaltante {
    final falta = siguientePendiente;
    if (falta == null) return null;
    return switch (falta) {
      CaraDelCarril.anverso => 'Falta el anverso de tu carnet.',
      CaraDelCarril.reverso => 'Falta el reverso de tu carnet.',
      CaraDelCarril.selfie => 'Falta tu selfie de frente.',
      CaraDelCarril.perfilIzquierdo =>
        'Falta la selfie de tu perfil izquierdo.',
      CaraDelCarril.perfilDerecho => 'Falta la selfie de tu perfil derecho.',
    };
  }

  CapturasDelExpediente conCaptura(CaraDelCarril cara, Captura captura) =>
      CapturasDelExpediente(porCara: {...porCara, cara: captura});

  CapturasDelExpediente sinCaptura(CaraDelCarril cara) =>
      CapturasDelExpediente(porCara: {...porCara}..remove(cara));
}

/// El título de cada paso, igual que Atlas: «Anverso del carnet», «Selfie de
/// frente», «Selfie: perfil izquierdo»…
String tituloDeCaptura(CaraDelCarril cara) => switch (cara) {
  CaraDelCarril.anverso => 'Anverso del carnet',
  CaraDelCarril.reverso => 'Reverso del carnet',
  CaraDelCarril.selfie => 'Selfie de frente',
  CaraDelCarril.perfilIzquierdo => 'Selfie: perfil izquierdo',
  CaraDelCarril.perfilDerecho => 'Selfie: perfil derecho',
};

/// La pista que se muestra arriba del visor mientras se toma la foto. Para los
/// perfiles es la misma de Atlas: gira la cabeza, no el teléfono.
String pistaDeCaptura(CaraDelCarril cara) => switch (cara) {
  CaraDelCarril.anverso =>
    'Apoyá el carnet en una mesa lisa y oscura, con los cuatro bordes a la vista.',
  CaraDelCarril.reverso =>
    'Dale vuelta al carnet y repetí: los cuatro bordes a la vista, sin reflejos.',
  CaraDelCarril.selfie => 'Mirá de frente a la cámara, con el rostro centrado.',
  CaraDelCarril.perfilIzquierdo =>
    'Gira la cabeza hacia tu IZQUIERDA hasta que se vea tu oreja derecha.',
  CaraDelCarril.perfilDerecho =>
    'Gira la cabeza hacia tu DERECHA hasta que se vea tu oreja izquierda.',
};
