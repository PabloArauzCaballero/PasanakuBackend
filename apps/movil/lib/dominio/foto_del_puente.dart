import 'package:dio/dio.dart';

/// Pide una foto al puente de cámara de desarrollo (`scripts/camara_del_mac.py`).
///
/// Vive acá y no en el adaptador porque **toda la red del cliente sale de
/// `dominio/`** — lo comprueba `scripts/verificar_frontend.py`. Que el destino sea un
/// servicio local de desarrollo no lo hace menos red.
///
/// Devuelve `null` si el puente no está levantado o si macOS todavía no concedió el
/// permiso de cámara: quien llama ya tiene su camino alternativo.
Future<List<int>?> pedirFotoAlPuente(String url, {Dio? cliente}) async {
  try {
    final r = await (cliente ?? Dio()).get<List<int>>(
      url,
      options: Options(
        responseType: ResponseType.bytes,
        // La captura arranca la cámara y descarta los primeros cuadros: tarda.
        receiveTimeout: const Duration(seconds: 25),
      ),
    );
    final bytes = r.data;
    return (bytes == null || bytes.isEmpty) ? null : bytes;
  } on Object {
    return null;
  }
}
