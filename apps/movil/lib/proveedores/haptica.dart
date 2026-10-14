import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/haptica.dart';
import '../infraestructura/plataforma.dart';

/// Único proveedor y punto de política para la retroalimentación táctil.
final hapticaProvider = Provider<Haptica>((_) => hapticaDeLaPlataforma());
