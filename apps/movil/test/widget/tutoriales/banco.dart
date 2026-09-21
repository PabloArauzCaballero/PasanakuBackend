import 'dart:async';

import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_de_progreso.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/infraestructura/almacen_de_progreso_seguro.dart';
import 'package:aportaya_movil/pantallas/soporte/capa_de_tutorial.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../../comun.dart';

/// Dos pantallas de mentira con anclas de verdad: alcanza para probar el motor entero
/// sin arrastrar el alta, el saldo ni la red.
class PantallaDePrueba extends StatelessWidget {
  const PantallaDePrueba({
    super.key,
    required this.nombre,
    this.tardio = false,
  });

  final String nombre;
  final bool tardio;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: Column(
      children: [
        MarcaDeTutorial(id: '$nombre.uno', hijo: Text('$nombre uno')),
        MarcaDeTutorial(id: '$nombre.dos', hijo: Text('$nombre dos')),
        if (tardio)
          MarcaDeTutorial(id: '$nombre.tardio', hijo: Text('$nombre tardío')),
      ],
    ),
  );
}

/// El banco de pruebas: enrutador con `/uno` y `/dos`, anclas montadas y la capa del
/// tutorial encima, igual que en `app.dart`.
class BancoDeTutoriales {
  BancoDeTutoriales({required this.catalogo, this.inicial = '/uno'});

  final List<TutorialDefinicion> catalogo;
  final String inicial;
  final AlmacenDeProgreso almacen = AlmacenDeProgresoSeguro(AlmacenEnMemoria());
  late final ProviderContainer contenedor;
  late final GoRouter enrutador;
  final ValueNotifier<bool> _tardio = ValueNotifier(false);

  /// Hace aparecer el ancla que llega después, como la fila que trae una petición.
  void mostrarTardio() => _tardio.value = true;

  Widget montar() {
    enrutador = GoRouter(
      initialLocation: inicial,
      routes: [
        GoRoute(
          path: '/uno',
          builder: (context, state) => ValueListenableBuilder<bool>(
            valueListenable: _tardio,
            builder: (context, tardio, _) =>
                PantallaDePrueba(nombre: 'uno', tardio: tardio),
          ),
        ),
        GoRoute(
          path: '/dos',
          builder: (context, state) => const PantallaDePrueba(nombre: 'dos'),
        ),
      ],
    );
    contenedor = ProviderContainer(
      retry: (_, _) => null,
      overrides: [
        catalogoDeTutorialesProvider.overrideWithValue(catalogo),
        almacenDeProgresoProvider.overrideWithValue(almacen),
        capacidadesProvider.overrideWith((_) async => {Capacidad.sesion}),
      ],
    );
    return UncontrolledProviderScope(
      container: contenedor,
      child: MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        routerConfig: enrutador,
        builder: (context, child) => ProveedorDeAnclas(
          hijo: CapaDeTutorial(
            enrutador: enrutador,
            hijo: child ?? const SizedBox.shrink(),
          ),
        ),
      ),
    );
  }
}

/// **Bombear, no esperar.** El motor espera al ancla con temporizadores, y en
/// `flutter_test` el reloj solo avanza si alguien bombea cuadros: un `await` sobre
/// `iniciar()` se traba para siempre. Se lanza la acción y se bombea.
Future<void> correr(WidgetTester tester, Future<void> Function() accion) async {
  unawaited(accion());
  await asentarTutorial(tester);
}

/// Deja pasar el tiempo suficiente para que el motor encuentre —o deje de buscar— el
/// ancla del paso.
Future<void> asentarTutorial(WidgetTester tester, {int cuadros = 16}) async {
  for (var i = 0; i < cuadros; i++) {
    await tester.pump(const Duration(milliseconds: 50));
  }
}
