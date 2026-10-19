import 'dart:convert';

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cu13_cuenta_propia.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/pantallas/billetera/resolver_cuenta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_verificacion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

const _usuario = 'usuario-sintetico';

class _AlmacenEnMemoria implements AlmacenSeguro {
  final _datos = <String, String>{};

  @override
  Future<String?> leer(String clave) async => _datos[clave];

  @override
  Future<void> guardar(String clave, String valor) async =>
      _datos[clave] = valor;

  @override
  Future<void> borrar(String clave) async => _datos.remove(clave);
}

class _ConsultaFalsa extends ConsultarVerificacion {
  _ConsultaFalsa(super.ref, this.estado);

  final EstadoDeVerificacion estado;

  @override
  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) async {
    expect(usuarioId, _usuario);
    return estado;
  }
}

String _tokenDeSesion() {
  String parte(Object valor) =>
      base64Url.encode(utf8.encode(jsonEncode(valor))).replaceAll('=', '');
  return '${parte({'alg': 'none'})}.${parte({
    'sub': _usuario,
    'permisos': ['BILLETERA_VER'],
  })}.';
}

void main() {
  for (final estado in [
    EstadoDeVerificacionEstadoEnum.PENDIENTE,
    EstadoDeVerificacionEstadoEnum.EN_REVISION,
  ]) {
    testWidgets(
      'un 403 de billetera con KYC $estado explica la revisión, sin habilitar acceso',
      (tester) async {
        final sesion = Sesion(_AlmacenEnMemoria());
        await sesion.abrir(acceso: _tokenDeSesion());
        addTearDown(sesion.dispose);
        final rechazo = ErrorDeApi(
          codigo: 'AP-SEG-01',
          trazaId: 'traza-sintetica',
          estado: 403,
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              sesionProvider.overrideWithValue(sesion),
              cuentaPropiaProvider.overrideWith((ref) => Future.error(rechazo)),
              consultarVerificacionProvider.overrideWith(
                (ref) => _ConsultaFalsa(
                  ref,
                  EstadoDeVerificacion(
                    verificacionId: 'caso-sintetico',
                    estado: estado,
                    fotos: const [],
                  ),
                ),
              ),
            ],
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              home: Scaffold(
                body: ResolverCuenta(construir: (_) => const Text('Billetera')),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(
          find.textContaining(TextosDeVerificacion.cuentaEnVerificacionTitulo),
          findsOneWidget,
        );
        expect(find.text('Completar verificación'), findsOneWidget);
        expect(find.text('Billetera'), findsNothing);
        expect(find.text('No tenés acceso a esto'), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets('si KYC figura aprobada no presenta el caso como en revisión', (
    tester,
  ) async {
    final sesion = Sesion(_AlmacenEnMemoria());
    await sesion.abrir(acceso: _tokenDeSesion());
    addTearDown(sesion.dispose);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sesionProvider.overrideWithValue(sesion),
          cuentaPropiaProvider.overrideWith(
            (ref) => Future.error(
              ErrorDeApi(
                codigo: 'AP-SEG-01',
                trazaId: 'traza-sintetica',
                estado: 403,
              ),
            ),
          ),
          consultarVerificacionProvider.overrideWith(
            (ref) => _ConsultaFalsa(
              ref,
              EstadoDeVerificacion(
                verificacionId: 'caso-sintetico',
                estado: EstadoDeVerificacionEstadoEnum.APROBADA,
                fotos: const [],
              ),
            ),
          ),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: ResolverCuenta(construir: (_) => const Text('Billetera')),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text(TextosDeVerificacion.accesoAunNoHabilitado),
      findsOneWidget,
    );
    expect(find.text('Completar verificación'), findsNothing);
    expect(find.text('Billetera'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
