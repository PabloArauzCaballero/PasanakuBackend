import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cu02_subir_foto.dart';
import 'estado_alta.dart';

/// Sube las fotos y devuelve si alguna quedó pendiente de envío.
Future<bool> subirFotosDelExpediente(
  Ref ref,
  EstadoAlta state,
  String usuarioId,
) async {
  final subida = ref.read(subidaDeFotosProvider);
  var huboFallos = false;
  final pendientes = <CaraDelExpediente, String?>{
    CaraDelExpediente.anverso: state.rutaAnverso,
    CaraDelExpediente.reverso: state.rutaReverso,
    CaraDelExpediente.selfie: state.rutaSelfie,
  };
  for (final entrada in pendientes.entries) {
    final ruta = entrada.value;
    if (ruta == null || ruta.isEmpty) continue;
    try {
      await subida.subir(
        usuarioId: usuarioId,
        cara: entrada.key,
        rutaLocal: ruta,
        formularioId: 'alta-${entrada.key.name}',
      );
    } on Object {
      // Una foto que no sube no frena el alta. Queda como expediente incompleto y
      // se informa el pendiente: la cuenta ya existe y no debe crearse otra.
      huboFallos = true;
    }
  }
  return huboFallos;
}
