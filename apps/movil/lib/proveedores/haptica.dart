import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/haptica.dart';
import '../infraestructura/plataforma.dart';

final hapticaProvider = Provider<Haptica>((_) => hapticaDeLaPlataforma());
