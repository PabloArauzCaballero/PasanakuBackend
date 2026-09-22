import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cu02_subir_foto.dart';
import 'estado_alta.dart';

/// Sube al servidor de archivos las fotos que se sacaron durante el alta.
Future<void> subirFotosDelExpediente(
  Ref ref,
  EstadoAlta state,
  String usuarioId,
) async {
  final subida = ref.read(subidaDeFotosProvider);
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
      // el backoffice lo ve; decirle a alguien «volvé a empezar» porque se corto
      // la red al subir la tercera foto seria peor.
    }
  }
}
