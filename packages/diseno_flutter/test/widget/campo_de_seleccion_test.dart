import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/lista_con_buscador.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// La disciplina del buscador: con muchas opciones se elige escribiendo, no
/// leyendo una lista que tapa la pantalla.
void main() {
  List<({String valor, String texto})> opciones(int n) => [
    for (var i = 1; i <= n; i++) (valor: 'v$i', texto: 'Opción $i'),
  ];

  Future<String?> montar(
    WidgetTester tester,
    List<({String valor, String texto})> lista,
  ) async {
    String? elegida;
    await tester.pumpWidget(
      conTema(
        StatefulBuilder(
          builder: (context, setState) => CampoDeSeleccion<String>(
            etiqueta: 'Actividad',
            textoVacio: 'Elegí una opción',
            valor: elegida,
            opciones: lista,
            onElegida: (v) => setState(() => elegida = v),
          ),
        ),
      ),
    );
    return elegida;
  }

  testWidgets('con 12 opciones sigue siendo un desplegable', (tester) async {
    await montar(tester, opciones(CampoDeSeleccion.umbralDelBuscador));
    expect(find.byType(DropdownButtonFormField<String>), findsOneWidget);
  });

  testWidgets('con 13 opciones se abre una hoja con buscador', (tester) async {
    await montar(tester, opciones(CampoDeSeleccion.umbralDelBuscador + 1));
    expect(find.byType(DropdownButtonFormField<String>), findsNothing);

    await tester.tap(find.text('Elegí una opción'));
    await tester.pumpAndSettle();
    expect(find.text('Buscar'), findsOneWidget);
    expect(find.text('Opción 1'), findsOneWidget);
    // La lista es perezosa: la última no está construida hasta buscarla.
    await tester.enterText(find.byType(TextField), '13');
    await tester.pumpAndSettle();
    expect(find.text('Opción 13'), findsOneWidget);
  });

  testWidgets('el buscador filtra sin tildes ni mayúsculas y elegir cierra', (
    tester,
  ) async {
    final lista = [
      ...opciones(12),
      (valor: 'P', texto: 'Educación'),
      (valor: 'Q', texto: 'Salud o asistencia social'),
    ];
    String? elegida;
    await tester.pumpWidget(
      conTema(
        StatefulBuilder(
          builder: (context, setState) => CampoDeSeleccion<String>(
            etiqueta: 'Actividad',
            textoVacio: 'Elegí una opción',
            valor: elegida,
            opciones: lista,
            onElegida: (v) => setState(() => elegida = v),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Elegí una opción'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), 'EDUCACION');
    await tester.pumpAndSettle();
    expect(find.text('Educación'), findsOneWidget);
    expect(find.text('Salud o asistencia social'), findsNothing);
    expect(find.text('Opción 1'), findsNothing);

    await tester.tap(find.text('Educación'));
    await tester.pumpAndSettle();
    expect(elegida, 'P');
    expect(find.text('Buscar'), findsNothing);
    // El campo muestra lo elegido.
    expect(find.text('Educación'), findsOneWidget);
  });

  testWidgets('sin coincidencias, la hoja lo dice en vez de quedar vacía', (
    tester,
  ) async {
    await montar(tester, opciones(20));
    await tester.tap(find.text('Elegí una opción'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'zzz');
    await tester.pumpAndSettle();
    expect(find.text('No hay opciones con «zzz».'), findsOneWidget);
  });

  test('normalizar quita tildes, eñes y mayúsculas', () {
    expect(normalizarParaBuscar('  Educación AÑO  '), 'educacion ano');
  });
}
