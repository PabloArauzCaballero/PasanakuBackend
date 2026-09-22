import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Una pantalla del tour: ilustración, título, texto. Saltable (D-8).
class PantallaDeOnboarding extends StatelessWidget {
  const PantallaDeOnboarding({
    super.key,
    required this.ilustracion,
    required this.titulo,
    required this.texto,
    required this.pie,
  });
  final Widget ilustracion;
  final String titulo;
  final String texto;
  final Widget pie;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final tt = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.all(Espacio.s5),
      child: Column(
        children: [
          const Spacer(),
          SizedBox(height: Espacio.s7 * 4, child: ilustracion),
          const SizedBox(height: Espacio.s6),
          Semantics(
            header: true,
            child: Text(
              titulo,
              textAlign: TextAlign.center,
              style: tt.headlineSmall?.copyWith(
                fontFamily: Fuente.display,
                color: t.text,
              ),
            ),
          ),
          const SizedBox(height: Espacio.s3),
          Text(
            texto,
            textAlign: TextAlign.center,
            style: tt.bodyLarge?.copyWith(color: t.text2),
          ),
          const Spacer(),
          pie,
        ],
      ),
    );
  }
}
