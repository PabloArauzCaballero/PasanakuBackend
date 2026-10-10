import 'dart:io';

import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../textos_de_captura.dart';
import 'guia_de_encuadre.dart';
import 'riel_de_capturas.dart';

/// Un cuadro del carrusel: el título de la cara con su insignia y el marco donde va
/// la foto. La acción («Tomar foto» o «Repetir la foto») vive fuera, en
/// `PasoCapturas`: dentro del `PageView` su sombra quedaba recortada por el borde de
/// la página.
///
/// Vacío no es una caja gris con una cámara: es el **marco de encuadre** de esa cara
/// (las esquinas de un carnet o las del rostro) dibujado en el verde de la marca, con
/// el ícono de la cara y la pista de cómo sacarla. Así la persona ve qué se le pide
/// antes de abrir la cámara. Con la foto, la foto ocupa el marco.
class TarjetaDeCaptura extends StatelessWidget {
  const TarjetaDeCaptura({
    super.key,
    required this.cara,
    required this.captura,
  });

  final CaraDelCarril cara;
  final Captura? captura;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final lista = captura != null;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Espacio.s1),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
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
          const SizedBox(height: Espacio.s3),
          Expanded(
            child: DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(Radios.lg),
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [t.brandBg, t.surface],
                ),
                border: Border.all(color: t.border),
                boxShadow: [t.sombra1],
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(Radios.lg),
                child: AnimatedSwitcher(
                  duration: Movimiento.entrada,
                  switchInCurve: Movimiento.llega,
                  switchOutCurve: Movimiento.sale,
                  child: lista
                      ? _Foto(key: const ValueKey('foto'), captura: captura!)
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
    return Stack(
      fit: StackFit.expand,
      children: [
        Padding(
          padding: const EdgeInsets.all(Espacio.s3),
          child: GuiaDeEncuadre(
            esDocumento: cara.esDocumento,
            color: t.brand.withValues(alpha: 0.55),
            grosor: Borde.foco,
          ),
        ),
        Center(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: Espacio.s6),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(Espacio.s3),
                  decoration: BoxDecoration(
                    color: t.surface,
                    shape: BoxShape.circle,
                    boxShadow: [t.sombra1, t.sombra2],
                  ),
                  child: Icon(
                    iconoDeCara(cara),
                    size: Espacio.s6,
                    color: t.brandTexto,
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
          ),
        ),
      ],
    );
  }
}

class _Foto extends StatelessWidget {
  const _Foto({super.key, required this.captura});
  final Captura captura;

  @override
  Widget build(BuildContext context) {
    // Android-first (ADR-036): la ruta de una captura siempre es un archivo local,
    // nunca una URL `blob:` de la web.
    return Image.file(File(captura.ruta), fit: BoxFit.cover);
  }
}
