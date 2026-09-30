import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/avisos_push.dart';
import '../infraestructura/plataforma.dart';

/// Los avisos push de esta plataforma. Se sobreescribe en pruebas con uno de mentira.
final avisosPushProvider = Provider<AvisosPush>(
  (_) => avisosPushDeLaPlataforma(),
);
