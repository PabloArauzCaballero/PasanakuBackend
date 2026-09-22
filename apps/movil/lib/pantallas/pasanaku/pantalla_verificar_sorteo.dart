import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu61_verificar_sorteo.dart';
import 'textos.dart';

/// CU-61 · verificación pública, sin sesión: es el enlace que exige el gate propio
/// de F5 ("el sorteo se ve verificable desde la app"). El veredicto es el que
/// calcula el servidor recomputando con el mismo átomo que sorteó — esta pantalla
/// nunca reproduce el barajado por su cuenta.
class PantallaVerificarSorteo extends ConsumerWidget {
  const PantallaVerificarSorteo({super.key, required this.sorteoId});
  final String sorteoId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final verificacion = ref.watch(verificarSorteoProvider(sorteoId));
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloVerificacion)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: EstadoDePantalla(
            valor: verificacion,
            etiquetaDeCarga: TextosPasanaku.cargando,
            mensajeVacio: 'Este sorteo todavía no tiene semilla revelada.',
            reintentar: () => ref.invalidate(verificarSorteoProvider(sorteoId)),
            exito: (v) => SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    TextosPasanaku.verificacionAyuda,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const SizedBox(height: Espacio.s4),
                  ChipEstado(
                    texto: v.verifica
                        ? TextosPasanaku.coincideSi
                        : TextosPasanaku.coincideNo,
                    tono: v.verifica ? Tono.ok : Tono.error,
                    icono: v.verifica ? Icons.verified : Icons.error_outline,
                  ),
                  const SizedBox(height: Espacio.s3),
                  Text('${TextosPasanaku.hashComprometido}: ${v.hashEsperado}'),
                  Text('Hash recomputado: ${v.hashRecomputado}'),
                  Text(
                    'Orden ${v.ordenCoincide ? "coincide" : "NO coincide"}'
                    '${v.primerCupoDiscrepante != null ? " desde el cupo ${v.primerCupoDiscrepante}" : ""}',
                  ),
                  const SizedBox(height: Espacio.s3),
                  Text(
                    '${TextosPasanaku.semillaRevelada}: ${v.paquete.semilla}',
                  ),
                  Text('Método: ${v.paquete.metodo}'),
                  Text(
                    '${TextosPasanaku.ordenPublicado}: ${v.paquete.cupos.join(', ')}',
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
