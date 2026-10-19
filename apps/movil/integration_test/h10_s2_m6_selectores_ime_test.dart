import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/fila_de_seleccion.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_documento.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_perfil_transaccional.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_del_alta.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'H10.S2.M6 Expedición y actividad se buscan y eligen sobre el IME',
    (tester) async {
      final contenedor = ProviderContainer(
        overrides: [generacionDeSesionProvider.overrideWithValue(0)],
      );
      addTearDown(contenedor.dispose);

      Future<void> montar(Widget pantalla) async {
        await tester.pumpWidget(
          UncontrolledProviderScope(
            container: contenedor,
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              home: Scaffold(body: SingleChildScrollView(child: pantalla)),
            ),
          ),
        );
        await tester.pumpAndSettle();
      }

      Future<void> probarSelector({
        required String etiqueta,
        required String archivo,
      }) async {
        final control = find.byWidgetPredicate(
          (widget) => widget is CampoDeSeleccion && widget.etiqueta == etiqueta,
        );
        expect(control, findsOneWidget);
        await tester.ensureVisible(control);
        await tester.pumpAndSettle();
        final disparador = find.descendant(
          of: control,
          matching: find.byType(InkWell),
        );
        expect(disparador, findsOneWidget);
        await tester.tap(disparador);
        await tester.pumpAndSettle();

        final buscador = find.byType(TextField).last;
        expect(buscador, findsOneWidget);
        await tester.tap(buscador);
        var inset = 0.0;
        for (var intento = 0; intento < 80; intento++) {
          await tester.pump(const Duration(milliseconds: 100));
          inset = MediaQuery.of(tester.element(buscador)).viewInsets.bottom;
          if (inset > 0) break;
        }
        expect(inset, greaterThan(0), reason: 'IME ausente para "$etiqueta".');

        // Una consulta amplia conserva varias opciones y prueba que la búsqueda
        // real filtra la lista sin ocultar el teclado.
        await tester.enterText(buscador, 'a');
        await tester.pumpAndSettle();
        final media = MediaQuery.of(tester.element(buscador));
        final bordeTeclado = media.size.height - media.viewInsets.bottom;
        final rectBuscador = tester.getRect(buscador);
        final filas = find.byType(FilaDeSeleccion);
        final rects = [
          for (var indice = 0; indice < filas.evaluate().length; indice++)
            tester.getRect(filas.at(indice)),
        ];
        final visibles = rects
            .where((rect) => rect.top >= 0 && rect.bottom <= bordeTeclado)
            .length;
        // ignore: avoid_print
        print(
          'H10S2M6_CAPTURE_READY archivo=$archivo campo=$etiqueta '
          'pantalla=${media.size.width}x${media.size.height} '
          'inset=${media.viewInsets.bottom} buscador=$rectBuscador '
          'filas=$rects visibles=$visibles',
        );
        // Pausa solo para que el runner copie la captura del AVD con el IME real.
        await tester.runAsync(
          () => Future<void>.delayed(const Duration(seconds: 15)),
        );
        expect(rectBuscador.bottom, lessThanOrEqualTo(bordeTeclado));
        expect(visibles, greaterThanOrEqualTo(3));

        final primera = tester.widget<FilaDeSeleccion>(filas.first).texto;
        await tester.tap(filas.first);
        await tester.pumpAndSettle();
        expect(find.byType(FilaDeSeleccion), findsNothing);
        expect(find.text(primera), findsOneWidget);
      }

      await montar(const PasoDocumento());
      await probarSelector(
        etiqueta: TextosIdentidad.lugarExpedicion,
        archivo: 'h10-s2-m6-expedido-en-360x640.png',
      );

      await montar(const PasoActividad());
      await probarSelector(
        etiqueta: TextosDelAlta.actividadEconomica,
        archivo: 'h10-s2-m6-actividad-economica-360x640.png',
      );
    },
  );
}
