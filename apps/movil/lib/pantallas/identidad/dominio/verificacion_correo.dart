import 'dart:convert';

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart' show almacenSeguroProvider;
import 'estado_sesion.dart' show mensajeDeError;
import 'verificacion_correo_guardada.dart';

export 'verificacion_correo_guardada.dart' show VerificacionSolicitada;

/// Cliente del codigo de correo previo al alta. El codigo nunca se valida en la app.
class VerificacionCorreo {
  VerificacionCorreo(this._ref);
  final Ref _ref;

  static const _formulario = 'cu01-verificacion-correo';

  /// Clave del almacén seguro donde sobrevive la verificación si la app se cierra a
  /// mitad del alta. Sin esto, volver a empezar pedía otro código y el servidor
  /// cortaba por tope de emisiones (AP-CU01-08).
  static const claveGuardada = 'alta.verificacion_correo';

  /// Si el almacén del teléfono no responde, el alta sigue sin el rescate: nunca
  /// queda esperando al Keystore.
  static const _topeAlmacen = Duration(seconds: 3);

  Future<VerificacionSolicitada> solicitar(
    String correo, {
    bool nueva = false,
  }) async {
    final idempotencia = _ref.read(idempotenciaProvider.notifier);
    if (nueva) {
      idempotencia.cerrar(_formulario);
      await olvidar();
    } else {
      final guardada = await _leer(correo);
      if (guardada != null) {
        // Confirmada antes del cierre: no hace falta otro código.
        if (guardada.confirmada) return guardada.comoSolicitada();
        // Pendiente: la misma clave hace que el servidor devuelva la misma
        // verificación sin mandar otro correo ni contar otra emisión.
        idempotencia.restaurar(_formulario, guardada.clave);
        try {
          return await _pedir(correo);
        } on VerificacionCorreoException {
          // La anterior ya no sirve (venció o se usó): se pide una nueva.
          idempotencia.cerrar(_formulario);
          await olvidar();
        }
      }
    }
    return _pedir(correo);
  }

  Future<VerificacionSolicitada> _pedir(String correo) async {
    final idempotencia = _ref.read(idempotenciaProvider.notifier);
    final clave = idempotencia.claveDe(_formulario);
    try {
      final respuesta = await DefaultApi(_ref.read(dioProvider))
          .solicitarVerificacionCorreo(
            idempotencyKey: clave,
            solicitudVerificacionCorreo: SolicitudVerificacionCorreo(
              correo: correo,
            ),
          );
      final cuerpo = respuesta.data;
      if (cuerpo == null) {
        throw StateError('El servidor no devolvio la verificacion.');
      }
      final guardada = VerificacionGuardada(
        correo: _normalizar(correo),
        clave: clave,
        id: cuerpo.verificacionId,
        destino: cuerpo.destinoEnmascarado,
        expiraEn: cuerpo.expiraEn,
        confirmada: false,
      );
      await _escribir(guardada);
      return guardada.comoSolicitada();
    } on DioException catch (e) {
      if (e.response != null) idempotencia.cerrar(_formulario);
      final problema = errorDeDominio(e);
      throw VerificacionCorreoException(
        mensajeDeError(problema),
        problema is ErrorPresentable ? problema.trazaId : null,
      );
    }
  }

  void reiniciar() {
    _ref.read(idempotenciaProvider.notifier).cerrar(_formulario);
    olvidar().ignore();
  }

  /// Borra la verificación guardada: al cambiar de correo, al pedir una nueva o
  /// cuando la cuenta ya se creó.
  Future<void> olvidar() async {
    try {
      await _ref
          .read(almacenSeguroProvider)
          .borrar(claveGuardada)
          .timeout(_topeAlmacen);
    } on Object {
      // Si el almacén falla, lo guardado vence solo (expiraEn).
    }
  }

  Future<VerificacionGuardada?> _leer(String correo) async {
    try {
      final crudo = await _ref
          .read(almacenSeguroProvider)
          .leer(claveGuardada)
          .timeout(_topeAlmacen);
      if (crudo == null || crudo.isEmpty) return null;
      final g = VerificacionGuardada.desdeJson(jsonDecode(crudo));
      if (g == null || g.correo != _normalizar(correo)) return null;
      if (!DateTime.now().toUtc().isBefore(g.expiraEn.toUtc())) {
        await olvidar();
        return null;
      }
      return g;
    } on Object {
      return null;
    }
  }

  Future<void> _escribir(VerificacionGuardada g) async {
    try {
      await _ref
          .read(almacenSeguroProvider)
          .guardar(claveGuardada, jsonEncode(g.aJson()))
          .timeout(_topeAlmacen);
    } on Object {
      // Sin almacén el alta sigue igual; solo se pierde el rescate tras un cierre.
    }
  }

  static String _normalizar(String correo) => correo.trim().toLowerCase();

  Future<void> confirmar({
    required String id,
    required String correo,
    required String codigo,
  }) async {
    try {
      await DefaultApi(_ref.read(dioProvider)).confirmarVerificacionCorreo(
        verificacionId: id,
        confirmacionVerificacionCorreo: ConfirmacionVerificacionCorreo(
          correo: correo,
          codigo: codigo,
        ),
      );
      final guardada = await _leer(correo);
      if (guardada != null && guardada.id == id) {
        await _escribir(guardada.confirmar());
      }
    } on DioException catch (e) {
      final problema = errorDeDominio(e);
      throw VerificacionCorreoException(
        mensajeDeError(problema),
        problema is ErrorPresentable ? problema.trazaId : null,
      );
    }
  }
}

class VerificacionCorreoException implements Exception {
  const VerificacionCorreoException(this.mensaje, [this.trazaId]);
  final String mensaje;
  final String? trazaId;
}

final verificacionCorreoProvider = Provider<VerificacionCorreo>(
  VerificacionCorreo.new,
);
