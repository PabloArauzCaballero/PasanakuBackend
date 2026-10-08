import 'dart:io';

import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/widgets.dart';
import 'package:permission_handler/permission_handler.dart';

import '../dominio/puertos/camara_en_vivo.dart';
import 'lector_de_rostro_mlkit.dart';

/// El adaptador real sobre el plugin `camera`. Un `CamaraEnVivoPlugin` es de un solo
/// uso: se crea al entrar a la pantalla de cámara y se tira al salir — no se
/// reutiliza entre caras, porque cada una puede pedir un sensor distinto.
class CamaraEnVivoPlugin implements CamaraEnVivo {
  CameraController? _controlador;
  CameraDescription? _descripcion;
  LectorDeRostroMlKit? _lector;

  @override
  Future<EstadoDePermiso> pedirPermiso() async {
    final estado = await Permission.camera.request();
    if (estado.isGranted) return EstadoDePermiso.concedido;
    if (estado.isPermanentlyDenied) return EstadoDePermiso.denegadoParaSiempre;
    return EstadoDePermiso.denegado;
  }

  @override
  Future<void> iniciar({
    required bool frontal,
    bool conDeteccion = false,
  }) async {
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
    // `veryHigh` (~1080p) y no `high` (~720p): con 720p la foto sale de 1280×720 y
    // el chequeo de calidad, que pide 1400 px de lado largo como Atlas, la
    // rechazaba siempre por «muy pequeña».
    final controlador = CameraController(
      elegida,
      ResolutionPreset.veryHigh,
      enableAudio: false,
      // El formato es el del VIDEO (la foto sale en JPEG igual): el detector de
      // rostro lee NV21 en Android y BGRA en iOS.
      imageFormatGroup: !conDeteccion
          ? ImageFormatGroup.jpeg
          : Platform.isIOS
          ? ImageFormatGroup.bgra8888
          : ImageFormatGroup.nv21,
    );
    await controlador.initialize();
    _controlador = controlador;
    _descripcion = elegida;
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
    if (controlador.value.isStreamingImages) {
      await controlador.stopImageStream();
    }
    final archivo = await controlador.takePicture();
    return archivo.path;
  }

  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer) async {
    final controlador = _controlador;
    final descripcion = _descripcion;
    if (kIsWeb || controlador == null || descripcion == null) return false;
    final lector = _lector ??= LectorDeRostroMlKit(descripcion, controlador);
    var ocupado = false;
    try {
      await controlador.startImageStream((cuadro) async {
        // Un cuadro a la vez: los que llegan mientras ML Kit piensa se descartan,
        // en vez de encolarse y llegar tarde.
        if (ocupado) return;
        ocupado = true;
        try {
          alLeer(await lector.leer(cuadro));
        } on Object {
          // Un cuadro que no se pudo leer no frena la prueba: viene otro enseguida,
          // y si no anda nunca la pantalla ofrece la foto a mano.
        } finally {
          ocupado = false;
        }
      });
      return true;
    } on Object {
      return false;
    }
  }

  @override
  Future<void> dejarDeEscuchar() async {
    final controlador = _controlador;
    if (controlador != null && controlador.value.isStreamingImages) {
      await controlador.stopImageStream();
    }
  }

  @override
  Future<void> liberar() async {
    await dejarDeEscuchar();
    final controlador = _controlador;
    _controlador = null;
    await _lector?.cerrar();
    _lector = null;
    await controlador?.dispose();
  }
}
