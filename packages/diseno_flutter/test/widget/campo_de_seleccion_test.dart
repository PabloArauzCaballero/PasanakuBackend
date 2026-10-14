import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/lista_con_buscador.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// La disciplina del buscador: con muchas opciones se elige escribiendo, no
/// leyendo una lista que tapa la pantalla.
void main() {
  List<({String valor, String texto, String? detalle})> opciones(int n) => [
    for (var i = 1; i <= n; i++)
      (valor: 'v$i', texto: 'Opción $i', detalle: null),
  ];

  Future<String?> montar(
    WidgetTester tester,
    List<({String valor, String texto, String? detalle})> lista,
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

  testWidgets('con pocas opciones también abre una hoja', (tester) async {
    await montar(tester, opciones(3));
    expect(find.byType(DropdownButtonFormField<String>), findsNothing);
    await tester.tap(find.text('Elegí una opción').first);
    await tester.pumpAndSettle();
    expect(find.text('Buscar en actividad'), findsOneWidget);
    expect(find.text('Opción 1', findRichText: true), findsOneWidget);
  });

  testWidgets('con una sola opción informa el valor sin control interactivo', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        CampoDeSeleccion<String>(
          etiqueta: 'Actividad',
          opciones: const [(valor: 'v1', texto: 'Comercio', detalle: null)],
          valor: 'v1',
          onElegida: (_) {},
        ),
      ),
    );

    expect(find.text('Comercio'), findsOneWidget);
    expect(find.byType(InkWell), findsNothing);
  });

  testWidgets('con 13 opciones se abre una hoja con buscador', (tester) async {
    await montar(tester, opciones(13));
    expect(find.byType(DropdownButtonFormField<String>), findsNothing);

    await tester.tap(find.text('Elegí una opción'));
    await tester.pumpAndSettle();
    expect(find.text('Buscar en actividad'), findsOneWidget);
    expect(find.text('Opción 1', findRichText: true), findsOneWidget);
    // La lista es perezosa: la última no está construida hasta buscarla.
    await tester.enterText(find.byType(TextField), '13');
    await tester.pumpAndSettle();
    expect(find.text('Opción 13', findRichText: true), findsOneWidget);
  });

  testWidgets('el buscador filtra sin tildes ni mayúsculas y elegir cierra', (
    tester,
  ) async {
    final lista = [
      ...opciones(12),
      (valor: 'P', texto: 'Educación', detalle: null),
      (valor: 'Q', texto: 'Salud o asistencia social', detalle: null),
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
    expect(find.text('Educación', findRichText: true), findsOneWidget);
    expect(find.text('Salud o asistencia social'), findsNothing);
    expect(find.text('Opción 1'), findsNothing);

    await tester.tap(find.text('Educación', findRichText: true));
    await tester.pumpAndSettle();
    expect(elegida, 'P');
    // ignore: avoid_print
    expect(find.text('Buscar en actividad'), findsNothing);
    // El campo muestra lo elegido.
    expect(find.text('Educación'), findsOneWidget);
    await tester.tap(find.text('Educación'));
    await tester.pumpAndSettle();
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      isEmpty,
    );
  });

  testWidgets('sin coincidencias, la hoja lo dice en vez de quedar vacía', (
    tester,
  ) async {
    await montar(tester, opciones(20));
    await tester.tap(find.text('Elegí una opción'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'zzz');
    await tester.pumpAndSettle();
    expect(find.text('Nada coincide con «zzz».'), findsOneWidget);
  });

  testWidgets('placeholder por defecto, ayuda y motivo de deshabilitación', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        Column(
          children: [
            CampoDeSeleccion<String>(
              etiqueta: 'Banco',
              opciones: const [
                (valor: 'b', texto: 'Banco Uno', detalle: 'Cuenta de ahorro'),
                (valor: 'b2', texto: 'Banco Dos', detalle: 'Cuenta corriente'),
              ],
              onElegida: (_) {},
              ayuda: 'Elegí el banco de origen',
            ),
            CampoDeSeleccion<String>(
              etiqueta: 'Ciudad',
              opciones: const [],
              onElegida: (_) {},
              habilitado: false,
              deshabilitadoPorque: 'Primero elegí un departamento.',
            ),
          ],
        ),
      ),
    );
    expect(find.text('Elegí una opción'), findsNWidgets(2));
    expect(find.text('Primero elegí un departamento.'), findsOneWidget);
    await tester.tap(find.text('Elegí una opción').first);
    await tester.pumpAndSettle();
    expect(find.text('Elegí el banco de origen'), findsNWidgets(2));
    expect(find.text('Cuenta de ahorro', findRichText: true), findsOneWidget);
    expect(find.text('Buscar en banco'), findsOneWidget);
  });

  testWidgets('semántica del campo y retorno del foco al cerrar con atrás', (
    tester,
  ) async {
    final foco = FocusNode();
    addTearDown(foco.dispose);
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(
      conTema(
        CampoDeSeleccion<String>(
          etiqueta: 'Departamento',
          foco: foco,
          opciones: const [
            (valor: 'LP', texto: 'La Paz', detalle: 'Sede de gobierno'),
            (valor: 'SC', texto: 'Santa Cruz', detalle: 'Capital oriental'),
          ],
          onElegida: (_) {},
        ),
      ),
    );
    final campoSemantico = find.byWidgetPredicate(
      (w) => w is Semantics && w.properties.button == true,
    );
    expect(tester.getSemantics(campoSemantico).label, 'Departamento');
    expect(tester.getSemantics(campoSemantico).hint, 'Tocá para elegir');
    await tester.tap(find.text('Elegí una opción'));
    await tester.pumpAndSettle();
    expect(foco.hasFocus, isFalse);
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(foco.hasFocus, isTrue);
    semantics.dispose();
  });

  test('normalizar quita tildes, eñes y mayúsculas', () {
    expect(normalizarParaBuscar('  Educación AÑO  '), 'educacion ano');
  });
}
