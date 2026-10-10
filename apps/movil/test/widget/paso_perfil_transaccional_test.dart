import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/catalogo_del_perfil.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_perfil_transaccional.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// Actividad económica: lista corta, con buscador y con «Otra, ¿cuál?».
void main() {
  Future<ProviderContainer> abrir(WidgetTester tester) async {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    // El paso 7 viene después de los datos: se arranca parado en él.
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(
            body: SingleChildScrollView(child: PasoPerfilTransaccional()),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return contenedor;
  }

  Future<void> elegirOrigenSueldo(WidgetTester tester) async {
    await tester.tap(find.text('Elegir').first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Sueldo o salario').last);
    await tester.pumpAndSettle();
  }

  Future<void> buscarActividad(
    WidgetTester tester,
    String q,
    String opcion,
  ) async {
    await tester.tap(find.text('Elegir').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, q);
    await tester.pumpAndSettle();
    await tester.tap(find.text(opcion).last);
    await tester.pumpAndSettle();
  }

  Future<void> continuar(WidgetTester tester) async {
    await tester.ensureVisible(find.text('Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
  }

  test('las actividades entran en un renglón y existe «Otra»', () {
    final largas = [
      for (final a in actividadesEconomicas)
        if (a.texto.length > 40) a.texto,
    ];
    expect(largas, isEmpty);
    expect(actividadesEconomicas.map((a) => a.codigo), contains(actividadOtra));
    expect(
      actividadesEconomicas.map((a) => a.codigo).toSet().length,
      actividadesEconomicas.length,
    );
  });

  testWidgets('la actividad se elige con buscador', (tester) async {
    await abrir(tester);
    await tester.tap(find.text('Elegir').last);
    await tester.pumpAndSettle();
    expect(find.text('Buscar'), findsOneWidget);
    await tester.enterText(find.byType(TextField).last, 'salud');
    await tester.pumpAndSettle();
    expect(find.text('Salud o asistencia social'), findsOneWidget);
    expect(find.text('Educación'), findsNothing);
  });

  testWidgets('«Otra» pide «¿Cuál?» y no deja seguir sin escribirla', (
    tester,
  ) async {
    final contenedor = await abrir(tester);
    await elegirOrigenSueldo(tester);
    await buscarActividad(tester, 'otra', 'Otra (¿cuál?)');
    expect(find.text('¿Cuál?'), findsOneWidget);

    await continuar(tester);
    expect(find.text('Escribí a qué te dedicás.'), findsOneWidget);
    expect(contenedor.read(altaProvider).actividadEconomica, isEmpty);

    await tester.enterText(find.byType(TextField).first, 'Músico');
    await tester.pump();
    await continuar(tester);
    final estado = contenedor.read(altaProvider);
    expect(estado.actividadEconomica, actividadOtra);
    expect(estado.detalleDeLaActividad, 'Músico');
  });

  testWidgets('con una actividad de la lista, «¿Cuál?» no aparece', (
    tester,
  ) async {
    final contenedor = await abrir(tester);
    await elegirOrigenSueldo(tester);
    await buscarActividad(tester, 'educ', 'Educación');
    expect(find.text('¿Cuál?'), findsNothing);
    await continuar(tester);
    final estado = contenedor.read(altaProvider);
    expect(estado.actividadEconomica, 'P');
    expect(estado.detalleDeLaActividad, isEmpty);
  });
}
