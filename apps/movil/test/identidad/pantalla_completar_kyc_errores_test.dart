import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_completar_kyc.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _ConsultaFalsa extends ConsultarVerificacion {
  _ConsultaFalsa(super.ref);

  @override
  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) async => EstadoDeVerificacion(
    verificacionId: 'solo-prueba',
    estado: EstadoDeVerificacionEstadoEnum.EN_REVISION,
    fotos: const [],
  );
}

void main() {
  testWidgets('muestra el rechazo API y su referencia al guardar la fecha', (
    tester,
  ) async {
    final dio = Dio(BaseOptions(baseUrl: 'https://api.example.test'))
      ..interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) => handler.reject(
            DioException(
              requestOptions: options,
              error: ErrorDeApi(
                codigo: 'AP-SEG-01',
                trazaId: 'traza-sintetica',
                estado: 403,
              ),
            ),
          ),
        ),
      );
    addTearDown(dio.close);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          consultarVerificacionProvider.overrideWith(
            (ref) => _ConsultaFalsa(ref),
          ),
          dioProvider.overrideWithValue(dio),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaCompletarKyc(usuarioId: 'usuario-sintetico'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final campoFecha = tester.widget<CampoDeFecha>(find.byType(CampoDeFecha));
    campoFecha.onElegida(DateTime(2031, 12, 17));
    await tester.pump();
    await tester.ensureVisible(find.text('Guardar fecha'));
    await tester.tap(find.text('Guardar fecha'));
    await tester.pumpAndSettle();

    expect(
      find.text('Esta acción todavía no está disponible para tu cuenta.'),
      findsOneWidget,
    );
    expect(find.text('Referencia: traza-sintetica'), findsOneWidget);
    expect(find.textContaining('ingreso'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
