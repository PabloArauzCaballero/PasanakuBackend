import 'dart:io';

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
    final archivo = File(rutaLocal);
    final cuerpo = FormData.fromMap({
      'cara': cara.name.toUpperCase(),
      'archivo': await MultipartFile.fromFile(
        rutaLocal,
        filename: rutaLocal.split(Platform.pathSeparator).last,
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

final subidaDeFotosProvider = Provider<SubidaDeFotos>(SubidaDeFotos.new);
