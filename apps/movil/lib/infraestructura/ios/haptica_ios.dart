import 'package:flutter/services.dart';

import '../../dominio/puertos/haptica.dart';

/// `HapticFeedback` es del SDK de Flutter: la misma implementación sirve en iOS.
class HapticaIos implements Haptica {
  @override
  Future<void> exito() => HapticFeedback.mediumImpact();

  @override
  Future<void> error() => HapticFeedback.vibrate();

  @override
  Future<void> toqueLigero() => HapticFeedback.selectionClick();
}
