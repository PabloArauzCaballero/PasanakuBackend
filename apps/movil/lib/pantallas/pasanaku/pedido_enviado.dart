import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// «Tu pedido de cupo» (D-15): el pedido salió y está en espera. Pedir no ocupa el cupo, y
/// la pantalla lo dice una vez, con las etapas, y debajo lo que la persona suele preguntar.
class PedidoEnviado extends StatelessWidget {
  const PedidoEnviado({super.key, required this.motivos});

  /// Por qué el emparejamiento propuso ese puntaje; vacío si no hay.
  final List<String> motivos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SizedBox(height: Espacio.s4),
          const EstadoDeTramite(
            icono: IconosDeMarca.enEspera,
            tono: Tono.info,
            titulo: TextosPasanaku.pedidoEnviadoTitulo,
            detalle: TextosPasanaku.cupoNoOcupadoAviso,
            pasos: TextosPasanaku.pasosDelPedido,
            pasoActual: 1,
          ),
          const SizedBox(height: Espacio.s5),
          Tarjeta(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                for (final (i, texto) in const [
                  TextosPasanaku.quienDecide,
                  TextosPasanaku.enCuanto,
                  TextosPasanaku.queVeDeVos,
                  TextosPasanaku.tresSalidas,
                ].indexed) ...[
                  if (i > 0) const SizedBox(height: Espacio.s3),
                  Text(texto, style: Tipo.cuerpo.copyWith(color: t.text2)),
                ],
              ],
            ),
          ),
          if (motivos.isNotEmpty) ...[
            const SizedBox(height: Espacio.s5),
            Text(
              TextosPasanaku.puntajeTitulo,
              style: Tipo.titulo3.copyWith(color: t.text),
            ),
            const SizedBox(height: Espacio.s2),
            for (final m in motivos)
              Padding(
                padding: const EdgeInsets.only(bottom: Espacio.s1),
                child: Text(
                  '• $m',
                  style: Tipo.cuerpo.copyWith(color: t.text2),
                ),
              ),
          ],
          const SizedBox(height: Espacio.s5),
        ],
      ),
    );
  }
}
