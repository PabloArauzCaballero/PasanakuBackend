import 'package:flutter/services.dart';

import '../dominio/puertos/haptica.dart';

/// Traduce acciones semánticas de producto a la API común de Flutter.
class HapticaFlutter implements Haptica {
  @override
  Future<void> seleccion() => HapticFeedback.selectionClick();

  @override
  Future<void> botonPrimario() => HapticFeedback.lightImpact();

  @override
  Future<void> avisoDeCredencialesIncorrectas() => HapticFeedback.lightImpact();

  @override
  Future<void> exito() => HapticFeedback.mediumImpact();

  @override
  Future<void> errorQueBloqueaDinero() => HapticFeedback.heavyImpact();
}
