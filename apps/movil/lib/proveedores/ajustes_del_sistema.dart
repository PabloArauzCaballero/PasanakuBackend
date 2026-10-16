import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/ajustes_del_sistema.dart';
import '../infraestructura/ajustes_del_sistema_app_settings.dart';

/// Los ajustes del teléfono. Se sobreescribe en pruebas con uno que solo anota.
final ajustesDelSistemaProvider = Provider<AjustesDelSistema>(
  (_) => const AjustesDelSistemaAppSettings(),
);
