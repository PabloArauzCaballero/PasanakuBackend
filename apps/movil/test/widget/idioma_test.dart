import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/localizacion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// D3 del carril H2: con el teléfono en inglés, el selector de fecha del alta salía en inglés
/// («Thu, Oct 11», «October 1990»). La app ahora declara español de Bolivia (y español) como idiomas
/// soportados, así que un teléfono en inglés cae en español.
void main() {
  Future<void> abrirSelector(
    WidgetTester tester, {
    required Locale telefono,
  }) async {
    tester.platformDispatcher.localesTestValue = [telefono];
    tester.platformDispatcher.localeTestValue = telefono;
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        supportedLocales: idiomasDeLaApp,
        localizationsDelegates: delegadosDeIdioma,
        home: Scaffold(
          body: CampoDeFecha(
            etiqueta: 'Fecha de nacimiento',
            valor: null,
            inicial: DateTime(1990, 10, 11),
            onElegida: (_) {},
          ),
        ),
      ),
    );
    await tester.tap(find.byIcon(Icons.calendar_month_outlined));
    await tester.pumpAndSettle();
  }

  testWidgets('teléfono en inglés: el selector de fecha sale en español', (
    tester,
  ) async {
    await abrirSelector(tester, telefono: const Locale('en', 'US'));
    final contexto = tester.element(find.byType(Dialog));
    expect(Localizations.localeOf(contexto).languageCode, 'es');
    // Los textos propios de Material salen del idioma: en inglés serían «CANCEL» y «Thu, Oct 11»; en español, «Cancelar».
    final textos = MaterialLocalizations.of(contexto);
    expect(textos.cancelButtonLabel, 'Cancelar');
    expect(textos.formatMediumDate(DateTime(1990, 10, 11)), contains('oct'));
    expect(find.textContaining('Oct'), findsNothing);
    expect(find.textContaining('oct'), findsWidgets);
  });

  testWidgets('teléfono en español de Bolivia: sigue en es_BO', (tester) async {
    await abrirSelector(tester, telefono: const Locale('es', 'BO'));
    final contexto = tester.element(find.byType(Dialog));
    expect(Localizations.localeOf(contexto).languageCode, 'es');
    expect(Localizations.localeOf(contexto).countryCode, 'BO');
  });

  test('los idiomas soportados son es_BO y es, en ese orden', () {
    expect(idiomasDeLaApp, const [Locale('es', 'BO'), Locale('es')]);
  });
}
