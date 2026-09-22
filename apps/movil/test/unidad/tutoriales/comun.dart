import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';

/// El andamiaje de las pruebas de tutoriales: tutoriales de mentira, cortos y claros.
TutorialDefinicion tutorialDePrueba(
  String id, {
  String version = '1.0.0',
  String categoria = 'Pruebas',
  String? ruta,
  int pasos = 2,
  bool obligatorio = false,
  List<String> requisitos = const [],
  String? siguiente,
  Set<Capacidad> requiere = const {},
}) => TutorialDefinicion(
  id: id,
  version: version,
  titulo: 'Tutorial $id',
  descripcion: 'Descripción de $id',
  categoria: categoria,
  dificultad: Dificultad.inicial,
  ruta: ruta,
  minutos: 3,
  obligatorio: obligatorio,
  requisitos: requisitos,
  siguiente: siguiente,
  requiere: requiere,
  pasos: [
    for (var i = 1; i <= pasos; i++)
      PasoDeTutorial(
        id: 'p$i',
        titulo: 'Paso $i',
        descripcion: 'Texto',
        ancla: 'ancla$i',
      ),
  ],
);

final DateTime ahoraDePrueba = DateTime.utc(2026, 9, 17, 12);
