import 'dart:io';

import 'package:flutter/foundation.dart';

export 'cara_del_carril.dart';
import 'cara_del_carril.dart';

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

  /// La representación apta para diagnóstico nunca incluye la ruta del archivo.
  @override
  String toString() =>
      'Captura($kilobytes, ${ancho}x$alto, sha256:$sha256Corto)';

  /// Borra el archivo temporal sin propagar errores de plataforma que incluyen
  /// la ruta local en su texto (las rutas de documentos no deben quedar en logs).
  Future<void> eliminarArchivoLocal() => eliminarRutaLocal(ruta);
}

Future<void> eliminarRutaLocal(String ruta) async {
  try {
    final archivo = File(ruta);
    if (await archivo.exists()) await archivo.delete();
  } on FileSystemException {
    // Limpieza de mejor esfuerzo; nunca imprimir la excepción: lleva `ruta`.
  }
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
  /// nombra la primera que falta («Falta la foto de tu perfil izquierdo.»), no un
  /// genérico «faltan fotos».
  String? get primeraFaltante {
    final falta = siguientePendiente;
    if (falta == null) return null;
    return switch (falta) {
      CaraDelCarril.anverso => 'Falta el anverso de tu carnet.',
      CaraDelCarril.reverso => 'Falta el reverso de tu carnet.',
      CaraDelCarril.selfie => 'Falta tu foto de frente.',
      CaraDelCarril.perfilIzquierdo => 'Falta la foto de tu perfil izquierdo.',
      CaraDelCarril.perfilDerecho => 'Falta la foto de tu perfil derecho.',
    };
  }

  CapturasDelExpediente conCaptura(CaraDelCarril cara, Captura captura) =>
      CapturasDelExpediente(porCara: {...porCara, cara: captura});

  /// Reemplaza la cara y elimina su archivo anterior antes de publicar el estado.
  /// Si ambas capturas apuntan al mismo archivo, lo conserva.
  Future<CapturasDelExpediente> reemplazarCaptura(
    CaraDelCarril cara,
    Captura captura,
  ) async {
    final anterior = porCara[cara];
    if (anterior != null && anterior.ruta != captura.ruta) {
      await anterior.eliminarArchivoLocal();
    }
    return conCaptura(cara, captura);
  }

  /// Elimina todas las rutas únicas del expediente. No se registran rutas ni
  /// excepciones de filesystem en logs.
  Future<void> eliminarArchivosLocales() async {
    final rutas = porCara.values.map((captura) => captura.ruta).toSet();
    for (final ruta in rutas) {
      await eliminarRutaLocal(ruta);
    }
  }

  CapturasDelExpediente sinCaptura(CaraDelCarril cara) =>
      CapturasDelExpediente(porCara: {...porCara}..remove(cara));
}

/// El título de cada paso, igual que Atlas: «Anverso del carnet», «Selfie de
/// frente», «Selfie: perfil izquierdo»…
String tituloDeCaptura(CaraDelCarril cara) => switch (cara) {
  CaraDelCarril.anverso => 'Anverso del carnet',
  CaraDelCarril.reverso => 'Reverso del carnet',
  CaraDelCarril.selfie => 'Foto de frente',
  CaraDelCarril.perfilIzquierdo => 'Foto: perfil izquierdo',
  CaraDelCarril.perfilDerecho => 'Foto: perfil derecho',
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
