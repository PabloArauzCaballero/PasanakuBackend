import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_tamano.dart';
import '../atomos/boton_variante.dart';
import '../tokens/tokens.dart';

/// Panel inferior con asa y esquinas `Radios.xl`. Para confirmar una operación de
/// dinero: dice **qué** y **por cuánto**, y el botón de confirmar es el único naranja.
class HojaDeConfirmacion extends StatelessWidget {
  const HojaDeConfirmacion({
    super.key,
    required this.titulo,
    required this.detalle,
    required this.textoConfirmar,
    required this.onConfirmar,
    this.onCancelar,
    this.cargando = false,
    this.peligro = false,
  });
  final String titulo;
  final Widget detalle;
  final String textoConfirmar;
  final VoidCallback? onConfirmar;
  final VoidCallback? onCancelar;
  final bool cargando;
  final bool peligro;

  static Future<T?> mostrar<T>(BuildContext context, {required Widget hoja}) =>
      showModalBottomSheet<T>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (_) => hoja,
      );

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Container(
      padding: const EdgeInsets.fromLTRB(
        Espacio.s5,
        Espacio.s3,
        Espacio.s5,
        Espacio.s5,
      ),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(Radios.xl),
        ),
        boxShadow: [t.sombra3],
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: Espacio.s7,
                height: Espacio.s1,
                decoration: BoxDecoration(
                  color: t.border,
                  borderRadius: BorderRadius.circular(Radios.pill),
                ),
              ),
            ),
            const SizedBox(height: Espacio.s4),
            Semantics(
              header: true,
              child: Text(
                titulo,
                style: texto.titleLarge?.copyWith(
                  fontFamily: Fuente.display,
                  color: t.text,
                ),
              ),
            ),
            const SizedBox(height: Espacio.s3),
            detalle,
            const SizedBox(height: Espacio.s5),
            Boton(
              texto: textoConfirmar,
              variante: peligro
                  ? BotonVariante.peligro
                  : BotonVariante.primario,
              onPressed: onConfirmar,
              cargando: cargando,
              expandido: true,
              tamano: BotonTamano.lg,
            ),
            const SizedBox(height: Espacio.s2),
            Boton(
              texto: 'Cancelar',
              variante: BotonVariante.enlace,
              onPressed: cargando
                  ? null
                  : (onCancelar ?? () => Navigator.of(context).maybePop()),
              expandido: true,
            ),
          ],
        ),
      ),
    );
  }
}
