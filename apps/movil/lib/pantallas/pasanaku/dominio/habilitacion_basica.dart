import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// CU-90 vía básica: el contrato de organizador Aprendiz que se acepta desde la app para
/// poder crear el primer grupo, sin esperar una aprobación manual.
///
/// Las tres rutas son nuevas en los contratos de `organizador` e `identidad` y todavía no
/// están en los clientes Dart generados de esta rama: se llaman con el mismo `Dio` de la
/// app (mismos interceptores de sesión, traza y errores) y JSON explícito.
class ContratoDeOrganizador {
  const ContratoDeOrganizador({
    required this.version,
    required this.titulo,
    required this.obligaciones,
    required this.causalesRescision,
    required this.limiteDeGrupos,
    required this.limiteDeMonto,
    required this.habilitado,
    this.organizadorId,
  });

  final String version;
  final String titulo;
  final String obligaciones;
  final String causalesRescision;
  final int limiteDeGrupos;

  /// Bs, como cadena decimal (nunca `double`).
  final String limiteDeMonto;
  final bool habilitado;
  final String? organizadorId;

  static ContratoDeOrganizador desdeJson(Map<String, dynamic> j) =>
      ContratoDeOrganizador(
        version: j['version'] as String,
        titulo: j['titulo'] as String,
        obligaciones: j['obligaciones'] as String,
        causalesRescision: j['causalesRescision'] as String,
        limiteDeGrupos: (j['limiteDeGrupos'] as num).toInt(),
        limiteDeMonto: j['limiteDeMonto'] as String,
        habilitado: j['habilitado'] == true,
        organizadorId: j['organizadorId'] as String?,
      );
}

/// El contrato vigente y si la persona ya es organizadora.
final habilitacionBasicaProvider =
    FutureProvider.autoDispose<ContratoDeOrganizador>((ref) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await ref
            .read(dioProvider)
            .get<Map<String, dynamic>>('/organizadores/yo/habilitacion-basica');
        return ContratoDeOrganizador.desdeJson(r.data!);
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

class AceptarContratoDeOrganizador {
  AceptarContratoDeOrganizador(this._ref);
  final Ref _ref;

  static const formularioId = 'cu90-habilitacion-basica';

  /// Acepta la versión leída, la firma y renueva la sesión para que el token traiga el
  /// rol ORGANIZADOR. Cada paso es repetible: si algo falla, volver a tocar completa lo
  /// que faltaba sin duplicar nada.
  Future<void> ejecutar(ContratoDeOrganizador contrato) async {
    final dio = _ref.read(dioProvider);
    final idempotencia = _ref.read(idempotenciaProvider.notifier);
    final sesion = _ref.read(sesionProvider);
    try {
      final aceptado = await dio.post<Map<String, dynamic>>(
        '/organizadores/yo/habilitacion-basica',
        data: {'aceptaContrato': true, 'version': contrato.version},
        options: Options(
          headers: {'Idempotency-Key': idempotencia.claveDe(formularioId)},
        ),
      );
      final usuarioId = await sesion.usuarioId();
      if (usuarioId == null) {
        throw StateError('Sin sesión no se firma el contrato.');
      }
      await dio.post<Map<String, dynamic>>(
        '/usuarios/$usuarioId/roles/organizador',
        data: {
          'contratoId': aceptado.data!['contratoId'],
          'contenidoHash': aceptado.data!['contenidoHash'],
        },
      );
      idempotencia.cerrar(formularioId);
      // El rol nuevo viaja en el próximo token: se renueva ya, con el mismo transporte
      // y sin los interceptores (igual que `instalarInterceptores`).
      final dioDeRefresco = Dio(BaseOptions(baseUrl: dio.options.baseUrl))
        ..httpClientAdapter = dio.httpClientAdapter;
      await Refrescador(sesion, dioDeRefresco).refrescar();
    } catch (e) {
      throw errorDeDominio(e);
    }
  }
}

final aceptarContratoDeOrganizadorProvider =
    Provider<AceptarContratoDeOrganizador>(AceptarContratoDeOrganizador.new);
