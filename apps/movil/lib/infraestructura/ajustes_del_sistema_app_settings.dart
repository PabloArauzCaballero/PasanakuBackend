import 'package:app_settings/app_settings.dart';

import '../dominio/puertos/ajustes_del_sistema.dart';

/// El adaptador real sobre `app_settings`. Cualquier falla del canal nativo es `false`:
/// la pantalla ya le dijo a la persona dónde habilitarlo a mano.
class AjustesDelSistemaAppSettings implements AjustesDelSistema {
  const AjustesDelSistemaAppSettings();

  @override
  Future<bool> abrir() async {
    try {
      await AppSettings.openAppSettings();
      return true;
    } on Object {
      return false;
    }
  }
}
