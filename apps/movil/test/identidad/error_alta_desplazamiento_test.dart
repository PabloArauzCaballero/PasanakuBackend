import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/proteccion_pantalla.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/proveedores/proteccion_pantalla.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets(
    'un error remoto nuevo desplaza al banner desde el final del paso',
    (tester) async {
      tester.view.physicalSize = const Size(360, 640);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final dio = dioSimulado().dio;
      final contenedor = ProviderContainer(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
          proteccionPantallaProvider.overrideWithValue(_ProteccionNula()),
          altaProvider.overrideWith(_AltaMutable.new),
        ],
      );
      addTearDown(contenedor.dispose);
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: contenedor,
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: const PantallaDeRegistro(),
          ),
        ),
      );

      final scroll = find.byType(SingleChildScrollView);
      final alta = contenedor.read(altaProvider.notifier) as _AltaMutable;

      for (final paso in PasoAlta.values) {
        expect(contenedor.read(altaProvider).paso, paso);
        await tester.drag(scroll, const Offset(0, -900));
        await tester.pumpAndSettle();

        final mensaje = 'Error remoto en ${paso.name}';
        alta.mostrarError(mensaje);
        await tester.pumpAndSettle();

        final banner = find.text(mensaje);
        expect(banner, findsOneWidget, reason: 'paso ${paso.name}');
        expect(find.text('Referencia: ${paso.name}'), findsOneWidget);
        final arriba = tester.getTopLeft(banner).dy;
        expect(arriba, greaterThanOrEqualTo(0), reason: 'paso ${paso.name}');
        expect(arriba, lessThan(300), reason: 'paso ${paso.name}');

        alta.limpiarError();
        await tester.pumpAndSettle();
        if (paso != PasoAlta.values.last) {
          alta.siguiente();
          await tester.pumpAndSettle();
        }
      }
    },
  );
}

class _AltaMutable extends AltaNotifier {
  @override
  EstadoAlta build() => const EstadoAlta();

  void mostrarError(String mensaje) {
    state = state.copiarCon(error: mensaje, trazaId: state.paso.name);
  }

  void limpiarError() => state = state.copiarCon();
}

class _ProteccionNula implements ProteccionPantalla {
  @override
  Future<void> activar() async {}

  @override
  Future<void> desactivar() async {}
}
