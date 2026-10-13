import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/campos_de_nombre.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_datos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  for (final ancho in [360.0, 412.0]) {
    testWidgets('nombres se acomodan a $ancho dp', (tester) async {
      tester.view.devicePixelRatio = 1;
      tester.view.physicalSize = Size(ancho, 900);
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final controladores = List.generate(4, (_) => TextEditingController());
      final focos = List.generate(5, (_) => FocusNode());
      addTearDown(() {
        for (final c in controladores) {
          c.dispose();
        }
        for (final f in focos) {
          f.dispose();
        }
      });
      await tester.pumpWidget(
        MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: Padding(
              padding: const EdgeInsets.all(16),
              child: CamposDeNombre(
                controladores: controladores,
                focos: focos,
                errores: const {},
                onCambio: (_) {},
              ),
            ),
          ),
        ),
      );

      final campos = find.byType(TextField);
      expect(campos, findsNWidgets(4));
      final y = [
        for (var i = 0; i < 4; i++) tester.getTopLeft(campos.at(i)).dy,
      ];
      if (ancho < 400) {
        expect(y[1], greaterThan(y[0]));
        expect(y[3], greaterThan(y[2]));
      } else {
        expect(y[1], y[0]);
        expect(y[3], y[2]);
        expect(y[2], greaterThan(y[1]));
      }

      final primerCampo = tester.widget<TextField>(campos.first);
      expect(primerCampo.textCapitalization, TextCapitalization.words);
      expect(primerCampo.textInputAction, TextInputAction.next);
      expect(primerCampo.autofillHints, contains(AutofillHints.givenName));
      expect(
        tester.widget<TextField>(campos.at(1)).autofillHints,
        contains(AutofillHints.middleName),
      );
      expect(
        tester.widget<TextField>(campos.at(2)).autofillHints,
        contains(AutofillHints.familyName),
      );
    });
  }

  testWidgets('todos los campos de texto del paso de datos tienen ejemplo', (
    tester,
  ) async {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(body: SingleChildScrollView(child: PasoDatos())),
        ),
      ),
    );
    final fields = tester.widgetList<TextField>(find.byType(TextField));
    expect(fields, hasLength(4));
    expect(
      fields.every((field) => field.decoration!.hintText!.isNotEmpty),
      isTrue,
    );
  });

  testWidgets('Siguiente recorre los cuatro campos de nombre en orden', (
    tester,
  ) async {
    final controladores = List.generate(4, (_) => TextEditingController());
    final focos = List.generate(5, (_) => FocusNode());
    addTearDown(() {
      for (final c in controladores) {
        c.dispose();
      }
      for (final f in focos) {
        f.dispose();
      }
    });
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: CamposDeNombre(
            controladores: controladores,
            focos: focos,
            errores: const {},
            onCambio: (_) {},
          ),
        ),
      ),
    );
    await tester.tap(find.byType(TextField).first);
    await tester.pump();
    for (var indice = 1; indice < 4; indice++) {
      await tester.testTextInput.receiveAction(TextInputAction.next);
      await tester.pump();
      expect(focos[indice].hasFocus, isTrue);
    }
  });
}
