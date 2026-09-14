import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/moleculas/chips_de_filtro.dart';
import 'package:aportaya_diseno/moviles/teclado_numerico.dart';
import 'package:aportaya_diseno/organismos/tarjeta_de_solicitud.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Comportamiento observable: escribe, valida, emite. Lo que el usuario hace.
void main() {
  testWidgets(
    'Boton cargando no dispara dos veces: el doble toque no duplica un efecto',
    (tester) async {
      var toques = 0;
      await tester.pumpWidget(
        conTema(
          StatefulBuilder(
            builder: (context, setState) {
              return Boton(
                texto: 'Confirmar',
                variante: BotonVariante.primario,
                cargando: toques > 0,
                onPressed: () => setState(() => toques += 1),
              );
            },
          ),
        ),
      );
      await tester.tap(find.text('Confirmar'));
      await tester.pump();
      await tester.tap(find.text('Confirmar'), warnIfMissed: false);
      await tester.pump();
      expect(toques, 1);
      expect(find.bySemanticsLabel('Confirmar, enviando'), findsOneWidget);
    },
  );

  testWidgets('CampoMonto entrega la cadena del contrato, nunca un double', (
    tester,
  ) async {
    String? recibido;
    await tester.pumpWidget(
      conTema(CampoMonto(etiqueta: 'Monto', onChanged: (v) => recibido = v)),
    );
    await tester.enterText(find.byType(TextField), '1.240,5');
    expect(recibido, '1240.50');
    await tester.enterText(find.byType(TextField), '12,345');
    expect(recibido, isNull);
    // Sin el espacio de antes: el prefijo dejo de ser `prefixText` —que Flutter
    // esconde hasta que el campo tiene foco— y pasa a ser un `Text` en el adorno,
    // visible siempre.
    expect(find.text('Bs'), findsOneWidget);
  });

  testWidgets('CampoOTP avanza solo y emite al completar los seis dígitos', (
    tester,
  ) async {
    String? codigo;
    await tester.pumpWidget(conTema(CampoOTP(onCompleto: (c) => codigo = c)));
    final celdas = find.byType(TextField);
    expect(celdas, findsNWidgets(6));
    for (var i = 0; i < 6; i += 1) {
      await tester.enterText(celdas.at(i), '$i');
      await tester.pump();
    }
    expect(codigo, '012345');
  });

  testWidgets(
    'SelectorSegmentado pinta el elegido con relleno de marca y cambia al tocar',
    (tester) async {
      var valor = 'lista';
      await tester.pumpWidget(
        conTema(
          StatefulBuilder(
            builder: (context, setState) {
              return SelectorSegmentado<String>(
                opciones: const {'lista': 'Lista', 'cal': 'Calendario'},
                valor: valor,
                onChanged: (v) => setState(() => valor = v),
              );
            },
          ),
        ),
      );
      await tester.tap(find.text('Calendario'));
      await tester.pumpAndSettle();
      expect(valor, 'cal');
      final contenedor = tester.widget<AnimatedContainer>(
        find.ancestor(
          of: find.text('Calendario'),
          matching: find.byType(AnimatedContainer),
        ),
      );
      // El elegido no depende de que dos fondos difieran: lleva el verde sólido (D-12).
      expect(
        (contenedor.decoration as BoxDecoration).color,
        Tokens.claro.verdeSolido,
      );
    },
  );

  testWidgets('ChipsDeFiltro se acomodan en varias líneas, nunca en carrusel', (
    tester,
  ) async {
    pantallaChica(tester);
    await tester.pumpWidget(
      conTema(
        ChipsDeFiltro<int>(
          opciones: [
            for (var i = 0; i < 8; i += 1)
              (clave: i, texto: 'Filtro $i', icono: Icons.filter_alt),
          ],
          valor: 0,
          onChanged: (_) {},
        ),
      ),
    );
    final wrap = tester.widget<Wrap>(find.byType(Wrap));
    expect(wrap.children.length, 8);
    expect(
      find
          .byType(SingleChildScrollView, skipOffstage: false)
          .evaluate()
          .where(
            (e) =>
                (e.widget as SingleChildScrollView).scrollDirection ==
                Axis.horizontal,
          ),
      isEmpty,
    );
  });

  testWidgets('TecladoNumerico emite dígitos, coma y borrar', (tester) async {
    final tecleado = <String>[];
    var borrados = 0;
    await tester.pumpWidget(
      conTema(
        TecladoNumerico(onTecla: tecleado.add, onBorrar: () => borrados += 1),
      ),
    );
    await tester.tap(find.text('7'));
    await tester.tap(find.text(','));
    await tester.tap(find.bySemanticsLabel('Borrar'));
    expect(tecleado, ['7', ',']);
    expect(borrados, 1);
  });

  testWidgets('TarjetaDeSolicitud no deja rechazar en blanco', (tester) async {
    String? motivo;
    await tester.pumpWidget(
      conTema(
        TarjetaDeSolicitud(
          nombre: 'Juan',
          venceIso: '2026-09-10T12:00:00Z',
          aFavor: const ['ok'],
          enContra: const [],
          onAceptar: () {},
          onRechazar: (m) => motivo = m,
        ),
      ),
    );
    await tester.tap(find.text('Rechazar'));
    await tester.pumpAndSettle();
    final confirmar = find.text('Confirmar rechazo');
    expect(confirmar, findsOneWidget);
    await tester.tap(confirmar, warnIfMissed: false);
    await tester.pump();
    expect(motivo, isNull, reason: 'sin motivo no hay nada que apelar');
    await tester.enterText(
      find.byType(TextField),
      'Es su primer grupo de este monto y ya tiene uno en mora.',
    );
    await tester.pumpAndSettle();
    await tester.tap(confirmar);
    expect(motivo, isNotNull);
  });
}
