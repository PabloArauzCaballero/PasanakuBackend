import 'package:flutter/material.dart';

import 'pantalla_gestion_no_disponible.dart';
import 'textos.dart';

/// No ofrecer confianza de 30 días sin una operación que la registre y confirme.
class PantallaDeDispositivos extends StatelessWidget {
  const PantallaDeDispositivos({super.key});

  @override
  Widget build(BuildContext context) => const PantallaGestionNoDisponible(
    titulo: TextosIdentidad.tusDispositivos,
    aviso: TextosIdentidad.dispositivosNoDisponibles,
    detalle: TextosIdentidad.detalleDispositivosNoDisponibles,
  );
}
