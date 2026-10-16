import 'dart:convert';

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_estado_de_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_verificacion.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../goldens/cargar_fuentes_de_aplicacion.dart';

const _usuarioSintetico = 'uuuuuuuu-0000-4000-8000-000000000099';
const _verificacionSintetica = 'a11ce001-0000-4000-8000-000000000099';

class _AlmacenEnMemoria implements AlmacenSeguro {
  final datos = <String, String>{};

  @override
  Future<String?> leer(String clave) async => datos[clave];

  @override
  Future<void> guardar(String clave, String valor) async =>
      datos[clave] = valor;

  @override
  Future<void> borrar(String clave) async => datos.remove(clave);
}

class _ConsultaFalsa extends ConsultarVerificacion {
  _ConsultaFalsa(super.ref);

  @override
  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) async => EstadoDeVerificacion(
    verificacionId: _verificacionSintetica,
    estado: EstadoDeVerificacionEstadoEnum.EN_REVISION,
    fotos: const [],
  );
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('al abrir billetera, la sesión pendiente muestra su verificación', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(412, 850);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    String parte(Object valor) =>
        base64Url.encode(utf8.encode(jsonEncode(valor))).replaceAll('=', '');
    final token =
        '${parte({'alg': 'none'})}.${parte({'sub': _usuarioSintetico, 'permisos': <String>[]})}.';
    final sesion = Sesion(_AlmacenEnMemoria());
    await sesion.abrir(acceso: token);
    final enrutador = crearEnrutador(
      inicial: '/billetera/inicio',
      sesion: sesion,
    );
    addTearDown(enrutador.dispose);
    addTearDown(sesion.dispose);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          consultarVerificacionProvider.overrideWith(
            (ref) => _ConsultaFalsa(ref),
          ),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: enrutador,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text(TextosDeVerificacion.cuentaEnVerificacionTitulo),
      findsOneWidget,
    );
    expect(find.text('No tenés acceso a esto'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('sesión activa explica que la cuenta está en verificación', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(412, 850);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          consultarVerificacionProvider.overrideWith(
            (ref) => _ConsultaFalsa(ref),
          ),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaDeEstadoDeVerificacion(
            usuarioId: _usuarioSintetico,
            sesionActiva: true,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text(TextosDeVerificacion.cuentaEnVerificacionTitulo),
      findsOneWidget,
    );
    expect(
      find.text(TextosDeVerificacion.cuentaEnVerificacionDetalle),
      findsOneWidget,
    );
    expect(find.text(TextosDeVerificacion.salirDeCuenta), findsOneWidget);
    expect(find.text(TextosDeVerificacion.seguirConElRegistro), findsNothing);
    expect(find.text('No tenés acceso a esto'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
