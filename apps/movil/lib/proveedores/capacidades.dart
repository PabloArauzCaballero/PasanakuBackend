import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../infraestructura/capacidades.dart';
import '../infraestructura/plataforma.dart';

/// El mapa de capacidades como proveedor consultable (H2.S2.M1, madre H6.S2.M1):
/// cualquier pantalla puede leer `ref.watch(capacidadesProvider)` para saber si
/// biometría, avisos push o protección de pantalla están soportados en ESTA
/// plataforma, sin preguntar `Platform.is*` (eso sigue prohibido fuera de
/// `infraestructura/`, ADR-036).
///
/// Es un `Provider` común, no `FutureProvider`: `capacidadesDeLaPlataforma()` es
/// síncrono (lee `Platform.isIOS`, no un canal), así que no hace falta un estado de
/// carga que solo confundiría a quien lo consuma.
final capacidadesProvider = Provider<Capacidades>(
  (_) => capacidadesDeLaPlataforma(),
);
