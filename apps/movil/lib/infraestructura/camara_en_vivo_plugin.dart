import 'package:camera/camera.dart';
import 'package:flutter/widgets.dart';
import 'package:permission_handler/permission_handler.dart';

import '../dominio/puertos/camara_en_vivo.dart';

/// El adaptador real sobre el plugin `camera`. Un `CamaraEnVivoPlugin` es de un solo
/// uso: se crea al entrar a la pantalla de cámara y se tira al salir — no se
/// reutiliza entre caras, porque cada una puede pedir un sensor distinto.
class CamaraEnVivoPlugin implements CamaraEnVivo {
  CameraController? _controlador;

  @override
  Future<EstadoDePermiso> pedirPermiso() async {
    final estado = await Permission.camera.request();
    if (estado.isGranted) return EstadoDePermiso.concedido;
    if (estado.isPermanentlyDenied) return EstadoDePermiso.denegadoParaSiempre;
    return EstadoDePermiso.denegado;
  }

  @override
  Future<void> iniciar({required bool frontal}) async {
    final camaras = await availableCameras();
    if (camaras.isEmpty) {
      throw StateError('Este dispositivo no tiene cámara disponible.');
    }
    final direccion = frontal
        ? CameraLensDirection.front
        : CameraLensDirection.back;
    final elegida = camaras.firstWhere(
      (c) => c.lensDirection == direccion,
      orElse: () => camaras.first,
    );
    final controlador = CameraController(
      elegida,
      ResolutionPreset.high,
      enableAudio: false,
      imageFormatGroup: ImageFormatGroup.jpeg,
    );
    await controlador.initialize();
    _controlador = controlador;
  }

  @override
  Widget vista() {
    final controlador = _controlador;
    if (controlador == null || !controlador.value.isInitialized) {
      throw StateError('La cámara todavía no se inició.');
    }
    return CameraPreview(controlador);
  }

  @override
  Future<String> tomarFoto() async {
    final controlador = _controlador;
    if (controlador == null) {
      throw StateError('La cámara todavía no se inició.');
    }
    final archivo = await controlador.takePicture();
    return archivo.path;
  }

  @override
  Future<void> liberar() async {
    final controlador = _controlador;
    _controlador = null;
    await controlador?.dispose();
  }
}
