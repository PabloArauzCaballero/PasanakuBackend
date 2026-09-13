import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import 'lamina_del_tour.dart';
import 'laminas_del_tour.dart';
import 'textos.dart';

/// El tour de bienvenida (D-8 de la maqueta, `identidad/tour`): cuatro láminas,
/// saltable, y la última abre la cuenta.
///
/// Se pasa con el dedo o con «Siguiente». El deslizamiento rebota en los extremos
/// —el mismo resorte de las listas de iOS— y cada cambio de lámina da un golpecito
/// háptico, así el gesto se siente en la mano además de verse.
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
    _paginas.nextPage(
      duration: const Duration(milliseconds: 520),
      curve: Curves.easeInOutCubicEmphasized,
    );
  }

  void _volver() =>
      context.canPop() ? context.pop() : context.go('/portada');

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Scaffold(
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
                    duration: const Duration(milliseconds: 220),
                    child: TextButton(
                      onPressed: _enLaUltima
                          ? null
                          : () => context.push('/registro'),
                      child: const Text(TextosIdentidad.portadaSaltar),
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
                  HapticFeedback.selectionClick();
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
              child: AnimatedSize(
                duration: const Duration(milliseconds: 260),
                curve: Curves.easeOutCubic,
                alignment: Alignment.topCenter,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    BarraDePuntos(
                      total: laminasDelTour.length,
                      actual: _actual,
                    ),
                    const SizedBox(height: Espacio.s5),
                    Boton(
                      texto: _enLaUltima
                          ? TextosIdentidad.portadaCrearCuenta
                          : TextosIdentidad.tourSiguiente,
                      icono: _enLaUltima ? null : Icons.arrow_forward,
                      variante: BotonVariante.primario,
                      expandido: true,
                      onPressed: _siguiente,
                    ),
                    if (_enLaUltima)
                      Padding(
                        padding: const EdgeInsets.only(top: Espacio.s2),
                        child: TextButton(
                          onPressed: () => context.push('/ingreso'),
                          child: Text(
                            TextosIdentidad.portadaYaTengoCuenta,
                            style: Tipo.boton.copyWith(color: t.brandTexto),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
