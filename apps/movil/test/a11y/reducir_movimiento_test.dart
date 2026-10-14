import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/atomos/monto_que_sube.dart';
import 'package:aportaya_diseno/atomos/numero_que_sube.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/cabecera_de_fase.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/cuenta_atras_grande.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/riel_de_capturas.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('el helper central convierte cualquier animación en cero', (
    tester,
  ) async {
    late bool quieto;
    late Duration sinMovimiento;
    late Duration normal;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => MediaQuery(
            data: MediaQuery.of(context).copyWith(disableAnimations: true),
            child: Builder(
              builder: (context) {
                quieto = reducirMovimiento(context);
                sinMovimiento = duracion(context, Movimiento.pagina);
                normal = duracion(context, const Duration(milliseconds: 200));
                return const SizedBox.shrink();
              },
            ),
          ),
        ),
      ),
    );

    expect(quieto, isTrue);
    expect(sinMovimiento, Duration.zero);
    expect(normal, Duration.zero);
  });

  testWidgets(
    'pantallas y controles animados se asientan con reducir movimiento',
    (tester) async {
      final escenarios = <(String, Widget)>[
        ('fase', const CabeceraDeFase(numero: 2, titulo: 'Tu identidad')),
        ('cuenta atrás', const CuentaAtrasGrande(segundos: 3)),
        ('saldo', const MontoQueSube(monto: '123.45', moneda: 'BOB')),
        ('contador', const NumeroQueSube(valor: 5)),
        (
          'selector segmentado',
          SelectorSegmentado<String>(
            opciones: const {'a': 'A', 'b': 'B'},
            valor: 'a',
            onChanged: (_) {},
          ),
        ),
        ('barra de puntos', const BarraDePuntos(total: 4, actual: 1)),
        (
          'riel de capturas',
          RielDeCapturas(
            actual: CaraDelCarril.anverso,
            listas: const {},
            onElegir: (_) {},
          ),
        ),
        (
          'tarjeta de captura',
          const SizedBox(
            height: 300,
            child: TarjetaDeCaptura(cara: CaraDelCarril.anverso, captura: null),
          ),
        ),
        (
          'entrada escalonada',
          const AparicionEscalonada(
            children: [Text('Primero'), Text('Después')],
          ),
        ),
      ];

      for (final (nombre, widget) in escenarios) {
        await tester.pumpWidget(
          MediaQuery(
            data: const MediaQueryData(
              size: Size(400, 800),
              disableAnimations: true,
            ),
            child: Directionality(
              textDirection: TextDirection.ltr,
              child: Theme(
                data: temaDesde(Tokens.claro, Brightness.light),
                child: Scaffold(body: widget),
              ),
            ),
          ),
        );
        final cuadros = await tester.pumpAndSettle();
        expect(cuadros, 1, reason: '$nombre debe asentarse en un cuadro');
        for (final animacion in tester.widgetList<AnimatedContainer>(
          find.byType(AnimatedContainer),
        )) {
          expect(animacion.duration, Duration.zero, reason: nombre);
        }
        for (final animacion in tester.widgetList<AnimatedSwitcher>(
          find.byType(AnimatedSwitcher),
        )) {
          expect(animacion.duration, Duration.zero, reason: nombre);
        }
        for (final animacion
            in tester.widgetList<TweenAnimationBuilder<dynamic>>(
              find.byType(TweenAnimationBuilder),
            )) {
          expect(animacion.duration, Duration.zero, reason: nombre);
        }
      }
    },
  );
}
