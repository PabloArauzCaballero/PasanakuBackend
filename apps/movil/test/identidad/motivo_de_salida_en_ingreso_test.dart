import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import '../goldens/cargar_fuentes_de_aplicacion.dart';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  Future<void> mostrarIngreso(WidgetTester tester, Sesion sesion) async {
    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sesionProvider.overrideWithValue(sesion)],
        child: conApp(const PantallaDeSesion(), dio: dio),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('avisa una vez cuando la sesión caducó por el tope de 8 horas', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(412, 850));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final sesion = Sesion(AlmacenEnMemoria());
    await sesion.cerrar(motivo: MotivoDeSalida.caducada);
    await mostrarIngreso(tester, sesion);

    expect(find.text('Por seguridad, tu sesión dura 8 horas.'), findsOneWidget);
    expect(find.text('Volvé a entrar con tu contraseña.'), findsOneWidget);
    expect(tester.widget<Alerta>(find.byType(Alerta)).tono, Tono.aviso);
    expect(sesion.tomarMotivoDeSalida(), isNull);
    await expectLater(
      find.byType(PantallaDeSesion),
      matchesGoldenFile(
        '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
        'evidencia/h1-ingreso-caducada.png',
      ),
    );
  });

  testWidgets('informa una vez cuando la sesión se cerró por otra razón', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(412, 850));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final sesion = Sesion(AlmacenEnMemoria());
    await sesion.cerrar(motivo: MotivoDeSalida.cerrada);
    await mostrarIngreso(tester, sesion);

    expect(find.text('Tu sesión se cerró.'), findsOneWidget);
    expect(find.text('Volvé a entrar con tu contraseña.'), findsOneWidget);
    expect(tester.widget<Alerta>(find.byType(Alerta)).tono, Tono.info);
    expect(sesion.tomarMotivoDeSalida(), isNull);
    await expectLater(
      find.byType(PantallaDeSesion),
      matchesGoldenFile(
        '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
        'evidencia/h1-ingreso-cerrada.png',
      ),
    );
  });

  testWidgets('el motivo consumido no reaparece al abrir el ingreso otra vez', (
    tester,
  ) async {
    final sesion = Sesion(AlmacenEnMemoria());
    await sesion.cerrar(motivo: MotivoDeSalida.cerrada);
    await mostrarIngreso(tester, sesion);
    expect(find.text('Tu sesión se cerró.'), findsOneWidget);

    await tester.pumpWidget(const SizedBox.shrink());
    await mostrarIngreso(tester, sesion);
    expect(find.text('Tu sesión se cerró.'), findsNothing);
  });
}
