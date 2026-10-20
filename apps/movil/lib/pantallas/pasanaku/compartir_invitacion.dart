import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/compartir.dart';
import '../../proveedores/imagen_del_qr.dart';
import 'dominio/enlace_del_grupo.dart';
import 'textos_enlace_del_grupo.dart';

/// Las dos formas de invitar (2026-10-20). Cada una devuelve el aviso para la persona
/// si algo no salió, o `null` si se abrió la app elegida.

/// El enlace como texto: primero WhatsApp (sin número, abre la lista de chats y
/// grupos); si no está, la hoja de compartir del sistema.
Future<String?> compartirEnlaceDelGrupo(
  WidgetRef ref,
  String enlace, {
  String? nombre,
}) async {
  final texto = mensajeDelEnlaceDelGrupo(enlace, nombreDelGrupo: nombre);
  final compartir = ref.read(compartirProvider);
  final abrio =
      await compartir.porWhatsApp(telefono: '', texto: texto) ||
      await compartir.conOtraApp(texto: texto);
  return abrio ? null : TextosEnlaceDelGrupo.noSePudoAbrir;
}

/// El QR como imagen PNG, para guardarla desde WhatsApp y subirla en «Unirse a un
/// grupo».
Future<String?> compartirQrDelGrupo(
  WidgetRef ref,
  String enlace, {
  String? nombre,
}) async {
  final String ruta;
  try {
    ruta = await ref.read(imagenDelQrProvider)(enlace, titulo: nombre);
  } on Object {
    return TextosEnlaceDelGrupo.noSePudoArmarQr;
  }
  final abrio = await ref
      .read(compartirProvider)
      .imagen(ruta: ruta, texto: TextosEnlaceDelGrupo.mensajeDelQr(nombre));
  return abrio ? null : TextosEnlaceDelGrupo.noSePudoAbrir;
}
