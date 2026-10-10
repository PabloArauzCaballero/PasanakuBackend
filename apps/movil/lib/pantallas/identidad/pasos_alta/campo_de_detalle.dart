import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El campo que aparece debajo de una lista cerrada cuando se elige «Otro» u
/// «Otra»: ahí se escribe lo que la lista no tenía. Lo usan el origen de fondos y la
/// actividad económica del paso 7, con el mismo aspecto y el mismo espacio arriba.
class CampoDeDetalle extends StatelessWidget {
  const CampoDeDetalle({
    super.key,
    required this.etiqueta,
    required this.ayuda,
    required this.controlador,
    required this.error,
    required this.onCambio,
  });

  final String etiqueta;
  final String ayuda;
  final TextEditingController controlador;
  final String? error;
  final VoidCallback onCambio;

  /// Menos de tres letras no dice nada que se pueda revisar después.
  static bool alcanza(TextEditingController c) => c.text.trim().length >= 3;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: Espacio.s3),
    child: Campo(
      etiqueta: etiqueta,
      controlador: controlador,
      icono: Icons.edit_outlined,
      ayuda: ayuda,
      error: error,
      onChanged: (_) => onCambio(),
    ),
  );
}
