import 'dart:io';
import 'dart:math' as math;

import 'package:camera/camera.dart';
import 'package:flutter/services.dart';
import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

import '../dominio/puertos/camara_en_vivo.dart';

/// Lee el rostro de un cuadro del video con ML Kit, en el teléfono: la imagen no
/// sale del dispositivo. Traduce lo que devuelve ML Kit a [LecturaDeRostro], que es
/// lo único que ve el dominio.
///
/// La conversión del cuadro sigue el ejemplo oficial de `google_mlkit_commons`:
/// NV21 de un plano en Android (el plugin `camera` con CameraX lo entrega así cuando
/// se pide `ImageFormatGroup.nv21`) y BGRA en iOS.
class LectorDeRostroMlKit {
  LectorDeRostroMlKit(this._camara, this._controlador);

  final CameraDescription _camara;
  final CameraController _controlador;

  // `accurate`: ML Kit solo garantiza el giro de la cabeza (Euler Y) en ese modo.
  final _detector = FaceDetector(
    options: FaceDetectorOptions(
      enableClassification: true,
      performanceMode: FaceDetectorMode.accurate,
      minFaceSize: 0.15,
    ),
  );

  static const _grados = {
    DeviceOrientation.portraitUp: 0,
    DeviceOrientation.landscapeLeft: 90,
    DeviceOrientation.portraitDown: 180,
    DeviceOrientation.landscapeRight: 270,
  };

  /// `null` si el cuadro no se pudo convertir (formato inesperado) o no hay cara.
  Future<LecturaDeRostro?> leer(CameraImage cuadro) async {
    final rotacion = _rotacion();
    final entrada = rotacion == null ? null : _entrada(cuadro, rotacion);
    if (entrada == null) return null;
    final caras = await _detector.processImage(entrada);
    if (caras.isEmpty) return null;
    final cara = caras.reduce(
      (a, b) => a.boundingBox.width >= b.boundingBox.width ? a : b,
    );
    // Las cajas vienen en el cuadro ya rotado: de pie, alto y ancho se cruzan.
    final deCostado =
        rotacion == InputImageRotation.rotation90deg ||
        rotacion == InputImageRotation.rotation270deg;
    final ancho = (deCostado ? cuadro.height : cuadro.width).toDouble();
    final alto = (deCostado ? cuadro.width : cuadro.height).toDouble();
    final caja = cara.boundingBox;
    final izq = cara.leftEyeOpenProbability;
    final der = cara.rightEyeOpenProbability;
    return LecturaDeRostro(
      rostros: caras.length,
      centroX: caja.center.dx / ancho,
      centroY: caja.center.dy / alto,
      ancho: caja.width / ancho,
      giro: _haciaLaIzquierda(cara.headEulerAngleY ?? 0),
      inclinacion: cara.headEulerAngleX ?? 0,
      ojosAbiertos: izq == null || der == null ? null : math.min(izq, der),
    );
  }

  /// ML Kit: Y positivo es la cara girada hacia el lado derecho **de la imagen**.
  /// En Android el cuadro de la cámara delantera llega sin espejar —como te ve otra
  /// persona—, así que tu izquierda queda a la derecha de la imagen: Y positivo es
  /// girar a tu izquierda. iOS lo entrega espejado (`camera_avfoundation`,
  /// `DefaultCamera.swift`: `isVideoMirrored = true` en la delantera), y el signo se
  /// invierte. Supuesto a confirmar en un teléfono real de cada plataforma.
  double _haciaLaIzquierda(double y) => Platform.isIOS ? -y : y;

  InputImageRotation? _rotacion() {
    final sensor = _camara.sensorOrientation;
    if (Platform.isIOS) return InputImageRotationValue.fromRawValue(sensor);
    final dispositivo = _grados[_controlador.value.deviceOrientation];
    if (dispositivo == null) return null;
    final compensada = _camara.lensDirection == CameraLensDirection.front
        ? (sensor + dispositivo) % 360
        : (sensor - dispositivo + 360) % 360;
    return InputImageRotationValue.fromRawValue(compensada);
  }

  InputImage? _entrada(CameraImage cuadro, InputImageRotation rotacion) {
    final crudo = cuadro.format.raw;
    final formato = crudo is int
        ? InputImageFormatValue.fromRawValue(crudo)
        : null;
    final esperado = Platform.isIOS
        ? InputImageFormat.bgra8888
        : InputImageFormat.nv21;
    if (formato != esperado || cuadro.planes.length != 1) return null;
    final plano = cuadro.planes.first;
    return InputImage.fromBytes(
      bytes: plano.bytes,
      metadata: InputImageMetadata(
        size: Size(cuadro.width.toDouble(), cuadro.height.toDouble()),
        rotation: rotacion,
        format: formato!,
        bytesPerRow: plano.bytesPerRow,
      ),
    );
  }

  Future<void> cerrar() => _detector.close();
}
