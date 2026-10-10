import 'dart:io';

import 'package:crypto/crypto.dart';

import 'calidad_de_captura.dart' as calidad;
import 'capturas_del_expediente.dart';

/// Lee la foto que dejó la cámara, el escáner o la prueba de vida, le pasa el
/// chequeo de calidad de Atlas y la convierte en [Captura].
///
/// Devuelve la [Captura] aceptada o el `String` con el motivo del rechazo, para que
/// cada pantalla lo muestre a su manera. Antes vivía dentro de la pantalla de cámara;
/// con el escáner y la prueba de vida eran tres copias del mismo chequeo.
///
/// [recortadaAlCarnet] es `true` solo para lo que devuelve el escáner, que entrega
/// el carnet ya recortado: ahí sí se exige la proporción ID-1, como hace Atlas. Una
/// foto de la cámara de la app es el cuadro entero (16:9 o 4:3, con mesa alrededor)
/// y la proporción del carnet no se le puede pedir: antes se le pedía, y todo
/// anverso sacado con la cámara se rechazaba con «No parece el carnet entero».
Future<Object> capturaDesdeArchivo(
  String ruta, {
  required bool recortadaAlCarnet,
}) async {
  final bytes = await File(ruta).readAsBytes();
  final dimensiones = await calidad.medir(bytes);
  final motivo = calidad.evaluarCalidad(
    ancho: dimensiones.ancho,
    alto: dimensiones.alto,
    esDocumento: recortadaAlCarnet,
  );
  if (motivo != null) return motivo;
  return Captura(
    ruta: ruta,
    bytes: bytes.length,
    ancho: dimensiones.ancho,
    alto: dimensiones.alto,
    sha256Corto: sha256.convert(bytes).toString().substring(0, 12),
  );
}
