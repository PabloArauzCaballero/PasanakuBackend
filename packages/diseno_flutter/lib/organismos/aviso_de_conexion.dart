import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../atomos/tono.dart';
import '../moleculas/alerta.dart';
import '../tokens/tokens.dart';
import 'banner_sin_conexion.dart';

/// Explica por qué una operación sensible está pausada durante la sonda inicial,
/// sin red o si falló la comprobación. No confunde «comprobando» con «offline».
class AvisoDeConexion extends StatelessWidget {
  const AvisoDeConexion({
    super.key,
    required this.estado,
    required this.detalleSinConexion,
    required this.reintentar,
  });

  final AsyncValue<bool> estado;
  final String detalleSinConexion;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) {
    if (estado.value == true && !estado.isLoading && !estado.hasError) {
      return const SizedBox.shrink();
    }
    if (estado.hasError) {
      return Semantics(
        liveRegion: true,
        child: Alerta(
          titulo: 'No pudimos comprobar la conexión',
          detalle: 'Las operaciones siguen pausadas.',
          tono: Tono.aviso,
          accion: TextButton(
            onPressed: reintentar,
            style: TextButton.styleFrom(
              minimumSize: const Size(Tactil.minimo, Tactil.minimo),
              foregroundColor: Tokens.of(context).avisoTexto,
            ),
            child: const Text('Volver a comprobar'),
          ),
        ),
      );
    }
    if (estado.isLoading || !estado.hasValue) {
      return Semantics(
        liveRegion: true,
        child: const Alerta(
          titulo: 'Comprobando conexión',
          detalle: 'Las operaciones se habilitarán cuando confirmemos la red.',
          tono: Tono.info,
        ),
      );
    }
    return BannerSinConexion(detalle: detalleSinConexion);
  }
}
