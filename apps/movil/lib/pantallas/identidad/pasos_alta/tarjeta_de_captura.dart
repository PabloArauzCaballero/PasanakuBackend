import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../textos_de_captura.dart';
import 'guia_de_encuadre.dart';
import 'visor_ampliado_de_captura.dart';

/// Un cuadro del carrusel: el título de la cara con su insignia y el marco donde va
/// la foto. La acción («Tomar foto» o «Repetir la foto») vive fuera, en
/// `PasoCapturas`: dentro del `PageView` su sombra quedaba recortada por el borde de
/// la página.
///
/// Vacío no es una caja gris con una cámara: es el **marco de encuadre** de esa cara
/// (el contorno de un carnet con su esquema, o el óvalo del rostro) dibujado en
/// color neutral, con la pista de cómo sacarla debajo. Así la persona ve qué se le pide
/// antes de abrir la cámara. Con la foto, la foto ocupa el marco.
class TarjetaDeCaptura extends StatelessWidget {
  const TarjetaDeCaptura({
    super.key,
    required this.cara,
    required this.captura,
    this.mostrarEncabezado = true,
  });

  final CaraDelCarril cara;
  final Captura? captura;
  final bool mostrarEncabezado;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final lista = captura != null;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Espacio.s1),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Visibility(
            visible: mostrarEncabezado,
            maintainSize: true,
            maintainAnimation: true,
            maintainState: true,
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    tituloDeCaptura(cara),
                    style: Tipo.titulo3.copyWith(color: t.text),
                  ),
                ),
                ChipEstado(
                  texto: lista
                      ? TextosDeCaptura.listo
                      : TextosDeCaptura.pendiente,
                  tono: lista ? Tono.ok : Tono.neutro,
                ),
              ],
            ),
          ),
          const SizedBox(height: Espacio.s3),
          Expanded(
            child: DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(Radios.lg),
                color: t.surface,
                border: Border.all(color: t.border),
                boxShadow: Elevacion.tarjeta(t),
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(Radios.lg),
                child: AnimatedSwitcher(
                  duration: duracion(context, Movimiento.popDeCaptura),
                  switchInCurve: Movimiento.llega,
                  switchOutCurve: Movimiento.sale,
                  transitionBuilder: (hijo, animacion) => ScaleTransition(
                    scale: Tween<double>(begin: 0.96, end: 1).animate(
                      CurvedAnimation(
                        parent: animacion,
                        curve: Movimiento.llega,
                      ),
                    ),
                    child: FadeTransition(opacity: animacion, child: hijo),
                  ),
                  child: lista
                      ? _Foto(
                          key: ValueKey('foto-${captura!.sha256Corto}'),
                          cara: cara,
                          captura: captura!,
                        )
                      : _Marco(key: const ValueKey('marco'), cara: cara),
                ),
              ),
            ),
          ),
          if (lista) ...[
            const SizedBox(height: Espacio.s2),
            Text(
              '${captura!.kilobytes} · SHA-256 ${captura!.sha256Corto}…',
              style: Tipo.ayuda.copyWith(
                color: t.text3,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _Marco extends StatelessWidget {
  const _Marco({super.key, required this.cara});
  final CaraDelCarril cara;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        children: [
          Expanded(
            child: GuiaDeEncuadre(
              esDocumento: cara.esDocumento,
              color: t.text2,
              grosor: Borde.foco,
              esquemaDe: cara,
            ),
          ),
          const SizedBox(height: Espacio.s3),
          Text(
            TextosDeCaptura.pista(cara.valorApi),
            textAlign: TextAlign.center,
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
        ],
      ),
    );
  }
}

class _Foto extends StatelessWidget {
  const _Foto({super.key, required this.cara, required this.captura});
  final CaraDelCarril cara;
  final Captura captura;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Ampliar ${tituloDeCaptura(cara)}',
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: () => mostrarCapturaAmpliada(context, captura),
        child: Hero(
          tag: etiquetaHeroDeCaptura(captura),
          child: imagenDeCaptura(context, captura, fit: BoxFit.cover),
        ),
      ),
    );
  }
}
