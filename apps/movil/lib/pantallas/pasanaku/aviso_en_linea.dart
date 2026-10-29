import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Un rechazo pegado a lo que lo causó (la tarjeta del cupo, una condición), con ícono
/// y texto: el error no se transmite solo por color. Se anuncia al aparecer.
class AvisoEnLinea extends StatelessWidget {
  const AvisoEnLinea(this.mensaje, {super.key});
  final String mensaje;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: Espacio.s2),
      child: Semantics(
        liveRegion: true,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ExcludeSemantics(
              child: Icon(
                IconosDeMarca.tramiteFallido,
                size: Espacio.s4 + Espacio.s1,
                color: t.errTexto,
              ),
            ),
            const SizedBox(width: Espacio.s2),
            Expanded(
              child: Text(
                mensaje,
                style: Tipo.cuerpoChico.copyWith(color: t.errTexto),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
