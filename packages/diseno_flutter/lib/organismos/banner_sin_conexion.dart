import 'package:flutter/material.dart';

import '../atomos/tono.dart';
import '../moleculas/alerta.dart';

/// Estado de red persistente: sigue visible mientras la app no puede llegar al
/// gateway. La pantalla decide qué operaciones pausar y explica la consecuencia.
class BannerSinConexion extends StatelessWidget {
  const BannerSinConexion({super.key, required this.detalle});

  final String detalle;

  @override
  Widget build(BuildContext context) => Semantics(
    liveRegion: true,
    child: Alerta(titulo: 'Sin conexión', detalle: detalle, tono: Tono.aviso),
  );
}
