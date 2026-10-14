import 'package:aportaya_diseno/moviles/celebracion_de_exito.dart';
import 'package:flutter/material.dart';

/// Presenta la confirmación visual y espera a que termine antes de navegar.
Future<void> mostrarCelebracionDeAlta(BuildContext context) => showDialog<void>(
  context: context,
  barrierDismissible: false,
  barrierColor: Colors.transparent,
  builder: (contexto) => Dialog(
    backgroundColor: Colors.transparent,
    elevation: 0,
    child: CelebracionDeExito(onCompleto: () => Navigator.of(contexto).pop()),
  ),
);
