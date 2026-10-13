import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/proteccion_pantalla.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/proveedores/proteccion_pantalla.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _ProteccionFalsa implements ProteccionPantalla {
  final activaciones = <String>[];

  @override
  Future<void> activar() async => activaciones.add('activar');

  @override
  Future<void> desactivar() async => activaciones.add('desactivar');
}

void main() {
  testWidgets('alta protege fases 1 y 2 y libera en fase 3 y al salir', (
    tester,
  ) async {
    final proteccion = _ProteccionFalsa();
    final contenedor = _contenedor(proteccion);
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(_app(contenedor, const PantallaDeRegistro()));
    await tester.pump();
    await tester.pump();
    expect(proteccion.activaciones, ['activar']);

    final alta = contenedor.read(altaProvider.notifier);
    alta
      ..siguiente()
      ..siguiente();
    await tester.pump();
    await tester.pump();
    expect(contenedor.read(altaProvider).paso.fase, FaseAlta.identidad);
    expect(proteccion.activaciones, ['activar']);

    alta
      ..siguiente()
      ..siguiente();
    await tester.pump();
    await tester.pump();
    expect(contenedor.read(altaProvider).paso.fase, FaseAlta.datos);
    expect(proteccion.activaciones, ['activar', 'desactivar']);

    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
    expect(proteccion.activaciones, ['activar', 'desactivar']);
  });

  testWidgets('ingreso activa al entrar y desactiva al salir', (tester) async {
    final proteccion = _ProteccionFalsa();
    final contenedor = _contenedor(proteccion);
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(_app(contenedor, const PantallaDeSesion()));
    await tester.pump();
    await tester.pump();
    expect(proteccion.activaciones, ['activar']);

    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
    expect(proteccion.activaciones, ['activar', 'desactivar']);
  });
}

ProviderContainer _contenedor(_ProteccionFalsa proteccion) {
  final dio = dioSimulado().dio;
  return ProviderContainer(
    overrides: [
      almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
      dioProvider.overrideWithValue(dio),
      proteccionPantallaProvider.overrideWithValue(proteccion),
    ],
  );
}

Widget _app(ProviderContainer container, Widget child) =>
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: child,
      ),
    );
