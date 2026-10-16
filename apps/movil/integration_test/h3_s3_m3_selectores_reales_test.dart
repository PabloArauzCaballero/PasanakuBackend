import 'package:aportaya_diseno/atomos/fila_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_de_progreso.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/dominio/tutoriales/registro.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_documento.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_perfil_transaccional.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_del_alta.dart';
import 'package:aportaya_movil/pantallas/soporte/catalogo/catalogo.dart';
import 'package:aportaya_movil/pantallas/soporte/pantalla_centro_de_ayuda.dart';
import 'package:aportaya_movil/pantallas/soporte/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

class _AlmacenDeProgresoEnMemoria implements AlmacenDeProgreso {
  final _progresos = <String, ProgresoDeTutorial>{};

  @override
  Future<List<ProgresoDeTutorial>> leer() async => _progresos.values.toList();

  @override
  Future<void> guardar(ProgresoDeTutorial progreso) async {
    _progresos[progreso.tutorialId] = progreso;
  }

  @override
  Future<void> reiniciar(String tutorialId) async {
    _progresos.remove(tutorialId);
  }
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'H3.S3.M3 los selectores reales de alta y soporte dejan filas sobre el IME',
    (tester) async {
      final contenedor = ProviderContainer(
        overrides: [
          generacionDeSesionProvider.overrideWithValue(0),
          catalogoDeTutorialesProvider.overrideWithValue(catalogoDeTutoriales),
          registroDeTutorialesProvider.overrideWithValue(
            const RegistroDeTutoriales(catalogoDeTutoriales, {}),
          ),
          almacenDeProgresoProvider.overrideWithValue(
            _AlmacenDeProgresoEnMemoria(),
          ),
        ],
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

      Future<void> abrirYCapturar({
        required String campo,
        required String archivo,
        int opcionASeleccionar = 0,
      }) async {
        // Buscar el widget de dominio evita que find.bySemanticsLabel recoja
        // tanto la etiqueta como el nodo semántico del campo en Android.
        final control = find.byWidgetPredicate(
          (widget) => widget is CampoDeSeleccion && widget.etiqueta == campo,
        );
        expect(control, findsOneWidget);
        await tester.ensureVisible(control);
        await tester.pumpAndSettle();
        final botonDeSeleccion = find.descendant(
          of: control,
          matching: find.byType(InkWell),
        );
        expect(botonDeSeleccion, findsOneWidget);
        await tester.tap(botonDeSeleccion);
        await tester.pumpAndSettle();

        final buscador = find.byType(TextField).last;
        expect(buscador, findsOneWidget);
        await tester.tap(buscador);

        var insetInferior = 0.0;
        for (var intento = 0; intento < 80; intento++) {
          await tester.pump(const Duration(milliseconds: 100));
          insetInferior = MediaQuery.of(
            tester.element(buscador),
          ).viewInsets.bottom;
          if (insetInferior > 0) break;
        }
        expect(
          insetInferior,
          greaterThan(0),
          reason: 'El teclado Android no se abrió para "$campo".',
        );

        // La hoja anima su altura al llegar el inset del teclado. Espera ese
        // cambio antes de medir y dejar la pantalla estable para screencap.
        await tester.pump(const Duration(milliseconds: 800));
        final medio = MediaQuery.of(tester.element(buscador));
        final bordeTeclado = medio.size.height - medio.viewInsets.bottom;
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
          'H3S3M3_CAPTURE_READY campo=$campo archivo=$archivo '
          'pantalla=${medio.size.width}x${medio.size.height} '
          'inset=${medio.viewInsets.bottom} tecladoTop=$bordeTeclado '
          'buscador=$rectBuscador filas=$rects visibles=$visibles',
        );
        // Pausa de captura manual con `adb shell screencap`; incluye el IME
        // Android del emulador, que no forma parte del render Flutter.
        await tester.runAsync(
          () => Future<void>.delayed(const Duration(seconds: 20)),
        );
        expect(rectBuscador.bottom, lessThanOrEqualTo(bordeTeclado));
        expect(visibles, greaterThanOrEqualTo(2));

        // Elegir la primera opción cierra el modal y permite montar el otro paso
        // sin conservar ni el foco ni el IME de esta pantalla.
        final opcion = find.byType(FilaDeSeleccion).at(opcionASeleccionar);
        await tester.ensureVisible(opcion);
        await tester.tap(opcion);
        await tester.pumpAndSettle();
      }

      await montar(const PasoDocumento());
      await abrirYCapturar(
        campo: TextosIdentidad.lugarExpedicion,
        archivo: 'departamento.png',
      );

      await montar(const PasoActividad());
      await abrirYCapturar(
        campo: TextosDelAlta.actividadEconomica,
        archivo: 'actividad-economica.png',
      );
      // Este es un tercer selector real disponible en el mismo paso. No se lo
      // presenta como selector de soporte: cubre el caso adicional sin inventar
      // un control que el producto todavía no ofrece.
      await abrirYCapturar(
        campo: TextosDelAlta.origenDeFondos,
        archivo: 'origen-de-fondos.png',
      );

      // Tercer selector de soporte: sus opciones son las categorías reales que el
      // propio centro de ayuda usa para recortar el catálogo de tutoriales.
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: contenedor,
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: const PantallaCentroDeAyuda(),
          ),
        ),
      );
      await tester.pumpAndSettle();
      await abrirYCapturar(
        campo: TextosSoporte.temaDelTutorial,
        archivo: 'tema-soporte.png',
        opcionASeleccionar: 1,
      );
      final temaSeleccionado = find.byWidgetPredicate(
        (widget) =>
            widget is CampoDeSeleccion &&
            widget.etiqueta == TextosSoporte.temaDelTutorial,
      );
      expect(
        tester.widget<CampoDeSeleccion<String>>(temaSeleccionado).valor,
        'Primeros pasos',
      );
    },
  );
}
