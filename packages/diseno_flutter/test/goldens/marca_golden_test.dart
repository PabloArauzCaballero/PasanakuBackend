import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/moviles/apertura_de_marca.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// La marca y su apertura, congeladas en tres momentos.
///
/// Una animación no se revisa mirándola pasar: se revisa cuadro por cuadro. Estos
/// goldens fijan cómo se ve el acercamiento al principio, a mitad y cuando ya se está
/// yendo, así un cambio en las curvas se ve en el diff en vez de descubrirse en el
/// teléfono de alguien.
void main() {
  testWidgets('la marca, en sus dos temas', (tester) async {
    pantallaChica(tester);
    for (final (nombre, brillo) in [
      ('claro', Brightness.light),
      ('oscuro', Brightness.dark),
    ]) {
      await tester.pumpWidget(
        conTema(const Center(child: Marca(tamano: 120)), brillo: brillo),
      );
      await tester.pumpAndSettle();
      await expectLater(
        find.byType(Marca),
        matchesGoldenFile('imagenes/marca_$nombre.png'),
      );
    }
  });

  testWidgets('la apertura, cuadro por cuadro', (tester) async {
    pantallaChica(tester);
    await tester.pumpWidget(
      conTema(
        AperturaDeMarca(alTerminar: () {}),
        pantallaEntera: true,
      ),
    );
    // Los tres momentos que cuentan: la marca escribiéndose, el acercamiento a media
    // altura, y el final ya casi transparente. Los milisegundos son **incrementos**:
    // `pump` adelanta el reloj, no lo posiciona.
    for (final (nombre, avance) in [
      ('temprano', 280),
      ('medio', 420),
      ('tarde', 550),
    ]) {
      await tester.pump(Duration(milliseconds: avance));
      await expectLater(
        find.byType(AperturaDeMarca),
        matchesGoldenFile('imagenes/apertura_$nombre.png'),
      );
    }
    await tester.pumpAndSettle();
  });
}
