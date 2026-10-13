import 'package:flutter/material.dart';

import 'hoja_de_seleccion.dart';

export 'normalizar_para_buscar.dart' show normalizarParaBuscar;

/// Fachada compatible mientras las pantallas migran a `CampoDeSeleccion`.
Future<int?> mostrarListaConBuscador(
  BuildContext context, {
  required String titulo,
  required List<String> textos,
  int? elegido,
  TextInputType teclado = TextInputType.text,
}) => mostrarHojaDeSeleccion(
  context,
  titulo: titulo,
  elegida: elegido,
  teclado: teclado,
  opciones: [for (final texto in textos) (texto: texto, detalle: null)],
);
