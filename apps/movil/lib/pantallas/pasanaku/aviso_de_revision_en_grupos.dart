import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/sesion.dart';
import '../identidad/boton_consultar_estado.dart';
import '../identidad/dominio/verificacion_diferida.dart';
import '../identidad/textos_de_verificacion.dart';
import 'dominio/mis_grupos.dart';

/// En la pestaña Grupos, mientras la identidad está en revisión (2026-10-19): dice
/// qué se puede hacer ya y deja preguntar si la aprobaron. Antes, tocar «Organizar
/// un grupo nuevo» llevaba a una pantalla de espera; ahora la persona ve sus grupos
/// y consulta desde acá. Con la cuenta habilitada o sin carnet enviado no dibuja nada
/// (sin carnet, el aviso de arriba ya invita a verificarse).
class AvisoDeRevisionEnGrupos extends ConsumerWidget {
  const AvisoDeRevisionEnGrupos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sesion = ref.watch(sesionProvider);
    return ListenableBuilder(
      listenable: sesion,
      builder: (context, _) {
        if (!sesion.requiereVerificacion) return const SizedBox.shrink();
        final cuenta = ref.watch(estadoDeCuentaProvider).asData?.value;
        if (cuenta != EstadoDeCuenta.enRevision &&
            cuenta != EstadoDeCuenta.verificada) {
          return const SizedBox.shrink();
        }
        final t = Tokens.of(context);
        final texto = Theme.of(context).textTheme;
        return Padding(
          padding: const EdgeInsets.only(bottom: Espacio.s4),
          child: Tarjeta(
            etiqueta: TextosDeVerificacion.cuentaEnVerificacionTitulo,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Icon(Icons.hourglass_top, color: t.brand),
                    const SizedBox(width: Espacio.s2),
                    Expanded(
                      child: Text(
                        TextosDeVerificacion.cuentaEnVerificacionTitulo,
                        style: texto.titleMedium?.copyWith(color: t.text),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: Espacio.s2),
                Text(
                  TextosDeVerificacion.enRevisionGrupos,
                  style: texto.bodyMedium?.copyWith(color: t.text2),
                ),
                const SizedBox(height: Espacio.s3),
                BotonConsultarEstado(
                  variante: BotonVariante.secundario,
                  expandido: true,
                  alActivar: () => ref.invalidate(misGruposProvider),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
