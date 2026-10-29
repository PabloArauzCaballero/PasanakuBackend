import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/tutoriales.dart';
import 'tarjeta_de_tutorial.dart';
import 'vista_de_tutoriales.dart';

/// Conecta una fila del catálogo con las acciones del motor de tutoriales.
class TarjetaDeTutorialEnLista extends ConsumerWidget {
  const TarjetaDeTutorialEnLista({
    super.key,
    required this.fila,
    this.destacada = false,
  });

  final TutorialEnLista fila;
  final bool destacada;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final motor = ref.read(motorDeTutorialesProvider.notifier);
    final progresos = ref.read(progresoDeTutorialesProvider.notifier);
    return TarjetaDeTutorial(
      fila: fila,
      destacada: destacada,
      alComenzar: () => motor.iniciar(
        fila.tutorial.id,
        repeticiones: fila.progreso?.repeticiones ?? 0,
      ),
      alContinuar: () => fila.progreso == null
          ? motor.iniciar(fila.tutorial.id)
          : motor.continuar(fila.tutorial.id, fila.progreso!),
      alReiniciar: () => progresos.reiniciar(fila.tutorial.id),
    );
  }
}
