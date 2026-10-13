import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

import '../../../infraestructura/escaner_de_documentos.dart';
import '../dominio/captura_desde_archivo.dart';
import '../dominio/capturas_del_expediente.dart';
import '../dominio/juez_de_pose.dart';
import 'opciones_de_captura_rechazada.dart';

/// Cómo se saca cada cara del expediente, igual en el alta y al repetir una foto
/// desde la subida:
///
/// - **Anverso y reverso:** el escáner del sistema, que encuentra los bordes y
///   dispara solo. Si no hay escáner en el teléfono, la cámara de la app.
/// - **Rostro:** la prueba de vida automática, que saca sola las tres poses.
///
/// Devuelve un `Map<CaraDelCarril, Captura>` con lo logrado, `'prueba'` (atajo de
/// desarrollo de la cámara de la app) o `null` si no salió nada.
Future<Object?> sacarCaptura(
  BuildContext context,
  CaraDelCarril cara, {
  Set<CaraDelCarril> listas = const {},
  Future<ResultadoDelEscaner> Function() escanear = escanearDocumento,
}) async {
  if (!cara.esDocumento) {
    return context.push<Object?>(
      '/registro/prueba-de-vida',
      extra: posesDesde(cara, listas),
    );
  }
  while (context.mounted) {
    final escaneo = await escanear();
    if (!context.mounted || escaneo is EscaneoCancelado) return null;
    if (escaneo is EscaneoCapturado) {
      final evaluacion = await evaluarCapturaDesdeArchivo(
        escaneo.ruta,
        recortadaAlCarnet: true,
      );
      if (!context.mounted) return null;
      final motivo = evaluacion.rechazo;
      if (motivo == null) return {cara: evaluacion.captura};
      final eleccion = await showModalBottomSheet<_EleccionDeFoto>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (hoja) => DecoratedBox(
          decoration: BoxDecoration(
            color: Tokens.of(hoja).surface,
            borderRadius: const BorderRadius.vertical(
              top: Radius.circular(Radios.xl),
            ),
          ),
          child: SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.all(Espacio.s5),
              child: OpcionesDeCapturaRechazada(
                cara: cara,
                motivo: motivo,
                onRepetir: () =>
                    Navigator.of(hoja).pop(_EleccionDeFoto.repetir),
                onUsarEstaFoto: () =>
                    Navigator.of(hoja).pop(_EleccionDeFoto.usar),
              ),
            ),
          ),
        ),
      );
      if (!context.mounted) return null;
      if (eleccion == _EleccionDeFoto.usar) {
        return {cara: evaluacion.captura};
      }
      if (eleccion != _EleccionDeFoto.repetir) return null;
      continue;
    }
    // `EscaneoNoDisponible`: la cámara de la app, en silencio, como Atlas.
    break;
  }
  if (!context.mounted) return null;
  final resultado = await context.push<Object?>(
    '/registro/camara',
    extra: cara,
  );
  return resultado is Captura ? {cara: resultado} : resultado;
}

enum _EleccionDeFoto { repetir, usar }

/// Las poses que se piden al tocar [cara]: si ya estaba lista, solo esa (se está
/// repitiendo); si no, esa y las que le siguen y todavía faltan, de corrido —la
/// prueba de vida es una sola toma, no tres visitas a la cámara.
List<PoseDeVida> posesDesde(CaraDelCarril cara, Set<CaraDelCarril> listas) {
  final primera = PoseDeVida.de(cara)!;
  if (listas.contains(cara)) return [primera];
  return [
    for (final p in PoseDeVida.values)
      if (p == primera || (p.index > primera.index && !listas.contains(p.cara)))
        p,
  ];
}
