import 'package:flutter/material.dart';

import 'pantalla_gestion_no_disponible.dart';
import 'textos_verificacion_profunda.dart';

/// CU-03 requiere un titular identificado y autorizado por el servidor. La app
/// todavía no dispone de ese identificador tras el login; no recoge una declaración
/// que quedaría solo en memoria ni acepta un ID de un enlace como titularidad.
class PantallaDeVerificacionProfunda extends StatelessWidget {
  const PantallaDeVerificacionProfunda({super.key});

  @override
  Widget build(BuildContext context) => const PantallaGestionNoDisponible(
    titulo: TextosVerificacionProfunda.titulo,
    aviso: TextosVerificacionProfunda.aviso,
    detalle: TextosVerificacionProfunda.detalle,
  );
}
