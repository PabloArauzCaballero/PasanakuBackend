import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter_test/flutter_test.dart';

import 'banco.dart';

const _tresPasos = TutorialDefinicion(
  id: 'tres',
  version: '1.0.0',
  titulo: 'Tres pasos',
  descripcion: 'd',
  categoria: 'Pruebas',
  ruta: '/uno',
  minutos: 2,
  dificultad: Dificultad.inicial,
  pasos: [
    PasoDeTutorial(
      id: 'p1',
      titulo: 'Primero esto',
      descripcion: 'Mirá arriba',
      ancla: 'uno.uno',
    ),
    PasoDeTutorial(
      id: 'p2',
      titulo: 'Después esto',
      descripcion: 'Mirá abajo',
      ancla: 'uno.dos',
    ),
    PasoDeTutorial(
      id: 'p3',
      titulo: 'Y listo',
      descripcion: 'Terminaste',
      ancla: 'uno.uno',
    ),
  ],
);

const _viaja = TutorialDefinicion(
  id: 'viaja',
  version: '1.0.0',
  titulo: 'Cruza pantallas',
  descripcion: 'd',
  categoria: 'Pruebas',
  ruta: '/uno',
  minutos: 2,
  dificultad: Dificultad.inicial,
  pasos: [
    PasoDeTutorial(
      id: 'p1',
      titulo: 'Acá',
      descripcion: 'd',
      ancla: 'uno.uno',
      ruta: '/uno',
    ),
    PasoDeTutorial(
      id: 'p2',
      titulo: 'Allá',
      descripcion: 'd',
      ancla: 'dos.uno',
      ruta: '/dos',
    ),
  ],
);

const _fantasma = TutorialDefinicion(
  id: 'fantasma',
  version: '1.0.0',
  titulo: 'Señala lo que no está',
  descripcion: 'd',
  categoria: 'Pruebas',
  minutos: 2,
  dificultad: Dificultad.inicial,
  pasos: [
    PasoDeTutorial(
      id: 'p1',
      titulo: 'No está',
      descripcion: 'd',
      ancla: 'uno.tardio',
      espera: Duration(milliseconds: 120),
    ),
  ],
);

void main() {
  testWidgets('iniciar muestra el primer paso, su cuenta y resalta el ancla', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: const [_tresPasos]);
    await tester.pumpWidget(banco.montar());
    final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);

    await correr(tester, () => motor.iniciar('tres'));

    expect(find.text('Primero esto'), findsOneWidget);
    expect(find.text('Paso 1 de 3'), findsOneWidget);
    expect(
      banco.contenedor.read(motorDeTutorialesProvider).recuadro,
      isNotNull,
    );
  });

  testWidgets(
    'avanza con el botón, retrocede, y no pasa del primero hacia atrás',
    (tester) async {
      final banco = BancoDeTutoriales(catalogo: const [_tresPasos]);
      await tester.pumpWidget(banco.montar());
      final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);
      await correr(tester, () => motor.iniciar('tres'));

      await tester.tap(find.text('Siguiente'));
      await asentarTutorial(tester);
      expect(find.text('Después esto'), findsOneWidget);

      await tester.tap(find.text('Atrás'));
      await asentarTutorial(tester);
      expect(find.text('Primero esto'), findsOneWidget);

      await correr(tester, motor.retroceder);
      expect(banco.contenedor.read(motorDeTutorialesProvider).indice, 0);
    },
  );

  testWidgets('el último paso dice Terminar, completa y queda guardado', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: const [_tresPasos]);
    await tester.pumpWidget(banco.montar());
    final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);
    await correr(tester, () => motor.iniciar('tres'));
    await correr(tester, motor.avanzar);
    await correr(tester, motor.avanzar);

    expect(find.text('Terminar'), findsOneWidget);
    await tester.tap(find.text('Terminar'));
    await asentarTutorial(tester);

    expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isFalse);
    final guardado = (await banco.almacen.leer()).single;
    expect(guardado.estado, EstadoDeProgreso.completado);
    expect(guardado.repeticiones, 1);
  });

  testWidgets('el primer paso hereda la ruta del tutorial', (tester) async {
    // `_viaja` declara `ruta: '/uno'`; se arranca estando en `/dos`.
    final banco = BancoDeTutoriales(catalogo: const [_viaja], inicial: '/dos');
    await tester.pumpWidget(banco.montar());
    final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);

    await correr(tester, () => motor.iniciar('viaja'));

    expect(banco.enrutador.state.uri.path, '/uno');
    expect(banco.contenedor.read(motorDeTutorialesProvider).problema, isNull);
  });

  testWidgets('un paso de otra pantalla navega solo', (tester) async {
    final banco = BancoDeTutoriales(catalogo: const [_viaja]);
    await tester.pumpWidget(banco.montar());
    final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);
    await correr(tester, () => motor.iniciar('viaja'));
    expect(banco.enrutador.state.uri.path, '/uno');

    await correr(tester, motor.avanzar);
    expect(banco.enrutador.state.uri.path, '/dos');
    expect(find.text('Allá'), findsOneWidget);
  });

  testWidgets(
    'un ancla que no aparece deja el problema a la vista y NO traba',
    (tester) async {
      final banco = BancoDeTutoriales(catalogo: const [_fantasma]);
      await tester.pumpWidget(banco.montar());
      final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);

      await correr(tester, () => motor.iniciar('fantasma'));

      expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isTrue);
      expect(find.textContaining('No encontramos'), findsOneWidget);
      expect(find.text('Reintentar'), findsOneWidget);
    },
  );

  testWidgets('reintentar encuentra el ancla que llegó después', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: const [_fantasma]);
    await tester.pumpWidget(banco.montar());
    final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);
    await correr(tester, () => motor.iniciar('fantasma'));
    expect(find.textContaining('No encontramos'), findsOneWidget);

    banco.mostrarTardio();
    await tester.pump();
    await correr(tester, motor.reintentar);

    expect(find.textContaining('No encontramos'), findsNothing);
    expect(
      banco.contenedor.read(motorDeTutorialesProvider).recuadro,
      isNotNull,
    );
  });

  testWidgets('salir en el primer paso cierra; a la mitad pregunta antes', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: const [_tresPasos]);
    await tester.pumpWidget(banco.montar());
    final motor = banco.contenedor.read(motorDeTutorialesProvider.notifier);

    await correr(tester, () => motor.iniciar('tres'));
    motor.pedirSalida();
    await asentarTutorial(tester);
    expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isFalse);

    await correr(tester, () => motor.iniciar('tres'));
    await correr(tester, motor.avanzar);
    motor.pedirSalida();
    await asentarTutorial(tester);
    expect(find.text('¿Dejamos el tutorial acá?'), findsOneWidget);

    await tester.tap(find.text('Dejarlo por ahora'));
    await asentarTutorial(tester);
    final guardado = (await banco.almacen.leer()).single;
    expect(guardado.estado, EstadoDeProgreso.omitido);
    expect(guardado.indice, 1);
  });
}
