// Helpers compartidos por los siete recorridos de F12.1. Sin `patrol` en pubspec.yaml
// (ver LEEME.md) esto no compila: es andamiaje, no código ejecutable en este sandbox.
import 'package:aportaya_movil/app.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:patrol/patrol.dart';

/// Arranca la app real (mismo árbol que `lib/main.dart`) contra el gateway simulado
/// (`yarn dev:mock`, puerto 4010) — nunca un doble en memoria: la promesa de F12.1 es
/// que esto ejercite la app de punta a punta, tal como la instala una persona.
Future<void> arrancarApp(PatrolIntegrationTester $) async {
  await $.pumpWidgetAndSettle(
    ProviderScope(retry: (retryCount, error) => null, child: AppAportaYa()),
  );
}
