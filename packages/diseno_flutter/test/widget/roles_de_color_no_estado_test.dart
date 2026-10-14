import 'package:aportaya_diseno/atomos/boton_icono.dart';
import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/atomos/pintor_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/moleculas/acciones_rapidas.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/moleculas/desglose_de_cobro.dart';
import 'package:aportaya_diseno/moleculas/fila_de_movimiento.dart';
import 'package:aportaya_diseno/moleculas/medidor_de_rango.dart';
import 'package:aportaya_diseno/moleculas/resumen_de_periodo.dart';
import 'package:aportaya_diseno/moleculas/tarjeta_saldo.dart';
import 'package:aportaya_diseno/moleculas/tipo_de_movimiento.dart';
import 'package:aportaya_diseno/organismos/lista_de_movimientos.dart';
import 'package:aportaya_diseno/organismos/panel_sorteo.dart';
import 'package:aportaya_diseno/organismos/tarjeta_de_solicitud.dart';
import 'package:aportaya_diseno/organismos/tarjeta_de_oferta.dart';
import 'package:aportaya_diseno/moviles/notificacion_emergente.dart';
import 'package:aportaya_diseno/moviles/fondo_de_marca.dart';
import 'package:aportaya_diseno/atomos/pintor_de_aura.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in Brightness.values) {
    final tokens = brillo == Brightness.dark ? Tokens.oscuro : Tokens.claro;

    testWidgets('marca usa un acento y trazo neutral: $brillo', (tester) async {
      await tester.pumpWidget(conTema(const Marca(tamano: 64), brillo: brillo));
      final marca = tester.widget<CustomPaint>(find.byType(CustomPaint));
      final pintorDeMarca = marca.painter! as PintorDeMarca;
      expect(pintorDeMarca.trazo, tokens.text);
      expect(pintorDeMarca.acento, tokens.accent);

      await tester.pumpWidget(
        conTema(
          const FondoDeMarca(hijo: SizedBox.expand()),
          brillo: brillo,
          pantallaEntera: true,
        ),
      );
      final aura = tester.widget<CustomPaint>(find.byType(CustomPaint));
      final pintorDeAura = aura.painter! as PintorDeAura;
      final alpha = brillo == Brightness.dark ? 0.18 : 0.09;
      expect(pintorDeAura.halo, tokens.surface.withValues(alpha: alpha));
      expect(
        pintorDeAura.linea,
        tokens.surface.withValues(
          alpha: brillo == Brightness.dark ? 0.10 : 0.06,
        ),
      );
      expect(
        pintorDeAura.brillo,
        tokens.accent.withValues(
          alpha: brillo == Brightness.dark ? 0.14 : 0.08,
        ),
      );
    });

    testWidgets('polaridad de solicitud usa iconos neutrales: $brillo', (
      tester,
    ) async {
      await tester.pumpWidget(
        conTema(
          TarjetaDeSolicitud(
            nombre: 'Ana',
            venceIso: '2026-10-20',
            aFavor: const ['Aporte'],
            enContra: const ['Reverso'],
            onAceptar: _sinAccion,
            onRechazar: (_) {},
          ),
          brillo: brillo,
        ),
      );
      expect(tester.widget<Icon>(find.byIcon(Icons.add)).color, tokens.text2);
      expect(
        tester.widget<Icon>(find.byIcon(Icons.remove)).color,
        tokens.text2,
      );
    });

    testWidgets('acciones y valores no semánticos no usan verde: $brillo', (
      tester,
    ) async {
      await tester.pumpWidget(
        conTema(
          BotonIcono(icono: Icons.close, etiqueta: 'Cerrar', onPressed: () {}),
          brillo: brillo,
        ),
      );
      expect(
        tester.widget<IconButton>(find.byType(IconButton)).color,
        tokens.accentTexto,
      );

      await tester.pumpWidget(
        conTema(const Avatar(nombre: 'Rosa Aduviri'), brillo: brillo),
      );
      final avatar = tester.widget<CircleAvatar>(find.byType(CircleAvatar));
      expect(avatar.backgroundColor, tokens.surface2);
      expect(avatar.foregroundColor, tokens.text);

      await tester.pumpWidget(
        conTema(
          CampoDeFecha(
            etiqueta: 'Fecha de nacimiento',
            valor: null,
            primera: DateTime(1920),
            ultima: DateTime(2008, 10, 3),
            onElegida: (_) {},
          ),
          brillo: brillo,
        ),
      );
      expect(
        tester.widget<Icon>(find.byIcon(Icons.calendar_month_outlined)).color,
        tokens.accentTexto,
      );

      await tester.pumpWidget(
        conTema(
          const AccionesRapidas(
            acciones: [
              (texto: 'Recargar', icono: Icons.refresh, onTap: _sinAccion),
              (texto: 'Retirar', icono: Icons.north_east, onTap: _sinAccion),
            ],
          ),
          brillo: brillo,
        ),
      );
      for (final icono in [Icons.refresh, Icons.north_east]) {
        expect(
          tester.widget<Icon>(find.byIcon(icono)).color,
          tokens.accentTexto,
        );
        final mosaico = find
            .ancestor(of: find.byIcon(icono), matching: find.byType(Container))
            .first;
        expect(
          (tester.widget<Container>(mosaico).decoration! as BoxDecoration)
              .color,
          tokens.surface2,
        );
      }

      await tester.pumpWidget(
        conTema(
          const TarjetaSaldo(disponible: '100.00', moneda: 'BOB'),
          brillo: brillo,
        ),
      );
      final tarjeta = find
          .descendant(
            of: find.byType(TarjetaSaldo),
            matching: find.byType(Container),
          )
          .first;
      expect(
        (tester.widget<Container>(tarjeta).decoration! as BoxDecoration).color,
        tokens.surface2,
      );
      expect(
        tester.widget<Monto>(find.byType(Monto).first).estilo?.color,
        tokens.text,
      );

      await tester.pumpWidget(
        conTema(
          const FilaDeMovimiento(
            tipo: TipoDeMovimiento.aporte,
            concepto: 'Aporte al grupo',
            monto: '25.00',
            moneda: 'BOB',
          ),
          brillo: brillo,
        ),
      );
      expect(
        tester.widget<Icon>(find.byIcon(Icons.groups_outlined)).color,
        tokens.text2,
      );
      expect(
        tester
            .widget<Monto>(
              find.byWidgetPredicate(
                (widget) => widget is Monto && widget.monto == '25.00',
              ),
            )
            .estilo
            ?.color,
        tokens.text,
      );

      await tester.pumpWidget(
        conTema(
          const PanelSorteo(
            pasos: [(nombre: 'Paso 1', hash: 'abc123', cuando: 'hoy')],
            coincide: null,
          ),
          brillo: brillo,
        ),
      );
      final numeroDePaso = tester.widget<CircleAvatar>(
        find.byType(CircleAvatar),
      );
      expect(numeroDePaso.backgroundColor, tokens.surface2);
      expect(tester.widget<Text>(find.text('1')).style?.color, tokens.text2);

      await tester.pumpWidget(
        conTema(
          const NotificacionEmergente(
            titulo: 'Aviso',
            detalle: 'Tienes una notificación nueva',
            enEspera: 0,
            onAbrir: _sinAccion,
            onCerrar: _sinAccion,
          ),
          brillo: brillo,
        ),
      );
      expect(
        tester
            .widget<Icon>(find.byIcon(Icons.notifications_active_outlined))
            .color,
        tokens.text2,
      );

      await tester.pumpWidget(
        conTema(
          const TarjetaDeOferta(
            quien: 'Ana',
            turnoQueCede: 2,
            turnoQuePide: 4,
            compensacion: '25.00',
            moneda: 'BOB',
            estado: 'NUEVA',
          ),
          brillo: brillo,
        ),
      );
      expect(
        tester
            .widget<Monto>(
              find.byWidgetPredicate(
                (widget) =>
                    widget is Monto && widget.etiqueta == 'Compensación',
              ),
            )
            .estilo
            ?.color,
        tokens.text,
      );
      await tester.pumpWidget(
        conTema(
          const SizedBox(
            height: 500,
            child: ListaDeMovimientos(
              moneda: 'BOB',
              movimientos: [
                (
                  fechaIso: '2026-10-13T10:00:00Z',
                  tipo: TipoDeMovimiento.aporte,
                  concepto: 'Aporte',
                  monto: '25.00',
                  saldoCorrido: '125.00',
                  pendiente: false,
                ),
              ],
            ),
          ),
          brillo: brillo,
        ),
      );
      expect(
        tester
            .widget<Monto>(
              find.byWidgetPredicate(
                (widget) =>
                    widget is Monto &&
                    widget.etiqueta == 'Saldo al cierre del día',
              ),
            )
            .estilo
            ?.color,
        tokens.text2,
      );

      await tester.pumpWidget(
        conTema(
          const DesgloseDeCobro(
            lineas: [(concepto: 'Aporte', monto: '100.00')],
            neto: '95.00',
            moneda: 'BOB',
          ),
          brillo: brillo,
        ),
      );
      expect(
        tester
            .widget<Monto>(
              find.byWidgetPredicate(
                (widget) => widget is Monto && widget.etiqueta == 'Neto',
              ),
            )
            .estilo
            ?.color,
        tokens.text,
      );
      await tester.pumpWidget(
        conTema(
          const ResumenDePeriodo(
            titulo: 'Octubre',
            entro: '100.00',
            salio: '25.00',
            saldoInicial: '50.00',
            saldoFinal: '125.00',
            moneda: 'BOB',
          ),
          brillo: brillo,
        ),
      );
      for (final etiqueta in ['Entró', 'Saldo de hoy']) {
        expect(
          tester
              .widget<Monto>(
                find.byWidgetPredicate(
                  (widget) => widget is Monto && widget.etiqueta == etiqueta,
                ),
              )
              .estilo
              ?.color,
          tokens.text,
          reason: etiqueta,
        );
      }

      await tester.pumpWidget(
        conTema(
          const MedidorDeRango(
            etiqueta: 'Aporte mensual',
            minimo: '100.00',
            maximo: '200.00',
            estimado: '150.00',
            moneda: 'BOB',
          ),
          brillo: brillo,
        ),
      );
      expect(
        tester
            .widget<Monto>(
              find.byWidgetPredicate(
                (widget) => widget is Monto && widget.etiqueta == 'Estimado',
              ),
            )
            .estilo
            ?.color,
        tokens.text,
      );
      final barra = find.descendant(
        of: find.byType(MedidorDeRango),
        matching: find.byType(Container),
      );
      expect(
        (tester.widget<Container>(barra).decoration! as BoxDecoration).color,
        tokens.accent,
      );
      expect(
        (tester.widget<Container>(find.byType(Container).first).decoration!
                as BoxDecoration)
            .color,
        tokens.accent,
      );
    });
  }
}

void _sinAccion() {}
