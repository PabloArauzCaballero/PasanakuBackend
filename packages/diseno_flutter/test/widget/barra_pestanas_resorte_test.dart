import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _destinos = [
  (texto: 'Inicio', icono: Icons.home, iconoActivo: Icons.home, novedades: 0),
  (texto: 'Grupos', icono: Icons.group, iconoActivo: Icons.group, novedades: 0),
  (
    texto: 'Perfil',
    icono: Icons.person,
    iconoActivo: Icons.person,
    novedades: 0,
  ),
];

Widget _barra(int actual, {bool reducido = false}) => conTema(
  MediaQuery(
    data: MediaQueryData(
      disableAnimations: reducido,
      size: const Size(360, 800),
    ),
    child: Scaffold(
      bottomNavigationBar: BarraPestanas(
        destinos: _destinos,
        actual: actual,
        onChanged: (_) {},
      ),
    ),
  ),
  pantallaEntera: true,
);

/// H5.S2.M4 (apple-design): una sola píldora que se desliza con resorte crítico entre pestañas.
void main() {
  Finder pildora() => find.byWidgetPredicate(
    (w) => w is SizedBox && w.width == 64 && w.height == 36,
  );

  testWidgets('la píldora se desliza por posiciones intermedias y se asienta', (
    tester,
  ) async {
    await tester.pumpWidget(_barra(0));
    await tester.pumpAndSettle();
    final inicio = tester.getCenter(pildora()).dx;

    await tester.pumpWidget(_barra(2));
    await tester.pump(const Duration(milliseconds: 120));
    final enCamino = tester.getCenter(pildora()).dx;
    await tester.pumpAndSettle();
    final fin = tester.getCenter(pildora()).dx;

    expect(fin, greaterThan(inicio));
    expect(enCamino, greaterThan(inicio), reason: 'ya salió');
    expect(enCamino, lessThan(fin), reason: 'todavía no llegó: no es un salto');
    // Amortiguación crítica: no se pasa del destino.
    expect(enCamino, lessThanOrEqualTo(fin + 0.5));
  });

  testWidgets('con movimiento reducido llega de una vez', (tester) async {
    await tester.pumpWidget(_barra(0, reducido: true));
    await tester.pumpAndSettle();
    final inicio = tester.getCenter(pildora()).dx;
    await tester.pumpWidget(_barra(2, reducido: true));
    await tester.pump();
    final despues = tester.getCenter(pildora()).dx;
    await tester.pumpAndSettle();
    expect(despues, closeTo(tester.getCenter(pildora()).dx, 0.5));
    expect(despues, greaterThan(inicio));
  });

  testWidgets('la píldora queda detrás del ícono activo', (tester) async {
    await tester.pumpWidget(_barra(1));
    await tester.pumpAndSettle();
    final icono = tester.getCenter(find.byIcon(Icons.group));
    expect(tester.getCenter(pildora()).dx, closeTo(icono.dx, 1));
    expect(tester.getCenter(pildora()).dy, closeTo(icono.dy, 1));
  });
}
