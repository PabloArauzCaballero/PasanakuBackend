import 'dart:async';

import 'ruta_del_aviso.dart';

/// **Escucha los toques de avisos push y abre la pantalla que corresponde.**
///
/// La misma pulsación puede llegar dos veces (al arrancar desde el aviso y por el flujo
/// de toques): un mismo destino repetido dentro de [ventana] se ignora. Lo que trae el
/// aviso pasa por [rutaDelAviso]; lo que no es un destino de la casa se descarta sin
/// ruido. Sin sesión no hace falta nada especial: las rutas protegidas ya redirigen al
/// ingreso y vuelven (`Guardias.exigirSesion`).
class AbridorDeAvisos {
  AbridorDeAvisos({
    required this.toques,
    required this.abrir,
    DateTime Function()? ahora,
    this.ventana = const Duration(seconds: 2),
  }) : _ahora = ahora ?? DateTime.now;

  final Stream<String> toques;
  final void Function(String ruta) abrir;
  final DateTime Function() _ahora;
  final Duration ventana;

  StreamSubscription<String>? _suscripcion;
  String? _ultimaRuta;
  DateTime? _ultimaHora;

  void iniciar() {
    _suscripcion ??= toques.listen(_alTocar, onError: (Object _) {});
  }

  void _alTocar(String texto) {
    final ruta = rutaDelAviso(texto);
    if (ruta == null) return;
    final ahora = _ahora();
    final repetido =
        ruta == _ultimaRuta &&
        _ultimaHora != null &&
        ahora.difference(_ultimaHora!) < ventana;
    if (repetido) return;
    _ultimaRuta = ruta;
    _ultimaHora = ahora;
    abrir(ruta);
  }

  Future<void> detener() async {
    await _suscripcion?.cancel();
    _suscripcion = null;
  }
}
