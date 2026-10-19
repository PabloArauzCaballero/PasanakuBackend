import 'dart:async';

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_estado_de_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_verificacion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// 2026-10-19: la pantalla de estado consultaba cada 2 s y en cada vuelta borraba lo
/// que mostraba para poner el círculo de carga: el usuario la vio «parpadear en
/// bucle». Y recién creada la cuenta decía «No cierres la app», como si hubiera que
/// esperar la aprobación ahí.
const _usuario = 'uuuuuuuu-0000-4000-8000-000000000077';

/// Doble de la consulta: cada llamada devuelve la respuesta que el test le da.
class _ConsultaControlada extends ConsultarVerificacion {
  _ConsultaControlada(super.ref, this.respuestas);
  final List<Completer<EstadoDeVerificacion>> respuestas;
  var llamadas = 0;

  @override
  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) {
    final i = llamadas++;
    return i < respuestas.length
        ? respuestas[i].future
        : Completer<EstadoDeVerificacion>().future;
  }
}

EstadoDeVerificacion _estado(EstadoDeVerificacionEstadoEnum e) =>
    EstadoDeVerificacion(
      verificacionId: 'a11ce001-0000-4000-8000-000000000077',
      estado: e,
      fotos: const [],
    );

void main() {
  late List<Completer<EstadoDeVerificacion>> respuestas;
  late _ConsultaControlada consulta;

  Future<void> montar(WidgetTester tester) async {
    respuestas = List.generate(3, (_) => Completer<EstadoDeVerificacion>());
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          consultarVerificacionProvider.overrideWith(
            (ref) => consulta = _ConsultaControlada(ref, respuestas),
          ),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaDeEstadoDeVerificacion(usuarioId: _usuario),
        ),
      ),
    );
    await tester.pump();
  }

  testWidgets('correcto: el sondeo no vuelve a poner el círculo de carga', (
    tester,
  ) async {
    await montar(tester);
    respuestas[0].complete(_estado(EstadoDeVerificacionEstadoEnum.PENDIENTE));
    await tester.pump();
    await tester.pump();
    expect(find.bySemanticsLabel('Cargando'), findsNothing);

    // Pasan los 2 s: sale la segunda consulta y queda en vuelo.
    await tester.pump(const Duration(seconds: 2));
    expect(consulta.llamadas, 2);
    expect(find.bySemanticsLabel('Cargando'), findsNothing);
    expect(find.text(TextosDeVerificacion.enRevisionTitulo), findsOneWidget);

    respuestas[1].complete(_estado(EstadoDeVerificacionEstadoEnum.APROBADA));
    await tester.pump();
    await tester.pump();
    expect(find.text(TextosDeVerificacion.aprobadaTitulo), findsOneWidget);
  });

  testWidgets(
    'límite: recién creada la cuenta (PENDIENTE, sin sesión) no pide esperar',
    (tester) async {
      await montar(tester);
      respuestas[0].complete(_estado(EstadoDeVerificacionEstadoEnum.PENDIENTE));
      await tester.pump();
      await tester.pump();
      expect(find.text(TextosDeVerificacion.pendienteDetalle), findsNothing);
      expect(find.text(TextosDeVerificacion.enRevisionDetalle), findsOneWidget);
      expect(
        find.text(TextosDeVerificacion.seguirConElRegistro),
        findsOneWidget,
      );
      // Desmontar cancela el sondeo pendiente.
      await tester.pumpWidget(const SizedBox());
    },
  );

  testWidgets('inválido: un corte de red en el sondeo no tapa lo que se veía', (
    tester,
  ) async {
    await montar(tester);
    respuestas[0].complete(_estado(EstadoDeVerificacionEstadoEnum.EN_REVISION));
    await tester.pump();
    await tester.pump();
    await tester.pump(const Duration(seconds: 2));
    respuestas[1].completeError(StateError('sin red'));
    await tester.pump();
    expect(find.text(TextosDeVerificacion.enRevisionTitulo), findsOneWidget);
    expect(find.bySemanticsLabel('Cargando'), findsNothing);
    // Y el sondeo sigue: a los 2 s vuelve a consultar.
    await tester.pump(const Duration(seconds: 2));
    expect(consulta.llamadas, 3);
    await tester.pumpWidget(const SizedBox());
  });
}
