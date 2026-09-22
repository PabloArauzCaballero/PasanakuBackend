import 'package:aportaya_movil/infraestructura/capacidades.dart';
import 'package:aportaya_movil/pantallas/soporte/arranque_segun_capacidades.dart';
import 'package:aportaya_movil/pantallas/soporte/pantalla_bloqueo_plataforma.dart';
import 'package:aportaya_movil/proveedores/capacidades.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// H2.S2.M3 (madre H6.S2.M3) -- en un release donde el almacén seguro o la
/// protección de pantalla no están soportados, se muestra la pantalla de bloqueo Y
/// EL RESTO DE LA APP NO SE CONSTRUYE: `builder` (que en `app.dart` es quien termina
/// creando el cliente HTTP) no debe llamarse.
///
/// `esRelease` se fuerza a `true` de forma explícita porque `kReleaseMode` es una
/// constante de compilación: un widget test SIEMPRE corre en modo debug/test, así
/// que sin este parámetro esta prueba no podría existir.
const _capacidadesSinProteccion = Capacidades(
  almacenSeguro: GradoDeSoporte.soportado,
  conectividad: GradoDeSoporte.soportado,
  biometria: GradoDeSoporte.noSoportado,
  avisosPush: GradoDeSoporte.noSoportado,
  camara: GradoDeSoporte.soportado,
  proteccionPantalla: GradoDeSoporte.noSoportado, // la crítica que dispara el gate
  haptica: GradoDeSoporte.soportado,
);

const _capacidadesPlenas = Capacidades(
  almacenSeguro: GradoDeSoporte.soportado,
  conectividad: GradoDeSoporte.soportado,
  biometria: GradoDeSoporte.soportado,
  avisosPush: GradoDeSoporte.soportado,
  camara: GradoDeSoporte.soportado,
  proteccionPantalla: GradoDeSoporte.soportado,
  haptica: GradoDeSoporte.soportado,
);

void main() {
  testWidgets(
    'release + seguridad crítica no soportada: pantalla de bloqueo, y el builder NUNCA se llama',
    (tester) async {
      var builderLlamado = false;
      await tester.pumpWidget(
        ProviderScope(
          overrides: [capacidadesProvider.overrideWithValue(_capacidadesSinProteccion)],
          child: MaterialApp(
            home: ArranqueSegunCapacidades(
              esRelease: true,
              builder: (context) {
                builderLlamado = true;
                return const Scaffold(body: Text('app real'));
              },
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(PantallaBloqueoPlataforma), findsOneWidget);
      expect(find.text('app real'), findsNothing);
      expect(
        builderLlamado,
        isFalse,
        reason: 'si el builder se llamó, el cliente HTTP se creó sin protección',
      );
    },
  );

  testWidgets(
    'release + capacidades plenas: se construye la app real, sin pantalla de bloqueo',
    (tester) async {
      var builderLlamado = false;
      await tester.pumpWidget(
        ProviderScope(
          overrides: [capacidadesProvider.overrideWithValue(_capacidadesPlenas)],
          child: MaterialApp(
            home: ArranqueSegunCapacidades(
              esRelease: true,
              builder: (context) {
                builderLlamado = true;
                return const Scaffold(body: Text('app real'));
              },
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(PantallaBloqueoPlataforma), findsNothing);
      expect(find.text('app real'), findsOneWidget);
      expect(builderLlamado, isTrue);
    },
  );

  testWidgets(
    'fuera de release (debug/perfil), no bloquea aunque falte protección -- solo protege el build de tienda',
    (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [capacidadesProvider.overrideWithValue(_capacidadesSinProteccion)],
          child: MaterialApp(
            home: ArranqueSegunCapacidades(
              esRelease: false,
              builder: (context) => const Scaffold(body: Text('app real')),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(PantallaBloqueoPlataforma), findsNothing);
      expect(find.text('app real'), findsOneWidget);
    },
  );
}
