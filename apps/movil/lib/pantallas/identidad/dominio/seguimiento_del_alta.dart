import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'capturas_del_expediente.dart';
import '../../../proveedores/sesion.dart';

/// Lo que la pantalla de subida necesita después de `POST /usuarios`: el
/// `usuarioId` que devolvió y una copia de las cinco capturas.
@immutable
class AltaEnSeguimiento {
  const AltaEnSeguimiento({
    required this.usuarioId,
    required this.capturas,
    this.tokenDeAlta,
  });

  final String usuarioId;

  /// Lo que autoriza subir las fotos sin sesión; solo vive en memoria.
  final String? tokenDeAlta;
  final CapturasDelExpediente capturas;
}

/// Sobrevive a `AltaNotifier.reiniciar()`, que vacía el asistente apenas se crea
/// la cuenta. Las capturas viajan acá y no en `altaProvider`: si la pantalla de
/// subida las leyera de ahí, encontraría el mapa vacío y no subiría ninguna. Vive
/// en su propio provider — igual que `idempotenciaProvider` — por la misma razón:
/// sobrevive a algo que el resto del alta no tiene por qué sobrevivir.
class SeguimientoDelAlta extends Notifier<AltaEnSeguimiento?> {
  @override
  AltaEnSeguimiento? build() {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  void fijar(
    String usuarioId,
    CapturasDelExpediente capturas, {
    String? tokenDeAlta,
  }) => state = AltaEnSeguimiento(
    usuarioId: usuarioId,
    capturas: capturas,
    tokenDeAlta: tokenDeAlta,
  );

  /// "Repetir la foto" desde la pantalla de subida: la nueva reemplaza a la vieja.
  Future<void> reemplazarCaptura(CaraDelCarril cara, Captura captura) async {
    final actual = state;
    if (actual == null) return;
    final capturas = await actual.capturas.reemplazarCaptura(cara, captura);
    state = AltaEnSeguimiento(
      usuarioId: actual.usuarioId,
      tokenDeAlta: actual.tokenDeAlta,
      capturas: capturas,
    );
  }

  /// Las fotos ya subieron: se borran del teléfono, pero el `usuarioId` y el token
  /// del alta se conservan. La pantalla de estado los necesita para consultar el
  /// expediente sin sesión; sin el token, el servidor respondía 403 y la persona veía
  /// «No tenés acceso» justo al terminar de crear su cuenta.
  Future<void> liberarCapturas() async {
    final actual = state;
    if (actual == null) return;
    state = AltaEnSeguimiento(
      usuarioId: actual.usuarioId,
      tokenDeAlta: actual.tokenDeAlta,
      capturas: const CapturasDelExpediente(),
    );
    await actual.capturas.eliminarArchivosLocales();
  }

  Future<void> limpiar() async {
    final actual = state;
    if (actual == null) return;
    state = null;
    await actual.capturas.eliminarArchivosLocales();
  }
}

final seguimientoDelAltaProvider =
    NotifierProvider<SeguimientoDelAlta, AltaEnSeguimiento?>(
      SeguimientoDelAlta.new,
    );
