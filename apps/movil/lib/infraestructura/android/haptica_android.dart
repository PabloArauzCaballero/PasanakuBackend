import 'package:flutter/services.dart';

import '../../dominio/puertos/haptica.dart';

/// `HapticFeedback` es parte del SDK de Flutter: no agrega dependencia nueva.
/// Real en Android e iOS por igual (no hay pase pendiente para este puerto).
class HapticaAndroid implements Haptica {
  @override
  Future<void> exito() => HapticFeedback.mediumImpact();

  @override
  Future<void> error() => HapticFeedback.vibrate();

  @override
  Future<void> toqueLigero() => HapticFeedback.selectionClick();
}
