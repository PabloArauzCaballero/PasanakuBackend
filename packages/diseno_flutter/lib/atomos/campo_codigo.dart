import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../atomos/campo.dart';

/// Código de invitación: cinco caracteres, en mayúsculas, dictables por teléfono.
class CampoCodigo extends StatelessWidget {
  const CampoCodigo({
    super.key,
    required this.controlador,
    this.largo = 5,
    this.error,
  });

  final TextEditingController controlador;
  final int largo;
  final String? error;

  @override
  Widget build(BuildContext context) {
    return Campo(
      etiqueta: 'Código de invitación',
      controlador: controlador,
      error: error,
      ayuda:
          'Son $largo caracteres. Si te lo dictan por teléfono, escribilo acá.',
      formateadores: [
        FilteringTextInputFormatter.allow(RegExp('[A-Za-z0-9]')),
        LengthLimitingTextInputFormatter(largo),
        TextInputFormatter.withFunction(
          (_, nuevo) => nuevo.copyWith(text: nuevo.text.toUpperCase()),
        ),
      ],
    );
  }
}
