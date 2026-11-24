import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../proveedores/proteccion_pantalla.dart';

/// Gate propio del carril: `screen_protector` activo en las rutas con saldo. El
/// puerto ya lo declaró el carril M1 (`dominio/puertos/proteccion_pantalla.dart`);
/// como `navegacion/` (congelado) no activa nada por ruta todavía, cada pantalla de
/// billetera que muestra saldo se protege a sí misma al entrar y se libera al salir,
/// para no dejarla prendida para toda la app.
///
/// Lleva la cuenta de cuántas pantallas protegidas hay vivas por puerto (DEFECTO-004):
/// con dos apiladas, cerrar la de arriba no puede apagar la protección de la de abajo.
/// Solo descuenta quien llegó a activar: una pantalla que se desmonta antes de su
/// primer cuadro no apaga la de otra.
mixin ConProteccionDePantalla<T extends ConsumerStatefulWidget>
    on ConsumerState<T> {
  static final Expando<int> _vivasPorPuerto = Expando();
  late final _proteccion = ref.read(proteccionPantallaProvider);
  bool _activo = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _activo = true;
      final vivas = _vivasPorPuerto[_proteccion] ?? 0;
      _vivasPorPuerto[_proteccion] = vivas + 1;
      if (vivas == 0) _proteccion.activar();
    });
  }

  @override
  void dispose() {
    if (_activo) {
      final vivas = (_vivasPorPuerto[_proteccion] ?? 1) - 1;
      _vivasPorPuerto[_proteccion] = vivas <= 0 ? null : vivas;
      if (vivas <= 0) _proteccion.desactivar();
    }
    super.dispose();
  }
}
