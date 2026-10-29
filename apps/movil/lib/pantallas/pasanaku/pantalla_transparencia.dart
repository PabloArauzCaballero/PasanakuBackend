import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
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

import 'dominio/cu73_verificar_transparencia.dart';
import 'textos.dart';
import 'textos_de_verificacion_del_grupo.dart';

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
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosPasanaku.tituloTransparencia,
        cuerpo: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.all(Espacio.s4),
            child: EstadoDePantalla<SalidaVerificacionCadena>(
              valor: verificacion,
              etiquetaDeCarga: TextosPasanaku.cargando,
              mensajeVacio: 'Este grupo todavía no tiene bloques sellados.',
              vacio: (v) => v.bloquesVerificados == 0,
              reintentar: () =>
                  ref.invalidate(verificarCadenaProvider(grupoId)),
              exito: (v) => _Resultado(v: v),
            ),
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
    final fallido = v.componenteFallido;
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const IntroDePantalla(
            icono: IconosDeMarca.custodia,
            titulo: TextosDeVerificacionDelGrupo.transparenciaTitulo,
            explicacion: TextosDeVerificacionDelGrupo.transparenciaExplicacion,
          ),
          Align(
            alignment: Alignment.centerLeft,
            child: ChipEstado(
              texto: v.integra
                  ? TextosPasanaku.cadenaVerificada
                  : TextosPasanaku.cadenaRota,
              tono: v.integra ? Tono.ok : Tono.error,
              icono: v.integra ? Icons.verified : Icons.error_outline,
            ),
          ),
          const SizedBox(height: Espacio.s4),
          Tarjeta(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                FilaDeDato(
                  etiqueta: TextosDeVerificacionDelGrupo.bloquesVerificados,
                  valor: '${v.bloquesVerificados}',
                ),
                if (!v.integra && v.primerBloqueFallido != null)
                  FilaDeDato(
                    etiqueta: TextosDeVerificacionDelGrupo.fallaEn,
                    valor: TextosDeVerificacionDelGrupo.desdeBloque(
                      v.primerBloqueFallido!,
                      TextosDeVerificacionDelGrupo.componente(
                        fallido?.name ?? '',
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
