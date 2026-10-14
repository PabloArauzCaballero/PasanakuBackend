import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// La fecha se elige en tres selects —Día, Mes, Año—, no escribiendo `mm/dd/yyyy`.
void main() {
  Future<List<DateTime?>> montar(WidgetTester tester) async {
    final emitidas = <DateTime?>[];
    await tester.pumpWidget(
      conTema(
        CampoDeFecha(
          etiqueta: 'Fecha de nacimiento',
          valor: null,
          primera: DateTime(1920),
          ultima: DateTime(2008, 10, 3),
          onElegida: emitidas.add,
        ),
      ),
    );
    return emitidas;
  }

  /// Día y año tienen buscador; el mes es desplegable. Conserva el flujo de
  /// selección desde la hoja modal buscable.
  Future<void> buscarYElegir(
    WidgetTester tester,
    String vacio,
    String q,
  ) async {
    await tester.tap(find.text(vacio));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), q);
    await tester.pumpAndSettle();
    await tester.tap(find.text(q, findRichText: true).last);
    await tester.pumpAndSettle();
  }

  Future<void> elegirMes(WidgetTester tester, String mes) async {
    await tester.tap(find.text('Mes'));
    await tester.pumpAndSettle();
    await tester.tap(find.text(mes, findRichText: true).last);
    await tester.pumpAndSettle();
  }

  testWidgets('son tres selects, en orden Día, Mes, Año', (tester) async {
    await montar(tester);
    final dia = tester.getTopLeft(find.text('Día')).dx;
    final mes = tester.getTopLeft(find.text('Mes')).dx;
    final anio = tester.getTopLeft(find.text('Año')).dx;
    expect(dia < mes && mes < anio, isTrue);
  });

  testWidgets('apila a 479 dp y alinea en fila desde 480 dp', (tester) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(511, 760); // 479 dp tras el padding.
    addTearDown(tester.view.reset);
    await montar(tester);
    final yDiaApilado = tester.getTopLeft(find.text('Día')).dy;
    final yMesApilado = tester.getTopLeft(find.text('Mes')).dy;
    expect(yDiaApilado < yMesApilado, isTrue);

    tester.view.physicalSize = const Size(512, 760); // 480 dp tras el padding.
    await tester.pump();
    final yDiaEnFila = tester.getTopLeft(find.text('Día')).dy;
    final yMesEnFila = tester.getTopLeft(find.text('Mes')).dy;
    expect(yDiaEnFila, yMesEnFila);
  });

  testWidgets('elegir 12, abril y 1995 emite el 12 de abril de 1995', (
    tester,
  ) async {
    final emitidas = await montar(tester);
    await buscarYElegir(tester, 'Día', '12');
    await elegirMes(tester, 'Abril');
    await buscarYElegir(tester, 'Año', '1995');
    expect(emitidas.last, DateTime(1995, 4, 12));
    // Mientras faltaba una parte, no había fecha.
    expect(emitidas.first, isNull);
  });

  testWidgets('el 31 de febrero no se emite y se explica', (tester) async {
    final emitidas = await montar(tester);
    await buscarYElegir(tester, 'Día', '31');
    await elegirMes(tester, 'Febrero');
    await buscarYElegir(tester, 'Año', '1990');
    expect(emitidas.last, isNull);
    expect(
      find.text('Esa fecha no existe: febrero de 1990 tiene 28 días.'),
      findsOneWidget,
    );
  });

  testWidgets('el 29 de febrero de un año bisiesto sí existe', (tester) async {
    final emitidas = await montar(tester);
    await buscarYElegir(tester, 'Día', '29');
    await elegirMes(tester, 'Febrero');
    await buscarYElegir(tester, 'Año', '1996');
    expect(emitidas.last, DateTime(1996, 2, 29));
  });

  testWidgets('el calendario abre sin el modo de escribir la fecha', (
    tester,
  ) async {
    await montar(tester);
    await tester.tap(find.byTooltip('Elegir en el calendario'));
    await tester.pumpAndSettle();
    final picker = tester.widget<DatePickerDialog>(
      find.byType(DatePickerDialog),
    );
    expect(picker.initialEntryMode, DatePickerEntryMode.calendarOnly);
    // Sin el lápiz que cambiaba al campo `mm/dd/yyyy`.
    expect(find.byIcon(Icons.edit_outlined), findsNothing);
  });

  testWidgets('con un valor, los tres selects lo muestran', (tester) async {
    await tester.pumpWidget(
      conTema(
        CampoDeFecha(
          etiqueta: 'Fecha',
          valor: DateTime(1987, 11, 5),
          primera: DateTime(1920),
          ultima: DateTime(2008),
          onElegida: (_) {},
        ),
      ),
    );
    expect(find.text('5'), findsOneWidget);
    expect(find.text('Noviembre'), findsOneWidget);
    expect(find.text('1987'), findsOneWidget);
  });
}
