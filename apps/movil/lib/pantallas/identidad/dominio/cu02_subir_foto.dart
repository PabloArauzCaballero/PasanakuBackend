import 'dart:io';

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:image_picker/image_picker.dart' show XFile;

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import 'capturas_del_expediente.dart';

export 'capturas_del_expediente.dart' show CaraDelCarril;

/// CU-02 · sube una foto del expediente al **servidor de archivos**.
///
/// La foto no se queda en el teléfono: el alta la necesita en el expediente, y quien
/// la revisa en el backoffice tiene que poder verla. Lo que vuelve es la clave del
/// objeto y su SHA-256 — nunca una URL pública (ADR-034).
///
/// Va con `Idempotency-Key` porque una subida que se corta y se reintenta no puede
/// dejar dos fotos del mismo momento en el expediente: «Reintentar» (mismo archivo)
/// reusa la clave; «Repetir la foto» pide una nueva, igual que en Atlas.
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

  /// 45 s de base más el tiempo que tardaría a 100 KB/s — el mismo cálculo que usa
  /// Atlas para decidir cuándo avisar «está tardando más de lo normal».
  static Duration plazoDeSubidaMs(int bytes) =>
      Duration(milliseconds: 45000 + (bytes / 100 * 1000).round());

  Future<FotoDelExpediente> subir({
    required String usuarioId,
    required CaraDelCarril cara,
    required String rutaLocal,
    required String formularioId,
    CancelToken? cancelToken,
  }) async {
    // Bytes y no `MultipartFile.fromFile`: en la web la camara devuelve una URL `blob:`,
    // no una ruta de disco, y `File` no existe. `XFile` lee las dos.
    final foto = XFile(rutaLocal);
    final contenido = await foto.readAsBytes();
    final cuerpo = FormData.fromMap({
      'cara': cara.valorApi,
      'archivo': MultipartFile.fromBytes(
        contenido,
        filename: _nombreDeArchivo(rutaLocal),
      ),
    });
    final r = await _ref
        .read(dioProvider)
        .post<Map<String, dynamic>>(
          '/usuarios/$usuarioId/documentos',
          data: cuerpo,
          cancelToken: cancelToken,
          options: Options(
            sendTimeout: plazoDeSubidaMs(contenido.length),
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
  }

  /// Se llama al aceptar el expediente completo (todas subidas) o al repetir una
  /// foto puntual — nunca automáticamente al fallar: la copia local es lo único que
  /// permite reintentar sin volver a la cámara.
  Future<void> borrarCopiaLocal(String rutaLocal) async {
    if (kIsWeb) return;
    final archivo = File(rutaLocal);
    if (archivo.existsSync()) {
      try {
        await archivo.delete();
      } on FileSystemException {
        // Si el sistema no deja borrarla, no es un error que frene nada.
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
