import 'dart:async';

import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/moviles/alcance_haptica.dart';
import 'package:aportaya_diseno/moviles/fondo_de_marca.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'lamina_del_tour.dart';
import 'laminas_del_tour.dart';
import 'textos.dart';

/// El tour de bienvenida (D-8 de la maqueta, `identidad/tour`): cuatro láminas,
/// saltable, y la última abre la cuenta.
///
/// Se pasa con el dedo o con «Siguiente». El deslizamiento rebota en los extremos
/// —el mismo resorte de las listas de iOS— y cada cambio de lámina da un golpecito
/// háptico, así el gesto se siente en el dedo además de verse.
class PantallaDeTour extends StatefulWidget {
  const PantallaDeTour({super.key});

  @override
  State<PantallaDeTour> createState() => _PantallaDeTourState();
}

class _PantallaDeTourState extends State<PantallaDeTour> {
  final _paginas = PageController();
  int _actual = 0;

  bool get _enLaUltima => _actual == laminasDelTour.length - 1;

  /// La posición fraccionaria mientras el dedo arrastra; antes del primer cuadro
  /// el controlador todavía no tiene medidas y se usa la página entera.
  double get _pagina => _paginas.hasClients && _paginas.position.haveDimensions
      ? (_paginas.page ?? _actual.toDouble())
      : _actual.toDouble();

  @override
  void dispose() {
    _paginas.dispose();
    super.dispose();
  }

  void _siguiente() {
    if (_enLaUltima) {
      context.push('/registro');
      return;
    }
    if (reducirMovimiento(context)) {
      _paginas.jumpToPage(_actual + 1);
      return;
    }
    _paginas.nextPage(
      duration: Movimiento.pagina,
      curve: Curves.easeInOutCubicEmphasized,
    );
  }

  void _volver() => context.canPop() ? context.pop() : context.go('/portada');

  @override
  Widget build(BuildContext context) {
    final duracionAnimacion = duracion(context, Movimiento.entrada);
    return FondoDeMarca(
      hijo: Scaffold(
        backgroundColor: Colors.transparent,
        body: SafeArea(
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  Espacio.s2,
                  Espacio.s2,
                  0,
                ),
                child: Row(
                  children: [
                    BotonDeCabecera(
                      icono: Icons.arrow_back,
                      etiqueta: 'Volver',
                      onTap: _volver,
                    ),
                    const Spacer(),
                    // Se desvanece en la última lámina en vez de desaparecer de
                    // golpe: la cabecera no salta mientras el dedo todavía está.
                    AnimatedOpacity(
                      opacity: _enLaUltima ? 0 : 1,
                      duration: duracionAnimacion,
                      child: TextButton(
                        onPressed: _enLaUltima
                            ? null
                            : () => context.push('/registro'),
                        child: ExcludeSemantics(
                          excluding: _enLaUltima,
                          child: const Text(TextosIdentidad.portadaSaltar),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: PageView.builder(
                  controller: _paginas,
                  physics: const PageScrollPhysics(
                    parent: BouncingScrollPhysics(),
                  ),
                  itemCount: laminasDelTour.length,
                  onPageChanged: (i) {
                    unawaited(AlcanceHaptica.maybeOf(context)?.seleccion());
                    setState(() => _actual = i);
                  },
                  itemBuilder: (context, i) => AnimatedBuilder(
                    animation: _paginas,
                    builder: (context, _) => LaminaDelTour(
                      lamina: laminasDelTour[i],
                      desplazamiento: _pagina - i,
                    ),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  Espacio.s3,
                  Espacio.s4,
                  Espacio.s4,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    BarraDePuntos(
                      total: laminasDelTour.length,
                      actual: _actual,
                    ),
                    const SizedBox(height: Espacio.s5),
                    SizedBox(
                      width: double.infinity,
                      child: AnimatedSwitcher(
                        duration: duracionAnimacion,
                        transitionBuilder: (child, animation) {
                          final opacidad = animation.drive(
                            CurveTween(curve: Movimiento.llega),
                          );
                          final desplazamiento = animation.drive(
                            Tween<double>(
                              begin: 8,
                              end: 0,
                            ).chain(CurveTween(curve: Movimiento.llega)),
                          );
                          return FadeTransition(
                            opacity: opacidad,
                            child: AnimatedBuilder(
                              animation: desplazamiento,
                              child: child,
                              builder: (context, child) => Transform.translate(
                                offset: Offset(0, desplazamiento.value),
                                child: child,
                              ),
                            ),
                          );
                        },
                        child: Boton(
                          key: ValueKey(
                            _enLaUltima
                                ? TextosIdentidad.tourEmpezar
                                : TextosIdentidad.tourSiguiente,
                          ),
                          texto: _enLaUltima
                              ? TextosIdentidad.tourEmpezar
                              : TextosIdentidad.tourSiguiente,
                          icono: Icons.arrow_forward,
                          variante: BotonVariante.primario,
                          expandido: true,
                          onPressed: _siguiente,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
