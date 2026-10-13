import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/atomos/esqueleto.dart';
import 'package:aportaya_diseno/atomos/esqueleto_brillante.dart';
import 'package:aportaya_diseno/atomos/entrada_de_estado.dart';
import 'package:aportaya_diseno/atomos/progreso.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/atomos/chip_elegible.dart';
import 'package:aportaya_diseno/atomos/grupo_radio.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/moleculas/chips_de_filtro.dart';
import 'package:aportaya_diseno/moleculas/riel_de_turnos.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/teclado_numerico.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/moviles/celebracion_de_exito.dart';
import 'package:aportaya_diseno/organismos/tarjeta_de_solicitud.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void _sinAccion() {}

/// Comportamiento observable: escribe, valida, emite. Lo que el usuario hace.
void main() {
  test('los estilos tipográficos salen de roles y usan cifras tabulares', () {
    expect(Tipo.titulo1.fontFamily, Fuente.display);
    expect(Tipo.cuerpo.fontFamily, Fuente.cuerpo);
    expect(Tipo.cifra.fontFamily, Fuente.display);
    expect(
      Tipo.cifraGrande.fontFeatures,
      contains(const FontFeature.tabularFigures()),
    );
    expect(
      Tipo.cifra.fontFeatures,
      contains(const FontFeature.tabularFigures()),
    );
    expect(Tipo.titulo1.height, 1.15);
    expect(Tipo.cuerpo.height, 1.5);
  });

  testWidgets('las tarjetas usan radios y sombras por tokens en dos capas', (
    tester,
  ) async {
    await tester.pumpWidget(conTema(const Tarjeta(child: Text('superficie'))));
    final decoracion = tester.widget<DecoratedBox>(
      find.descendant(
        of: find.byType(Tarjeta),
        matching: find.byType(DecoratedBox),
      ),
    );
    final caja = decoracion.decoration as BoxDecoration;
    expect(caja.borderRadius, BorderRadius.circular(Radios.lg));
    expect(caja.boxShadow, [Tokens.claro.sombra1, Tokens.claro.sombra2]);
  });

  testWidgets('la pestaña activa entra con fundido y rebote de 160 ms', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        BarraPestanas(
          destinos: [
            (
              texto: 'Inicio',
              icono: Icons.home_outlined,
              iconoActivo: Icons.home,
              novedades: 0,
            ),
            (
              texto: 'Grupos',
              icono: Icons.group_outlined,
              iconoActivo: Icons.group,
              novedades: 0,
            ),
            (
              texto: 'Perfil',
              icono: Icons.person_outline,
              iconoActivo: Icons.person,
              novedades: 0,
            ),
          ],
          actual: 0,
          onChanged: (_) {},
        ),
      ),
    );
    final cambio = tester.widget<AnimatedSwitcher>(
      find.descendant(
        of: find.byKey(const ValueKey('destino-Inicio')),
        matching: find.byType(AnimatedSwitcher),
      ),
    );
    expect(cambio.child, isA<Icon>());
    expect(cambio.duration, Movimiento.tildeDeValidez);
    final entrada = cambio.transitionBuilder(
      const Icon(Icons.home),
      const AlwaysStoppedAnimation<double>(0.5),
    );
    expect(entrada, isA<FadeTransition>());
    final rebote = (entrada as FadeTransition).child as ScaleTransition;
    expect(rebote.scale.value, greaterThan(1));
    expect(rebote.scale.value, lessThanOrEqualTo(1.12));
  });

  testWidgets('los estados vacío/error entran con fundido de 220 ms', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(const EstadoVacio(mensaje: 'Todavía no hay datos')),
    );
    expect(find.byType(EntradaDeEstado), findsOneWidget);
    expect(
      tester
          .widget<TweenAnimationBuilder<double>>(
            find.descendant(
              of: find.byType(EntradaDeEstado),
              matching: find.byType(TweenAnimationBuilder<double>),
            ),
          )
          .duration,
      Movimiento.entrada,
    );
    final entrada = find.descendant(
      of: find.byType(EntradaDeEstado),
      matching: find.byType(Opacity),
    );
    expect(tester.widget<Opacity>(entrada).opacity, 0);
    await tester.pumpAndSettle();
    expect(tester.widget<Opacity>(entrada).opacity, 1);

    await tester.pumpWidget(
      conTema(const EstadoError(error: 'fallo', reintentar: _sinAccion)),
    );
    expect(find.byType(EntradaDeEstado), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.text('Volver a intentar'), findsOneWidget);
  });

  testWidgets('reintentar gira una vez en 400 ms y ejecuta su acción', (
    tester,
  ) async {
    var reintentos = 0;
    await tester.pumpWidget(
      conTema(EstadoError(error: 'fallo', reintentar: () => reintentos++)),
    );
    await tester.pumpAndSettle();
    final giroIcono = find.descendant(
      of: find.byType(FilledButton),
      matching: find.byType(RotationTransition),
    );
    final giro = tester.widget<RotationTransition>(giroIcono);
    expect(giro.turns.value, 0);
    await tester.tap(find.text('Volver a intentar'));
    expect(reintentos, 1);
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 200));
    expect(
      tester.widget<RotationTransition>(giroIcono).turns.value,
      closeTo(0.5, 0.03),
    );
    await tester.pump(const Duration(milliseconds: 200));
    expect(tester.widget<RotationTransition>(giroIcono).turns.value, 1);
  });

  testWidgets('reintentar no gira con movimiento reducido', (tester) async {
    await tester.pumpWidget(
      conTema(
        Builder(
          builder: (context) => MediaQuery(
            data: MediaQuery.of(context).copyWith(disableAnimations: true),
            child: const EstadoError(error: 'fallo', reintentar: _sinAccion),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Volver a intentar'));
    await tester.pump(const Duration(milliseconds: 450));
    final giroIcono = find.descendant(
      of: find.byType(FilledButton),
      matching: find.byType(RotationTransition),
    );
    expect(tester.widget<RotationTransition>(giroIcono).turns.value, 0);
  });

  testWidgets('el turno actual pulsa entre 0,6 y 1 en 1,6 s', (tester) async {
    await tester.pumpWidget(
      conTema(const RielDeTurnos(total: 3, actual: 2, mio: 3)),
    );
    final turno = find.byKey(const ValueKey('turno-2'));
    final opacidadTurno = find.descendant(
      of: turno,
      matching: find.byType(FadeTransition),
    );
    expect(tester.widget<FadeTransition>(opacidadTurno).opacity.value, 0.6);
    await tester.pump(const Duration(milliseconds: 400));
    final opacidad = tester.widget<FadeTransition>(opacidadTurno).opacity.value;
    expect(opacidad, greaterThan(0.6));
    expect(opacidad, lessThanOrEqualTo(1));
    await tester.pump(const Duration(milliseconds: 400));
    expect(tester.widget<FadeTransition>(opacidadTurno).opacity.value, 1);
    await tester.pump(const Duration(milliseconds: 800));
    expect(tester.widget<FadeTransition>(opacidadTurno).opacity.value, 0.6);
  });

  testWidgets('el pulso se suprime al reducir movimiento', (tester) async {
    await tester.pumpWidget(
      conTema(
        Builder(
          builder: (context) => MediaQuery(
            data: MediaQuery.of(context).copyWith(disableAnimations: true),
            child: const RielDeTurnos(total: 3, actual: 2, mio: 3),
          ),
        ),
      ),
    );
    final turno = find.byKey(const ValueKey('turno-2'));
    final opacidadTurno = find.descendant(
      of: turno,
      matching: find.byType(FadeTransition),
    );
    expect(tester.widget<FadeTransition>(opacidadTurno).opacity.value, 1);
    await tester.pump(const Duration(seconds: 2));
    expect(tester.widget<FadeTransition>(opacidadTurno).opacity.value, 1);
  });

  testWidgets('el progreso numérico anima y conserva valor semántico final', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(const Progreso(valor: 0.8, etiqueta: 'Aportes del ciclo')),
    );
    expect(find.byType(TweenAnimationBuilder<double>), findsOneWidget);
    expect(find.bySemanticsLabel('Aportes del ciclo'), findsOneWidget);
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<LinearProgressIndicator>(find.byType(LinearProgressIndicator))
          .value,
      0.8,
    );
  });

  testWidgets('el esqueleto brilla con movimiento normal', (tester) async {
    await tester.pumpWidget(
      conTema(const EsqueletoBrillante(child: Esqueleto(filas: 2))),
    );
    expect(find.byType(ShaderMask), findsOneWidget);
    await tester.pump(const Duration(milliseconds: 500));
    expect(tester.takeException(), isNull);
  });

  testWidgets('el esqueleto no anima con movimiento reducido', (tester) async {
    await tester.pumpWidget(
      conTema(
        Builder(
          builder: (context) => MediaQuery(
            data: MediaQuery.of(context).copyWith(disableAnimations: true),
            child: const EsqueletoBrillante(child: Esqueleto(filas: 2)),
          ),
        ),
      ),
    );
    expect(find.byType(ShaderMask), findsNothing);
  });

  testWidgets('la celebración dibuja la tilde y termina a los 360 ms', (
    tester,
  ) async {
    var completa = 0;
    await tester.pumpWidget(
      conTema(CelebracionDeExito(onCompleto: () => completa++)),
    );
    expect(find.bySemanticsLabel('Cuenta creada'), findsOneWidget);
    await tester.pumpAndSettle();
    expect(completa, 1);
    expect(find.byType(CustomPaint), findsOneWidget);
  });

  testWidgets(
    'la celebración se asienta de inmediato con movimiento reducido',
    (tester) async {
      var completa = 0;
      await tester.pumpWidget(
        conTema(
          Builder(
            builder: (context) => MediaQuery(
              data: MediaQuery.of(context).copyWith(disableAnimations: true),
              child: CelebracionDeExito(onCompleto: () => completa++),
            ),
          ),
        ),
      );
      expect(completa, 1);
    },
  );

  testWidgets('la tilde de campo vÃ¡lido aparece escalada en 160 ms', (
    tester,
  ) async {
    var valido = false;
    await tester.pumpWidget(
      conTema(
        StatefulBuilder(
          builder: (context, setState) => Column(
            children: [
              Campo(etiqueta: 'Correo', exito: valido),
              TextButton(
                onPressed: () => setState(() => valido = true),
                child: const Text('validar'),
              ),
            ],
          ),
        ),
      ),
    );
    expect(find.byIcon(Icons.check_rounded), findsNothing);
    await tester.tap(find.text('validar'));
    await tester.pump();
    expect(find.byIcon(Icons.check_rounded), findsOneWidget);
    final aparicion = tester.widget<TweenAnimationBuilder<double>>(
      find.byType(TweenAnimationBuilder<double>),
    );
    expect(aparicion.duration, const Duration(milliseconds: 160));
    final escalaInicial = tester.widget<Transform>(
      find
          .ancestor(
            of: find.byIcon(Icons.check_rounded),
            matching: find.byType(Transform),
          )
          .first,
    );
    expect(escalaInicial.transform.storage[0], closeTo(0.6, 0.01));
    await tester.pump(const Duration(milliseconds: 160));
    final escalaFinal = tester.widget<Transform>(
      find
          .ancestor(
            of: find.byIcon(Icons.check_rounded),
            matching: find.byType(Transform),
          )
          .first,
    );
    expect(escalaFinal.transform.storage[0], closeTo(1, 0.01));
    expect(tester.takeException(), isNull);
  });

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

  testWidgets('SelectorSegmentado no ofrece control con una sola opción', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        SelectorSegmentado<String>(
          opciones: {'correo': 'Correo electrónico'},
          valor: 'correo',
          onChanged: (_) {},
        ),
      ),
    );

    expect(find.text('Correo electrónico'), findsOneWidget);
    expect(find.byType(GestureDetector), findsNothing);
  });

  testWidgets('GrupoRadio no ofrece control con una sola opción', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        GrupoRadio<String>(
          opciones: const {'correo': 'Correo electrónico'},
          valor: 'correo',
          onChanged: (_) {},
        ),
      ),
    );

    expect(find.text('Correo electrónico'), findsOneWidget);
    expect(find.byType(RadioListTile<String>), findsNothing);
  });

  testWidgets('ChipsDeFiltro no ofrece un chip con una sola opción', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        ChipsDeFiltro<int>(
          opciones: const [(clave: 1, texto: 'Todos', icono: Icons.filter_alt)],
          valor: 1,
          onChanged: (_) {},
        ),
      ),
    );

    expect(find.text('Todos'), findsOneWidget);
    expect(find.byType(ChipElegible), findsNothing);
  });

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
