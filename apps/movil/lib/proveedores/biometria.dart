import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/biometria.dart';
import '../infraestructura/plataforma.dart';

final biometriaProvider = Provider<Biometria>((_) => biometriaDeLaPlataforma());

/// Si conviene ofrecer el atajo biométrico en esta sesión (sensor + enrolada).
final biometriaDisponibleProvider = FutureProvider<bool>(
  (ref) => ref.watch(biometriaProvider).disponible(),
);
