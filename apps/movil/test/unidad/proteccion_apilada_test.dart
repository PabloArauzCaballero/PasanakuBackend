import 'package:aportaya_movil/dominio/proteccion_de_pantalla_dinero.dart';
import 'package:aportaya_movil/dominio/puertos/proteccion_pantalla.dart';
import 'package:aportaya_movil/proveedores/proteccion_pantalla.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// DEFECTO-004: con dos pantallas protegidas apiladas, cerrar la de arriba apagaba
/// FLAG_SECURE y dejaba la de abajo (con saldo) capturable.
class _Puerto implements ProteccionPantalla {
  bool activa = false;
  int activaciones = 0;
  @override
  Future<void> activar() async {
    activa = true;
    activaciones++;
  }

  @override
  Future<void> desactivar() async => activa = false;
}

class _Protegida extends ConsumerStatefulWidget {
  const _Protegida(this.nombre);
  final String nombre;
  @override
  ConsumerState<_Protegida> createState() => _ProtegidaState();
}

class _ProtegidaState extends ConsumerState<_Protegida>
    with ConProteccionDePantalla {
  @override
  Widget build(BuildContext context) => Text(widget.nombre);
}

Future<GlobalKey<NavigatorState>> _montar(
  WidgetTester tester,
  _Puerto puerto,
) async {
  final navegador = GlobalKey<NavigatorState>();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [proteccionPantallaProvider.overrideWithValue(puerto)],
      child: MaterialApp(navigatorKey: navegador, home: const Text('inicio')),
    ),
  );
  return navegador;
}

void main() {
  testWidgets('correcto: una pantalla protegida activa y al salir desactiva', (
    tester,
  ) async {
    final puerto = _Puerto();
    final nav = await _montar(tester, puerto);
    nav.currentState!.push(
      MaterialPageRoute<void>(builder: (_) => const _Protegida('saldo')),
    );
    await tester.pumpAndSettle();
    expect(puerto.activa, isTrue);
    nav.currentState!.pop();
    await tester.pumpAndSettle();
    expect(puerto.activa, isFalse);
  });

  testWidgets(
    'límite: al cerrar la protegida de arriba, la de abajo sigue protegida',
    (tester) async {
      final puerto = _Puerto();
      final nav = await _montar(tester, puerto);
      nav.currentState!.push(
        MaterialPageRoute<void>(builder: (_) => const _Protegida('extracto')),
      );
      await tester.pumpAndSettle();
      nav.currentState!.push(
        MaterialPageRoute<void>(builder: (_) => const _Protegida('retirar')),
      );
      await tester.pumpAndSettle();
      nav.currentState!.pop();
      await tester.pumpAndSettle();
      expect(puerto.activa, isTrue, reason: 'el extracto sigue en pantalla');
      nav.currentState!.pop();
      await tester.pumpAndSettle();
      expect(puerto.activa, isFalse);
    },
  );

  testWidgets(
    'inválido: una protegida que se desmonta antes de activarse no apaga a otra',
    (tester) async {
      final puerto = _Puerto();
      final nav = await _montar(tester, puerto);
      nav.currentState!.push(
        MaterialPageRoute<void>(builder: (_) => const _Protegida('extracto')),
      );
      await tester.pumpAndSettle();
      // Se monta y se quita en el mismo cuadro: nunca llega a activar.
      nav.currentState!.push(
        MaterialPageRoute<void>(builder: (_) => const _Protegida('fugaz')),
      );
      nav.currentState!.pop();
      await tester.pumpAndSettle();
      expect(puerto.activa, isTrue);
    },
  );
}
