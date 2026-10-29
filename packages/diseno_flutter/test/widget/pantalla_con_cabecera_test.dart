import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

Widget _pantalla(String titulo) => Scaffold(
  body: PantallaConCabecera(
    titulo: titulo,
    cuerpo: ListView(
      children: [
        for (var i = 0; i < 40; i++)
          SizedBox(height: 50, child: Text('fila $i')),
      ],
    ),
  ),
);

Finder _caja() => find
    .descendant(
      of: find.byType(SliverPersistentHeader),
      matching: find.byType(DecoratedBox),
    )
    .first;

Finder _tituloGrande(String titulo) => find.byWidgetPredicate(
  (w) => w is Text && w.data == titulo && w.maxLines == 3,
);

void main() {
  testWidgets('arriba el título grande; al desplazar se compacta', (t) async {
    t.view.physicalSize = const Size(360, 640);
    t.view.devicePixelRatio = 1;
    addTearDown(t.view.reset);
    await t.pumpWidget(conTema(_pantalla('Título'), pantallaEntera: true));
    final alta = t.getRect(_caja()).height;

    await t.drag(find.text('fila 5'), const Offset(0, -300));
    await t.pumpAndSettle();

    expect(t.getRect(_caja()).height, lessThan(alta));
    expect(t.getRect(_caja()).height, kToolbarHeight);
  });

  testWidgets('el contenido no queda debajo de la cabecera compacta', (
    t,
  ) async {
    t.view.physicalSize = const Size(360, 640);
    t.view.devicePixelRatio = 1;
    addTearDown(t.view.reset);
    await t.pumpWidget(conTema(_pantalla('Título'), pantallaEntera: true));
    final colapso = t.getRect(_caja()).height - kToolbarHeight;

    await t.drag(find.text('fila 5'), Offset(0, -(colapso + 37)));
    await t.pumpAndSettle();

    // Lo que se desplazó de más es exactamente lo que la fila 0 subió por encima
    // del borde de la cabecera: nada se esconde detrás de ella.
    expect(
      t.getRect(find.text('fila 0')).top,
      closeTo(t.getRect(_caja()).bottom - 37, 0.5),
    );
  });

  testWidgets(
    'un solo encabezado para el lector, aunque el título se vea dos veces',
    (t) async {
      final semantica = t.ensureSemantics();
      await t.pumpWidget(conTema(_pantalla('Título'), pantallaEntera: true));
      expect(find.bySemanticsLabel('Título'), findsOneWidget);
      semantica.dispose();
    },
  );

  testWidgets(
    'un título largo con letra al 200 % baja de renglón sin desbordar',
    (t) async {
      t.view.physicalSize = const Size(360, 640);
      t.view.devicePixelRatio = 1;
      addTearDown(t.view.reset);
      const largo = 'Verificación pública del sorteo de tu grupo';
      await t.pumpWidget(
        conTema(_pantalla(largo), pantallaEntera: true, escala: 2),
      );
      expect(t.takeException(), isNull);
      final opacidad = t.widget<Opacity>(
        find
            .ancestor(of: _tituloGrande(largo), matching: find.byType(Opacity))
            .first,
      );
      expect(opacidad.opacity, 1);
      // Entero dentro de la cabecera: la medida del alto contempla los renglones.
      expect(
        t.getRect(_tituloGrande(largo)).bottom,
        lessThanOrEqualTo(t.getRect(_caja()).bottom),
      );
    },
  );
}
