import '../../dominio/puertos/proteccion_pantalla.dart';

/// **No soportado en iOS, explícito** (H2.S1.M2, madre H6.S1.M2).
///
/// `FLAG_SECURE` es una API de Android; no existe un equivalente registrado por
/// `MethodChannel` en este carril. `activar()`/`desactivar()` son no-ops SIN abrir
/// ningún canal -- antes caían en `ProteccionPantallaAndroid`, que sí abre
/// `bo.aportaya/proteccion_pantalla`, sin receptor nativo de iOS detrás.
///
/// Es una capability CRÍTICA de seguridad (`Capacidades.seguridadCriticaSoportada`):
/// una pantalla de saldo sin bloqueo de captura en iOS no debe mostrarse en release
/// sin que la app lo sepa. La responsabilidad de bloquear el arranque cuando esto
/// falta es de `app.dart` (H2.S2.M3), no de este adaptador -- este solo declara la
/// verdad de la plataforma, sin fingir que protege lo que no puede proteger.
class ProteccionPantallaIos implements ProteccionPantalla {
  const ProteccionPantallaIos();

  @override
  Future<void> activar() async {}

  @override
  Future<void> desactivar() async {}
}
