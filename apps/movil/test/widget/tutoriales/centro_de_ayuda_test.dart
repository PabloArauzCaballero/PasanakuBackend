import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/tutoriales/avance.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/infraestructura/almacen_de_progreso_seguro.dart';
import 'package:aportaya_movil/pantallas/soporte/pantalla_centro_de_ayuda.dart';
import 'package:aportaya_movil/dominio/tutoriales/registro.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../comun.dart';

const _abierto = TutorialDefinicion(
  id: 'abierto',
  version: '1.0.0',
  titulo: 'Qué es AportaYa',
  descripcion: 'Una vuelta general por la app',
  categoria: 'Primeros pasos',
  minutos: 3,
  dificultad: Dificultad.inicial,
  obligatorio: true,
  pasos: [PasoDeTutorial(id: 'p1', titulo: 'a', descripcion: 'b', ancla: 'x')],
);

const _conSesion = TutorialDefinicion(
  id: 'con-sesion',
  version: '1.0.0',
  titulo: 'Entender tu saldo',
  descripcion: 'Disponible y retenido',
  categoria: 'Tu plata',
  minutos: 4,
  dificultad: Dificultad.inicial,
  requiere: {Capacidad.sesion},
  pasos: [PasoDeTutorial(id: 'p1', titulo: 'a', descripcion: 'b', ancla: 'x')],
);

Widget _centro({required ProviderContainer contenedor}) =>
    UncontrolledProviderScope(
      container: contenedor,
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const PantallaCentroDeAyuda(),
      ),
    );

ProviderContainer _contenedor(
  Set<Capacidad> capacidades,
  AlmacenDeProgresoSeguro almacen,
) => ProviderContainer(
  retry: (_, _) => null,
  overrides: [
    catalogoDeTutorialesProvider.overrideWithValue(const [
      _abierto,
      _conSesion,
    ]),
    almacenDeProgresoProvider.overrideWithValue(almacen),
    registroDeTutorialesProvider.overrideWithValue(
      RegistroDeTutoriales(const [_abierto, _conSesion], capacidades),
    ),
  ],
);

/// **Una pantalla alta a propósito.** El centro de ayuda es un `ListView`, y un
/// `ListView` solo construye lo que entra en la ventana: con los 800×600 del entorno de
/// pruebas, las tarjetas quedaban fuera del árbol y los `find` no las veían. Se agranda
/// la ventana en vez de desplazar en cada prueba.
Future<void> montarCentro(
  WidgetTester tester,
  ProviderContainer contenedor,
) async {
  tester.view.physicalSize = const Size(1200, 3600);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(_centro(contenedor: contenedor));
  await tester.pump();
}

void main() {
  testWidgets('lista solo lo que le sirve a quien mira', (tester) async {
    final contenedor = _contenedor(
      const {},
      AlmacenDeProgresoSeguro(AlmacenEnMemoria()),
    );
    await montarCentro(tester, contenedor);

    expect(find.text('Qué es AportaYa'), findsWidgets);
    expect(find.text('Entender tu saldo'), findsNothing);
  });

  testWidgets('con sesión aparece el tutorial de la billetera', (tester) async {
    final contenedor = _contenedor({
      Capacidad.sesion,
    }, AlmacenDeProgresoSeguro(AlmacenEnMemoria()));
    await montarCentro(tester, contenedor);

    expect(find.text('Entender tu saldo'), findsWidgets);
  });

  testWidgets('muestra el avance y marca lo importante y lo recomendado', (
    tester,
  ) async {
    final contenedor = _contenedor(
      const {},
      AlmacenDeProgresoSeguro(AlmacenEnMemoria()),
    );
    await montarCentro(tester, contenedor);

    expect(find.text('0 de 1 tutoriales completados'), findsOneWidget);
    expect(find.textContaining('Importante'), findsWidgets);
    expect(find.text('Te conviene empezar por acá'), findsOneWidget);
  });

  testWidgets('el buscador recorta, y el vacío explica por qué', (
    tester,
  ) async {
    final contenedor = _contenedor({
      Capacidad.sesion,
    }, AlmacenDeProgresoSeguro(AlmacenEnMemoria()));
    await montarCentro(tester, contenedor);

    await tester.enterText(find.byType(TextField).first, 'saldo');
    await tester.pump();
    // Mientras se busca, la recomendación se esconde: si no, quien busca algo que no
    // está ve igual una tarjeta y duda de si el buscador funcionó.
    expect(find.text('Qué es AportaYa'), findsNothing);
    expect(find.text('Entender tu saldo'), findsWidgets);

    await tester.enterText(find.byType(TextField).first, 'zzzzz');
    await tester.pump();
    expect(find.textContaining('Ningún tutorial coincide'), findsOneWidget);
  });

  testWidgets('lo completado se ve completado y se puede repetir', (
    tester,
  ) async {
    final almacen = AlmacenDeProgresoSeguro(AlmacenEnMemoria());
    await almacen.guardar(
      cerrado(
        progresoInicial(_abierto, DateTime.utc(2026, 9, 17)),
        EstadoDeProgreso.completado,
        DateTime.utc(2026, 9, 17),
      ),
    );
    final contenedor = _contenedor(const {}, almacen);
    await contenedor.read(progresoDeTutorialesProvider.notifier).cargar();
    await montarCentro(tester, contenedor);

    expect(find.text('Completado'), findsWidgets);
    expect(find.text('Repetir'), findsWidgets);
    expect(find.text('1 de 1 tutoriales completados'), findsOneWidget);
  });

  testWidgets('reiniciar borra el avance de ese tutorial', (tester) async {
    final almacen = AlmacenDeProgresoSeguro(AlmacenEnMemoria());
    await almacen.guardar(
      cerrado(
        progresoInicial(_abierto, DateTime.utc(2026, 9, 17)),
        EstadoDeProgreso.completado,
        DateTime.utc(2026, 9, 17),
      ),
    );
    final contenedor = _contenedor(const {}, almacen);
    await contenedor.read(progresoDeTutorialesProvider.notifier).cargar();
    await montarCentro(tester, contenedor);

    await tester.tap(find.text('Reiniciar').first);
    await tester.pump();
    await tester.pump();

    expect(contenedor.read(progresoDeTutorialesProvider)['abierto'], isNull);
    expect(await almacen.leer(), isEmpty);
  });
}
