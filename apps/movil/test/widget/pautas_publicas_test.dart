import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/navegacion/shell.dart';
import 'package:aportaya_movil/pantallas/identidad/ilustraciones/ilustracion_de_pauta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_pautas_de_fotos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

void main() {
  Widget appCon(GoRouter router) => ProviderScope(
    overrides: [almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria())],
    child: MaterialApp.router(
      theme: temaDesde(Tokens.claro, Brightness.light),
      routerConfig: router,
    ),
  );

  // La cabecera que se compacta suma su propio Scrollable (el del NestedScrollView):
  // se desplaza la lista de pautas, que es la que tiene el contenido.
  Finder lista() => find.descendant(
    of: find.descendant(
      of: find.byType(PantallaDePautasDeFotos),
      matching: find.byType(ListView),
    ),
    matching: find.byType(Scrollable),
  );

  Future<void> comprobarPareja(
    WidgetTester tester, {
    required String pauta,
    required String correcta,
    required String incorrecta,
  }) async {
    await tester.scrollUntilVisible(find.text(pauta), 260, scrollable: lista());
    await tester.pumpAndSettle();
    expect(find.text(pauta), findsOneWidget);
    expect(find.text(correcta), findsOneWidget);
    expect(find.text(incorrecta), findsOneWidget);
    expect(find.byType(IlustracionDePauta), findsWidgets);
    expect(find.text('Correcto'), findsWidgets);
    expect(find.text('Incorrecto'), findsWidgets);
  }

  testWidgets('la ruta pública muestra las cuatro parejas de documento', (
    tester,
  ) async {
    final router = crearEnrutador(
      inicial: '/registro/pautas',
      sesion: await sesionAnonimaDePrueba(),
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(router.state.uri.path, '/registro/pautas');
    expect(encabezadoDePantalla('Pautas para tus fotos'), findsOneWidget);
    expect(find.text('Pautas para el documento de identidad'), findsOneWidget);
    expect(find.byType(ShellPrincipal), findsNothing);
    expect(find.byType(BarraPestanas), findsNothing);

    await comprobarPareja(
      tester,
      pauta: 'Buena iluminación',
      correcta: 'Carnet bien iluminado, se lee todo',
      incorrecta: 'Carnet demasiado oscuro, no se lee',
    );
    await comprobarPareja(
      tester,
      pauta: 'Evitá los reflejos',
      correcta: 'Carnet sin reflejos',
      incorrecta: 'Una franja de reflejo tapa el texto',
    );
    await comprobarPareja(
      tester,
      pauta: 'Enfoque y nitidez',
      correcta: 'Carnet nítido',
      incorrecta: 'Carnet borroso o movido',
    );
    await comprobarPareja(
      tester,
      pauta: 'De frente, sin inclinar',
      correcta: 'Carnet visto de frente',
      incorrecta: 'Carnet inclinado, visto de lado',
    );
  });

  testWidgets('la pestaña Foto de frente muestra sus tres parejas bien/mal', (
    tester,
  ) async {
    final router = crearEnrutador(
      inicial: '/registro/pautas',
      sesion: await sesionAnonimaDePrueba(),
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Foto de frente'));
    await tester.pumpAndSettle();
    expect(find.text('Pautas para las fotos de rostro'), findsOneWidget);

    await comprobarPareja(
      tester,
      pauta: 'De frente y centrado',
      correcta: 'Rostro de frente y centrado',
      incorrecta: 'Rostro ladeado y fuera del centro',
    );
    await comprobarPareja(
      tester,
      pauta: 'Sin accesorios',
      correcta: 'Rostro descubierto',
      incorrecta: 'Rostro con lentes oscuros',
    );
    await comprobarPareja(
      tester,
      pauta: 'La luz, de frente',
      correcta: 'Rostro con luz de frente',
      incorrecta: 'Rostro a contraluz, oscuro',
    );
  });
}
