import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moleculas/fila_de_dato.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu61_verificar_sorteo.dart';
import 'textos.dart';
import 'textos_de_verificacion_del_grupo.dart';

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
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosPasanaku.tituloVerificacion,
        cuerpo: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.all(Espacio.s4),
            child: EstadoDePantalla(
              valor: verificacion,
              etiquetaDeCarga: TextosPasanaku.cargando,
              mensajeVacio: 'Este sorteo todavía no tiene semilla revelada.',
              reintentar: () =>
                  ref.invalidate(verificarSorteoProvider(sorteoId)),
              exito: (v) => SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const IntroDePantalla(
                      icono: IconosDeMarca.verificada,
                      titulo: TextosDeVerificacionDelGrupo.sorteoTitulo,
                      explicacion: TextosPasanaku.verificacionAyuda,
                    ),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: ChipEstado(
                        texto: v.verifica
                            ? TextosPasanaku.coincideSi
                            : TextosPasanaku.coincideNo,
                        tono: v.verifica ? Tono.ok : Tono.error,
                        icono: v.verifica
                            ? Icons.verified
                            : Icons.error_outline,
                      ),
                    ),
                    const SizedBox(height: Espacio.s4),
                    Text(
                      TextosDeVerificacionDelGrupo.datosParaReproducir,
                      style: Tipo.titulo3.copyWith(
                        color: Tokens.of(context).text,
                      ),
                    ),
                    const SizedBox(height: Espacio.s2),
                    Tarjeta(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          FilaDeDato(
                            etiqueta: TextosPasanaku.hashComprometido,
                            valor: v.hashEsperado,
                          ),
                          FilaDeDato(
                            etiqueta:
                                TextosDeVerificacionDelGrupo.hashRecomputado,
                            valor: v.hashRecomputado,
                          ),
                          FilaDeDato(
                            etiqueta: TextosPasanaku.semillaRevelada,
                            valor: v.paquete.semilla,
                          ),
                          FilaDeDato(
                            etiqueta: TextosDeVerificacionDelGrupo.metodo,
                            valor: v.paquete.metodo,
                          ),
                          FilaDeDato(
                            etiqueta: TextosPasanaku.ordenPublicado,
                            valor: v.paquete.cupos.join(', '),
                          ),
                          FilaDeDato(
                            etiqueta:
                                TextosDeVerificacionDelGrupo.ordenReproducido,
                            valor: v.ordenCoincide
                                ? TextosDeVerificacionDelGrupo.ordenCoincide
                                : TextosDeVerificacionDelGrupo.ordenNoCoincide(
                                    v.primerCupoDiscrepante,
                                  ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
