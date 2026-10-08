import 'package:cunning_document_scanner/cunning_document_scanner.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/services.dart' show MissingPluginException;

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

/// El escáner de documentos del sistema, el mismo que usa Atlas: VisionKit en iOS y
/// el Document Scanner de ML Kit en Android. Encuentra los bordes del carnet, dispara
/// solo cuando lo tiene entero y devuelve la imagen ya recortada y enderezada.
///
/// Opciones de Atlas (`escaner-documento.ts`): una sola página, sin importar de la
/// galería (la foto tiene que ser de ahora), modo `base` en Android (recorta y
/// endereza, sin filtros de «limpieza» que le borran detalle al carnet) y JPEG 0,9.
/// En iOS se fija el filtro original y se esconde la barra: un carnet en blanco y
/// negro pierde lo que un revisor necesita mirar.
///
/// Encendido por omisión, como en Atlas para desarrollo, preview y producción. Con
/// `--dart-define=ESCANER_DOCUMENTO=false` se apaga (simulador de iOS, que no tiene
/// VisionKit de cámara). Si el escáner no existe en el dispositivo —sin Play
/// Services, plugin ausente— la pantalla cae a la cámara de la app, en silencio.
const _escanerEncendido = bool.fromEnvironment(
  'ESCANER_DOCUMENTO',
  defaultValue: true,
);

Future<ResultadoDelEscaner> escanearDocumento() async {
  if (kIsWeb) return const EscaneoNoDisponible('web');
  if (!_escanerEncendido) return const EscaneoNoDisponible('apagado');
  try {
    final rutas = await CunningDocumentScanner.getPictures(
      noOfPages: 1,
      scannerSource: ScannerSource.camera,
      androidScannerMode: AndroidScannerMode.base,
      iosScannerOptions: IosScannerOptions(
        imageFormat: IosImageFormat.jpg,
        jpgCompressionQuality: 0.9,
        defaultFilter: IosDocumentFilter.original,
        showFilterBar: false,
      ),
    );
    if (rutas == null || rutas.isEmpty) return const EscaneoCancelado();
    return EscaneoCapturado(rutas.first);
  } on MissingPluginException {
    return const EscaneoNoDisponible('sin_soporte');
  } on CunningDocumentScannerException catch (e) {
    return EscaneoNoDisponible(e.code ?? 'error');
  } on Object {
    return const EscaneoNoDisponible('error');
  }
}
