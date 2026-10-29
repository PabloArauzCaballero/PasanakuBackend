import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La entrada de una pantalla interior: el ícono en su círculo, qué se hace acá y por
/// qué, antes del formulario (ADR-053).
///
/// Las pantallas de trámite (cambiar contraseña, pedir el retiro, permutar el turno)
/// abrían directo en un campo suelto sobre el fondo: nada decía qué estaba por pasar.
/// Con [peligro] el círculo va en el tono de error, para lo que no tiene vuelta atrás
/// (dar de baja la cuenta).
class IntroDePantalla extends StatelessWidget {
  const IntroDePantalla({
    super.key,
    required this.icono,
    required this.titulo,
    required this.explicacion,
    this.peligro = false,
  });

  final IconData icono;
  final String titulo;
  final String explicacion;
  final bool peligro;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final (fondo, tinta) = peligro
        ? (t.errBg, t.errTexto)
        : (t.brandBg, t.brandTexto);
    return Padding(
      padding: const EdgeInsets.only(bottom: Espacio.s4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ExcludeSemantics(
            child: DecoratedBox(
              decoration: BoxDecoration(color: fondo, shape: BoxShape.circle),
              child: SizedBox.square(
                dimension: Tactil.minimo,
                child: Icon(icono, size: Espacio.s5, color: tinta),
              ),
            ),
          ),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Semantics(
                  header: true,
                  child: Text(
                    titulo,
                    style: Tipo.titulo3.copyWith(color: t.text),
                  ),
                ),
                const SizedBox(height: Espacio.s1),
                Text(
                  explicacion,
                  style: Tipo.cuerpoChico.copyWith(color: t.text2),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
