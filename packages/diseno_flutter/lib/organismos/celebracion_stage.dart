import 'dart:math' as math;
import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_variante.dart';
import '../atomos/movimiento.dart';
import '../atomos/numero_que_sube.dart';
import '../moleculas/insignia_tile.dart';
import '../tokens/tokens.dart';

part 'celebracion_stage_tarjeta.dart';
part 'celebracion_stage_piezas.dart';
part 'celebracion_stage_pintores.dart';

/// Escenario de celebración inmersivo para logros e insignias,
/// inspirado en la filosofía de GymSheet (`CelebrationStage`):
/// - Rayos de luz radiantes animados
/// - Chispas y partículas flotantes
/// - Tarjeta holográfica con estampilla de logro
/// - Conteo ascendente de puntos otorgados
class CelebracionStage extends StatefulWidget {
  const CelebracionStage({
    super.key,
    required this.nombre,
    required this.descripcion,
    required this.icono,
    required this.puntos,
    this.tier = InsigniaTier.oro,
    this.flavorText,
    this.eyebrow = 'LOGRO DESBLOQUEADO',
    this.onCerrar,
  });

  final String nombre;
  final String descripcion;
  final String? flavorText;
  final String eyebrow;
  final IconData icono;
  final int puntos;
  final InsigniaTier tier;
  final VoidCallback? onCerrar;

  static Future<void> mostrar(
    BuildContext context, {
    required String nombre,
    required String descripcion,
    required IconData icono,
    required int puntos,
    InsigniaTier tier = InsigniaTier.oro,
    String? flavorText,
    String eyebrow = 'LOGRO DESBLOQUEADO',
  }) => showDialog<void>(
    context: context,
    barrierDismissible: true,
    barrierColor: Paleta.ink.withValues(alpha: 0.78),
    builder: (c) => CelebracionStage(
      nombre: nombre,
      descripcion: descripcion,
      icono: icono,
      puntos: puntos,
      tier: tier,
      flavorText: flavorText,
      eyebrow: eyebrow,
      onCerrar: () => Navigator.of(c).pop(),
    ),
  );

  @override
  State<CelebracionStage> createState() => _CelebracionStageState();
}

class _CelebracionStageState extends State<CelebracionStage>
    with TickerProviderStateMixin {
  late final AnimationController _rayosCtrl;
  late final AnimationController _particulasCtrl;
  late final AnimationController _entradaCtrl;

  late final Animation<double> _escala;
  late final Animation<double> _opacidad;

  @override
  void initState() {
    super.initState();
    _rayosCtrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 12),
    )..repeat();

    _particulasCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2500),
    )..repeat();

    _entradaCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    );

    _escala = CurvedAnimation(parent: _entradaCtrl, curve: Curves.easeOutBack);
    _opacidad = CurvedAnimation(
      parent: _entradaCtrl,
      curve: const Interval(0.0, 0.4, curve: Curves.easeIn),
    );

    _entradaCtrl.forward();
  }

  @override
  void dispose() {
    _rayosCtrl.dispose();
    _particulasCtrl.dispose();
    _entradaCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final reduceMotion = reducirMovimiento(context);

    return Dialog(
      backgroundColor: Colors.transparent,
      elevation: 0,
      insetPadding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
      child: Center(
        child: SingleChildScrollView(
          child: AnimatedBuilder(
            animation: _entradaCtrl,
            builder: (context, child) {
              final s = reduceMotion ? 1.0 : _escala.value.clamp(0.0, 1.05);
              final o = reduceMotion ? 1.0 : _opacidad.value;
              return Opacity(
                opacity: o,
                child: Transform.scale(scale: s, child: child),
              );
            },
            child: Stack(
              alignment: Alignment.center,
              clipBehavior: Clip.none,
              children: [
                // Rayos de luz radiante en rotación suave
                if (!reduceMotion)
                  Positioned.fill(
                    child: AnimatedBuilder(
                      animation: _rayosCtrl,
                      builder: (context, _) => CustomPaint(
                        painter: _PintorDeRayos(
                          angulo: _rayosCtrl.value * 2 * math.pi,
                          color: widget.tier.colorPrimario.withValues(
                            alpha: 0.22,
                          ),
                        ),
                      ),
                    ),
                  ),

                // Partículas flotantes alrededor de la tarjeta
                if (!reduceMotion)
                  Positioned.fill(
                    child: AnimatedBuilder(
                      animation: _particulasCtrl,
                      builder: (context, _) => CustomPaint(
                        painter: _PintorDeParticulas(
                          progreso: _particulasCtrl.value,
                          color: widget.tier.colorPrimario,
                        ),
                      ),
                    ),
                  ),

                // Tarjeta central de la celebración
                _TarjetaCentral(celebracion: widget),

                // Estampilla superior inclinada (-6 grados)
                _EstampillaDeLogro(tier: widget.tier),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
