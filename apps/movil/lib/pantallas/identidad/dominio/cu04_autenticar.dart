import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/huella_de_dispositivo.dart';
import '../../../proveedores/sesion.dart';

/// CU-04 · autenticar. `POST /sesiones`, la única ruta pública de la app: es el
/// momento en que todavía no hay sesión.
///
/// **El servidor decide si hace falta un segundo factor**, nunca la pantalla
/// (invariante 7). Por eso el resultado no es «entró / no entró» sino
/// [ResultadoDeAutenticacion], que la pantalla traduce en a dónde ir.
///
/// El mismo endpoint atiende los dos pasos: primero con la credencial, después con el
/// factor. Así el servidor puede pedir MFA cuando quiera —dispositivo nuevo, monto
/// alto, señal rara— sin que la app tenga que adivinar de antemano.
enum ResultadoDeAutenticacion { entro, faltaSegundoFactor }

class Autenticacion {
  Autenticacion(this._ref);
  final Ref _ref;

  DefaultApi get _api => DefaultApi(_ref.read(dioProvider));

  /// Paso 1: teléfono y contraseña.
  Future<ResultadoDeAutenticacion> conCredenciales({
    required String telefonoE164,
    required String credencial,
  }) => _pedir(telefonoE164: telefonoE164, credencial: credencial);

  /// Paso 2: el código del segundo factor. Se manda la credencial de nuevo porque el
  /// contrato la sigue pidiendo: la sesión no está abierta hasta que el factor pasa.
  Future<ResultadoDeAutenticacion> conFactor({
    required String telefonoE164,
    required String credencial,
    required String codigo,
  }) => _pedir(
    telefonoE164: telefonoE164,
    credencial: credencial,
    factor: FactorPresentado(tipo: FactorPresentadoTipoEnum.OTP, valor: codigo),
  );

  Future<ResultadoDeAutenticacion> _pedir({
    required String telefonoE164,
    required String credencial,
    FactorPresentado? factor,
  }) async {
    try {
      final r = await _api.autenticar(
        entradaAutenticacion: EntradaAutenticacion(
          telefonoE164: telefonoE164,
          credencial: credencial,
          huellaDispositivo: await _ref
              .read(huellaDeDispositivoProvider)
              .obtener(),
          plataforma: _plataforma(_ref.read(plataformaProvider)),
          factor: factor,
        ),
      );
      final salida = r.data;
      if (salida == null) throw ErrorDeRed();
      if (salida.requiereFactorAdicional && factor == null) {
        return ResultadoDeAutenticacion.faltaSegundoFactor;
      }
      final token = salida.tokenAcceso;
      if (token == null || token.isEmpty) {
        // El servidor dijo que no hace falta otro factor pero no mandó token: sin
        // token no hay sesión, y entrar «a medias» es peor que no entrar.
        throw ErrorDeApi(
          codigo: 'AP-CU04-00',
          trazaId: r.headers.value('x-request-id') ?? '',
          estado: r.statusCode ?? 200,
        );
      }
      // El contrato de CU-04 entrega un solo token. `sesionId` es lo que identifica
      // la sesión para renovarla, así que es lo que se guarda como refresco.
      await _ref
          .read(sesionProvider)
          .guardar(acceso: token, refresco: salida.sesionId ?? token);
      return ResultadoDeAutenticacion.entro;
    } on DioException catch (e) {
      throw errorDeDominio(e);
    }
  }

  EntradaAutenticacionPlataformaEnum _plataforma(String nombre) =>
      nombre == 'IOS'
      ? EntradaAutenticacionPlataformaEnum.IOS
      : EntradaAutenticacionPlataformaEnum.ANDROID;
}

final autenticacionProvider = Provider<Autenticacion>(Autenticacion.new);
