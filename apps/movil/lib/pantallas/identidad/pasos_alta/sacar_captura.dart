import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../infraestructura/escaner_de_documentos.dart';
import '../dominio/captura_desde_archivo.dart';
import '../dominio/capturas_del_expediente.dart';
import '../dominio/juez_de_pose.dart';

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
  final escaneo = await escanear();
  if (!context.mounted || escaneo is EscaneoCancelado) return null;
  if (escaneo is EscaneoCapturado) {
    final resultado = await capturaDesdeArchivo(
      escaneo.ruta,
      recortadaAlCarnet: true,
    );
    if (resultado is Captura) return {cara: resultado};
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(resultado as String)));
    }
    return null;
  }
  // `EscaneoNoDisponible`: la cámara de la app, en silencio, como Atlas.
  final resultado = await context.push<Object?>(
    '/registro/camara',
    extra: cara,
  );
  return resultado is Captura ? {cara: resultado} : resultado;
}

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
