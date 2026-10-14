import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/dominio/tutoriales/registro.dart';
import 'package:aportaya_movil/infraestructura/almacen_de_progreso_seguro.dart';
import 'package:aportaya_movil/pantallas/soporte/pantalla_centro_de_ayuda.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';
import '../comun.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/';

const _tutoriales = <TutorialDefinicion>[
  TutorialDefinicion(
    id: 'primeros-pasos',
    version: '1.0.0',
    titulo: 'Empezar en AportaYa',
    descripcion: 'Un recorrido breve para conocer la app.',
    categoria: 'Primeros pasos',
    minutos: 3,
    dificultad: Dificultad.inicial,
    obligatorio: true,
    pasos: [
      PasoDeTutorial(
        id: 'inicio',
        titulo: 'Mirá el inicio',
        descripcion: 'Acá encontrás el resumen de tu cuenta.',
        ancla: 'inicio',
      ),
    ],
  ),
  TutorialDefinicion(
    id: 'billetera',
    version: '1.0.0',
    titulo: 'Entender tu billetera',
    descripcion: 'Aprendé qué significan tus saldos.',
    categoria: 'Tu plata',
    minutos: 4,
    dificultad: Dificultad.inicial,
    obligatorio: false,
    requiere: {Capacidad.sesion},
    pasos: [
      PasoDeTutorial(
        id: 'saldo',
        titulo: 'Mirá tu saldo',
        descripcion: 'El saldo disponible y el retenido se muestran aparte.',
        ancla: 'saldo',
      ),
    ],
  ),
  TutorialDefinicion(
    id: 'grupos',
    version: '1.0.0',
    titulo: 'Participar en un grupo',
    descripcion: 'Conocé cómo se organiza un grupo de ahorro.',
    categoria: 'Grupos',
    minutos: 5,
    dificultad: Dificultad.inicial,
    obligatorio: false,
    requiere: {Capacidad.sesion},
    pasos: [
      PasoDeTutorial(
        id: 'grupo',
        titulo: 'Revisá el grupo',
        descripcion: 'Los turnos y aportes quedan a la vista.',
        ancla: 'grupo',
      ),
    ],
  ),
];

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('centro de ayuda · oscuro · 360 px · texto 130 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          catalogoDeTutorialesProvider.overrideWithValue(_tutoriales),
          registroDeTutorialesProvider.overrideWithValue(
            const RegistroDeTutoriales(_tutoriales, {Capacidad.sesion}),
          ),
          almacenDeProgresoProvider.overrideWithValue(
            AlmacenDeProgresoSeguro(AlmacenEnMemoria()),
          ),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: ThemeMode.dark,
          home: const PantallaCentroDeAyuda(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final contexto = tester.element(find.byType(PantallaCentroDeAyuda));
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.text('Ayuda'), findsOneWidget);
    expect(find.text('Empezar en AportaYa'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaCentroDeAyuda),
      matchesGoldenFile('${_evidencia}h8-ayuda-oscura-360-130-inicio.png'),
    );

    final ultimoTutorial = find.text('Participar en un grupo');
    for (
      var intento = 0;
      intento < 8 && ultimoTutorial.evaluate().isEmpty;
      intento++
    ) {
      await tester.drag(find.byType(ListView), const Offset(0, -520));
      await tester.pumpAndSettle();
    }
    expect(ultimoTutorial, findsOneWidget);
    final ultimoBoton = find.text('Comenzar').last;
    await Scrollable.ensureVisible(
      tester.element(ultimoBoton),
      alignment: 0.5,
      duration: Duration.zero,
    );
    await tester.pumpAndSettle();
    expect(tester.getRect(ultimoTutorial).top, greaterThanOrEqualTo(0));
    expect(tester.getRect(ultimoTutorial).bottom, lessThanOrEqualTo(740));
    expect(tester.getRect(ultimoBoton).top, greaterThanOrEqualTo(0));
    expect(tester.getRect(ultimoBoton).bottom, lessThanOrEqualTo(740));
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaCentroDeAyuda),
      matchesGoldenFile('${_evidencia}h8-ayuda-oscura-360-130-lista.png'),
    );
  });
}
