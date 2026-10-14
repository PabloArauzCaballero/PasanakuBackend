import 'package:aportaya_movil/app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('DatePicker muestra los meses en español boliviano', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        locale: localeDeAportaYa,
        supportedLocales: localesDeAportaYa,
        localizationsDelegates: delegadosDeLocalizacionDeAportaYa,
        home: Builder(
          builder: (context) => Scaffold(
            body: TextButton(
              onPressed: () => showDatePicker(
                context: context,
                initialDate: DateTime(2026, 10, 12),
                firstDate: DateTime(2026),
                lastDate: DateTime(2027),
              ),
              child: const Text('Abrir calendario'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Abrir calendario'));
    await tester.pumpAndSettle();

    expect(find.textContaining('octubre'), findsWidgets);
    expect(find.byTooltip('Mes anterior'), findsOneWidget);
  });
}
