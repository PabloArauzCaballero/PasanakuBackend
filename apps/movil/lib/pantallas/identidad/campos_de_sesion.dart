import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';
import 'textos_del_alta.dart';

/// Campos del paso de credenciales del ingreso.
class CamposDeSesion extends StatelessWidget {
  const CamposDeSesion({
    super.key,
    required this.telefono,
    required this.contrasena,
    required this.onChanged,
  });

  final TextEditingController telefono;
  final TextEditingController contrasena;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Campo(
        etiqueta: TextosIdentidad.telefono,
        icono: Icons.phone_iphone,
        prefijo: '+591',
        ayuda: TextosIdentidad.sesionCelularAyuda,
        ejemplo: TextosDelAlta.ejemploTelefono,
        controlador: telefono,
        tipoDeTeclado: TextInputType.phone,
        onChanged: (_) => onChanged(),
      ),
      const SizedBox(height: Espacio.s4),
      CampoContrasena(
        etiqueta: TextosIdentidad.contrasena,
        ejemplo: TextosDelAlta.ejemploContrasena,
        controlador: contrasena,
        onChanged: (_) => onChanged(),
      ),
    ],
  );
}
