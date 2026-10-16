import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu04_autenticar.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_contrato.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/subida_del_expediente.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

const _telefonoSintetico = '+59170000001';
const _claveSintetica = 'SYNTHETIC_PASSWORD_H10_S4_M1';
const _accesoSintetico = 'SYNTHETIC_ACCESS_TOKEN_H10_S4_M1';
const _refrescoSintetico = 'SYNTHETIC_REFRESH_TOKEN_H10_S4_M1';
const _telefonoAltaSintetico = '+59170000001';
const _claveAltaSintetica = 'SYNTHETIC_SIGNUP_PASSWORD_H10_S4_M1';
const _ciSintetico = '9999999-H10-S4-M1-SYNTHETIC';
const _correoSintetico = 'h10-s4-m1@example.test';
const _codigoCorreoSintetico = 'SYNTHETIC_EMAIL_OTP_H10_S4_M1';
const _tokenAltaSintetico = 'SYNTHETIC_ENROLLMENT_TOKEN_H10_S4_M1';
const _rutaSintetica = 'H10_S4_M1_SYNTHETIC_PHOTO_PATH';

class _AlmacenEnMemoria implements AlmacenSeguro {
  final _valores = <String, String>{};

  @override
  Future<String?> leer(String clave) async => _valores[clave];

  @override
  Future<void> guardar(String clave, String valor) async {
    _valores[clave] = valor;
  }

  @override
  Future<void> borrar(String clave) async {
    _valores.remove(clave);
  }
}

class _RespuestaDeInicio implements HttpClientAdapter {
  _RespuestaDeInicio({this.tokenRefresco});

