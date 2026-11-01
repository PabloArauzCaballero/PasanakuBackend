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
    testWidgets(
      'la acción es verde, la urgencia naranja y lo irreversible rojo, en tema $brillo',
      (tester) async {
        final tokens = brillo == Brightness.dark ? Tokens.oscuro : Tokens.claro;
        for (final variante in [
          BotonVariante.urgencia,
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
            case BotonVariante.urgencia:
              // ADR-053 (A2): el naranja queda solo para pagar o aportar algo que vence.
              final superficie = tester.widget<SuperficieViva>(
                find.byType(SuperficieViva),
              );
              expect(superficie.color, tokens.accent, reason: '$variante');
              expect(frente, tokens.accentInk, reason: '$variante');
            case BotonVariante.secundario:
              final superficie = tester.widget<SuperficieViva>(
                find.byType(SuperficieViva),
              );
              expect(superficie.color, tokens.surface, reason: '$variante');
              expect(
                superficie.colorDelTexto,
                tokens.text,
                reason: '$variante',
              );
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
              expect(frente, tokens.brandTexto, reason: '$variante');
            case BotonVariante.primario:
              fail('La matriz no pretende cubrir $variante');
          }
        }

        final estiloEnlace = botonTextoDesde(tokens);
        expect(
          estiloEnlace.foregroundColor!.resolve({}),
          tokens.brandTexto,
          reason:
              'ADR-053 (A2): la acción común es verde de marca; el naranja '
              'queda para la urgencia de pagar',
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
        // Reintentar es la acción principal de un error: verde, no naranja.
        expect(
          botonReintentar.style!.backgroundColor!.resolve({}),
          tokens.verdeSolido,
        );
        expect(
          botonReintentar.style!.foregroundColor!.resolve({}),
          tokens.sobreVerdeSolido,
        );
      },
    );
  }
}

void _sinAccion() {}
