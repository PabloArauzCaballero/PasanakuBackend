import 'package:flutter/material.dart';

import '../errores.dart';
import '../tokens/tokens.dart';

/// Qué pasó en lenguaje humano, un botón de reintento y la traza para soporte.
/// Sin conexión es una variante: último estado conocido, operaciones bloqueadas.
class EstadoError extends StatelessWidget {
  const EstadoError({super.key, required this.error, required this.reintentar});

  final Object error;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final presentable = error is ErrorPresentable
        ? error as ErrorPresentable
        : null;
    final sinConexion = presentable?.sinConexion ?? false;
    final mensaje =
        presentable?.mensaje ??
        'Algo salió mal de nuestro lado. Probá de nuevo en un momento.';
    final traza = presentable?.trazaId;
    return Padding(
      padding: const EdgeInsets.all(Espacio.s5),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            sinConexion ? Icons.wifi_off_outlined : Icons.error_outline,
            size: Espacio.s7,
            color: t.errTexto,
          ),
          const SizedBox(height: Espacio.s3),
          Text(
            mensaje,
            textAlign: TextAlign.center,
            style: TextStyle(color: t.text),
          ),
          if (traza != null) ...[
            const SizedBox(height: Espacio.s2),
            Text(
              'Código de seguimiento: $traza',
              style: Theme.of(
                context,
              ).textTheme.bodySmall?.copyWith(color: t.text3),
            ),
          ],
          const SizedBox(height: Espacio.s4),
          FilledButton(
            onPressed: reintentar,
            style: FilledButton.styleFrom(
              minimumSize: const Size(Tactil.minimo * 3, Tactil.minimo),
              backgroundColor: t.verdeSolido,
              foregroundColor: t.sobreVerdeSolido,
            ),
            child: const Text('Volver a intentar'),
          ),
        ],
      ),
    );
  }
}
