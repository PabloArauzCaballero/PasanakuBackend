import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/proteccion_pantalla.dart';
import '../infraestructura/plataforma.dart';

final proteccionPantallaProvider = Provider<ProteccionPantalla>(
  (_) => proteccionPantallaDeLaPlataforma(),
);
