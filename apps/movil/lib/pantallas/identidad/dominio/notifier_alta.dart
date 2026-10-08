import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../proveedores/sesion.dart';
import 'borrador_de_alta.dart';
import 'estado_alta.dart';
import 'envio_del_alta.dart';

/// Orquesta los pasos y el envío del registro; la UI vive en sus widgets de paso.
class AltaNotifier extends Notifier<EstadoAlta> with WidgetsBindingObserver {
  Timer? _timerDeGuardado;
  Future<void> _colaDeGuardado = Future<void>.value();
  bool _intentoRestaurar = false;
  bool _hayCambiosGuardables = false;

  late final BorradorDeAlta _borrador = BorradorDeAlta(
    ref.read(almacenSeguroProvider),
  );

  @override
  EstadoAlta build() {
    WidgetsBinding.instance.addObserver(this);
    ref.onDispose(() {
      _timerDeGuardado?.cancel();
      WidgetsBinding.instance.removeObserver(this);
    });
    return const EstadoAlta();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused) {
      _guardarAhora();
    }
  }

  /// Restaura una sola vez; OTP, contraseña, contrato y fotos se repiten.
  Future<void> restaurarBorrador() async {
    if (_intentoRestaurar) return;
    _intentoRestaurar = true;
    state = state.copiarCon(restaurando: true);
    try {
      final guardado = await _borrador.restaurar();
      state = (guardado ?? state).copiarCon(restaurando: false);
    } on Object {
      state = state.copiarCon(restaurando: false, errorBorrador: 'recuperar');
    }
  }

  void actualizarDatos(DatosPersonales datos) {
    state = state.copiarCon(datos: datos);
    _programarGuardado();
  }

  void elegirContrasena(String clave) {
    state = state.copiarCon(contrasena: clave);
    _programarGuardado();
  }

  void capturarAnverso(String? ruta) {
    state = state.copiarCon(rutaAnverso: ruta);
    _programarGuardado();
  }

  void capturarReverso(String? ruta) {
    state = state.copiarCon(rutaReverso: ruta);
    _programarGuardado();
  }

  void capturarSelfie(String? ruta) {
    state = state.copiarCon(rutaSelfie: ruta);
    _programarGuardado();
  }

  void actualizarPerfilTransaccional({
    required String origen,
    required String detalleDelOrigen,
    required String actividad,
    required String monto,
  }) {
    state = state.copiarCon(
      origenDeFondos: origen,
      detalleDelOrigen: detalleDelOrigen,
      actividadEconomica: actividad,
      montoMensualEstimado: monto,
    );
    _programarGuardado();
  }

  void reiniciar() {
    _timerDeGuardado?.cancel();
    _hayCambiosGuardables = false;
    state = const EstadoAlta();
    unawaited(_encolar(_borrador.borrar));
  }

  void siguiente() {
    // No existe todavía validación OTP pre-registro en el contrato de Identidad.
    // Bloquear también el notifier impide saltar este gate sin pasar por la UI.
    if (state.paso == PasoAlta.celular && !state.codigoConfirmado) return;
    const orden = PasoAlta.values;
    final i = orden.indexOf(state.paso);
    if (i < orden.length - 1) {
      state = state.copiarCon(paso: orden[i + 1]);
      _programarGuardado();
    }
  }

  void atras() {
    const orden = PasoAlta.values;
    final i = orden.indexOf(state.paso);
    if (i > 0) {
      state = state.copiarCon(paso: orden[i - 1]);
      _programarGuardado();
    }
  }

  void _programarGuardado() {
    if (state.restaurando) return;
    _hayCambiosGuardables = true;
    _timerDeGuardado?.cancel();
    _timerDeGuardado = Timer(const Duration(milliseconds: 400), () {
      final fotografia = state;
      unawaited(_encolar(() => _borrador.guardar(fotografia)));
    });
  }

  void _guardarAhora() {
    _timerDeGuardado?.cancel();
    if (state.restaurando || !_hayCambiosGuardables) return;
    final fotografia = state;
    unawaited(_encolar(() => _borrador.guardar(fotografia)));
  }

  Future<void> _encolar(Future<void> Function() operacion) {
    _colaDeGuardado = _colaDeGuardado.then((_) => operacion()).catchError((_) {
      if (ref.mounted) state = state.copiarCon(errorBorrador: 'guardar');
    });
    return _colaDeGuardado;
  }

  Future<String?> enviarAlServidor() async {
    if (!state.codigoConfirmado) {
      state = state.copiarCon(
        error: 'No pudimos verificar tu contacto. No enviamos el alta.',
      );
      return null;
    }
    if (state.enviando) return null;
    state = state.copiarCon(enviando: true);
    final respuesta = await enviarElAlta(ref, state);
    if (respuesta.cuentaBilleteraId != null) {
      _timerDeGuardado?.cancel();
      _hayCambiosGuardables = false;
      try {
        await _colaDeGuardado;
        await _borrador.borrar();
      } on Object {
        state = state.copiarCon(errorBorrador: 'borrar');
      }
    }
    state = state.copiarCon(
      error: respuesta.error,
      fotosPendientes: respuesta.fotosPendientes,
    );
    return respuesta.cuentaBilleteraId;
  }
}

final altaProvider = NotifierProvider<AltaNotifier, EstadoAlta>(
  AltaNotifier.new,
);
