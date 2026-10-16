import 'package:flutter/material.dart';

import '../../../dominio/puertos/camara.dart';
import '../dominio/captura_desde_archivo.dart';
import '../dominio/capturas_del_expediente.dart';

Future<Captura?> elegirCapturaDeGaleria(
  BuildContext context,
  Camara camara,
) async {
  final ruta = await camara.elegirDeLasFotos();
  if (!context.mounted || ruta == null) return null;

  final resultado = await capturaDesdeArchivo(ruta, recortadaAlCarnet: false);
  if (!context.mounted) return null;
  if (resultado is Captura) return resultado;

  ScaffoldMessenger.of(
    context,
  ).showSnackBar(SnackBar(content: Text(resultado as String)));
  return null;
}
