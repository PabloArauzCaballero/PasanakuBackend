import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../unidad/tutoriales/comun.dart';
import 'banco.dart';

/// La guía de inicio contra el motor real y las anclas reales del banco: el lanzador
/// decide con el avance guardado, y nunca bloquea ni se repite.
TutorialDefinicion _intro() => const TutorialDefinicion(
  id: guiaDeInicioId,
  version: '1.0.0',
  titulo: 'Guía de inicio',
  descripcion: 'd',
  categoria: 'Pruebas',
  ruta: '/uno',
  minutos: 1,
  dificultad: Dificultad.inicial,
  pasos: [
    PasoDeTutorial(
      id: 'p1',
      titulo: 'Primero',
      descripcion: 'Mirá arriba',
      ancla: 'uno.uno',
    ),
    PasoDeTutorial(
      id: 'p2',
      titulo: 'Segundo',
      descripcion: 'Mirá abajo',
      ancla: 'uno.dos',
    ),
  ],
);

ProgresoDeTutorial _progreso(EstadoDeProgreso estado) => ProgresoDeTutorial(
  tutorialId: guiaDeInicioId,
  version: '1.0.0',
  estado: estado,
  indice: 0,
  iniciadoEn: ahoraDePrueba,
  ultimaInteraccion: ahoraDePrueba,
);

void main() {
  testWidgets('correcto: primera vez, arranca la guía y deja su avance', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: [_intro()]);
    await tester.pumpWidget(banco.montar());
    await tester.pump();

    late Future<bool> lanzo;
    await correr(
      tester,
      () => lanzo = banco.contenedor.read(autolanzarGuiaDeInicioProvider)(),
    );
    expect(await lanzo, isTrue);
    expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isTrue);
    expect((await banco.almacen.leer()).single.tutorialId, guiaDeInicioId);
  });

  testWidgets('límite: montar el inicio dos veces no la lanza dos veces', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: [_intro()]);
    await tester.pumpWidget(banco.montar());
    await tester.pump();

    final lanzar = banco.contenedor.read(autolanzarGuiaDeInicioProvider);
    late Future<bool> primera;
    await correr(tester, () => primera = lanzar());
    expect(await primera, isTrue);
    expect(await lanzar(), isFalse);
  });

  for (final estado in [
    EstadoDeProgreso.completado,
    EstadoDeProgreso.omitido,
  ]) {
    testWidgets('límite: ya estaba $estado, no se vuelve a ofrecer', (
      tester,
    ) async {
      final banco = BancoDeTutoriales(catalogo: [_intro()]);
      await banco.almacen.guardar(_progreso(estado));
      await tester.pumpWidget(banco.montar());
      await tester.pump();

      final lanzo = await banco.contenedor.read(
        autolanzarGuiaDeInicioProvider,
      )();
      expect(lanzo, isFalse);
      expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isFalse);
    });
  }

  testWidgets('inválido: sin la guía en el catálogo no hace nada ni falla', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: [tutorialDePrueba('otro')]);
    await tester.pumpWidget(banco.montar());
    await tester.pump();

    final lanzo = await banco.contenedor.read(autolanzarGuiaDeInicioProvider)();
    expect(lanzo, isFalse);
    expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isFalse);
  });
}
