import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/superficie_viva.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tema_de_controles.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in Brightness.values) {
    testWidgets('las acciones usan el acento; lo irreversible, rojo, en tema $brillo', (
      tester,
    ) async {
      final tokens = brillo == Brightness.dark ? Tokens.oscuro : Tokens.claro;
      for (final variante in [
        BotonVariante.secundario,
        BotonVariante.fantasma,
        BotonVariante.peligro,
        BotonVariante.enlace,
      ]) {
        await tester.pumpWidget(
          conTema(
            Boton(texto: 'Acción', variante: variante, onPressed: () {}),
            brillo: brillo,
          ),
        );
        final boton = tester.widget<FilledButton>(find.byType(FilledButton));
        final frente = boton.style!.foregroundColor!.resolve({});

        switch (variante) {
          case BotonVariante.secundario:
            final superficie = tester.widget<SuperficieViva>(
              find.byType(SuperficieViva),
            );
            expect(superficie.color, tokens.surface2, reason: '$variante');
            expect(superficie.colorDelTexto, tokens.text, reason: '$variante');
            expect(frente, tokens.text, reason: '$variante');
          case BotonVariante.peligro:
            final superficie = tester.widget<SuperficieViva>(
              find.byType(SuperficieViva),
            );
            // Lo irreversible se distingue de la acción principal: rojo sólido.
            expect(superficie.color, tokens.rojoSolido, reason: '$variante');
            expect(
              superficie.colorDelTexto,
              tokens.sobreRojoSolido,
              reason: '$variante',
            );
            expect(frente, tokens.sobreRojoSolido, reason: '$variante');
          case BotonVariante.fantasma:
            expect(frente, tokens.text2, reason: '$variante');
          case BotonVariante.enlace:
            expect(frente, tokens.accentTexto, reason: '$variante');
          case BotonVariante.primario:
            fail('La matriz no pretende cubrir $variante');
        }
      }

      final estiloEnlace = botonTextoDesde(tokens);
      expect(
        estiloEnlace.foregroundColor!.resolve({}),
        tokens.accentTexto,
        reason: 'TextButton usa el acento de acción en vez del verde de marca',
      );

      await tester.pumpWidget(
        conTema(
          const EstadoError(error: 'fallo', reintentar: _sinAccion),
          brillo: brillo,
        ),
      );
      final botonReintentar = tester.widget<FilledButton>(
        find.byType(FilledButton),
      );
      expect(
        botonReintentar.style!.backgroundColor!.resolve({}),
        tokens.accent,
      );
      expect(
        botonReintentar.style!.foregroundColor!.resolve({}),
        tokens.accentInk,
      );
    });
  }
}

void _sinAccion() {}
