import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/barra_de_etapa.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/cifras_de_etapa.dart';
import 'textos_de_etapas.dart';

/// La etapa actual con su barra hacia la siguiente, tal como la calcula `transparencia`
/// (`consultarPuntaje.etapa`). La app no recalcula el avance: lo dibuja.
class TarjetaDeEtapa extends StatelessWidget {
  const TarjetaDeEtapa({
    super.key,
    required this.etapa,
    required this.puntaje,
    this.sinHistorial = false,
    this.verCamino,
  });

  final EtapaDelPuntaje etapa;

  /// El puntaje como lo da el contrato; sin historial no se muestra (nunca un «0»).
  final String puntaje;
  final bool sinHistorial;
  final VoidCallback? verCamino;

  @override
  Widget build(BuildContext context) {
    final siguiente = etapa.siguiente;
    final faltan = siguiente == null
        ? null
        : centesimosQueFaltan(puntaje: puntaje, desde: siguiente.desde);
    final enteros = centesimosDe(puntaje);
    // La de arriba no trae su orden: es la contigua (los tramos del catálogo no
    // tienen huecos), así que su número es el de la actual más uno.
    final ordenSiguiente = siguiente == null ? null : etapa.orden + 1;
    final haciaArriba = siguiente == null
        ? null
        : TextosDeEtapas.rotulo(ordenSiguiente!, siguiente.nombre);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          BarraDeEtapa(
            nombre: TextosDeEtapas.rotulo(etapa.orden, etapa.nombre),
            orden: etapa.orden,
            avance: diezmilesimosDe(etapa.progreso) ?? 0,
            subtitulo: sinHistorial || enteros == null
                ? TextosDeEtapas.tuEtapa
                : TextosDeEtapas.tuPuntaje(puntosLegibles(enteros)),
            etiquetaDeAvance: siguiente == null
                ? TextosDeEtapas.avanceEnLaUltima
                : TextosDeEtapas.avanceHacia(
                    TextosDeEtapas.rotuloAccesible(
                      ordenSiguiente!,
                      siguiente.nombre,
                    ),
                  ),
            ordenSiguiente: ordenSiguiente,
            textoDeAvance: siguiente == null
                ? TextosDeEtapas.llegasteArriba
                : sinHistorial
                ? TextosDeEtapas.recienEmpezas
                : faltan == null
                ? null
                : TextosDeEtapas.faltanPuntos(
                    puntosLegibles(faltan),
                    haciaArriba!,
                  ),
          ),
          if (verCamino != null) ...[
            const SizedBox(height: Espacio.s3),
            Boton(
              texto: TextosDeEtapas.verCamino,
              variante: BotonVariante.secundario,
              expandido: true,
              onPressed: verCamino,
            ),
          ],
        ],
      ),
    );
  }
}
