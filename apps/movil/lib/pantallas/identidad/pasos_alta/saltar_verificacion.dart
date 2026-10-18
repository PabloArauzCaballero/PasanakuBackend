import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos_del_alta.dart';

/// La verificación de identidad no es obligatoria para crear la cuenta: quien no
/// quiere hacerla ahora la salta y la completa cuando quiera mover dinero o entrar a
/// un pasanaku. Va arriba del paso del carnet, antes de las fotos, para que se vea
/// antes de empezar lo más largo del registro.
class SaltarVerificacion extends StatelessWidget {
  const SaltarVerificacion({super.key, required this.onSaltar});
  final VoidCallback onSaltar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.md),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(TextosDelAlta.saltarTitulo, style: Tipo.cuerpoFuerte),
          const SizedBox(height: Espacio.s1),
          Text(
            TextosDelAlta.saltarDetalle,
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s3),
          Boton(
            key: const ValueKey('saltar-verificacion'),
            texto: TextosDelAlta.saltarBoton,
            variante: BotonVariante.secundario,
            expandido: true,
            onPressed: onSaltar,
          ),
        ],
      ),
    );
  }
}
