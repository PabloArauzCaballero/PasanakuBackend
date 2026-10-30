import 'package:flutter/material.dart';

import '../atomos/aspecto_de_arena.dart';
import '../atomos/insignia_de_arena.dart';
import '../atomos/movimiento.dart';
import '../tokens/tokens.dart';

/// La arena de hoy y cuánto falta para la siguiente, con una barra **continua**.
///
/// El avance llega en diezmilésimos enteros (0..10000), que es como el contrato lo da
/// («0.5000» → 5000): la barra no pasa por un `double` que alguien haya parseado de una
/// cadena; solo el pintor lo divide para dibujar.
///
/// La barra se llena al aparecer —contesta «¿dónde estoy?» siguiéndola con la vista— y
/// se interrumpe desde donde está si el avance cambia. Con reducir movimiento aparece ya
/// llena hasta su valor: el número de al lado dice lo mismo sin depender de la animación.
class BarraDeArena extends StatelessWidget {
  const BarraDeArena({
    super.key,
    required this.nombre,
    required this.orden,
    required this.avance,
    required this.etiquetaDeAvance,
    this.subtitulo,
    this.textoDeAvance,
    this.ordenSiguiente,
  });

  /// El nombre de la arena actual.
  final String nombre;
  final int orden;

  /// Diezmilésimos de avance dentro de la arena: 0..10000.
  final int avance;

  /// Lo que lee un lector de pantalla sobre la barra: «Avance hacia Aguayo».
  final String etiquetaDeAvance;

  /// Debajo del nombre: «Tu arena» o el puntaje.
  final String? subtitulo;

  /// Debajo de la barra: «Te faltan 25 puntos para Aguayo».
  final String? textoDeAvance;

  /// La arena de arriba; sin ella (la última), la barra no muestra insignia al final.
  final int? ordenSiguiente;

  int get _porcentaje => (avance.clamp(0, 10000) + 50) ~/ 100;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final fraccion = avance.clamp(0, 10000) / 10000;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            InsigniaDeArena(orden: orden, tamano: TamanoDeInsignia.grande),
            const SizedBox(width: Espacio.s3),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (subtitulo != null)
                    Text(
                      subtitulo!,
                      style: Tipo.cuerpoChico.copyWith(color: t.text2),
                    ),
                  Text(nombre, style: Tipo.titulo2.copyWith(color: t.text)),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: Espacio.s3),
        Row(
          children: [
            Expanded(
              child: Semantics(
                container: true,
                label: etiquetaDeAvance,
                value: '$_porcentaje por ciento',
                excludeSemantics: true,
                child: RepaintBoundary(
                  child: TweenAnimationBuilder<double>(
                    tween: Tween<double>(begin: 0, end: fraccion),
                    duration: duracion(context, Movimiento.pagina),
                    curve: Movimiento.llega,
                    builder: (context, valor, _) => CustomPaint(
                      size: const Size.fromHeight(Espacio.s3),
                      painter: PintorDeAvanceDeArena(
                        avance: valor,
                        pista: t.border,
                        relleno: AspectoDeArena.relleno(orden),
                      ),
                    ),
                  ),
                ),
              ),
            ),
            if (ordenSiguiente != null) ...[
              const SizedBox(width: Espacio.s2),
              InsigniaDeArena(
                orden: ordenSiguiente!,
                estado: EstadoDeArena.bloqueada,
              ),
            ],
          ],
        ),
        const SizedBox(height: Espacio.s2),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Text(
                textoDeAvance ?? '',
                style: Tipo.cuerpoChico.copyWith(color: t.text2),
              ),
            ),
            const SizedBox(width: Espacio.s2),
            ExcludeSemantics(
              child: Text(
                '$_porcentaje %',
                style: Tipo.cuerpoFuerte.copyWith(color: t.text),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// Pinta la pista y el tramo recorrido. Solo repinta: no relayout por cuadro.
class PintorDeAvanceDeArena extends CustomPainter {
  const PintorDeAvanceDeArena({
    required this.avance,
    required this.pista,
    required this.relleno,
  });

  /// 0..1, ya animado.
  final double avance;
  final Color pista;
  final Color relleno;

  @override
  void paint(Canvas canvas, Size size) {
    final radio = Radius.circular(size.height / 2);
    canvas.drawRRect(
      RRect.fromRectAndRadius(Offset.zero & size, radio),
      Paint()..color = pista,
    );
    final ancho = size.width * avance.clamp(0, 1);
    if (ancho <= 0) return;
    // Un avance mínimo se dibuja al menos como el extremo redondeado: un tramo más
    // angosto que su alto se vería como una mancha, no como un comienzo.
    final visible = ancho < size.height ? size.height : ancho;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(
          0,
          0,
          visible > size.width ? size.width : visible,
          size.height,
        ),
        radio,
      ),
      Paint()..color = relleno,
    );
  }

  @override
  bool shouldRepaint(PintorDeAvanceDeArena viejo) =>
      viejo.avance != avance ||
      viejo.pista != pista ||
      viejo.relleno != relleno;
}
