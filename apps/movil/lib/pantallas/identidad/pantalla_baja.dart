import 'package:flutter/material.dart';

import 'pantalla_gestion_no_disponible.dart';
import 'textos.dart';

/// No simular una baja con un diálogo que solo se cierra.
class PantallaDeBaja extends StatelessWidget {
  const PantallaDeBaja({super.key});

  @override
  Widget build(BuildContext context) => const PantallaGestionNoDisponible(
    titulo: TextosIdentidad.tituloBaja,
    aviso: TextosIdentidad.bajaNoDisponible,
    detalle: TextosIdentidad.detalleBajaNoDisponible,
  );
}
