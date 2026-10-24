import 'dart:math' as math;
import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_variante.dart';
import '../atomos/movimiento.dart';
import '../atomos/numero_que_sube.dart';
import '../moleculas/insignia_tile.dart';
import '../tokens/tokens.dart';

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
    barrierColor: Colors.black.withValues(alpha: 0.78),
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
    final t = Tokens.of(context);
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
                Container(
                  constraints: const BoxConstraints(maxWidth: 380),
                  padding: const EdgeInsets.fromLTRB(
                    Espacio.s5,
                    Espacio.s7,
                    Espacio.s5,
                    Espacio.s5,
                  ),
                  decoration: BoxDecoration(
                    color: t.surface,
                    borderRadius: BorderRadius.circular(Radios.xl),
                    border: Border.all(
                      color: widget.tier.colorPrimario.withValues(alpha: 0.6),
                      width: 2,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: widget.tier.colorPrimario.withValues(
                          alpha: 0.35,
                        ),
                        blurRadius: 36,
                        spreadRadius: 4,
                        offset: const Offset(0, 8),
                      ),
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.3),
                        blurRadius: 24,
                        offset: const Offset(0, 12),
                      ),
                    ],
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      // Ícono monumental con halo
                      Stack(
                        alignment: Alignment.center,
                        children: [
                          Container(
                            width: 104,
                            height: 104,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: widget.tier.colorPrimario.withValues(
                                alpha: 0.14,
                              ),
                              border: Border.all(
                                color: widget.tier.colorPrimario.withValues(
                                  alpha: 0.35,
                                ),
                                width: 2,
                              ),
                            ),
                          ),
                          Container(
                            width: 82,
                            height: 82,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              gradient: LinearGradient(
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                                colors: [
                                  widget.tier.colorPrimario,
                                  widget.tier.colorPrimario.withValues(
                                    alpha: 0.75,
                                  ),
                                ],
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: widget.tier.colorPrimario.withValues(
                                    alpha: 0.45,
                                  ),
                                  blurRadius: 18,
                                  offset: const Offset(0, 4),
                                ),
                              ],
                            ),
                            child: Center(
                              child: Icon(
                                widget.icono,
                                color: Colors.white,
                                size: 42,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: Espacio.s4),

                      // Eyebrow
                      Text(
                        widget.eyebrow.toUpperCase(),
                        style: Tipo.etiqueta.copyWith(
                          color: widget.tier.colorPrimario,
                          letterSpacing: 2.2,
                          fontWeight: FontWeight.w800,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: Espacio.s2),

                      // Título principal
                      Text(
                        widget.nombre,
                        style: Tipo.titulo1.copyWith(
                          color: t.text,
                          fontWeight: FontWeight.w800,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: Espacio.s2),

                      // Descripción o historia del logro
                      Text(
                        widget.flavorText ?? widget.descripcion,
                        style: Tipo.cuerpo.copyWith(
                          color: t.text2,
                          height: 1.45,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: Espacio.s5),

                      // Contador de puntos con animación
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: Espacio.s4,
                          vertical: Espacio.s3,
                        ),
                        decoration: BoxDecoration(
                          color: widget.tier.colorPrimario.withValues(
                            alpha: 0.12,
                          ),
                          borderRadius: BorderRadius.circular(Radios.lg),
                          border: Border.all(
                            color: widget.tier.colorPrimario.withValues(
                              alpha: 0.3,
                            ),
                          ),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.auto_awesome,
                              color: widget.tier.colorPrimario,
                              size: 18,
                            ),
                            const SizedBox(width: Espacio.s1),
                            Text(
                              '+',
                              style: Tipo.cifra.copyWith(
                                color: widget.tier.colorPrimario,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            NumeroQueSube(
                              valor: widget.puntos,
                              etiqueta: 'Puntos ganados',
                              estilo: Tipo.cifra.copyWith(
                                color: widget.tier.colorPrimario,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const SizedBox(width: 4),
                            Flexible(
                              child: Text(
                                'pts en mesa',
                                style: Tipo.etiqueta.copyWith(
                                  color: widget.tier.colorPrimario,
                                  fontWeight: FontWeight.w700,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: Espacio.s5),

                      // Botones de acción
                      SizedBox(
                        width: double.infinity,
                        child: Boton(
                          texto: 'Continuar en el Pasanaku',
                          variante: BotonVariante.primario,
                          onPressed:
                              widget.onCerrar ??
                              () => Navigator.of(context).pop(),
                        ),
                      ),
                    ],
                  ),
                ),

                // Estampilla superior inclinada (-6 grados)
                Positioned(
                  top: -14,
                  child: Transform.rotate(
                    angle: -0.1,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: Espacio.s4,
                        vertical: Espacio.s2,
                      ),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [
                            widget.tier.colorPrimario,
                            widget.tier.colorPrimario.withValues(alpha: 0.85),
                          ],
                        ),
                        borderRadius: BorderRadius.circular(Radios.pill),
                        boxShadow: [
                          BoxShadow(
                            color: widget.tier.colorPrimario.withValues(
                              alpha: 0.5,
                            ),
                            blurRadius: 12,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Text(
                        '¡NUEVO LOGRO!',
                        style: Tipo.etiqueta.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.5,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _PintorDeRayos extends CustomPainter {
  const _PintorDeRayos({required this.angulo, required this.color});

  final double angulo;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final centro = Offset(size.width / 2, size.height / 2);
    final radio = math.max(size.width, size.height) * 0.95;
    const rayos = 16;
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    canvas.save();
    canvas.translate(centro.dx, centro.dy);
    canvas.rotate(angulo);
    const paso = math.pi / rayos;

    for (var i = 0; i < rayos; i++) {
      final theta = (i * 2 * math.pi) / rayos;
      final path = Path()
        ..moveTo(0, 0)
        ..lineTo(radio * math.cos(theta), radio * math.sin(theta))
        ..lineTo(
          radio * math.cos(theta + paso * 0.6),
          radio * math.sin(theta + paso * 0.6),
        )
        ..close();
      canvas.drawPath(path, paint);
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(_PintorDeRayos old) =>
      old.angulo != angulo || old.color != color;
}

class _PintorDeParticulas extends CustomPainter {
  const _PintorDeParticulas({required this.progreso, required this.color});

  final double progreso;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final centro = Offset(size.width / 2, size.height / 2);
    final paint = Paint()..style = PaintingStyle.fill;

    const cantidad = 24;
    for (var i = 0; i < cantidad; i++) {
      final fase = (progreso + i / cantidad) % 1.0;
      final angulo = (i * 137.5) * (math.pi / 180);
      final distancia = 90 + fase * 130;
      final x = centro.dx + math.cos(angulo) * distancia;
      final y = centro.dy + math.sin(angulo) * distancia;
      final radio = (1.0 - fase) * 3.5;
      final opacidad = (math.sin(fase * math.pi)).clamp(0.0, 1.0);

      paint.color = color.withValues(alpha: opacidad * 0.85);
      canvas.drawCircle(Offset(x, y), radio, paint);
    }
  }

  @override
  bool shouldRepaint(_PintorDeParticulas old) => old.progreso != progreso;
}
