// Helpers compartidos por los siete recorridos de F12.1. Estado: LEEME.md.
import 'package:aportaya_movil/app.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:patrol/patrol.dart';

/// Arranca la app real (mismo árbol que `lib/main.dart`) contra el gateway simulado
/// (`yarn dev:mock`, puerto 4010) — nunca un doble en memoria: la promesa de F12.1 es
/// que esto ejercite la app de punta a punta, tal como la instala una persona.
///
/// Arranca con `pumpAndTrySettle`, el mismo criterio que usan las demás acciones de
/// Patrol por configuración: el botón primario tiene un brillo que cruza unas pocas veces
/// (~21 s en total, `SuperficieViva`) y `pumpWidgetAndSettle` exige que no quede ni un
/// cuadro pendiente en 10 s, así que fallaba sin que nada estuviera mal. Lo que cada
/// recorrido verifica sigue siendo un elemento visible (`waitUntilVisible`).
Future<void> arrancarApp(PatrolIntegrationTester $, {String? inicial}) async {
  await $.tester.pumpWidget(
    ProviderScope(
      retry: (retryCount, error) => null,
      child: AppAportaYa(inicial: inicial),
    ),
  );
  await $.pumpAndTrySettle();
}
