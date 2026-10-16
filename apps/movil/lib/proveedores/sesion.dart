import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/almacen_seguro.dart';
import '../infraestructura/plataforma.dart';

const _claveAcceso = 'sesion.acceso';
const _claveRefresco = 'sesion.refresco';
const _claveInicio = 'sesion.inicio-con-credencial';
const _claveHuella = 'dispositivo.huella';

/// Se sobreescribe en pruebas con un almacén en memoria.
final almacenSeguroProvider = Provider<AlmacenSeguro>(
  (_) => almacenSeguroDeLaPlataforma(),
);

final relojDeSesionProvider = Provider<DateTime Function()>(
  (_) => DateTime.now,
);

enum EstadoDeSesion { desconocida, anonima, activa }

enum MotivoDeSalida { caducada, cerrada }

/// La sesión: tokens en el almacén seguro, nunca en `SharedPreferences`.
class Sesion extends ChangeNotifier {
  Sesion(this._almacen, [DateTime Function()? ahora])
    : _ahora = ahora ?? DateTime.now;
  final AlmacenSeguro _almacen;
  final DateTime Function() _ahora;

  EstadoDeSesion _estado = EstadoDeSesion.desconocida;
  EstadoDeSesion get estado => _estado;
  bool get activa => _estado == EstadoDeSesion.activa;

  MotivoDeSalida? _motivo;
  int _generacion = 0;
  Future<void>? _cierreEnCurso;
  int get generacion => _generacion;

  Future<String?> tokenDeAcceso() => _almacen.leer(_claveAcceso);
  Future<String?> tokenDeRefresco() => _almacen.leer(_claveRefresco);

  /// El id del usuario de la sesión: el `sub` del token de acceso. Es solo para armar pantallas
  /// (qué ids pedir); NO es una barrera: cada servicio vuelve a validar el token.
  Future<String?> usuarioId() async => usuarioIdDelToken(await tokenDeAcceso());

  Future<DateTime?> inicioConCredencial() async {
    final crudo = await _almacen.leer(_claveInicio);
    final ms = crudo == null ? null : int.tryParse(crudo);
    return ms == null
        ? null
        : DateTime.fromMillisecondsSinceEpoch(ms, isUtc: true);
  }

  Future<void> hidratar({Duration tope = topeDeSesion}) async {
    EstadoDeSesion nuevo;
    try {
      final acceso = await tokenDeAcceso();
      // Aunque el refresh sea opcional, leerlo detecta fallos del almacén seguro.
      await tokenDeRefresco();
      final inicio = await inicioConCredencial();
      if (acceso == null || acceso.isEmpty) {
        nuevo = EstadoDeSesion.anonima;
      } else if (sesionVencida(inicio, _ahora().toUtc(), tope: tope)) {
        await _borrarTokens();
        _motivo = MotivoDeSalida.caducada;
        nuevo = EstadoDeSesion.anonima;
      } else {
        nuevo = EstadoDeSesion.activa;
      }
    } on Object {
      nuevo = EstadoDeSesion.anonima;
    }
    _cambiar(nuevo);
  }

  Future<void> abrir({required String acceso, String? refresco}) async {
    await _almacen.guardar(_claveAcceso, acceso);
    if (refresco == null || refresco.isEmpty) {
      await _almacen.borrar(_claveRefresco);
    } else {
      await _almacen.guardar(_claveRefresco, refresco);
    }
    await _almacen.guardar(
      _claveInicio,
      '${_ahora().toUtc().millisecondsSinceEpoch}',
    );
    _motivo = null;
    _generacion++;
    _cambiar(EstadoDeSesion.activa, forzarAviso: true);
  }

  Future<void> renovar({
    required String acceso,
    required String refresco,
  }) async {
    await _almacen.guardar(_claveAcceso, acceso);
    await _almacen.guardar(_claveRefresco, refresco);
  }

  Future<void> cerrar({MotivoDeSalida? motivo}) {
    final cierreActual = _cierreEnCurso;
    if (cierreActual != null) return cierreActual;
    if (_estado == EstadoDeSesion.anonima) return Future<void>.value();
    return _cierreEnCurso = _cerrar(motivo).whenComplete(() {
      _cierreEnCurso = null;
    });
  }

  Future<void> _cerrar(MotivoDeSalida? motivo) async {
    await _borrarTokens();
    _motivo = motivo;
    _generacion++;
    _cambiar(EstadoDeSesion.anonima, forzarAviso: true);
  }

  MotivoDeSalida? tomarMotivoDeSalida() {
    final motivo = _motivo;
    _motivo = null;
    return motivo;
  }

  /// La huella de **la instalación**, no de la persona: sobrevive al cierre de
  /// sesión a propósito, porque es lo que permite que el teléfono siga siendo «de
  /// confianza» para quien vuelve a entrar.
  Future<String?> leerHuella() => _almacen.leer(_claveHuella);
  Future<void> guardarHuella(String valor) =>
      _almacen.guardar(_claveHuella, valor);

  Future<void> _borrarTokens() async {
    await _almacen.borrar(_claveAcceso);
    await _almacen.borrar(_claveRefresco);
    await _almacen.borrar(_claveInicio);
  }

  void _cambiar(EstadoDeSesion nuevo, {bool forzarAviso = false}) {
    if (_estado == nuevo && !forzarAviso) return;
    _estado = nuevo;
    notifyListeners();
  }
}

const topeDeSesion = Duration(hours: 8);

bool sesionVencida(
  DateTime? inicio,
  DateTime ahora, {
  Duration tope = topeDeSesion,
}) {
  if (inicio == null) return true;
  final transcurrido = ahora.difference(inicio);
  return transcurrido.isNegative || transcurrido >= tope;
}

final sesionProvider = Provider<Sesion>((ref) {
  final sesion = Sesion(
    ref.watch(almacenSeguroProvider),
    ref.watch(relojDeSesionProvider),
  );
  ref.onDispose(sesion.dispose);
  return sesion;
});

final generacionDeSesionProvider = Provider<int>((ref) {
  final sesion = ref.watch(sesionProvider);
  void alCambiar() => ref.invalidateSelf();
  sesion.addListener(alCambiar);
  ref.onDispose(() => sesion.removeListener(alCambiar));
  return sesion.generacion;
});

/// El `sub` de un JWT, o `null` si no hay token o no tiene la forma esperada. No verifica la firma.
String? usuarioIdDelToken(String? token) {
  if (token == null) return null;
  final partes = token.split('.');
  if (partes.length != 3) return null;
  try {
    final carga = jsonDecode(
      utf8.decode(base64Url.decode(base64Url.normalize(partes[1]))),
    );
    final sub = carga is Map ? carga['sub'] : null;
    return sub is String && sub.isNotEmpty ? sub : null;
  } catch (_) {
    return null;
  }
}
