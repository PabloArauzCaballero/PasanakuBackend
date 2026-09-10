import 'package:go_router/go_router.dart';

import '../pantallas/alianzas/rutas.dart';
import '../pantallas/billetera/rutas.dart';
import '../pantallas/identidad/rutas.dart';
import '../pantallas/notificaciones/rutas.dart';
import '../pantallas/pasanaku/rutas.dart';
import '../pantallas/soporte/rutas.dart';

/// **El enchufe por dominio.** Lo escribe el shell (F2) UNA vez y se congela: cada
/// carril de pantallas llena el `rutas.dart` de su directorio y este archivo no cambia.
/// La prueba `enchufe_de_rutas_test.dart` lo verifica agregando una pantalla.
GoRouter crearEnrutador({String inicial = '/billetera/inicio'}) => GoRouter(
  initialLocation: inicial,
  routes: [
    ...rutasIdentidad,
    ...rutasBilletera,
    ...rutasAlianzas,
    ...rutasPasanaku,
    ...rutasSoporte,
    ...rutasNotificaciones,
  ],
);
