// Helpers compartidos por los siete recorridos de F12.1. Estado: LEEME.md.
import 'package:aportaya_movil/app.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
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

/// Espera [texto] visible y, si no aparece, falla diciendo **por qué** (F12.1, intermitente
/// del enlace en caliente en el simulador de CI, 2026-11-21).
///
/// El plazo es el de Patrol, sin cambios. Lo que se agrega es el diagnóstico: si el texto
/// está en el árbol pero algo lo tapa, en qué estado de ciclo de vida quedó la app y a qué
/// ruta llegó el enrutador. Con eso, la próxima falla distingue «el enlace no llegó» de
/// «llegó tarde» o de «llegó y quedó tapado», en lugar de solo «Found 0 widgets».
/// La ruta se recorta a sus dos primeros tramos: el código de invitación lleva un secreto.
Future<void> esperarVisibleConDiagnostico(
  PatrolIntegrationTester $,
  String texto,
) async {
  final inicio = DateTime.now();
  try {
    await $(texto).waitUntilVisible();
  } on Object catch (error) {
    final enArbol = find.text(texto).evaluate().length;
    final tocables = find.text(texto).hitTestable().evaluate().length;
    final navegadores = find.byType(Navigator).evaluate();
    String ruta = '?';
    if (navegadores.isNotEmpty) {
      try {
        final tramos = GoRouter.of(navegadores.first).state.uri.pathSegments;
        ruta = '/${tramos.take(2).join('/')}${tramos.length > 2 ? '/…' : ''}';
      } on Object {
        ruta = 'sin GoRouter en el árbol';
      }
    }
    throw TestFailure(
      '«$texto» no se vio en ${DateTime.now().difference(inicio).inMilliseconds} ms · '
      'en el árbol: $enArbol · tocables: $tocables · '
      'ciclo de vida: ${WidgetsBinding.instance.lifecycleState} · ruta: $ruta\n'
      '$error',
    );
  }
}
