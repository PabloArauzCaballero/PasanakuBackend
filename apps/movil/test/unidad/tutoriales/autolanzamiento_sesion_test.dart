import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'comun.dart';

class _AlmacenEnMemoria implements AlmacenSeguro {
  final valores = <String, String>{};

  @override
  Future<String?> leer(String clave) async => valores[clave];

  @override
  Future<void> guardar(String clave, String valor) async =>
      valores[clave] = valor;

  @override
  Future<void> borrar(String clave) async => valores.remove(clave);
}

class _ProgresoDePrueba extends ProgresoDeTutoriales {
  @override
  Map<String, ProgresoDeTutorial> build() => const {};

  @override
  Future<void> cargar() async {}

  @override
  Future<void> guardar(ProgresoDeTutorial progreso) async {
    state = {...state, progreso.tutorialId: progreso};
  }
}

ProviderContainer _crearContenedor() => ProviderContainer(
  overrides: [
    almacenSeguroProvider.overrideWithValue(_AlmacenEnMemoria()),
    catalogoDeTutorialesProvider.overrideWithValue([
      tutorialDePrueba(guiaDeInicioId, ruta: '/billetera/inicio', pasos: 1),
    ]),
    progresoDeTutorialesProvider.overrideWith(_ProgresoDePrueba.new),
  ],
);

void main() {
  test('las capacidades de tutorial siguen los cambios de sesión', () async {
    final container = _crearContenedor();
    addTearDown(container.dispose);
    final sesion = container.read(sesionProvider);
    await sesion.hidratar();
    expect(await container.read(capacidadesProvider.future), isEmpty);

    await sesion.abrir(acceso: 'token');

    expect(await container.read(capacidadesProvider.future), {
      Capacidad.sesion,
    });
  });

  test('no ofrece la guía mientras la sesión no está activa', () async {
    final container = _crearContenedor();
    addTearDown(container.dispose);
    final sesion = container.read(sesionProvider);
    await sesion.hidratar();

    final lanzada = await container.read(autolanzarGuiaDeInicioProvider)();

    expect(lanzada, isFalse);
    expect(container.read(motorDeTutorialesProvider).activo, isFalse);
  });

  test('cerrar sesión cancela la guía sin registrarla como omitida', () async {
    final container = _crearContenedor();
    addTearDown(container.dispose);
    final sesion = container.read(sesionProvider);
    await sesion.abrir(acceso: 'token');

    final lanzada = await container.read(autolanzarGuiaDeInicioProvider)();
    expect(lanzada, isTrue);
    expect(container.read(motorDeTutorialesProvider).activo, isTrue);

    await sesion.cerrar(motivo: MotivoDeSalida.cerrada);

    expect(container.read(motorDeTutorialesProvider).activo, isFalse);
    expect(
      container.read(progresoDeTutorialesProvider)[guiaDeInicioId]?.estado,
      EstadoDeProgreso.enProgreso,
    );
  });
}
