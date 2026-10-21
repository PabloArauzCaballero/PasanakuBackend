import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/hundido_al_tocar.dart';
import '../atomos/progreso.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

part 'insignia_tile_piezas.dart';

/// Niveles de prestigio de cada insignia o logro en la mesa de juego.
enum InsigniaTier {
  bronce(Paleta.o600, Paleta.o100),
  plata(Paleta.muted, Paleta.cloud),
  oro(Paleta.warn, Paleta.warnbg),
  diamante(Paleta.info, Paleta.infobg),
  esmeralda(Paleta.ok, Paleta.okbg);

  const InsigniaTier(this.colorPrimario, this.colorFondo);
  final Color colorPrimario;
  final Color colorFondo;
}

/// Tarjeta de insignia/trofeo con la estética y filosofía de GymSheet:
/// - Conseguida: resplandor sutil, borde con tinte metálico, chispa de celebración y puntos.
/// - En progreso: barra de avance precisa y texto explicativo del hito.
/// - Bloqueada: estado atenuado con candado y reto para desbloquear.
class InsigniaTile extends StatelessWidget {
  const InsigniaTile({
    super.key,
    required this.nombre,
    required this.descripcion,
    required this.icono,
    required this.puntos,
    this.flavorText,
    this.tier = InsigniaTier.oro,
    this.conseguida = false,
    this.progreso,
    this.progresoEtiqueta,
    this.fechaObtencion,
    this.onTap,
  });

  final String nombre;
  final String descripcion;
  final String? flavorText;
  final IconData icono;
  final int puntos;
  final InsigniaTier tier;
  final bool conseguida;
  final double? progreso;
  final String? progresoEtiqueta;
  final String? fechaObtencion;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final borderColor = conseguida
        ? tier.colorPrimario.withValues(alpha: 0.45)
        : t.border;
    final cardBg = conseguida
        ? tier.colorFondo.withValues(alpha: 0.25)
        : t.surface;

    final contenido = Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(Radios.lg),
        border: Border.all(
          color: borderColor,
          width: conseguida ? Borde.fino * 1.5 : Borde.fino,
        ),
        boxShadow: conseguida
            ? [
                BoxShadow(
                  color: tier.colorPrimario.withValues(alpha: 0.12),
                  blurRadius: 16,
                  offset: const Offset(0, 4),
                ),
              ]
            : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _circuloDelIcono(t, tier, conseguida, icono, progreso),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            nombre,
                            style: Tipo.titulo3.copyWith(
                              color: conseguida ? t.text : t.text2,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        if (conseguida)
                          _insigniaDePuntos(tier, puntos)
                        else
                          ChipEstado(
                            texto: progreso != null ? 'En curso' : 'Bloqueada',
                            tono: progreso != null ? Tono.aviso : Tono.neutro,
                          ),
                      ],
                    ),
                    const SizedBox(height: Espacio.s1),
                    Text(
                      conseguida && flavorText != null
                          ? flavorText!
                          : descripcion,
                      style: Tipo.cuerpoChico.copyWith(
                        color: conseguida ? t.text2 : t.text3,
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (!conseguida && progreso != null) ...[
            const SizedBox(height: Espacio.s3),
            Progreso(
              valor: progreso!.clamp(0.0, 1.0),
              etiqueta: 'Progreso de $nombre',
              tono: tier == InsigniaTier.esmeralda
                  ? Tono.ok
                  : tier == InsigniaTier.bronce
                  ? Tono.aviso
                  : Tono.marca,
            ),
            const SizedBox(height: Espacio.s1),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  progresoEtiqueta ??
                      '${(progreso! * 100).toInt()}% completado',
                  style: Tipo.etiqueta.copyWith(color: t.text3),
                ),
                Text(
                  '+$puntos pts al desbloquear',
                  style: Tipo.etiqueta.copyWith(
                    color: t.brand,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ],
          if (conseguida && fechaObtencion != null) ...[
            const SizedBox(height: Espacio.s2),
            Text(
              'Conseguida $fechaObtencion · Toca para celebrar',
              style: Tipo.etiqueta.copyWith(
                color: tier.colorPrimario,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ],
      ),
    );

    if (onTap == null) return contenido;

    return HundidoAlTocar(
      child: Semantics(
        button: true,
        label: '$nombre. ${conseguida ? "Conseguida." : "Bloqueada."}',
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(Radios.lg),
          child: contenido,
        ),
      ),
    );
  }
}
