import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart' show XFile;

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';

/// CU-07 — la foto de perfil de la cuenta de la sesión (`GET /usuarios/yo/foto-de-perfil`).
///
/// Vuelven los bytes y no una URL: el almacén vive en la red interna (ADR-034) y la
/// sirve el servicio dueño. `null` cuando la cuenta todavía no tiene foto (404): eso
/// no es un error, es el estado inicial y se muestran las iniciales.
final fotoDePerfilProvider = FutureProvider.autoDispose<Uint8List?>((
  ref,
) async {
  ref.watch(generacionDeSesionProvider);
  try {
    final r = await ref
        .read(dioProvider)
        .get<List<int>>(
          '/usuarios/yo/foto-de-perfil',
          // El Dio de la app pide JSON por omisión; la foto es imagen y el servidor
          // respondería 406 a un `Accept: application/json`.
          options: Options(
            responseType: ResponseType.bytes,
            headers: {'Accept': 'image/jpeg, image/png'},
          ),
        );
    final datos = r.data;
    return datos == null ? null : Uint8List.fromList(datos);
  } on DioException catch (e) {
    if (e.response?.statusCode == 404) return null;
    throw errorDeDominio(e);
  }
});

/// El nombre de la cuenta, para las iniciales del avatar mientras no hay foto.
final nombreDeLaCuentaProvider = FutureProvider.autoDispose<String>((
  ref,
) async {
  ref.watch(generacionDeSesionProvider);
  try {
    final r = await ref
        .read(dioProvider)
        .get<Map<String, dynamic>>('/usuarios/yo/perfil');
    return (r.data?['nombreCompleto'] as String?) ?? '';
  } catch (e) {
    throw errorDeDominio(e);
  }
});

/// `PUT /usuarios/yo/foto-de-perfil`: sube la foto y la deja vigente. Es un reemplazo,
/// así que reintentarlo con la misma foto deja el mismo resultado y no lleva
/// `Idempotency-Key` (lo dice el contrato).
final subirFotoDePerfilProvider = Provider<Future<void> Function(String)>((
  ref,
) {
  return (rutaLocal) async {
    // Bytes y no `MultipartFile.fromFile`, igual que el expediente: en la web la
    // foto llega como URL `blob:` y `File` no existe.
    final contenido = await XFile(rutaLocal).readAsBytes();
    try {
      await ref
          .read(dioProvider)
          .put<void>(
            '/usuarios/yo/foto-de-perfil',
            data: FormData.fromMap({
              'archivo': MultipartFile.fromBytes(
                contenido,
                filename: 'foto-de-perfil',
              ),
            }),
          );
    } catch (e) {
      throw errorDeDominio(e);
    }
    ref.invalidate(fotoDePerfilProvider);
  };
});