  final String? tokenRefresco;
  int llamadas = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    llamadas++;
    return ResponseBody.fromString(
      jsonEncode({
        'sesionId': 'sesion-sintetica-h10-s4-m1',
        'expiraEn': '2026-10-14T18:00:00Z',
        'requiereFactorAdicional': false,
        'dispositivoConfiable': false,
        'tokenAcceso': _accesoSintetico,
        if (tokenRefresco != null) 'tokenRefresco': tokenRefresco,
        // La ausencia de tokenRefresco activa el único logger de producción.
      }),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

/// Responde el recorrido CU-01 + las cinco subidas CU-02 sin abrir una conexiÃ³n.
/// Conserva solo contadores booleanos: ni cuerpos ni credenciales salen del proceso.
class _RespuestaDeAltaCompleta implements HttpClientAdapter {
  int altas = 0;
  int codigosSolicitados = 0;
  int codigosConfirmados = 0;
  int fotos = 0;
  bool autorizacionDeAltaCorrecta = true;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final path = options.path;
    if (path == '/usuarios/verificaciones/correo') {
      codigosSolicitados++;
      return _json(202, {
        'verificacionId': 'aaaaaaaa-0000-4000-8000-000000000001',
        'destinoEnmascarado': 'h***@example.test',
        'expiraEn': '2026-10-14T20:00:00Z',
      });
    }
    if (path.endsWith('/confirmacion') &&
        path.startsWith('/usuarios/verificaciones/correo/')) {
      codigosConfirmados++;
      return _json(200, {'verificada': true});
    }
    if (path == '/usuarios') {
      altas++;
      return _json(202, {
        'usuarioId': 'bbbbbbbb-0000-4000-8000-000000000001',
        'estado': 'PENDIENTE_VERIFICACION',
        'cuentaBilleteraId': 'cccccccc-0000-4000-8000-000000000001',
        'tokenDeAlta': _tokenAltaSintetico,
        'tokenDeAltaVenceEn': '2026-10-14T20:10:00Z',
      });
    }
    if (path.endsWith('/documentos')) {
      fotos++;
      final tokenHeaders = options.headers.entries
          .where((e) => e.key.toLowerCase() == 'token-de-alta')
          .toList(growable: false);
      autorizacionDeAltaCorrecta &=
          tokenHeaders.length == 1 &&
          tokenHeaders.single.value == _tokenAltaSintetico;
      return _json(201, {
        'claveObjeto': 'privado/sintetico/$fotos',
        'hashArchivo': 'a' * 64,
      });
    }
    throw StateError('Ruta no prevista en el adaptador sintÃ©tico.');
  }

  ResponseBody _json(int status, Map<String, Object?> body) =>
      ResponseBody.fromString(
        jsonEncode(body),
        status,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType],
        },
      );

  @override
  void close({bool force = false}) {}
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  test('H10.S4.M1 login sintetico no vuelca secretos en logs', () async {
    final respuesta = _RespuestaDeInicio();
    final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
      ..httpClientAdapter = respuesta;
    final contenedor = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(_AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(() {
      contenedor.dispose();
      dio.close(force: true);
    });

    final resultado = await contenedor
        .read(autenticacionProvider)
        .conCredenciales(
          telefonoE164: _telefonoSintetico,
          credencial: _claveSintetica,
        );

    expect(respuesta.llamadas, 1);
    expect(resultado, ResultadoDeAutenticacion.entro);
    expect(
      await contenedor.read(sesionProvider).tokenDeAcceso(),
      _accesoSintetico,
    );
    expect(await contenedor.read(sesionProvider).tokenDeRefresco(), isNull);
  });

  test('H10.S4.M1 token de refresco queda en sesión sin imprimirse', () async {
    final respuesta = _RespuestaDeInicio(tokenRefresco: _refrescoSintetico);
    final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
      ..httpClientAdapter = respuesta;
    final contenedor = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(_AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(() {
      contenedor.dispose();
      dio.close(force: true);
    });

    final resultado = await contenedor
        .read(autenticacionProvider)
        .conCredenciales(
          telefonoE164: _telefonoSintetico,
          credencial: _claveSintetica,
        );

    expect(respuesta.llamadas, 1);
    expect(resultado, ResultadoDeAutenticacion.entro);
    expect(
      await contenedor.read(sesionProvider).tokenDeRefresco(),
      _refrescoSintetico,
    );
  });

  test(
    'H10.S4.M1 alta y cinco fotos sintéticas no vuelcan datos en logs',
    () async {
      final adaptador = _RespuestaDeAltaCompleta();
      final dio = Dio(
        BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'),
      )..httpClientAdapter = adaptador;
      final contenedor = ProviderContainer(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(_AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
        ],
      );
      final temporal = await Directory.systemTemp.createTemp(_rutaSintetica);
      addTearDown(() async {
        contenedor.dispose();
        dio.close(force: true);
        if (await temporal.exists()) await temporal.delete(recursive: true);
      });

      final archivos = <File>[];
      var capturas = const CapturasDelExpediente();
      for (final cara in CaraDelCarril.values) {
        final archivo = File(
          '${temporal.path}${Platform.pathSeparator}${cara.name}.jpg',
        );
        await archivo.writeAsBytes([0xff, 0xd8, 0xff, cara.index]);
        archivos.add(archivo);
        capturas = capturas.conCaptura(
          cara,
          Captura(
            ruta: archivo.path,
            bytes: 4,
            ancho: 2,
            alto: 2,
            sha256Corto: 'sintetico',
          ),
        );
      }

      final alta = contenedor.read(altaProvider.notifier);
      alta.actualizarDatos(
        DatosPersonales(
          nombres: 'Prueba SintÃ©tica',
          apellidos: 'Alta Segura',
          fechaNacimiento: DateTime.utc(1990, 5, 20),
          telefono: _telefonoAltaSintetico,
          numeroDocumento: _ciSintetico,
          lugarExpedicion: 'LP',
          fechaExpiracion: DateTime.utc(2030, 12, 31),
          correo: _correoSintetico,
        ),
      );
      alta.elegirContrasena(_claveAltaSintetica);
      expect(await alta.solicitarCodigoCorreo(), isTrue);
      await alta.confirmarCodigoCorreo(_codigoCorreoSintetico);

      final contratos = [
        for (final (tipo, codigo, sufijo) in [
          ('BILLETERA', 'CTO-BILLETERA', '001'),
          ('TARIFAS', 'CTO-TARIFAS', '002'),
          ('TRATAMIENTO_DATOS', 'CTO-DATOS', '003'),
        ])
          ContratoVigente.fromJson({
            'id': 'dddddddd-0000-4000-8000-000000000$sufijo',
            'codigo': codigo,
            'version': 1,
            'tipo': tipo,
            'urlDocumento': 'https://aportaya.bo/legal/$codigo.pdf',
            'hashDocumento': 'b' * 64,
            'vigenteDesde': '2026-01-01T00:00:00Z',
          }),
      ];
      contenedor.read(contratoProvider.notifier)
        ..fijarVigentes(contratos)
        ..alternarContrato(true)
        ..alternarTarifario(true)
        ..alternarTratamientoDatos(true);
      for (var i = 0; i < PasoAlta.values.length - 1; i++) {
        alta.siguiente();
      }

      final resultado = await alta.enviarAlServidor();
      expect(resultado.error, isNull);
      expect(resultado.usuarioId, isNotNull);
      await contenedor
          .read(subidaProvider.notifier)
          .subirPendientes(
            usuarioId: resultado.usuarioId!,
            capturas: capturas,
            tokenDeAlta: resultado.tokenDeAlta,
          );

      expect(adaptador.codigosSolicitados, 1);
      expect(adaptador.codigosConfirmados, 1);
      expect(adaptador.altas, 1);
      expect(adaptador.fotos, 5);
      expect(adaptador.autorizacionDeAltaCorrecta, isTrue);
      expect(archivos.every((archivo) => !archivo.existsSync()), isTrue);
    },
  );
}
