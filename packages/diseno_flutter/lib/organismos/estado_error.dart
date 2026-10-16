import 'package:flutter/material.dart';

import '../atomos/entrada_de_estado.dart';
import '../atomos/movimiento.dart';
import '../errores.dart';
import '../tokens/tokens.dart';

/// Qué pasó en lenguaje humano, un botón de reintento y la traza para soporte.
/// Sin conexión es una variante: último estado conocido, operaciones bloqueadas.
class EstadoError extends StatelessWidget {
  const EstadoError({
    super.key,
    required this.error,
    required this.reintentar,
    this.etiquetaDeTraza = 'Referencia',
  });

  final Object error;
  final VoidCallback reintentar;
  final String etiquetaDeTraza;

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
    return EntradaDeEstado(
      child: Padding(
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
                '$etiquetaDeTraza: $traza',
                style: Theme.of(
                  context,
                ).textTheme.bodySmall?.copyWith(color: t.text3),
              ),
            ],
            const SizedBox(height: Espacio.s4),
            _BotonDeReintento(onPressed: reintentar),
          ],
        ),
      ),
    );
  }
}

class _BotonDeReintento extends StatefulWidget {
  const _BotonDeReintento({required this.onPressed});

  final VoidCallback onPressed;

  @override
  State<_BotonDeReintento> createState() => _BotonDeReintentoState();
}

class _BotonDeReintentoState extends State<_BotonDeReintento>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: Movimiento.reintento,
  );

  void _tocar() {
    if (!reducirMovimiento(context)) _control.forward(from: 0);
    widget.onPressed();
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FilledButton.icon(
    onPressed: _tocar,
    style: FilledButton.styleFrom(
      minimumSize: const Size(Tactil.minimo * 3, Tactil.minimo),
      backgroundColor: Tokens.of(context).accent,
      foregroundColor: Tokens.of(context).accentInk,
    ),
    icon: RotationTransition(
      turns: Tween<double>(begin: 0, end: 1).animate(_control),
      child: const Icon(Icons.refresh_rounded),
    ),
    label: const Text('Volver a intentar'),
  );
}
