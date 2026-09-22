import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu73_verificar_transparencia.dart';
import 'textos.dart';

/// CU-73 · verificar la cadena de transparencia del grupo. Ruta pública. Un grupo
/// sin bloques (`bloquesVerificados == 0`) NO es una cadena rota — el propio CU lo
/// aclara — así que esta pantalla los distingue con dos mensajes distintos, nunca
/// uno solo de "no hay nada".
class PantallaTransparencia extends ConsumerWidget {
  const PantallaTransparencia({super.key, required this.grupoId});
  final String grupoId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final verificacion = ref.watch(verificarCadenaProvider(grupoId));
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloTransparencia)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: EstadoDePantalla<SalidaVerificacionCadena>(
            valor: verificacion,
            etiquetaDeCarga: TextosPasanaku.cargando,
            mensajeVacio: 'Este grupo todavía no tiene bloques sellados.',
            vacio: (v) => v.bloquesVerificados == 0,
            reintentar: () => ref.invalidate(verificarCadenaProvider(grupoId)),
            exito: (v) => _Resultado(v: v),
          ),
        ),
      ),
    );
  }
}

class _Resultado extends StatelessWidget {
  const _Resultado({required this.v});
  final SalidaVerificacionCadena v;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        ChipEstado(
          texto: v.integra
              ? TextosPasanaku.cadenaVerificada
              : TextosPasanaku.cadenaRota,
          tono: v.integra ? Tono.ok : Tono.error,
        ),
        const SizedBox(height: Espacio.s3),
        Text(
          'Bloques verificados: ${v.bloquesVerificados}',
          style: texto.bodyMedium,
        ),
        if (!v.integra && v.primerBloqueFallido != null)
          Text(
            'Falla desde el bloque ${v.primerBloqueFallido} (${v.componenteFallido?.name}).',
            style: texto.bodyMedium,
          ),
      ],
    );
  }
}
