import 'dart:convert';
import 'dart:typed_data';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/habilitacion_basica.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _usuario = 'uuuuuuuu-0000-4000-8000-000000000077';

/// Doble de organizador + identidad: anota cada pedido y responde como sus contratos.
class _Servidor implements HttpClientAdapter {
  final pedidos = <RequestOptions>[];
  int fallarFirmaVeces = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions o,
    Stream<Uint8List>? cuerpo,
    Future<void>? cancelar,
  ) async {
    pedidos.add(o);
    if (o.path.endsWith('/organizadores/yo/habilitacion-basica')) {
      return _json(200, {
        'organizadorId': 'b8000000-0000-4000-8000-000000000001',
        'contratoId': 'b8000000-0000-4000-8000-000000000003',
        'contenidoHash': 'a' * 64,
        'nivel': 'APRENDIZ',
        'esNueva': true,
      });
    }
    if (o.path.endsWith('/roles/organizador')) {
      if (fallarFirmaVeces > 0) {
        fallarFirmaVeces--;
        return _json(422, {
          'codigo': 'AP-CU90-13',
          'mensaje': 'No pudimos firmar tu contrato de organizador.',
          'trazaId': 't-firma',
        });
      }
      return _json(200, {'activo': true, 'esNuevo': true});
    }
    if (o.path.endsWith('/sesion/refrescar')) {
      return _json(200, {'acceso': _token(), 'refresco': 'refresco-2'});
    }
    return _json(404, {'codigo': 'AP-X', 'mensaje': 'no', 'trazaId': 't'});
  }

  ResponseBody _json(int estado, Map<String, Object> c) =>
      ResponseBody.fromString(
        jsonEncode(c),
        estado,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType],
        },
      );

  @override
  void close({bool force = false}) {}
}

String _token() {
  String parte(Object v) =>
      base64Url.encode(utf8.encode(jsonEncode(v))).replaceAll('=', '');
  return '${parte({'alg': 'none'})}.${parte({'sub': _usuario})}.';
}

const _contrato = ContratoDeOrganizador(
  version: 'APR-PROV-TEST-2610',
  titulo: 'Contrato provisional',
  obligaciones: 'x',
  causalesRescision: 'y',
  limiteDeGrupos: 2,
  limiteDeMonto: '30000.00',
  habilitado: false,
);

Future<(ProviderContainer, _Servidor)> _montar() async {
  final sesion = Sesion(AlmacenEnMemoria());
  await sesion.abrir(acceso: _token(), refresco: 'refresco-1');
  final servidor = _Servidor();
  final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
    ..httpClientAdapter = servidor;
  instalarInterceptores(dio, sesion);
  final c = ProviderContainer(
    overrides: [
      sesionProvider.overrideWithValue(sesion),
      dioProvider.overrideWithValue(dio),
    ],
  );
  addTearDown(c.dispose);
  return (c, servidor);
}

void main() {
  test('correcto: acepta la versión leída, firma con su propio usuario y '
      'renueva la sesión para traer el rol', () async {
    final (c, servidor) = await _montar();

    await c.read(aceptarContratoDeOrganizadorProvider).ejecutar(_contrato);

    final rutas = servidor.pedidos.map((p) => p.path).toList();
    expect(rutas, [
      '/organizadores/yo/habilitacion-basica',
      '/usuarios/$_usuario/roles/organizador',
      '/sesion/refrescar',
    ]);
    final aceptar = servidor.pedidos.first;
    expect(aceptar.data, {
      'aceptaContrato': true,
      'version': 'APR-PROV-TEST-2610',
    });
    expect(aceptar.headers['Idempotency-Key'], isNotNull);
    expect(servidor.pedidos[1].data, {
      'contratoId': 'b8000000-0000-4000-8000-000000000003',
      'contenidoHash': 'a' * 64,
    });
  });

  test('inválido: si la firma falla llega el error con su traza y no se '
      'renueva la sesión', () async {
    final (c, servidor) = await _montar();
    servidor.fallarFirmaVeces = 1;

    await expectLater(
      c.read(aceptarContratoDeOrganizadorProvider).ejecutar(_contrato),
      throwsA(isA<ErrorDeApi>().having((e) => e.trazaId, 'trazaId', 't-firma')),
    );
    expect(
      servidor.pedidos.map((p) => p.path),
      isNot(contains('/sesion/refrescar')),
    );
  });

  test('límite: reintentar después de un fallo usa la misma clave de '
      'idempotencia al aceptar', () async {
    final (c, servidor) = await _montar();
    servidor.fallarFirmaVeces = 1;
    final caso = c.read(aceptarContratoDeOrganizadorProvider);

    await expectLater(caso.ejecutar(_contrato), throwsA(isA<ErrorDeApi>()));
    await caso.ejecutar(_contrato);

    final claves = servidor.pedidos
        .where((p) => p.path.endsWith('/organizadores/yo/habilitacion-basica'))
        .map((p) => p.headers['Idempotency-Key'])
        .toList();
    expect(claves, hasLength(2));
    expect(claves.toSet(), hasLength(1));
  });
}
