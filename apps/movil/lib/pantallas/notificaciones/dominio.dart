/// Un aviso de la bandeja. **`/notificaciones/bandeja` es la fuente de verdad**
/// (ADR-035): lo que llega por push solo avisa que hay algo nuevo acá — si el push
/// no llega, el aviso sigue estando en la lista.
class Notificacion {
  const Notificacion({
    required this.id,
    required this.titulo,
    required this.detalle,
    required this.cuando,
    required this.tono,
    required this.leida,
    this.ruta,
  });

  final String id;
  final String titulo;
  final String detalle;
  final DateTime cuando;

  /// `Tono` de `aportaya_diseno` como cadena (`info`, `exito`, `alerta`, `error`):
  /// este archivo es dominio puro y no importa Flutter.
  final String tono;
  final bool leida;

  /// A dónde navega `go_router` si se toca el aviso. `null` si es informativo.
  final String? ruta;

  Notificacion leidaComo(bool valor) => Notificacion(
    id: id,
    titulo: titulo,
    detalle: detalle,
    cuando: cuando,
    tono: tono,
    leida: valor,
    ruta: ruta,
  );
}

/// **Eventos que nunca producen un aviso** (regla del shell, D-11): LGI/FT y el
/// límite de intentos de acceso. La lista es cerrada a propósito — agregar un
/// evento que no debe notificar es una línea acá, no un `if` disperso por cada
/// pantalla que dispara notificaciones.
const Set<String> eventosSinAviso = {
  'ROS_GENERADO',
  'ALERTA_LGI_FT',
  'CASO_CUMPLIMIENTO_ABIERTO',
  'BLOQUEO_POR_INTENTOS',
};

bool notifica(String evento) => !eventosSinAviso.contains(evento);
