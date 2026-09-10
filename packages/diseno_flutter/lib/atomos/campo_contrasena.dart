import 'package:flutter/material.dart';

import '../atomos/campo.dart';
import '../tokens/tokens.dart';

/// Contraseña con el ojo, y medidor de fortaleza opcional (CU-09).
class CampoContrasena extends StatefulWidget {
  const CampoContrasena({
    super.key,
    required this.etiqueta,
    this.controlador,
    this.error,
    this.fortaleza,
    this.onChanged,
  });

  final String etiqueta;
  final TextEditingController? controlador;
  final String? error;

  /// 0..1; `null` no muestra medidor.
  final double? fortaleza;
  final ValueChanged<String>? onChanged;

  @override
  State<CampoContrasena> createState() => _CampoContrasenaState();
}

class _CampoContrasenaState extends State<CampoContrasena> {
  bool _visible = false;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Campo(
          etiqueta: widget.etiqueta,
          controlador: widget.controlador,
          error: widget.error,
          oculto: !_visible,
          onChanged: widget.onChanged,
          sufijo: IconButton(
            tooltip: _visible ? 'Ocultar contraseña' : 'Mostrar contraseña',
            icon: Icon(_visible ? Icons.visibility_off : Icons.visibility),
            color: t.text3,
            onPressed: () => setState(() => _visible = !_visible),
          ),
        ),
        if (widget.fortaleza != null) ...[
          const SizedBox(height: Espacio.s2),
          Semantics(
            label: 'Fortaleza de la contraseña',
            value: '${(widget.fortaleza! * 100).round()} por ciento',
            child: LinearProgressIndicator(
              value: widget.fortaleza,
              minHeight: Espacio.s1,
              backgroundColor: t.surface2,
              color: widget.fortaleza! < 0.5
                  ? t.err
                  : widget.fortaleza! < 0.8
                  ? t.warn
                  : t.ok,
              borderRadius: BorderRadius.circular(Radios.pill),
            ),
          ),
        ],
      ],
    );
  }
}
