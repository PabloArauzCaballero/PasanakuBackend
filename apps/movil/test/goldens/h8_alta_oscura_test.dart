import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_del_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_captura.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/';

void _verificarFechaApilada(WidgetTester tester) {
  final dia = tester.getRect(find.text('Día'));
  final mes = tester.getRect(find.text('Mes'));
  final anio = tester.getRect(find.text('Año'));
  expect(mes.top, greaterThan(dia.bottom));
  expect(anio.top, greaterThan(mes.bottom));
  final marcadorMes = tester.renderObject<RenderParagraph>(find.text('Mes'));
  expect(
    marcadorMes.didExceedMaxLines,
    isFalse,
    reason: 'El marcador del mes debe leerse completo, no como “M…”.',
  );
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('seis pasos del alta en oscuro · 360 px · texto 130 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);

    final (:dio, adaptador: _) = dioSimulado();
    final contenedor = ProviderContainer(
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(contenedor.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: ThemeMode.dark,
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(context).copyWith(disableAnimations: true),
            child: child!,
          ),
          home: const PantallaDeRegistro(),
        ),
      ),
    );
    await asentar(tester);

    for (var i = 0; i < PasoAlta.values.length; i++) {
      if (i > 0) {
        await tester.drag(
          find.byType(SingleChildScrollView).first,
          const Offset(0, 1200),
        );
        await tester.pumpAndSettle();
      }
      final estado = contenedor.read(altaProvider);
      final contexto = tester.element(find.byType(Scaffold).first);
      expect(estado.paso, PasoAlta.values[i]);
      expect(
        find.text('FASE ${estado.numeroDeFase} DE ${estado.totalDeFases}'),
        findsOneWidget,
      );
      expect(Theme.of(contexto).brightness, Brightness.dark);
      expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(PantallaDeRegistro),
        matchesGoldenFile(
          '${_evidencia}h8-alta-oscuro-paso-${i + 1}-360-130'
          '${i >= 3 ? '-scroll-restablecido' : ''}.png',
        ),
      );
      if (i == 4) {
        final ayudaMonto = tester.renderObject<RenderParagraph>(
          find.text(TextosDelAlta.montoMensualAyuda),
        );
        expect(
          ayudaMonto.didExceedMaxLines,
          isFalse,
          reason:
              'La ayuda completa debe explicar que el monto se puede omitir.',
        );
      }
      if ([0, 2, 3, 4].contains(i)) {
        await tester.drag(
          find.byType(SingleChildScrollView).first,
          const Offset(0, -560),
        );
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        if (i == 2) {
          final numero = tester.getRect(
            find.text(TextosIdentidad.numeroDocumento),
          );
          final expedicion = tester.getRect(
            find.text(TextosIdentidad.lugarExpedicion),
          );
          expect(
            expedicion.top,
            greaterThan(numero.bottom),
            reason:
                'A 360 dp y 130 % de escala, ambos campos deben apilarse '
                'para que sus etiquetas y ayudas conserven ancho legible.',
          );
          final ayudaDocumento = tester.renderObject<RenderParagraph>(
            find.text(TextosIdentidad.documentoAyuda),
          );
          expect(
            ayudaDocumento.didExceedMaxLines,
            isFalse,
            reason:
                'La ayuda completa del CI no debe terminar en puntos suspensivos.',
          );
        }
        await expectLater(
          find.byType(PantallaDeRegistro),
          matchesGoldenFile(
            '${_evidencia}h8-alta-oscuro-paso-${i + 1}-abajo-360-130'
            '${i == 2
                ? '-campos-responsive'
                : i >= 3
                ? '-scroll-restablecido'
                : ''}.png',
          ),
        );
        if (i == 2) {
          await Scrollable.ensureVisible(
            tester.element(find.text('Mes')),
            alignment: 0.5,
            duration: Duration.zero,
          );
          await tester.pumpAndSettle();
          _verificarFechaApilada(tester);
          await expectLater(
            find.byType(PantallaDeRegistro),
            matchesGoldenFile(
              '${_evidencia}h8-alta-oscuro-paso-3-fecha-responsive-360-130.png',
            ),
          );
          final enviar = find.text(TextosDeCaptura.enviarDocumento);
          await Scrollable.ensureVisible(
            tester.element(enviar),
            alignment: 1,
            duration: Duration.zero,
          );
          await tester.pumpAndSettle();
          final fecha = tester.getRect(
            find.text(TextosIdentidad.fechaExpiracion),
          );
          final accion = tester.getRect(enviar);
          expect(fecha.top, greaterThanOrEqualTo(0));
          expect(
            accion.bottom,
            lessThanOrEqualTo(tester.view.physicalSize.height),
          );
          await expectLater(
            find.byType(PantallaDeRegistro),
            matchesGoldenFile(
              '${_evidencia}h8-alta-oscuro-paso-3-final-responsive-360-130.png',
            ),
          );
        }
        if (i == 3) {
          await Scrollable.ensureVisible(
            tester.element(find.text('Mes')),
            alignment: 0.5,
            duration: Duration.zero,
          );
          await tester.pumpAndSettle();
          _verificarFechaApilada(tester);
          await expectLater(
            find.byType(PantallaDeRegistro),
            matchesGoldenFile(
              '${_evidencia}h8-alta-oscuro-paso-4-fecha-responsive-360-130.png',
            ),
          );
          final continuar = find.text(TextosIdentidad.continuar);
          final botonContinuar = find.ancestor(
            of: continuar,
            matching: find.byType(FilledButton),
          );
          await tester.drag(
            find.byType(SingleChildScrollView).first,
            const Offset(0, -1600),
          );
          await tester.pumpAndSettle();
          final rectBoton = tester.getRect(botonContinuar);
          expect(rectBoton.top, greaterThanOrEqualTo(0));
          expect(
            rectBoton.bottom,
            lessThanOrEqualTo(tester.view.physicalSize.height),
          );
          await expectLater(
            find.byType(PantallaDeRegistro),
            matchesGoldenFile(
              '${_evidencia}h8-alta-oscuro-paso-4-final-responsive-360-130.png',
            ),
          );
        }
      }
      if (i < PasoAlta.values.length - 1) {
        contenedor.read(altaProvider.notifier).siguiente();
        await tester.pumpAndSettle();
      }
    }
  });
}
