/// El resultado de intentar el escáner del sistema, igual que en Atlas:
/// `imagen` cuando capturó, `cancelado` cuando la persona cerró el escáner sin
/// sacar nada, y `noDisponible` cuando el escáner no existe en este dispositivo o
/// en este build — ahí la pantalla cae a la cámara de la app, en silencio.
sealed class ResultadoDelEscaner {
  const ResultadoDelEscaner();
}

class EscaneoCapturado extends ResultadoDelEscaner {
  const EscaneoCapturado(this.ruta);
  final String ruta;
}

class EscaneoCancelado extends ResultadoDelEscaner {
  const EscaneoCancelado();
}

class EscaneoNoDisponible extends ResultadoDelEscaner {
  const EscaneoNoDisponible(this.motivo);
  final String motivo;
}

/// El escáner de documentos del sistema (ML Kit en Android), detrás de
/// `--dart-define=ESCANER_DOCUMENTO=true` — apagado por omisión, igual que en Atlas
/// (`.env.example` lo trae en `false` porque el emulador no tiene cámara real).
///
/// **Decisión explícita de alcance**, no un hueco escondido: esta primera versión
/// no agrega la dependencia nativa del escáner (`google_mlkit_document_scanner`),
/// así que esta función siempre devuelve `EscaneoNoDisponible('sin_soporte')`. Es
/// exactamente uno de los tres estados que Atlas ya contempla como válidos —no un
/// camino roto—, y la pantalla de captura cae a la cámara de la app como con
/// cualquier otro "no disponible" de Atlas (emulador, Expo Go, web). Cablear el
/// plugin real queda pendiente y se declara así en el reporte del trabajo.
Future<ResultadoDelEscaner> escanearDocumento() async {
  return const EscaneoNoDisponible('sin_soporte');
}
