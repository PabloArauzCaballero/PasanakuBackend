import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/hundido_al_tocar.dart';
import '../atomos/progreso.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Niveles de prestigio de cada insignia o logro en la mesa de juego.
enum InsigniaTier {
  bronce(Color(0xFFCD7F32), Color(0xFFFAF0E6)),
  plata(Color(0xFF94A3B8), Color(0xFFF1F5F9)),
  oro(Color(0xFFF59E0B), Color(0xFFFEF3C7)),
  diamante(Color(0xFF0284C7), Color(0xFFE0F2FE)),
  esmeralda(Color(0xFF10B981), Color(0xFFD1FAE5));

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
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: conseguida
                      ? tier.colorPrimario.withValues(alpha: 0.18)
                      : t.surface2,
                  border: Border.all(
                    color: conseguida ? tier.colorPrimario : t.border,
                    width: 1.5,
                  ),
                ),
                child: Center(
                  child: Icon(
                    conseguida ? icono : (progreso != null ? icono : Icons.lock_outline),
                    color: conseguida ? tier.colorPrimario : t.text3,
                    size: 24,
                  ),
                ),
              ),
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
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: Espacio.s2,
                              vertical: 2,
                            ),
                            decoration: BoxDecoration(
                              color: tier.colorPrimario.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(Radios.sm),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(
                                  Icons.auto_awesome,
                                  size: 13,
                                  color: tier.colorPrimario,
                                ),
                                const SizedBox(width: 4),
                                Text(
                                  '+$puntos pts',
                                  style: Tipo.etiqueta.copyWith(
                                    color: tier.colorPrimario,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ],
                            ),
                          )
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
                Expanded(
                  child: Text(
                    progresoEtiqueta ?? '${(progreso! * 100).toInt()}% completado',
                    style: Tipo.etiqueta.copyWith(color: t.text3),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                const SizedBox(width: Espacio.s2),
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
