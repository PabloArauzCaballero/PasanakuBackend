import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'capturas_del_expediente.dart';

/// Lo que la pantalla de subida necesita después de `POST /usuarios`: el
/// `usuarioId` que devolvió y una copia de las cinco capturas.
@immutable
class AltaEnSeguimiento {
  const AltaEnSeguimiento({required this.usuarioId, required this.capturas});

  final String usuarioId;
  final CapturasDelExpediente capturas;
}

/// Sobrevive a `AltaNotifier.reiniciar()`, que vacía el asistente apenas se crea
/// la cuenta. Las capturas viajan acá y no en `altaProvider`: si la pantalla de
/// subida las leyera de ahí, encontraría el mapa vacío y no subiría ninguna. Vive
/// en su propio provider — igual que `idempotenciaProvider` — por la misma razón:
/// sobrevive a algo que el resto del alta no tiene por qué sobrevivir.
class SeguimientoDelAlta extends Notifier<AltaEnSeguimiento?> {
  @override
  AltaEnSeguimiento? build() => null;

  void fijar(String usuarioId, CapturasDelExpediente capturas) =>
      state = AltaEnSeguimiento(usuarioId: usuarioId, capturas: capturas);

  /// "Repetir la foto" desde la pantalla de subida: la nueva reemplaza a la vieja.
  void reemplazarCaptura(CaraDelCarril cara, Captura captura) {
    final actual = state;
    if (actual == null) return;
    state = AltaEnSeguimiento(
      usuarioId: actual.usuarioId,
      capturas: actual.capturas.conCaptura(cara, captura),
    );
  }

  void limpiar() => state = null;
}

final seguimientoDelAltaProvider =
    NotifierProvider<SeguimientoDelAlta, AltaEnSeguimiento?>(
      SeguimientoDelAlta.new,
    );
