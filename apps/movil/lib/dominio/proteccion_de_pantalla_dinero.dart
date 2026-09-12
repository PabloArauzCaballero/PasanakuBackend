import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../proveedores/proteccion_pantalla.dart';

/// Gate propio del carril: `screen_protector` activo en las rutas con saldo. El
/// puerto ya lo declaró el carril M1 (`dominio/puertos/proteccion_pantalla.dart`);
/// como `navegacion/` (congelado) no activa nada por ruta todavía, cada pantalla de
/// billetera que muestra saldo se protege a sí misma al entrar y se libera al salir,
/// para no dejarla prendida para toda la app.
mixin ConProteccionDePantalla<T extends ConsumerStatefulWidget>
    on ConsumerState<T> {
  late final _proteccion = ref.read(proteccionPantallaProvider);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _proteccion.activar();
    });
  }

  @override
  void dispose() {
    _proteccion.desactivar();
    super.dispose();
  }
}
