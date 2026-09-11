import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio.dart';

/// La bandeja completa. F2 la deja con datos de ejemplo — el CU real que la llena
/// desde el backend (con paginación e idempotencia de lectura) es de un carril de
/// pantallas; acá se congela el contrato: `Notifier<List<Notificacion>>`, no una
/// `Future` por pantalla, porque un push entrante inserta encima sin recargar todo.
class BandejaNotificaciones extends Notifier<List<Notificacion>> {
  @override
  List<Notificacion> build() => [];

  /// **Se encolan, no se pisan** (D-11): una notificación nueva se agrega arriba,
  /// nunca reemplaza a la anterior. El filtro de `eventosSinAviso` lo aplica quien
  /// dispara el evento (`notifica(codigoDeEvento)`, en `dominio.dart`) antes de
  /// llegar acá — este método ya recibe solo avisos que corresponde mostrar.
  void agregar(Notificacion aviso) {
    state = [aviso, ...state];
  }

  void marcarLeida(String id) {
    state = [
      for (final n in state)
        if (n.id == id) n.leidaComo(true) else n,
    ];
  }
}

final bandejaProvider =
    NotifierProvider<BandejaNotificaciones, List<Notificacion>>(
      BandejaNotificaciones.new,
    );

final noLeidasProvider = Provider<int>(
  (ref) => ref.watch(bandejaProvider).where((n) => !n.leida).length,
);
