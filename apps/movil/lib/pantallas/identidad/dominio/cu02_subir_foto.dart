import 'dart:io';

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:image_picker/image_picker.dart' show XFile;

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-02 · sube una foto del expediente al **servidor de archivos**.
///
/// La foto no se queda en el teléfono: el alta la necesita en el expediente, y quien
/// la revisa en el backoffice tiene que poder verla. Lo que vuelve es la clave del
/// objeto y su SHA-256 — nunca una URL pública (ADR-034).
///
/// Va con `Idempotency-Key` porque una subida que se corta y se reintenta no puede
/// dejar dos fotos del mismo momento en el expediente.
enum CaraDelExpediente { anverso, reverso, selfie }

class FotoDelExpediente {
  const FotoDelExpediente({
    required this.claveObjeto,
    required this.hashArchivo,
  });
  final String claveObjeto;
  final String hashArchivo;
}

class SubidaDeFotos {
  SubidaDeFotos(this._ref);
  final Ref _ref;

  Future<FotoDelExpediente> subir({
    required String usuarioId,
    required CaraDelExpediente cara,
    required String rutaLocal,
    required String formularioId,
  }) async {
    // Bytes y no `MultipartFile.fromFile`: en la web la camara devuelve una URL `blob:`,
    // no una ruta de disco, y `File` no existe. `XFile` lee las dos.
    final foto = XFile(rutaLocal);
    final cuerpo = FormData.fromMap({
      'cara': cara.name.toUpperCase(),
      'archivo': MultipartFile.fromBytes(
        await foto.readAsBytes(),
        filename: _nombreDeArchivo(rutaLocal),
      ),
    });
    try {
      final r = await _ref
          .read(dioProvider)
          .post<Map<String, dynamic>>(
            '/usuarios/$usuarioId/documentos',
            data: cuerpo,
            options: Options(
              headers: {
                'Idempotency-Key': _ref
                    .read(idempotenciaProvider.notifier)
                    .claveDe(formularioId),
              },
            ),
          );
      final datos = r.data!;
      return FotoDelExpediente(
        claveObjeto: datos['claveObjeto'] as String,
        hashArchivo: datos['hashArchivo'] as String,
      );
    } on DioException catch (e) {
      throw errorDeDominio(e);
    } finally {
      // La copia local se borra: la foto de una cédula en el carrete del teléfono es
      // el mismo dato personal que se cuida en el servidor, sin ninguna de sus
      // protecciones.
      // En la web no hay copia en disco: el navegador libera el `blob:` solo.
      if (!kIsWeb) {
        final archivo = File(rutaLocal);
        if (archivo.existsSync()) {
          try {
            await archivo.delete();
          } on FileSystemException {
            // Si el sistema no deja borrarla, no se frena el alta por eso.
          }
        }
      }
    }
  }
}

final subidaDeFotosProvider = Provider<SubidaDeFotos>(SubidaDeFotos.new);

/// El nombre que viaja en el multipart. Una URL `blob:` de la web no trae extension,
/// y el servidor decide el tipo por el contenido, pero un nombre sin extension
/// confunde cualquier registro: se nombra como lo que la camara entrega.
String _nombreDeArchivo(String ruta) {
  final ultimo = ruta.split(RegExp(r'[/\\]')).last;
  return ultimo.contains('.') ? ultimo : 'foto.jpg';
}
