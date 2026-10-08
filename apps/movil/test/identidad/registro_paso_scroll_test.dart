import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/moleculas/barra_de_pasos.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/notifier_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_contrasena.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_datos.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('cada paso abre arriba y conserva los datos al volver', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final contenedor = ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria())],
    );
    addTearDown(contenedor.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: const TextScaler.linear(2)),
            child: child!,
          ),
          home: const PantallaDeRegistro(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text(TextosIdentidad.continuar));
    await tester.pumpAndSettle();
    final posicionDatos = Scrollable.of(tester.element(find.byType(PasoDatos)));
    expect(posicionDatos.position.pixels, greaterThan(100));

    final alta = contenedor.read(altaProvider.notifier);
    alta.actualizarDatos(
      const DatosPersonales(nombres: 'Demo', apellidos: 'Prueba'),
    );
    alta.siguiente();
    await tester.pumpAndSettle();

    final posicionClave = Scrollable.of(
      tester.element(find.byType(PasoContrasena)),
    );
    expect(posicionClave.position.pixels, 0);
    expect(find.text('Paso 2 de 9 · Tu contraseña'), findsOneWidget);
    expect(
      tester.getTopLeft(find.text('Paso 2 de 9 · Tu contraseña')).dy,
      greaterThanOrEqualTo(0),
    );

    await tester.ensureVisible(find.text(TextosIdentidad.continuar));
    await tester.pumpAndSettle();
    expect(posicionClave.position.pixels, greaterThan(100));
    alta.atras();
    await tester.pumpAndSettle();
    await tester.pump(const Duration(milliseconds: 450));

    final posicionRetorno = Scrollable.of(
      tester.element(find.byType(PasoDatos)),
    );
    expect(posicionRetorno.position.pixels, 0);
    expect(contenedor.read(altaProvider).datos.nombres, 'Demo');
    expect(
      tester.widget<TextField>(find.byType(TextField).first).controller?.text,
      'Demo',
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('los nueve pasos abren arriba al avanzar y retroceder', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final contenedor = ProviderContainer(
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        altaProvider.overrideWith(_AltaParaRecorrido.new),
      ],
    );
    addTearDown(contenedor.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: const TextScaler.linear(2)),
            child: child!,
          ),
          home: const PantallaDeRegistro(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final alta = contenedor.read(altaProvider.notifier);
    var pasosDesplazables = 0;
    for (var siguiente = 1; siguiente < PasoAlta.values.length; siguiente++) {
      if (await _desplazarAlFinal(tester)) pasosDesplazables++;
      alta.siguiente();
      await tester.pumpAndSettle();
      _esperarPasoArriba(tester, siguiente + 1);
    }
    expect(pasosDesplazables, greaterThan(0));

    for (var anterior = PasoAlta.values.length - 1; anterior > 0; anterior--) {
      await _desplazarAlFinal(tester);
      alta.atras();
      await tester.pumpAndSettle();
      _esperarPasoArriba(tester, anterior);
    }
    expect(contenedor.read(altaProvider).datos.nombres, 'Demo');
    expect(tester.takeException(), isNull);
    await tester.pump(const Duration(milliseconds: 450));
  });
}

class _AltaParaRecorrido extends AltaNotifier {
  @override
  EstadoAlta build() => super.build().copiarCon(
    codigoConfirmado: true,
    datos: const DatosPersonales(nombres: 'Demo'),
  );

  @override
  Future<void> restaurarBorrador() async {}
}

Future<bool> _desplazarAlFinal(WidgetTester tester) async {
  final scroll = Scrollable.of(tester.element(find.byType(BarraDePasos)));
  final desplazable = scroll.position.maxScrollExtent > 0;
  if (desplazable) {
    scroll.position.jumpTo(scroll.position.maxScrollExtent);
    await tester.pumpAndSettle();
    expect(scroll.position.pixels, greaterThan(0));
  }
  return desplazable;
}

void _esperarPasoArriba(WidgetTester tester, int numero) {
  final barra = tester.widget<BarraDePasos>(find.byType(BarraDePasos));
  final scroll = Scrollable.of(tester.element(find.byType(BarraDePasos)));
  expect(barra.actual, numero);
  expect(scroll.position.pixels, 0);
  expect(
    tester.getTopLeft(find.byType(BarraDePasos)).dy,
    greaterThanOrEqualTo(0),
  );
  expect(
    tester.getTopLeft(find.text('Paso $numero de 9 · ${barra.nombre}')).dy,
    greaterThanOrEqualTo(0),
  );
}
