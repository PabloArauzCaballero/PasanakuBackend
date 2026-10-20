import 'dart:io';

import 'package:aportaya_diseno/atomos/codigo_q_r.dart';
import 'package:path_provider/path_provider.dart';

/// Guarda el QR del enlace del grupo como PNG en `cache/compartir` —la única carpeta que
/// `FileProvider` deja compartir (ver `res/xml/archivos_compartidos.xml`)— y devuelve la
/// ruta. Se pisa en cada envío: no se acumulan imágenes.
Future<String> guardarImagenDelQr(String contenido, {String? titulo}) async {
  final png = await codigoQrComoPng(contenido, titulo: titulo);
  final carpeta = Directory(
    '${(await getTemporaryDirectory()).path}/compartir',
  );
  await carpeta.create(recursive: true);
  final archivo = File('${carpeta.path}/qr-del-grupo.png');
  await archivo.writeAsBytes(png, flush: true);
  return archivo.path;
}
