import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/biometria.dart';
import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/capa_de_bloqueo.dart';
import 'package:aportaya_movil/proveedores/biometria.dart';
import 'package:aportaya_movil/proveedores/bloqueo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// 2026-10-19: en el teléfono del usuario «Entrar con huella o PIN» no hacía nada.
/// Si el sistema no puede abrir el diálogo, ahora el candado lo dice (con el código
/// del sistema) en vez de quedarse quieto. El diálogo nativo es un doble.
void main() {
  setUp(
    () => TestWidgetsFlutterBinding.instance.handleAppLifecycleStateChanged(
      AppLifecycleState.resumed,
    ),
  );

  Future<_BiometriaConFallo> montar(
    WidgetTester tester, {
    required List<(bool, String?)> respuestas,
  }) async {
    final biometria = _BiometriaConFallo(respuestas);
    final control = ControlDeBloqueo(esWeb: false)
      ..bloquear(MotivoDeBloqueo.segundoPlano);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bloqueoProvider.overrideWith((_) => control),
          biometriaProvider.overrideWithValue(biometria),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: CapaDeBloqueo(
            hijo: const Scaffold(body: Text('Contenido')),
            onIngresarDeNuevo: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return biometria;
  }

  final aviso = find.byKey(const ValueKey('candado-fallo-biometrico'));

  testWidgets('inválido: el sistema no abre el diálogo → el candado lo dice', (
    tester,
  ) async {
    // El pedido automático (al volver) falla con un error del sistema.
    await montar(tester, respuestas: [(false, '7: Demasiados intentos')]);
    expect(aviso, findsOneWidget);
    expect(find.textContaining('Demasiados intentos'), findsOneWidget);
  });

  testWidgets('límite: la persona cerró el diálogo → no hay mensaje de error', (
    tester,
  ) async {
    await montar(tester, respuestas: [(false, null)]);
    expect(aviso, findsNothing);
    expect(find.text('Entrar con huella o PIN'), findsOneWidget);
  });

  testWidgets('correcto: tocar de nuevo borra el error y vuelve a pedir', (
    tester,
  ) async {
    final biometria = await montar(
      tester,
      respuestas: [(false, '7: Demasiados intentos'), (true, null)],
    );
    expect(aviso, findsOneWidget);
    await tester.tap(find.text('Entrar con huella o PIN'));
    await tester.pumpAndSettle();
    expect(biometria.llamadas, 2);
    expect(find.text('Contenido'), findsOneWidget);
    expect(aviso, findsNothing);
  });
}

class _BiometriaConFallo implements Biometria, DiagnosticoDeBiometria {
  _BiometriaConFallo(this.respuestas);
  final List<(bool, String?)> respuestas;
  int llamadas = 0;

  @override
  String? ultimoFallo;

  @override
  Future<bool> disponible() async => true;

  @override
  Future<bool> confirmar(String motivo) async {
    final (confirmado, fallo) =
        respuestas[llamadas.clamp(0, respuestas.length - 1)];
    llamadas++;
    ultimoFallo = fallo;
    return confirmado;
  }
}
