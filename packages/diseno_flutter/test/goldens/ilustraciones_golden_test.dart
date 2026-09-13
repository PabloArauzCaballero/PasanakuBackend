import 'package:aportaya_diseno/ilustraciones/cuentas_a_la_vista.dart';
import 'package:aportaya_diseno/ilustraciones/lienzo.dart';
import 'package:aportaya_diseno/ilustraciones/plata_en_custodia.dart';
import 'package:aportaya_diseno/ilustraciones/rueda_de_gente.dart';
import 'package:aportaya_diseno/ilustraciones/sorteo_limpio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Las cuatro ilustraciones del tour, grandes y en los dos temas.
///
/// Son dibujos, no imágenes: acá se revisan como se revisaría un dibujo, mirándolo.
/// Y grandes a propósito —240 px— porque un vector que se ve bien chico puede ser un
/// borrón al tamaño que le toque mañana.
void main() {
  final pintores = <String, CustomPainter Function(Tokens)>{
    'rueda_de_gente': RuedaDeGente.new,
    'cuentas_a_la_vista': CuentasALaVista.new,
    'sorteo_limpio': SorteoLimpio.new,
    'plata_en_custodia': PlataEnCustodia.new,
  };

  for (final (tema, brillo) in [
    ('claro', Brightness.light),
    ('oscuro', Brightness.dark),
  ]) {
    testWidgets('las cuatro ilustraciones · $tema', (tester) async {
      pantallaChica(tester);
      await tester.pumpWidget(
        conTema(
          Wrap(
            spacing: Espacio.s4,
            runSpacing: Espacio.s4,
            children: [
              for (final entrada in pintores.entries)
                Ilustracion(
                  pintor: entrada.value,
                  etiqueta: entrada.key,
                  tamano: 150,
                ),
            ],
          ),
          brillo: brillo,
        ),
      );
      await tester.pumpAndSettle();
      await expectLater(
        find.byType(Wrap),
        matchesGoldenFile('imagenes/ilustraciones_$tema.png'),
      );
    });
  }
}
