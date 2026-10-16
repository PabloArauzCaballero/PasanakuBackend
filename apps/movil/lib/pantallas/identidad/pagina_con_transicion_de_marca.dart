import 'package:aportaya_diseno/moviles/transicion_de_marca.dart';
import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

Page<void> paginaConTransicionDeMarca(GoRouterState state, Widget pantalla) =>
    CustomTransitionPage<void>(
      key: state.pageKey,
      child: pantalla,
      transitionDuration: TransicionDeMarca.duracion,
      reverseTransitionDuration: TransicionDeMarca.duracionDeVuelta,
      transitionsBuilder: TransicionDeMarca.construir,
    );
