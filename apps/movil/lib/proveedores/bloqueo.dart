import 'package:flutter/foundation.dart' show ChangeNotifier, kIsWeb;
import 'package:flutter_riverpod/legacy.dart' show ChangeNotifierProvider;

import '../dominio/vigilante_de_sesion.dart';

/// Estado observable del candado local de la app.
class ControlDeBloqueo extends ChangeNotifier {
  ControlDeBloqueo({bool? esWeb}) : _esWeb = esWeb ?? kIsWeb;

  final bool _esWeb;

  bool _bloqueada = false;
  bool get bloqueada => _bloqueada;

  int _episodioBiometrico = 0;
  int get episodioBiometrico => _episodioBiometrico;

  MotivoDeBloqueo? _motivo;
  MotivoDeBloqueo? get motivo => _motivo;

  void bloquear(MotivoDeBloqueo motivo) {
    if (_bloqueada) return;
    _bloqueada = true;
    _motivo = motivo;
    notifyListeners();
  }

  void desbloquear() {
    if (!_bloqueada) return;
    _bloqueada = false;
    _motivo = null;
    notifyListeners();
  }

  /// El arranque en frío de una sesión móvil requiere volver a confirmar identidad.
  void bloquearAlArrancar() {
    if (_esWeb) return;
    bloquear(MotivoDeBloqueo.arranque);
  }

  /// Descarta el estado local al cerrar la sesión, sin conservar motivo anterior.
  void olvidar() {
    if (!_bloqueada && _motivo == null) return;
    _bloqueada = false;
    _motivo = null;
    notifyListeners();
  }

  /// Señala una nueva vuelta desde segundo plano sin levantar el candado.
  /// La capa visual puede volver a ofrecer la confirmación una sola vez.
  void nuevoEpisodioBiometrico() {
    if (!_bloqueada) return;
    _episodioBiometrico++;
    notifyListeners();
  }
}

final bloqueoProvider = ChangeNotifierProvider<ControlDeBloqueo>(
  (_) => ControlDeBloqueo(),
);
