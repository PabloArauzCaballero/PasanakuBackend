import 'package:flutter/material.dart';

import 'pantalla_gestion_no_disponible.dart';
import 'textos.dart';

/// No recoger una contraseña nueva cuando el servidor no ofrece el cambio.
class PantallaDeContrasena extends StatelessWidget {
  const PantallaDeContrasena({super.key});

  @override
  Widget build(BuildContext context) => const PantallaGestionNoDisponible(
    titulo: TextosIdentidad.tituloContrasenaNueva,
    aviso: TextosIdentidad.contrasenaNoDisponible,
    detalle: TextosIdentidad.detalleContrasenaNoDisponible,
  );
}
