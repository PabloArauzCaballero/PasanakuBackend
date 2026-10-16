import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../infraestructura/plataforma.dart';
import 'sesion.dart';

const _claveHuella = 'dispositivo.huella';

/// El identificador de **esta instalación**, que el contrato de CU-04 pide como
/// `huellaDispositivo` para poder ofrecer «confiar en este dispositivo por 30 días».
///
/// Se genera una vez y vive en el almacén seguro junto a los tokens. **No es un
/// identificador de la persona ni del teléfono**: no sale de un IMEI, de una MAC ni
/// de nada que siga a alguien entre apps. Es un número al azar que solo significa
/// algo para AportaYa, y que desaparece cuando se desinstala la app.
class HuellaDeDispositivo {
  HuellaDeDispositivo(this._almacen);
  final Sesion _almacen;

  Future<String> obtener() async {
    final guardada = await _almacen.leerHuella();
    if (guardada != null && guardada.isNotEmpty) return guardada;
    final nueva = _generar();
    await _almacen.guardarHuella(nueva);
    return nueva;
  }

  static String _generar() {
    final azar = Random.secure();
    final bytes = List<int>.generate(16, (_) => azar.nextInt(256));
    return bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }
}

final huellaDeDispositivoProvider = Provider<HuellaDeDispositivo>(
  (ref) => HuellaDeDispositivo(ref.watch(sesionProvider)),
);

/// `IOS` o `ANDROID`, como lo nombra el contrato. La pregunta por la plataforma vive
/// en `infraestructura/` (ADR-036); acá solo se reexporta el resultado.
final plataformaProvider = Provider<String>((_) => nombreDePlataforma());

const claveHuellaDeDispositivo = _claveHuella;
