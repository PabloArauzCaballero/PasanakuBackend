import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/garantias_de_portada.dart';
import 'package:aportaya_movil/pantallas/identidad/lamina_del_tour.dart';
import 'package:aportaya_movil/pantallas/identidad/laminas_del_tour.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/consejos_para_la_foto.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/guia_de_encuadre.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart';
import 'package:aportaya_movil/pantallas/billetera/lista_de_movimientos.dart';
import 'package:aportaya_movil/pantallas/notificaciones/dominio.dart';
import 'package:aportaya_movil/pantallas/notificaciones/pantalla_de_bandeja.dart';
import 'package:aportaya_movil/pantallas/notificaciones/proveedor_bandeja.dart';
import 'package:aportaya_movil/pantallas/soporte/resumen_de_avance.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../comun.dart';

void main() {
  for (final brillo in Brightness.values) {
    final tokens = brillo == Brightness.dark ? Tokens.oscuro : Tokens.claro;

    testWidgets('notificación de marca no comunica estado verde: $brillo', (
      tester,
    ) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [bandejaProvider.overrideWith(_BandejaConMarca.new)],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: brillo == Brightness.dark
                ? ThemeMode.dark
                : ThemeMode.light,
            home: const PantallaDeBandeja(),
          ),
        ),
      );

      expect(
        tester.widget<Icon>(find.byIcon(Icons.notifications_outlined)).color,
        tokens.text2,
      );
      expect(
        tester.widget<CircleAvatar>(find.byType(CircleAvatar)).backgroundColor,
        tokens.surface2,
      );
    });

    testWidgets('el tipo de movimiento no es un estado verde: $brillo', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();
      final credito = MovimientoBilletera(
        id: 'movimiento-credito',
        sentido: MovimientoBilleteraSentidoEnum.CREDITO,
        monto: Dinero(monto: '25.00', moneda: DineroMonedaEnum.BOB),
        saldoDisponiblePosterior: Dinero(
          monto: '125.00',
          moneda: DineroMonedaEnum.BOB,
        ),
        saldoRetenidoPosterior: Dinero(
          monto: '0.00',
          moneda: DineroMonedaEnum.BOB,
        ),
        glosa: 'Aporte',
        registradoEn: DateTime.utc(2026, 10, 13, 10),
      );

      await tester.pumpWidget(
        conApp(
          Scaffold(body: FilaDeMovimiento(movimiento: credito)),
          dio: dio,
          brillo: brillo,
        ),
      );

      expect(
        tester.widget<Icon>(find.byIcon(IconosDeMarca.entra)).color,
        tokens.text2,
      );
    });

    testWidgets('contenido no-estado usa roles neutros: $brillo', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();

      await tester.pumpWidget(
        conApp(const GarantiasDePortada(), dio: dio, brillo: brillo),
      );
      final garantia = find.textContaining('custodia');
      expect(tester.widget<Text>(garantia).style?.color, tokens.text2);
      final pastilla = find
          .ancestor(of: garantia, matching: find.byType(DecoratedBox))
          .first;
      expect(
        (tester.widget<DecoratedBox>(pastilla).decoration as BoxDecoration)
            .color,
        tokens.surface2,
      );

      await tester.pumpWidget(
        conApp(
          LaminaDelTour(lamina: laminasDelTour.first, desplazamiento: 0),
          dio: dio,
          brillo: brillo,
        ),
      );
      final titular = tester.widget<Text>(
        find.byWidgetPredicate(
          (widget) => widget is Text && widget.textSpan != null,
        ),
      );
      final span = titular.textSpan! as TextSpan;
      expect((span.children![1] as TextSpan).style?.color, tokens.text);

      await tester.pumpWidget(
        conApp(
          const ConsejosParaLaFoto(cara: CaraDelCarril.anverso, abierto: true),
          dio: dio,
          brillo: brillo,
        ),
      );
      expect(
        tester.widget<Icon>(find.byIcon(Icons.wb_sunny_outlined)).color,
        tokens.text2,
      );
      final acordeon = tester.widget<ExpansionTile>(find.byType(ExpansionTile));
      expect(acordeon.backgroundColor, tokens.surface2);
      expect(acordeon.collapsedBackgroundColor, tokens.surface2);

      await tester.pumpWidget(
        conApp(
          const ResumenDeAvance(hechos: 2, total: 5, fraccion: 0.4),
          dio: dio,
          brillo: brillo,
        ),
      );
      expect(
        tester.widget<Text>(find.text('Tu avance')).style?.color,
        tokens.text,
      );

      await tester.pumpWidget(
        conApp(
          const SizedBox(
            height: 400,
            child: TarjetaDeCaptura(cara: CaraDelCarril.anverso, captura: null),
          ),
          dio: dio,
          brillo: brillo,
        ),
      );
      final guia = tester.widget<GuiaDeEncuadre>(find.byType(GuiaDeEncuadre));
      expect(guia.color, tokens.text2);
      expect(guia.esquemaDe, CaraDelCarril.anverso);
      final tarjetaVisual = tester.widget<DecoratedBox>(
        find.descendant(
          of: find.byType(TarjetaDeCaptura),
          matching: find.byWidgetPredicate(
            (widget) =>
                widget is DecoratedBox &&
                widget.decoration is BoxDecoration &&
                (widget.decoration as BoxDecoration).borderRadius ==
                    BorderRadius.circular(Radios.lg),
          ),
        ),
      );
      final decoracion = tarjetaVisual.decoration as BoxDecoration;
      expect(decoracion.gradient, isNull);
      expect(decoracion.color, tokens.surface);
    });
  }
}

class _BandejaConMarca extends BandejaNotificaciones {
  @override
  List<Notificacion> build() => [
    Notificacion(
      id: 'aviso-marca',
      titulo: 'Novedad de AportaYa',
      detalle: 'Hay una actualización informativa.',
      cuando: DateTime.utc(2026, 10, 13),
      tono: 'marca',
      leida: false,
    ),
  ];
}
